package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.io.LocalInputFile
import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix

import java.time.*

class ParquetPredicateRepresentabilityTest {
  @TempDir File directory

  @Test
  void unrepresentableValuesUseExactResidualsAcrossCompositionsBatchesAndDatasets() {
    def schema = MessageTypeParser.parseMessageType('''message external {
      optional int32 d (DECIMAL(3,2)); optional int32 i;
      optional int64 ts (TIMESTAMP(MILLIS,true)); optional int32 t (TIME(MILLIS,true)); }''')
    File file = ParquetFixFixtures.write(directory, 'values', schema) { writer, factory ->
      writer.write(factory.newGroup().append('d', 123).append('i', 1).append('ts', 0L).append('t', 43200000))
      writer.write(factory.newGroup().append('d', 250).append('i', 2).append('ts', 1L).append('t', 43200001))
    }
    def rows = [[d: 1.23G, i: 1, ts: Instant.EPOCH, t: LocalTime.NOON],
                [d: 2.50G, i: 2, ts: Instant.EPOCH.plusMillis(1), t: LocalTime.NOON.plusNanos(1000000)]]
    [Predicates.gt('d', 1.005G), Predicates.eq('d', 1.005G), Predicates.le('d', 2.505G),
     Predicates.gt('i', 1.5G), Predicates.eq('i', 10_000_000_000L), Predicates.lt('i', 10_000_000_000L),
     Predicates.ne('i', Double.NaN), Predicates.lt('ts', Instant.EPOCH.plusNanos(1000)),
     Predicates.lt('t', LocalTime.NOON.plusNanos(1))].each { predicate ->
      assertNull(predicate.pushdown(schema))
      [predicate, Predicates.and(predicate, Predicates.ge('i', 1)), Predicates.or(predicate, Predicates.eq('i', 2)), Predicates.not(predicate)].each { composed ->
        def expected = rows.findAll { composed.test(it) }*.i
        assertEquals(expected, MatrixParquetReader.builder().filter(composed).read(file).column('i'))
        assertEquals(expected, MatrixParquetReader.builder().filter(composed).read([file]).column('i'))
        List actual = []
        MatrixParquetReader.builder().maxBatchRows(1).filter(composed).batches(file).withCloseable { batches ->
          while (batches.hasNext()) { actual.addAll(batches.next().column('i')) }
        }
        assertEquals(expected, actual)
      }
    }
  }

  @Test
  void exactIntegerDecimalTimeAndDateValuesRemainPushable() {
    def schema = MessageTypeParser.parseMessageType('''message exact {
      optional int32 d (DECIMAL(5,2)); optional int64 big (DECIMAL(12,2));
      optional int32 t (TIME(MILLIS,true)); optional int32 date (DATE); }''')
    File file = ParquetFixFixtures.write(directory, 'exact', schema) { writer, factory ->
      writer.write(factory.newGroup().append('d', 250).append('big', 250L).append('t', 43200000).append('date', 0))
    }
    [Predicates.gt('d', 1.25G), Predicates.gt('big', 1.25G), Predicates.ge('t', LocalTime.NOON), Predicates.eq('date', LocalDate.ofEpochDay(0))].each { predicate ->
      assertNotNull(predicate.pushdown(schema))
      assertEquals(1, MatrixParquetReader.builder().filter(predicate).read(file).rowCount())
    }
  }

  @Test
  void partialAndStillPrunesAndRemainsExactUnderOrAndNot() {
    File file = new File(directory, 'pruning.parquet')
    def data = Matrix.builder().columns(i: (0..<20000).collect { it % 3 }, j: (0..<20000).toList()).build()
    MatrixParquetWriter.builder(data).compressionCodec('UNCOMPRESSED').dictionaryEncoding(false).pageSize(1024).write(file)
    def partial = Predicates.and(Predicates.gt('i', 1.5G), Predicates.eq('j', 19997))
    def schema = ParquetFixFixtures.footer(file.bytes).schema
    assertNotNull(partial.pushdown(schema))
    List<Long> counts = []
    [false, true].each { indexed ->
      def input = new CountingInput(new LocalInputFile(file.toPath()))
      def options = new ParquetReadOptions(filter: partial, useColumnIndexFilter: indexed, useRowGroupFilters: false)
      assertEquals([19997], MatrixParquetReader.readInput(input, options, null).column('j'))
      counts.add(input.totalBytes)
    }
    assertTrue(counts[1] < counts[0])
    [Predicates.or(partial, Predicates.eq('j', 7)), Predicates.not(partial)].each { predicate ->
      assertEquals((0..<20000).findAll { predicate.test([i: it % 3, j: it]) }, MatrixParquetReader.builder().filter(predicate).read(file).column('j'))
    }
  }
}
