import static org.junit.jupiter.api.Assertions.*

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.*

import java.util.concurrent.atomic.AtomicLong

class ParquetDatasetAndHttpTest {
  @TempDir File directory

  @Test
  void orderedShardsParallelLimitsAndSchemaChecks() {
    File first = new File(directory, 'a.parquet')
    File second = new File(directory, 'b.parquet')
    [first, second].eachWithIndex { file, index ->
      MatrixParquetWriter.write(Matrix.builder().columns(id: [index * 2, index * 2 + 1]).build(), file)
    }
    [1, 2].each { workers ->
      def data = MatrixParquetReader.builder().parallelism(workers).sourceFileColumn('source')
          .limit(3).read([second, first])
      assertEquals([2, 3, 0], data.column('id'))
      assertEquals([second.canonicalPath, second.canonicalPath, first.canonicalPath], data.column('source'))
    }
    def early = MatrixParquetReader.builder().parallelism(2).maxBatchRows(1).batches([first, second])
    assertEquals([0], early.next().column('id'))
    early.close()
    assertFalse(early.hasNext())
    early.close()
    assertEquals([0, 1, 2, 3], MatrixParquetReader.read(directory).column('id'))
    assertEquals([0, 1, 2, 3], MatrixParquetReader.readGlob("${directory.absolutePath}/*.parquet").column('id'))
    assertEquals(['id'], MatrixParquetReader.builder().limit(0).read([first, second]).columnNames())
    assertEquals(0, MatrixParquetReader.builder().filter(Predicates.lt('id', 0)).read([first, second]).rowCount())
    def perFile = MatrixParquetReader.builder().parallelism(2)
        .rowGroupsByFile([(first.canonicalPath): [], (second.canonicalPath): [0]]).read([first, second])
    assertEquals([2, 3], perFile.column('id'))
    assertThrows(IllegalArgumentException) { MatrixParquetReader.read([first, first]) }
    MatrixParquetWriter.write(Matrix.builder().columns(id: ['incompatible']).build(), second)
    assertThrows(IllegalArgumentException) { MatrixParquetReader.read([first, second]) }
  }

  @Test
  void parallelRowGroupsKeepOrderAndMatchingLimit() {
    File file = new File('src/test/resources/optimized/cdc-1-index-1.parquet')
    [1, 2].each { workers ->
      def result = MatrixParquetReader.builder().parallelism(workers).columns(['id'])
          .filter(Predicates.ge('id', 2040L)).maxBatchRows(7).limit(20).read([file])
      assertEquals((2040L..<2060L).toList(), result.column('id'))
    }
  }

  @Test
  void bulkRangesUseOneRequestAndReuseRedirectAndCache() {
    byte[] bytes = new byte[2 * 1024 * 1024]
    Arrays.fill(bytes, (byte) 37)
    AtomicLong redirects = new AtomicLong()
    AtomicLong ranges = new AtomicLong()
    def server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
    server.createContext('/resolve') { exchange ->
      redirects.incrementAndGet()
      exchange.responseHeaders.add('Location', '/object')
      exchange.sendResponseHeaders(302, -1)
      exchange.close()
    }
    server.createContext('/object') { exchange ->
      ranges.incrementAndGet()
      def match = exchange.requestHeaders.getFirst('Range') =~ /bytes=(\d+)-(\d+)/
      assertTrue(match.matches())
      int start = Integer.parseInt(match.group(1))
      int end = Integer.parseInt(match.group(2))
      exchange.responseHeaders.add('ETag', '"stable"')
      exchange.responseHeaders.add('Content-Range', "bytes $start-$end/${bytes.length}")
      exchange.sendResponseHeaders(206, end - start + 1)
      exchange.responseBody.write(bytes, start, end - start + 1)
      exchange.close()
    }
    server.start()
    try {
      URL url = new URI("http://127.0.0.1:${server.address.port}/resolve").toURL()
      def input = new HttpRangeInputFile(url, new ParquetReadOptions())
      input.newStream().withCloseable { stream ->
        def target = java.nio.ByteBuffer.allocate(1024 * 1024)
        stream.readFully(target)
        assertEquals(37, target.get(0))
        assertEquals(2L, ranges.get()) // Probe plus one 1 MiB bulk request.
        stream.seek(0)
        stream.readFully(new byte[1024 * 1024])
        assertEquals(2L, ranges.get()) // Default cache retains the bulk range.
        def backing = java.nio.ByteBuffer.allocate(1024 * 1024 + 32)
        backing.position(13)
        backing.limit(1024 * 1024 + 13)
        def slice = backing.slice()
        slice.position(7)
        stream.seek(0)
        stream.readFully(slice)
        assertEquals(slice.limit(), slice.position())
        assertEquals(0, backing.get(19))
        assertEquals(37, backing.get(20))
        assertEquals(0, backing.array()[backing.limit()])
        assertEquals(2L, ranges.get())
        def direct = java.nio.ByteBuffer.allocateDirect(1024)
        stream.seek(0)
        stream.readFully(direct)
        assertEquals(37, direct.get(0))
        stream.seek(bytes.length)
        assertEquals(-1, stream.read(java.nio.ByteBuffer.allocate(1)))
        assertEquals(0, stream.read(java.nio.ByteBuffer.allocate(0)))
      }
      assertEquals(1L, redirects.get())
      def tinyCache = new HttpRangeInputFile(url, new ParquetReadOptions(rangeCacheBytes: 1024))
      tinyCache.newStream().withCloseable { stream ->
        long before = ranges.get()
        stream.readFully(new byte[1024 * 1024])
        assertEquals(before + 1, ranges.get()) // Request size is independent of cache size.
      }
      println "HTTP bulk 1 MiB: one data request; redirects=${redirects.get()}, ranges=${ranges.get()}"
      assertEquals(2L, redirects.get()) // One resolution per input, never per bulk request.
    } finally { server.stop(0) }
  }

  @Test
  void rangeReadsFallbackAndObjectChange() {
    File file = new File(directory, 'large.parquet')
    def data = Matrix.builder().columns(id: (0..<20000).toList(), text: (0..<20000).collect { UUID.randomUUID().toString() }).build()
    MatrixParquetWriter.builder(data).compressionCodec('UNCOMPRESSED').pageSize(4096).dictionaryEncoding(false).write(file)
    byte[] bytes = file.bytes
    AtomicLong transferred = new AtomicLong()
    boolean changed = false
    def server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
    server.createContext('/file') { exchange ->
      String range = exchange.requestHeaders.getFirst('Range')
      exchange.responseHeaders.add('ETag', changed ? '"changed"' : '"original"')
      if (range != null) {
        def match = range =~ /bytes=(\d+)-(\d+)/
        assertTrue(match.matches())
        int start = Integer.parseInt(match.group(1))
        int end = Integer.parseInt(match.group(2))
        exchange.responseHeaders.add('Content-Range', "bytes $start-$end/${bytes.length}")
        exchange.sendResponseHeaders(206, end - start + 1)
        exchange.responseBody.write(bytes, start, end - start + 1)
        transferred.addAndGet(end - start + 1)
      } else {
        exchange.sendResponseHeaders(200, bytes.length)
        exchange.responseBody.write(bytes)
        transferred.addAndGet(bytes.length)
      }
      exchange.close()
    }
    server.createContext('/download') { exchange ->
      exchange.sendResponseHeaders(200, bytes.length)
      exchange.responseBody.write(bytes)
      exchange.close()
    }
    server.start()
    try {
      URL url = new URI("http://127.0.0.1:${server.address.port}/file").toURL()
      def selected = MatrixParquetReader.builder().remoteReadMode('RANGE').columns(['id'])
          .filter(Predicates.ge('id', 19990)).read(url)
      assertEquals((19990..<20000).toList(), selected.column('id'))
      println "HTTP bytes full=${bytes.length} transferred=${transferred.get()}"
      assertTrue(transferred.get() < bytes.length, "Transferred ${transferred.get()} of ${bytes.length}")
      URL download = new URI("http://127.0.0.1:${server.address.port}/download").toURL()
      assertEquals(2, MatrixParquetReader.builder().remoteReadMode('AUTO').limit(2).read(download).rowCount())
      assertThrows(IOException) { MatrixParquetReader.builder().remoteReadMode('RANGE').read(download) }
      def options = new ParquetReadOptions(remoteReadMode: 'RANGE')
      def input = new HttpRangeInputFile(url, options)
      changed = true
      assertThrows(IOException) { input.newStream().withCloseable { it.read() } }
    } finally { server.stop(0) }
  }
  @Test
  void weakEtagsUseLastModifiedAndStillDetectChanges() {
    byte[] bytes = MatrixParquetWriter.writeBytes(Matrix.builder().columns(id: [1, 2]).build())
    AtomicLong ifMatch = new AtomicLong()
    AtomicLong unmodified = new AtomicLong()
    def version = new java.util.concurrent.atomic.AtomicReference<String>('W/"v1"')
    def server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
    server.createContext('/weak') { exchange ->
      if (exchange.requestHeaders.getFirst('If-Match') != null) {
        ifMatch.incrementAndGet(); exchange.sendResponseHeaders(412, -1); exchange.close(); return
      }
      if (exchange.requestHeaders.getFirst('If-Unmodified-Since') != null) { unmodified.incrementAndGet() }
      def match = exchange.requestHeaders.getFirst('Range') =~ /bytes=(\d+)-(\d+)/
      assertTrue(match.matches())
      int start = Integer.parseInt(match.group(1)), end = Integer.parseInt(match.group(2))
      exchange.responseHeaders.add('ETag', version.get())
      exchange.responseHeaders.add('Last-Modified', 'Wed, 01 Jan 2025 00:00:00 GMT')
      exchange.responseHeaders.add('Content-Range', "bytes $start-$end/${bytes.length}")
      exchange.sendResponseHeaders(206, end - start + 1)
      exchange.responseBody.write(bytes, start, end - start + 1)
      exchange.close()
    }
    server.start()
    try {
      URL url = new URI("http://127.0.0.1:${server.address.port}/weak").toURL()
      ['RANGE', 'AUTO'].each { mode -> assertEquals([1, 2], MatrixParquetReader.builder().remoteReadMode(mode).read(url).column('id')) }
      assertEquals(0L, ifMatch.get())
      assertTrue(unmodified.get() > 0)
      def input = new HttpRangeInputFile(url, new ParquetReadOptions(remoteReadMode: 'RANGE'))
      version.set('W/"v2"')
      def failure = assertThrows(IOException) { input.newStream().withCloseable { it.read() } }
      assertTrue(failure.message.contains('Remote object/range changed during read'))
    } finally { server.stop(0) }
  }

}
