package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.io.LocalInputFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix


class ParquetPruningTest {
  @TempDir File directory

  @Test
  void fixturesHavePhysicalIndexesAndMultiplePages() {
    [0, 1].each { cdc ->
      [0, 1].each { indexed ->
        File file = new File('src/test/resources/optimized', "cdc-${cdc}-index-${indexed}.parquet")
        def options = org.apache.parquet.ParquetReadOptions.builder(new PlainParquetConfiguration()).build()
        ParquetFileReader.open(new LocalInputFile(file.toPath()), options).withCloseable { reader ->
          assertEquals(2, reader.rowGroups.size())
          def column = reader.rowGroups[0].columns.find { it.path.toDotString() == 'id' }
          assertEquals(indexed == 1, column.columnIndexReference != null)
          assertEquals(indexed == 1, column.offsetIndexReference != null)
          if (indexed == 1) { checkIndexes(reader, column, file.length(), reader.rowGroups[0].rowCount) }
        }
      }
    }
  }

  private static void checkIndexes(ParquetFileReader reader, column, long length, long rows) {
    def offsets = reader.readOffsetIndex(column)
    def bounds = reader.readColumnIndex(column)
    assertTrue(offsets.pageCount > 1)
    assertEquals(offsets.pageCount, bounds.nullPages.size())
    assertEquals(0L, offsets.getFirstRowIndex(0))
    (0..<offsets.pageCount).each { int page ->
      assertTrue(offsets.getOffset(page) >= 4)
      assertTrue(offsets.getOffset(page) + offsets.getCompressedPageSize(page) <= length)
      assertTrue(offsets.getFirstRowIndex(page) < rows)
      if (page > 0) {
        assertTrue(offsets.getOffset(page) > offsets.getOffset(page - 1))
        assertTrue(offsets.getFirstRowIndex(page) > offsets.getFirstRowIndex(page - 1))
      }
    }
  }

  @Test
  void writerIndexesAcrossOutputPaths() {
    def data = Matrix.builder().columns(id: (0..<20000).toList(), text: ['label'] * 20000).build()
    File file = new File(directory, 'indexes.parquet')
    def writer = MatrixParquetWriter.builder(data).pageSize(1024).dictionaryEncoding(false)
    writer.write(file)
    def stream = new ByteArrayOutputStream()
    writer.write(stream)
    [new LocalInputFile(file.toPath()), new InMemoryInputFile(writer.writeBytes()),
     new InMemoryInputFile(stream.toByteArray())].each { input ->
      def options = org.apache.parquet.ParquetReadOptions.builder(new PlainParquetConfiguration()).build()
      ParquetFileReader.open(input, options).withCloseable { reader ->
        reader.rowGroups[0].columns.each { column -> checkIndexes(reader, column, input.length, reader.rowGroups[0].rowCount) }
      }
    }
  }

  @Test
  void pageIndexesReduceDataReadsWithoutRowGroupPruning() {
    File file = new File(directory, 'pages.parquet')
    def data = Matrix.builder().columns(id: (0..<20000).toList(), text: ['categorical'] * 20000).build()
    MatrixParquetWriter.builder(data).compressionCodec('UNCOMPRESSED').dictionaryEncoding(false)
        .pageSize(1024).rowGroupSize(16 * 1024 * 1024L).write(file)
    List<Long> counts = []
    [false, true].each { enabled ->
      def input = new CountingInput(new LocalInputFile(file.toPath()))
      def options = new ParquetReadOptions(columns: ['id'], filter: Predicates.ge('id', 19990), useColumnIndexFilter: enabled)
      options.useRowGroupFilters = false
      def result = MatrixParquetReader.readInput(input, options, null)
      assertEquals((19990..<20000).toList(), result.column('id'))
      counts.add(input.totalBytes)
    }
    println "PAGE_INDEX bytes full=${counts[0]} pruned=${counts[1]}"
    assertTrue(counts[1] < counts[0], "Indexed totalBytes ${counts[1]} versus full ${counts[0]}")
  }

  @Test
  void rejectedRowsNeverReachColumnsAndDictionaryStringsAreReused() {
    File file = new File(directory, 'dictionary.parquet')
    MatrixParquetWriter.write(Matrix.builder().columns(id: [1, 2, 3, 4, 5], text: ['same'] * 5).build(), file)
    def result = MatrixParquetReader.read(file)
    assertSame(result[0, 'text'], result[4, 'text'])
    [Predicates.ge('id', 3), Predicates.ne('id', 3), Predicates.not(Predicates.ge('id', 1))].each { predicate ->
      def filtered = MatrixParquetReader.builder().filter(predicate).read(file)
      assertEquals(filtered.column('id').size(), filtered.column('text').size())
      assertEquals((1..5).findAll { predicate.test([id: it]) }, filtered.column('id'))
    }
    def empty = MatrixParquetReader.builder().rowGroups([0]).filter(Predicates.gt('id', 100)).read(file)
    assertEquals(['id', 'text'], empty.columnNames())
    assertEquals(0, empty.rowCount())
  }

  @Test
  void bloomFilterSkipsAnInteriorAbsentKeyIndependently() {
    File file = new File(directory, 'bloom.parquet')
    def data = Matrix.builder().columns(id: (0..<10000).collect { 2 * it + 1 }).build()
    MatrixParquetWriter.builder(data).compressionCodec('UNCOMPRESSED').dictionaryEncoding(false)
        .rowGroupSize(16L * 1024 * 1024).bloomFilterEnabled([id: true]).bloomFilterNDV([id: 10000L]).write(file)
    int absent
    ParquetFileReader.open(new LocalInputFile(file.toPath()), org.apache.parquet.ParquetReadOptions.builder(new PlainParquetConfiguration()).build()).withCloseable { reader ->
      assertEquals(1, reader.rowGroups.size())
      def bloom = reader.readBloomFilter(reader.rowGroups[0].columns[0])
      assertNotNull(bloom)
      absent = (2..<19998).find { it % 2 == 0 && !bloom.findHash(bloom.hash(it)) }
    }
    List<Long> counts = []
    [false, true].each { enabled ->
      def input = new CountingInput(new LocalInputFile(file.toPath()))
      def options = new ParquetReadOptions(filter: Predicates.eq('id', absent), useColumnIndexFilter: false)
      options.useRowGroupFilters = enabled
      assertEquals(0, MatrixParquetReader.readInput(input, options, null).rowCount())
      counts.add(input.totalBytes)
    }
    assertTrue(counts[1] < counts[0], "Bloom totalBytes ${counts[1]} versus full ${counts[0]}")
    println "BLOOM bytes full=${counts[0]} pruned=${counts[1]}"
  }

}
