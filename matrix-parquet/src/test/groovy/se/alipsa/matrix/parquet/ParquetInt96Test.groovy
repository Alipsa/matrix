package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.*

class ParquetInt96Test {
  @TempDir File directory

  @Test
  void legacyTimestampsUseUtcAndReaderZoneAndMetadata() {
    def schema = MessageTypeParser.parseMessageType('message legacy { optional int96 ts; }')
    def instants = [Instant.EPOCH, Instant.parse('1969-12-31T23:59:59.5Z'), Instant.parse('2024-01-01T00:00:00.123456789Z')]
    [false, true].each { metadata ->
      File file = ParquetFixFixtures.write(directory, "int96-$metadata", schema, metadata ? ['matrix.columnTypes': Instant.name, 'matrix.parquet.version': '2'] : [:]) { writer, factory ->
        instants.each { value ->
          long day = Math.floorDiv(value.epochSecond, 86400L)
          long nanos = Math.floorMod(value.epochSecond, 86400L) * 1_000_000_000L + value.nano
          byte[] bytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(nanos).putInt((int) (day + 2_440_588L)).array()
          writer.write(factory.newGroup().append('ts', Binary.fromConstantByteArray(bytes)))
        }
      }
      def result = MatrixParquetReader.builder().zoneId('Europe/Stockholm').read(file)
      assertEquals(metadata ? Instant : LocalDateTime, result.type('ts'))
      assertEquals(metadata ? instants : instants.collect { LocalDateTime.ofInstant(it, ZoneId.of('Europe/Stockholm')) }, result.column('ts'))
      [Predicates.eq('ts', instants[2]), Predicates.gt('ts', Instant.EPOCH), Predicates.eq('ts', LocalDateTime.ofInstant(instants[2], ZoneId.of('Europe/Stockholm')))].each { predicate ->
        assertEquals(1, MatrixParquetReader.builder().zoneId('Europe/Stockholm').filter(predicate).read(file).rowCount())
        assertNull(predicate.pushdown(schema))
      }
    }
    def field = schema.getType('ts').asPrimitiveType()
    assertTrue(assertThrows(IllegalArgumentException) { MatrixParquetReader.decodePrimitive(Binary.fromConstantByteArray(new byte[11]), field, null, false) }.message.contains('ts'))
    byte[] invalid = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(-1L).putInt(2_440_588).array()
    assertThrows(IllegalArgumentException) { MatrixParquetReader.decodePrimitive(Binary.fromConstantByteArray(invalid), field, null, false) }
  }
}
