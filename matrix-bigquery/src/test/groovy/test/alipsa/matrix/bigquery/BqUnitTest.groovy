package test.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.bigquery.Bq

import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Unit tests for Bq utility methods that don't require BigQuery connections.
 * These tests can run in CI without external dependencies.
 */
@SuppressWarnings([
    'ClassEndsWithBlankLine',
    'UnnecessaryBigDecimalInstantiation',
    'UnnecessaryBigIntegerInstantiation',
    'UnnecessaryGString'
])
@CompileStatic
class BqUnitTest {

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

  // Tests for BigQuery date/time formatters

  @Test
  void testBqDateFormatter() {
    LocalDate date = LocalDate.of(2024, 1, 15)
    String formatted = date.format(Bq.BQ_DATE_FORMATTER)
    assertEquals("2024-01-15", formatted)

    LocalDate parsed = LocalDate.parse("2024-01-15", Bq.BQ_DATE_FORMATTER)
    assertEquals(date, parsed)
  }

  @Test
  void testBqDateTimeFormatter() {
    LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 13, 45, 30, 123456000)
    String formatted = dt.format(Bq.BQ_DATE_TIME_FORMATTER)
    assertEquals("2024-01-15T13:45:30.123456", formatted)
  }

  @Test
  void testBqTimeFormatter() {
    LocalTime time = LocalTime.of(13, 45, 30, 123456000)
    String formatted = time.format(Bq.BQ_TIME_FORMATTER)
    assertTrue(formatted.startsWith("13:45:30"))

    // Test parsing with microseconds
    LocalTime parsed = LocalTime.parse("13:45:30.123456", Bq.BQ_TIME_FORMATTER)
    assertEquals(13, parsed.hour)
    assertEquals(45, parsed.minute)
    assertEquals(30, parsed.second)
  }

  @Test
  void testBqTimeFormatterWithoutMicroseconds() {
    // The formatter should handle times without microseconds
    LocalTime parsed = LocalTime.parse("13:45:30", Bq.BQ_TIME_FORMATTER)
    assertEquals(13, parsed.hour)
    assertEquals(45, parsed.minute)
    assertEquals(30, parsed.second)
    assertEquals(0, parsed.nano)
  }

  // Tests for waitForTableTimeoutMs configuration

  @Test
  void testDefaultWaitForTableTimeout() {
    assertEquals(60_000L, Bq.DEFAULT_WAIT_FOR_TABLE_TIMEOUT_MS)
  }

  @Test
  void testWaitForTableTimeoutGetterSetter() {
    // We can't instantiate Bq without BigQuery credentials in unit tests,
    // but we can verify the constant is correct
    assertEquals(60_000L, Bq.DEFAULT_WAIT_FOR_TABLE_TIMEOUT_MS)

    // Verify the constant is 60 seconds (1 minute)
    assertEquals(60 * 1000, Bq.DEFAULT_WAIT_FOR_TABLE_TIMEOUT_MS)
  }
}
