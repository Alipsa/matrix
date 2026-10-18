package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ParquetUnsignedTest {
  @TempDir File directory

  @Test
  void unsignedValuesMetadataPredicatesAndBatches() {
    def schema = MessageTypeParser.parseMessageType('''message unsigned {
      optional int32 u32 (INTEGER(32,false)); optional int64 u64 (INTEGER(64,false));
      optional int32 u8 (INTEGER(8,false));
      optional group l (LIST) { repeated group list { optional int32 element (INTEGER(32,false)); } }
      optional int32 s16 (INTEGER(16,true)); }''')
    [false, true].each { wrong ->
      def metadata = wrong ? ['matrix.columnTypes': 'java.lang.Integer,java.lang.Long,java.lang.Byte,java.util.List,java.lang.Short', 'matrix.parquet.version': '2'] : [:]
      File file = ParquetFixFixtures.write(directory, "unsigned-$wrong", schema, metadata) { writer, factory ->
        def row = factory.newGroup().append('u32', (int) 3_000_000_000L).append('u64', -1L).append('u8', 255).append('s16', -123)
        row.addGroup('l').addGroup('list').append('element', (int) 3_000_000_000L)
        writer.write(row)
      }
      def result = MatrixParquetReader.read(file)
      assertEquals([Long, BigInteger, Integer, List, wrong ? Short : Integer], result.types())
      assertEquals(3_000_000_000L, result[0, 'u32'])
      assertEquals(18446744073709551615G, result[0, 'u64'])
      assertEquals(255, result[0, 'u8'])
      assertEquals([3_000_000_000L], result[0, 'l'])
      assertEquals((short) -123, result[0, 's16'])
      [Predicates.gt('u32', 2_000_000_000L), Predicates.eq('u64', 18446744073709551615G)].each { predicate ->
        assertNull(predicate.pushdown(schema))
        assertEquals(1, MatrixParquetReader.builder().filter(predicate).limit(1).read(file).rowCount())
        MatrixParquetReader.builder().filter(predicate).maxBatchRows(1).batches(file).withCloseable { batches ->
          assertEquals(1, batches.next().rowCount())
          assertFalse(batches.hasNext())
        }
      }
    }
  }
}
