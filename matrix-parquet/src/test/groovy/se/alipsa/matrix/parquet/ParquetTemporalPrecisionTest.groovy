package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix

import java.sql.Timestamp
import java.time.*
import java.time.temporal.ChronoUnit

class ParquetTemporalPrecisionTest {
  @Test
  void epochConversionIncludesNegativeAndExtremeBounds() {
    TimeUnit.values().each { unit ->
      assertEquals(0L, TemporalUnits.epochUnits(Instant.EPOCH, unit))
      long units = 1_000_000_000L.intdiv(TemporalUnits.nanosPerUnit(unit))
      assertEquals(-units, TemporalUnits.epochUnits(Instant.ofEpochSecond(-1), unit))
      assertEquals(-units.intdiv(2L), TemporalUnits.epochUnits(Instant.parse('1969-12-31T23:59:59.5Z'), unit))
    }
    [[TimeUnit.NANOS, '1677-09-21T00:12:43.145224192Z', '2262-04-11T23:47:16.854775807Z', 1L],
     [TimeUnit.MICROS, '-290308-12-21T19:59:05.224192Z', '+294247-01-10T04:00:54.775807Z', 1000L]].each { unit, minimum, maximum, tick ->
      Instant lower = Instant.parse(minimum), upper = Instant.parse(maximum)
      assertEquals(Long.MIN_VALUE, TemporalUnits.epochUnits(lower, unit))
      assertEquals(Long.MAX_VALUE, TemporalUnits.epochUnits(upper, unit))
      assertThrows(ArithmeticException) { TemporalUnits.epochUnits(lower.minusNanos(tick), unit) }
      assertThrows(ArithmeticException) { TemporalUnits.epochUnits(upper.plusNanos(tick), unit) }
    }
  }

  @Test
  void microsTruncateAllTimestampClassesAndExactPredicatesStayExact() {
    Instant value = Instant.EPOCH.plusNanos(123456789)
    def data = Matrix.builder().columns(ts: [value], local: [LocalDateTime.ofInstant(value, ZoneOffset.UTC)], stamp: [Timestamp.from(value)]).build()
    byte[] bytes = MatrixParquetWriter.builder(data).zoneId('UTC').writeBytes()
    def read = MatrixParquetReader.builder().zoneId('UTC').read(bytes)
    assertEquals(value.truncatedTo(ChronoUnit.MICROS), read[0, 'ts'])
    assertEquals(LocalDateTime.ofInstant(value.truncatedTo(ChronoUnit.MICROS), ZoneOffset.UTC), read[0, 'local'])
    assertEquals(Timestamp.from(value.truncatedTo(ChronoUnit.MICROS)), read[0, 'stamp'])
    [[Predicates.eq('ts', value), 0], [Predicates.eq('ts', value.truncatedTo(ChronoUnit.MICROS)), 1],
     [Predicates.ge('ts', value), 0], [Predicates.le('ts', value), 1], [Predicates.lt('ts', value), 1],
     [Predicates.ne('ts', value), 1]].each { predicate, count ->
      assertEquals(count, MatrixParquetReader.builder().filter(predicate).read(bytes).rowCount())
    }
    Instant negative = Instant.parse('1969-12-31T23:59:59.123456789Z')
    assertEquals(negative.truncatedTo(ChronoUnit.MICROS), MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [negative]).build()))[0, 'ts'])
    assertEquals(Instant.EPOCH, MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [Instant.EPOCH.plusNanos(1)]).build()))[0, 'ts'])
    assertEquals(1, MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [Instant.now()], local: [LocalDateTime.now()]).build())).rowCount())
  }

  @Test
  void nanosRoundTripBoundsAndPushdownAndSpi() {
    [Instant.EPOCH.plusNanos(123456789), Instant.parse('1677-09-21T00:12:43.145224192Z'), Instant.parse('2262-04-11T23:47:16.854775807Z')].each { value ->
      def matrix = Matrix.builder().columns(ts: [value], stamp: [Timestamp.from(value)], local: [LocalDateTime.ofInstant(value, ZoneOffset.UTC)]).build()
      def options = ParquetWriteOptions.fromMap([timestampUnit: ' nanos ', zoneId: 'UTC'])
      byte[] bytes = MatrixParquetWriter.writeBytes(matrix, options)
      def schema = ParquetFixFixtures.footer(bytes).schema
      assertEquals(TimeUnit.NANOS, schema.getType('ts').logicalTypeAnnotation.unit)
      assertNotNull(Predicates.eq('ts', value).pushdown(schema))
      def result = MatrixParquetReader.builder().zoneId('UTC').filter(Predicates.eq('ts', value)).read(bytes)
      assertEquals(value, result[0, 'ts'])
      assertEquals(Timestamp.from(value), result[0, 'stamp'])
      assertEquals(LocalDateTime.ofInstant(value, ZoneOffset.UTC), result[0, 'local'])
      assertEquals(value, MatrixParquetReader.read(MatrixParquetWriter.builder(Matrix.builder().columns(ts: [value]).build()).timestampUnit('NANOS').writeBytes())[0, 'ts'])
    }
    assertTrue(ParquetWriteOptions.descriptors().any { it.name == 'timestampUnit' && it.defaultValue == 'MICROS' })
    [null, 'millis', 'SECONDS'].each { unit -> assertThrows(IllegalArgumentException) { new ParquetWriteOptions().timestampUnit(unit) } }
  }

  @Test
  void overflowReportsColumnAndRowForEveryTemporalClass() {
    ['MICROS', 'NANOS'].each { unit ->
      def failure = assertThrows(IllegalArgumentException) {
        MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [Instant.MAX]).build(), new ParquetWriteOptions().timestampUnit(unit))
      }
      assertTrue(failure.message.contains('ts') && failure.message.contains('row 0'))
    }
    [Instant.parse('1677-09-21T00:12:43.145224191Z'), Instant.parse('2262-04-11T23:47:16.854775808Z')].each { value ->
      [value, Timestamp.from(value), LocalDateTime.ofInstant(value, ZoneOffset.UTC)].each { temporal ->
        def matrix = Matrix.builder().columns(ts: [temporal]).build()
        def failure = assertThrows(IllegalArgumentException) { MatrixParquetWriter.builder(matrix).zoneId('UTC').timestampUnit('NANOS').writeBytes() }
        assertTrue(failure.message.contains('ts') && failure.message.contains('row 0'))
        assertEquals(1, MatrixParquetReader.read(MatrixParquetWriter.builder(matrix).zoneId('UTC').writeBytes()).rowCount())
      }
    }
    Instant minimum = Instant.parse('-290308-12-21T19:59:05.224192Z')
    assertEquals(minimum, MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [minimum]).build()))[0, 'ts'])
    assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [minimum.minusNanos(1000)]).build()) }
  }

  @Test
  void farFutureLocalDateTimeAndTimestampFailAtSchemaValidationBeforeWriting(@TempDir File directory) {
    File file = new File(directory, 'overflow.parquet')
    LocalDateTime sentinel = LocalDateTime.of(9999, 12, 31, 0, 0)
    def localFailure = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.write(Matrix.builder().columns(ts: [sentinel]).build(), file, new ParquetWriteOptions().timestampUnit('NANOS'))
    }
    assertTrue(localFailure.message.contains('LocalDateTime') && localFailure.message.contains('row 0'), localFailure.message)
    assertFalse(file.exists(), 'schema validation must fail before any file is created')
    def timestampFailure = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.writeBytes(Matrix.builder().columns(ts: [Timestamp.from(Instant.parse('2263-01-01T00:00:00Z'))]).build(),
          new ParquetWriteOptions().timestampUnit('NANOS'))
    }
    assertTrue(timestampFailure.message.contains('Timestamp') && timestampFailure.message.contains('row 0'), timestampFailure.message)
  }

  @Test
  void nanosBoundaryHonorsThePinnedWriterZone() {
    LocalDateTime boundary = LocalDateTime.ofInstant(Instant.parse('2262-04-11T23:47:16.854775807Z'), ZoneOffset.UTC)
    def matrix = Matrix.builder().columns(ts: [boundary]).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(matrix, ParquetWriteOptions.fromMap([timestampUnit: 'NANOS', zoneId: 'UTC']))
    assertEquals(boundary, MatrixParquetReader.builder().zoneId('UTC').read(bytes)[0, 'ts'])
    def failure = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.writeBytes(matrix, ParquetWriteOptions.fromMap([timestampUnit: 'NANOS', zoneId: 'Pacific/Honolulu']))
    }
    assertTrue(failure.message.contains('LocalDateTime'), failure.message)
  }
}
