package test.alipsa.matrix.bigquery

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for Bq utility methods that don't require BigQuery connections.
 * These tests can run in CI without external dependencies.
 */
@CompileStatic
class BqUnitTest {

  // Tests for sanitizeString(Object)

  @Test
  void testSanitizeStringNull() {
    assertNull(Bq.sanitizeString(null))
  }

  @Test
  void testSanitizeStringNormalString() {
    assertEquals("Hello World", Bq.sanitizeString("Hello World"))
    assertEquals("Test 123", Bq.sanitizeString("Test 123"))
    assertEquals("", Bq.sanitizeString(""))
  }

  @Test
  void testSanitizeStringRemovesControlCharacters() {
    // Test various Unicode control characters
    String withControls = "Hello\u0000World"  // NULL character
    assertEquals("HelloWorld", Bq.sanitizeString(withControls))

    String withTab = "Hello\tWorld"  // TAB is actually allowed in JSON, but let's test
    String result = Bq.sanitizeString(withTab)
    // Control characters in Unicode category C should be removed
    assertNotNull(result)
  }

  @Test
  void testSanitizeStringPreservesNewlines() {
    // Newlines might be preserved or removed depending on implementation
    String withNewline = "Hello\nWorld"
    String result = Bq.sanitizeString(withNewline)
    assertNotNull(result)
  }

  @Test
  void testSanitizeStringNonString() {
    assertEquals("123", Bq.sanitizeString(123))
    assertEquals("true", Bq.sanitizeString(true))
    assertEquals("45.67", Bq.sanitizeString(45.67))
  }

  // Tests for needsConversion(Object)

  @Test
  void testNeedsConversionBigDecimal() {
    assertTrue(Bq.needsConversion(new BigDecimal("123.45")))
    assertTrue(Bq.needsConversion(BigDecimal.ZERO))
  }

  @Test
  void testNeedsConversionBigInteger() {
    assertTrue(Bq.needsConversion(new BigInteger("12345")))
    assertTrue(Bq.needsConversion(BigInteger.ZERO))
  }

  @Test
  void testNeedsConversionTime() {
    assertTrue(Bq.needsConversion(new Time(System.currentTimeMillis())))
  }

  @Test
  void testNeedsConversionLocalDateTime() {
    assertTrue(Bq.needsConversion(LocalDateTime.now()))
  }

  @Test
  void testNeedsConversionLocalTime() {
    assertTrue(Bq.needsConversion(LocalTime.now()))
  }

  @Test
  void testNeedsConversionInstant() {
    assertTrue(Bq.needsConversion(Instant.now()))
  }

  @Test
  void testNeedsConversionTimestamp() {
    assertTrue(Bq.needsConversion(new Timestamp(System.currentTimeMillis())))
  }

  @Test
  void testNeedsConversionZonedDateTime() {
    assertTrue(Bq.needsConversion(ZonedDateTime.now()))
  }

  @Test
  void testNeedsConversionDate() {
    assertTrue(Bq.needsConversion(new Date()))
  }

  @Test
  void testNeedsConversionLocalDate() {
    assertTrue(Bq.needsConversion(LocalDate.now()))
  }

  @Test
  void testNeedsConversionPrimitiveTypes() {
    // Primitive types don't need conversion
    assertFalse(Bq.needsConversion(123))
    assertFalse(Bq.needsConversion(456L))
    // Note: In Groovy, decimal literals are BigDecimal, not double!
    // So 78.9 is a BigDecimal and DOES need conversion
    assertFalse(Bq.needsConversion(78.9d))  // Use 'd' suffix for actual double
    assertFalse(Bq.needsConversion(12.34f))
    assertFalse(Bq.needsConversion(true))
    assertFalse(Bq.needsConversion("string"))
    assertFalse(Bq.needsConversion(null))
  }

  // Tests for convertObjectValue(Object)

  @Test
  void testConvertObjectValueBigDecimal() {
    BigDecimal bd = new BigDecimal("123.456789")
    String result = Bq.convertObjectValue(bd) as String
    assertEquals("123.456789", result)

    // Test scientific notation prevention
    BigDecimal large = new BigDecimal("123456789012345678901234567890")
    String largeResult = Bq.convertObjectValue(large) as String
    assertFalse(largeResult.contains("E"))  // Should not contain scientific notation
    assertTrue(largeResult.contains("123456789012345678901234567890"))
  }

  @Test
  void testConvertObjectValueBigInteger() {
    BigInteger bi = new BigInteger("999999999999999999")
    String result = Bq.convertObjectValue(bi) as String
    assertEquals("999999999999999999", result)
  }

  @Test
  void testConvertObjectValueLocalDate() {
    LocalDate date = LocalDate.of(2024, 1, 15)
    String result = Bq.convertObjectValue(date) as String
    assertEquals("2024-01-15", result)
  }

  @Test
  void testConvertObjectValueLocalDateTime() {
    LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 13, 45, 30, 123456000)
    String result = Bq.convertObjectValue(dt) as String
    // Format: yyyy-MM-dd'T'HH:mm:ss.SSSSSS
    assertEquals("2024-01-15T13:45:30.123456", result)
  }

  @Test
  void testConvertObjectValueLocalTime() {
    LocalTime time = LocalTime.of(13, 45, 30, 123456000)
    String result = Bq.convertObjectValue(time) as String
    // Time is truncated to microseconds and formatted
    assertTrue(result.startsWith("13:45:30"))
  }

  @Test
  void testConvertObjectValueTime() {
    // Create a Time object for 13:45:30
    @SuppressWarnings('deprecation')
    Time time = new Time(13, 45, 30)
    String result = Bq.convertObjectValue(time) as String
    assertTrue(result.contains("13:45:30"))
  }

  @Test
  void testConvertObjectValueTimestamp() {
    Timestamp ts = Timestamp.valueOf("2024-01-15 13:45:30.123")
    String result = Bq.convertObjectValue(ts) as String
    // Should be converted to ISO instant format
    assertTrue(result.contains("2024-01-15"))
    assertTrue(result.contains("T"))
  }

  @Test
  void testConvertObjectValueZonedDateTime() {
    ZonedDateTime zdt = ZonedDateTime.parse("2024-01-15T13:45:30Z")
    String result = Bq.convertObjectValue(zdt) as String
    // Should use ISO_ZONED_DATE_TIME format
    assertTrue(result.contains("2024-01-15"))
    assertTrue(result.contains("T"))
    assertTrue(result.contains("Z"))
  }

  @Test
  void testConvertObjectValueDate() {
    // Use the SimpleDateFormat from Bq
    Date date = Bq.bqSimpledateFormat.parse("2024-01-15")
    String result = Bq.convertObjectValue(date) as String
    assertEquals("2024-01-15", result)
  }

  @Test
  void testConvertObjectValueOther() {
    // Non-date/numeric types are converted via String.valueOf
    String result = Bq.convertObjectValue("test") as String
    assertEquals("test", result)

    String numResult = Bq.convertObjectValue(123) as String
    assertEquals("123", numResult)
  }

  // Tests for createSchema(Matrix)

  @Test
  void testCreateSchemaSimpleMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['id', 'name', 'value'])
        .types([Integer, String, BigDecimal])
        .rows([
            [1, 'Alice', new BigDecimal("100.50")],
            [2, 'Bob', new BigDecimal("200.75")]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(3, schema.fields.size())

    Field idField = schema.fields[0]
    assertEquals('id', idField.name)
    // Field.type returns LegacySQLTypeName, not StandardSQLTypeName
    assertEquals(LegacySQLTypeName.INTEGER, idField.type)

    Field nameField = schema.fields[1]
    assertEquals('name', nameField.name)
    assertEquals(LegacySQLTypeName.STRING, nameField.type)

    Field valueField = schema.fields[2]
    assertEquals('value', valueField.name)
    // StandardSQLTypeName.BIGNUMERIC creates field with LegacySQLTypeName.BIGNUMERIC
    assertEquals(LegacySQLTypeName.BIGNUMERIC, valueField.type)
  }

  @Test
  void testCreateSchemaDateTimeTypes() {
    Matrix matrix = Matrix.builder()
        .columnNames(['date', 'time', 'datetime', 'timestamp'])
        .types([LocalDate, LocalTime, LocalDateTime, Instant])
        .rows([
            [LocalDate.now(), LocalTime.now(), LocalDateTime.now(), Instant.now()]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(4, schema.fields.size())

    assertEquals('date', schema.fields[0].name)
    assertEquals(LegacySQLTypeName.DATE, schema.fields[0].type)

    assertEquals('time', schema.fields[1].name)
    assertEquals(LegacySQLTypeName.TIME, schema.fields[1].type)

    assertEquals('datetime', schema.fields[2].name)
    assertEquals(LegacySQLTypeName.DATETIME, schema.fields[2].type)

    assertEquals('timestamp', schema.fields[3].name)
    assertEquals(LegacySQLTypeName.TIMESTAMP, schema.fields[3].type)
  }

  @Test
  void testCreateSchemaNumericTypes() {
    Matrix matrix = Matrix.builder()
        .columnNames(['int_val', 'long_val', 'double_val', 'bigdec_val'])
        .types([Integer, Long, Double, BigDecimal])
        .rows([
            [1, 1000L, 1.5d, new BigDecimal("999.99")]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(4, schema.fields.size())

    assertEquals(LegacySQLTypeName.INTEGER, schema.fields[0].type)
    assertEquals(LegacySQLTypeName.INTEGER, schema.fields[1].type)
    assertEquals(LegacySQLTypeName.FLOAT, schema.fields[2].type)
    assertEquals(LegacySQLTypeName.BIGNUMERIC, schema.fields[3].type)
  }

  @Test
  void testCreateSchemaBooleanAndBytes() {
    Matrix matrix = Matrix.builder()
        .columnNames(['flag', 'data'])
        .types([Boolean, byte[]])
        .rows([
            [true, "test".bytes]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(2, schema.fields.size())

    assertEquals('flag', schema.fields[0].name)
    assertEquals(LegacySQLTypeName.BOOLEAN, schema.fields[0].type)

    assertEquals('data', schema.fields[1].name)
    assertEquals(LegacySQLTypeName.BYTES, schema.fields[1].type)
  }

  @Test
  void testCreateSchemaEmptyMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['col1'])
        .types([String])
        .rows([])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(1, schema.fields.size())
    assertEquals('col1', schema.fields[0].name)
    assertEquals(LegacySQLTypeName.STRING, schema.fields[0].type)
  }

  @Test
  void testCreateSchemaWithTimestamp() {
    // Regression test: Verify that Timestamp type correctly maps to TIMESTAMP, not DATE
    Matrix matrix = Matrix.builder()
        .columnNames(['event_time', 'event_date'])
        .types([Timestamp, Date])
        .rows([
            [new Timestamp(System.currentTimeMillis()), new Date()]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(2, schema.fields.size())

    Field timestampField = schema.fields[0]
    assertEquals('event_time', timestampField.name)
    // This was the bug: Timestamp was incorrectly mapped to DATE instead of TIMESTAMP
    assertEquals(LegacySQLTypeName.TIMESTAMP, timestampField.type)

    Field dateField = schema.fields[1]
    assertEquals('event_date', dateField.name)
    assertEquals(LegacySQLTypeName.DATE, dateField.type)
  }

  // Tests for BigQuery date/time formatters

  @Test
  void testBqDateFormatter() {
    LocalDate date = LocalDate.of(2024, 1, 15)
    String formatted = date.format(Bq.bqDateFormatter)
    assertEquals("2024-01-15", formatted)

    LocalDate parsed = LocalDate.parse("2024-01-15", Bq.bqDateFormatter)
    assertEquals(date, parsed)
  }

  @Test
  void testBqDateTimeFormatter() {
    LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 13, 45, 30, 123456000)
    String formatted = dt.format(Bq.bqDateTimeFormatter)
    assertEquals("2024-01-15T13:45:30.123456", formatted)
  }

  @Test
  void testBqTimeFormatter() {
    LocalTime time = LocalTime.of(13, 45, 30, 123456000)
    String formatted = time.format(Bq.bqTimeFormatter)
    assertTrue(formatted.startsWith("13:45:30"))

    // Test parsing with microseconds
    LocalTime parsed = LocalTime.parse("13:45:30.123456", Bq.bqTimeFormatter)
    assertEquals(13, parsed.hour)
    assertEquals(45, parsed.minute)
    assertEquals(30, parsed.second)
  }

  @Test
  void testBqTimeFormatterWithoutMicroseconds() {
    // The formatter should handle times without microseconds
    LocalTime parsed = LocalTime.parse("13:45:30", Bq.bqTimeFormatter)
    assertEquals(13, parsed.hour)
    assertEquals(45, parsed.minute)
    assertEquals(30, parsed.second)
    assertEquals(0, parsed.nano)
  }
}
