package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.io.LocalOutputFile
import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix

import java.time.LocalDateTime
import java.time.ZoneId

class ParquetReviewRegressionTest {
  @TempDir File directory

  @Test
  void arrowEmptyDictionaryAndJsonAreReadable() {
    File root = new File('src/test/resources/optimized')
    def allNull = MatrixParquetReader.read(new File(root, 'arrow-all-null-string.parquet'))
    assertEquals(String, allNull.type('s'))
    assertEquals([null, null, null], allNull.column('s'))
    [null, true, false].each { override ->
      def json = MatrixParquetReader.builder().binaryAsString(override).read(new File(root, 'arrow-json.parquet'))
      assertEquals(String, json.type('json'))
      assertEquals(['{"label":"α"}', null, '{}'], json.column('json'))
    }
  }

  @Test
  void enumIsTextAndBsonRemainsBinary() {
    def schema = MessageTypeParser.parseMessageType('message external { optional binary label (ENUM); optional binary document (BSON); }')
    File file = new File(directory, 'annotations.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration())
        .withType(schema).build().withCloseable { writer ->
          writer.write(new SimpleGroupFactory(schema).newGroup().append('label', 'CATEGORY').append('document', 'binary'))
        }
    def result = MatrixParquetReader.builder().binaryAsString(true).read(file)
    assertEquals(String, result.type('label'))
    assertEquals('CATEGORY', result[0, 'label'])
    assertEquals(byte[], result.type('document'))
    assertArrayEquals('binary'.getBytes('UTF-8'), (byte[]) result[0, 'document'])
  }

  @Test
  void failedDecimalWritesPreserveExistingFileAndCleanTemporaryFiles() {
    File file = new File(directory, 'safe.parquet')
    MatrixParquetWriter.write(Matrix.builder().columns(d: [1.25G]).build(), file)
    boolean posix = java.nio.file.Files.getFileStore(file.toPath()).supportsFileAttributeView('posix')
    def permissions = java.nio.file.attribute.PosixFilePermissions.fromString('rw-r-----')
    if (posix) { java.nio.file.Files.setPosixFilePermissions(file.toPath(), permissions) }
    byte[] original = file.bytes
    [2.255G, 123456.00G].each { value ->
      assertThrows(IllegalArgumentException) {
        MatrixParquetWriter.builder(Matrix.builder().columns(d: [1.00G, value]).build())
            .decimalMeta([d: [5, 2] as int[]]).write(file)
      }
      assertArrayEquals(original, file.bytes)
      assertEquals(['safe.parquet'], directory.list().toList())
    }
    MatrixParquetWriter.write(Matrix.builder().columns(d: [2.50G]).build(), file)
    assertEquals(2.50G, MatrixParquetReader.read(file)[0, 'd'])
    if (posix) { assertEquals(permissions, java.nio.file.Files.getPosixFilePermissions(file.toPath())) }
    assertEquals(['safe.parquet'], directory.list().toList())
  }

  @Test
  void newFilePermissionsMatchOrdinaryFiles() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        java.nio.file.Files.getFileStore(directory.toPath()).supportsFileAttributeView('posix'))
    def ordinary = java.nio.file.Files.createFile(new File(directory, 'ordinary').toPath())
    File output = new File(directory, 'new.parquet')
    MatrixParquetWriter.write(Matrix.builder().columns(id: [1]).build(), output)
    assertEquals(java.nio.file.Files.getPosixFilePermissions(ordinary),
        java.nio.file.Files.getPosixFilePermissions(output.toPath()))
  }

  @Test
  void writingThroughSymbolicLinkPreservesLinkAndUpdatesRealFile() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        java.nio.file.Files.getFileStore(directory.toPath()).supportsFileAttributeView('posix'))
    File output = new File(directory, 'real.parquet')
    MatrixParquetWriter.write(Matrix.builder().columns(d: [1.25G]).build(), output)
    def link = new File(directory, 'link.parquet').toPath()
    java.nio.file.Files.createSymbolicLink(link, output.toPath().fileName)
    byte[] original = output.bytes
    assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.builder(Matrix.builder().columns(d: [2.255G]).build())
          .decimalMeta([d: [5, 2] as int[]]).write(link.toFile())
    }
    assertTrue(java.nio.file.Files.isSymbolicLink(link))
    assertArrayEquals(original, output.bytes)
    MatrixParquetWriter.write(Matrix.builder().columns(d: [2.50G]).build(), link.toFile())
    assertTrue(java.nio.file.Files.isSymbolicLink(link))
    assertEquals(output.toPath().fileName, java.nio.file.Files.readSymbolicLink(link))
    assertEquals(2.50G, MatrixParquetReader.read(output)[0, 'd'])
    assertEquals(['link.parquet', 'real.parquet'], directory.list().toList().sort())
  }

  @Test
  void nullMapOptionsPreserveDefaultsAndRejectNullBloomEntries() {
    def read = ParquetReadOptions.fromMap([useColumnIndexFilter: null, binaryAsString: null])
    assertTrue(read.useColumnIndexFilter)
    assertNull(read.binaryAsString)
    def write = ParquetWriteOptions.fromMap([dictionaryEncoding: null, sizeStatisticsEnabled: null,
        bloomFilterEnabled: null, bloomFilterNDV: null, writerVersion: null, pageSize: null])
    assertTrue(write.dictionaryEncoding)
    assertTrue(write.sizeStatisticsEnabled)
    assertEquals([:], write.bloomFilterEnabled)
    assertEquals('V1', write.writerVersion)
    assertEquals(1048576, write.pageSize)
    assertThrows(IllegalArgumentException) { ParquetWriteOptions.fromMap([bloomFilterEnabled: [id: null]]) }
  }

  @Test
  void localSourcesAndCompatibleIndexesAgreeAcrossEntryPoints() {
    File file = new File(directory, 'indexed.parquet')
    def data = Matrix.builder().columns(id: [1, 2], when: [LocalDateTime.of(2024, 1, 1, 0, 0)] * 2).build()
    data.createIndex('id')
    MatrixParquetWriter.builder(data).zoneId('UTC').write(file)
    ZoneId zone = ZoneId.of('Europe/Stockholm')
    [MatrixParquetReader.read(file, zone), MatrixParquetReader.read(directory, zone),
     MatrixParquetReader.read(directory, 'named', zone), MatrixParquetReader.read(directory.toPath(), zone),
     MatrixParquetReader.read(directory, 'named')].each { result ->
      assertTrue(result.hasIndex())
      assertEquals(['id'], result.indexedColumns())
    }
    assertEquals(LocalDateTime.of(2024, 1, 1, 1, 0), MatrixParquetReader.read(directory, zone)[0, 'when'])
    [file, directory].each { source ->
      def result = MatrixParquetReader.builder().sourceFileColumn('_source').read(source)
      assertEquals([file.canonicalPath] * 2, result.column('_source'))
      assertTrue(result.hasIndex())
      MatrixParquetReader.builder().sourceFileColumn('_source').batches(source).withCloseable { batches ->
        assertEquals([file.canonicalPath] * 2, batches.next().column('_source'))
      }
    }
    MatrixParquetReader.builder().limit(1).batches([file]).withCloseable { batches ->
      def limited = batches.next()
      assertEquals(1, limited.rowCount())
      assertTrue(limited.hasIndex())
    }
    assertThrows(IllegalArgumentException) { MatrixParquetReader.builder().sourceFileColumn('id').read(file) }
  }

  @Test
  void shardBatchesAndCollectedReadsUseTheCommonIndexPolicy() {
    List<File> files = []
    ['id', 'text'].eachWithIndex { indexColumn, int number ->
      File file = new File(directory, "shard-${number}.parquet")
      def matrix = Matrix.builder().columns(id: [1, 2], text: ['a', 'b']).build()
      matrix.createIndex(indexColumn)
      MatrixParquetWriter.write(matrix, file)
      files.add(file)
    }
    assertFalse(MatrixParquetReader.read(files).hasIndex())
    MatrixParquetReader.builder().batches(files).withCloseable { batches ->
      while (batches.hasNext()) { assertFalse(batches.next().hasIndex()) }
    }
    assertFalse(MatrixParquetReader.builder().columns(['text']).read(files[0]).hasIndex())
  }

  @Test
  void workerThreadsAreDaemonAndNestedErrorsIncludeTheirPath() {
    File file = new File(directory, 'workers.parquet')
    MatrixParquetWriter.write(Matrix.builder().columns(id: [1, 2]).build(), file)
    MatrixParquetReader.builder().batches([file]).withCloseable { batches ->
      assertTrue(batches.hasNext())
      def workers = Thread.allStackTraces.keySet().findAll { it.name == 'matrix-parquet-dataset' }
      assertFalse(workers.empty)
      assertTrue(workers.every { it.daemon })
    }
    def invalid = Matrix.builder().columns(items: [[[value: [1, 'mixed']]]]).build()
    def failure = assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(invalid) }
    assertTrue(failure.message.contains('items.element.value.element'), failure.message)
  }

  @Test
  void projectedLegacyBinaryFailsOnlyWhenRequired() {
    def schema = MessageTypeParser.parseMessageType('message legacy { optional int32 id; optional binary payload; }')
    File file = new File(directory, 'legacy.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration())
        .withType(schema).withExtraMetaData(['matrix.columnTypes': 'java.lang.Integer,[B']).build().withCloseable { writer ->
          writer.write(new SimpleGroupFactory(schema).newGroup().append('id', 1).append('payload', '[B@lost'))
        }
    assertEquals([1], MatrixParquetReader.builder().columns(['id']).read(file).column('id'))
    assertThrows(IllegalArgumentException) { MatrixParquetReader.read(file) }
    assertThrows(IllegalArgumentException) {
      MatrixParquetReader.builder().columns(['id']).filter(Predicates.isNull('payload')).read(file)
    }
  }
}
