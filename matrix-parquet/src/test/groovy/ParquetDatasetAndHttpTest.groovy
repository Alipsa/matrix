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
}
