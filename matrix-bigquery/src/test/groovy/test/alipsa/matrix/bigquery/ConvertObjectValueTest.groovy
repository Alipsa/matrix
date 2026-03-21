package test.alipsa.matrix.bigquery

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.bigquery.Bq

import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class ConvertObjectValueTest {

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
    assertEquals("2024-01-15T13:45:30.123456", result)
  }

  @Test
  void testConvertObjectValueLocalTime() {
    LocalTime time = LocalTime.of(13, 45, 30, 123456000)
    String result = Bq.convertObjectValue(time) as String
    assertTrue(result.startsWith("13:45:30"))
  }

  @Test
  void testConvertObjectValueTime() {
    @SuppressWarnings('deprecation')
    Time time = new Time(13, 45, 30)
    String result = Bq.convertObjectValue(time) as String
    assertTrue(result.contains("13:45:30"))
  }

  @Test
  void testConvertObjectValueTimestamp() {
    Timestamp ts = Timestamp.valueOf("2024-01-15 13:45:30.123")
    String result = Bq.convertObjectValue(ts) as String
    assertTrue(result.contains("2024-01-15"))
    assertTrue(result.contains("T"))
  }

  @Test
  void testConvertObjectValueInstant() {
    Instant instant = Instant.parse('2024-01-15T13:45:30.123Z')
    String result = Bq.convertObjectValue(instant) as String
    assertEquals('2024-01-15T13:45:30.123Z', result)
  }

  @Test
  void testConvertObjectValueZonedDateTime() {
    ZonedDateTime zdt = ZonedDateTime.parse("2024-01-15T13:45:30Z")
    String result = Bq.convertObjectValue(zdt) as String
    assertTrue(result.contains("2024-01-15"))
    assertTrue(result.contains("T"))
    assertTrue(result.contains("Z"))
  }

  @Test
  void testConvertObjectValueDate() {
    Date date = Date.from(LocalDate.of(2024, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant())
    String result = Bq.convertObjectValue(date) as String
    assertEquals("2024-01-15", result)
  }

  @Test
  void testConvertObjectValueOther() {
    String result = Bq.convertObjectValue("test") as String
    assertEquals("test", result)

    String numResult = Bq.convertObjectValue(123) as String
    assertEquals("123", numResult)
  }
}
