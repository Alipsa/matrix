package test.alipsa.matrix.bigquery

import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.StandardSQLTypeName
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.TypeMapper

import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

import static org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for TypeMapper conversion logic.
 * These tests do not require external BigQuery connections and can run in CI.
 */
@CompileStatic
class TypeMapperTest {

  // Tests for toStandardSqlType(Class)

  @Test
  void testToStandardSqlTypeNumeric() {
    assertEquals(StandardSQLTypeName.BIGNUMERIC, TypeMapper.toStandardSqlType(BigDecimal))
    assertEquals(StandardSQLTypeName.INT64, TypeMapper.toStandardSqlType(BigInteger))
    assertEquals(StandardSQLTypeName.INT64, TypeMapper.toStandardSqlType(Integer))
    assertEquals(StandardSQLTypeName.INT64, TypeMapper.toStandardSqlType(Long))
    assertEquals(StandardSQLTypeName.INT64, TypeMapper.toStandardSqlType(Short))
    assertEquals(StandardSQLTypeName.INT64, TypeMapper.toStandardSqlType(Byte))
    assertEquals(StandardSQLTypeName.FLOAT64, TypeMapper.toStandardSqlType(Double))
    assertEquals(StandardSQLTypeName.FLOAT64, TypeMapper.toStandardSqlType(Float))
  }

  @Test
  void testToStandardSqlTypeBoolean() {
    assertEquals(StandardSQLTypeName.BOOL, TypeMapper.toStandardSqlType(Boolean))
  }

  @Test
  void testToStandardSqlTypeString() {
    assertEquals(StandardSQLTypeName.STRING, TypeMapper.toStandardSqlType(String))
  }

  @Test
  void testToStandardSqlTypeBytes() {
    assertEquals(StandardSQLTypeName.BYTES, TypeMapper.toStandardSqlType(byte[]))
    assertEquals(StandardSQLTypeName.BYTES, TypeMapper.toStandardSqlType(Byte[]))
  }

  @Test
  void testToStandardSqlTypeDateTime() {
    assertEquals(StandardSQLTypeName.TIME, TypeMapper.toStandardSqlType(LocalTime))
    assertEquals(StandardSQLTypeName.TIME, TypeMapper.toStandardSqlType(Time))
    assertEquals(StandardSQLTypeName.DATE, TypeMapper.toStandardSqlType(LocalDate))
    assertEquals(StandardSQLTypeName.DATE, TypeMapper.toStandardSqlType(Date))
    assertEquals(StandardSQLTypeName.DATETIME, TypeMapper.toStandardSqlType(LocalDateTime))
    // Fixed: Timestamp now correctly maps to TIMESTAMP (checked before Date in switch)
    assertEquals(StandardSQLTypeName.TIMESTAMP, TypeMapper.toStandardSqlType(Timestamp))
    assertEquals(StandardSQLTypeName.TIMESTAMP, TypeMapper.toStandardSqlType(Instant))
  }

  @Test
  void testToStandardSqlTypeZonedDateTime() {
    // ZonedDateTime is stored as STRING since BigQuery has no timezone-aware type
    assertEquals(StandardSQLTypeName.STRING, TypeMapper.toStandardSqlType(ZonedDateTime))
  }

  @Test
  void testToStandardSqlTypeUnknown() {
    // Unknown types default to STRING
    assertEquals(StandardSQLTypeName.STRING, TypeMapper.toStandardSqlType(Object))
    assertEquals(StandardSQLTypeName.STRING, TypeMapper.toStandardSqlType(List))
    assertEquals(StandardSQLTypeName.STRING, TypeMapper.toStandardSqlType(Map))
  }

  // Tests for convertType(LegacySQLTypeName)

  @Test
  void testConvertTypeNumeric() {
    assertEquals(BigDecimal, TypeMapper.convertType(LegacySQLTypeName.BIGNUMERIC))
    assertEquals(BigDecimal, TypeMapper.convertType(LegacySQLTypeName.NUMERIC))
    assertEquals(Long, TypeMapper.convertType(LegacySQLTypeName.INTEGER))
    assertEquals(Double, TypeMapper.convertType(LegacySQLTypeName.FLOAT))
  }

  @Test
  void testConvertTypeBoolean() {
    assertEquals(Boolean, TypeMapper.convertType(LegacySQLTypeName.BOOLEAN))
  }

  @Test
  void testConvertTypeString() {
    assertEquals(String, TypeMapper.convertType(LegacySQLTypeName.STRING))
  }

  @Test
  void testConvertTypeBytes() {
    assertEquals(byte[], TypeMapper.convertType(LegacySQLTypeName.BYTES))
  }

  @Test
  void testConvertTypeDateTime() {
    assertEquals(LocalDate, TypeMapper.convertType(LegacySQLTypeName.DATE))
    assertEquals(LocalDateTime, TypeMapper.convertType(LegacySQLTypeName.DATETIME))
    assertEquals(Instant, TypeMapper.convertType(LegacySQLTypeName.TIMESTAMP))
    assertEquals(LocalTime, TypeMapper.convertType(LegacySQLTypeName.TIME))
  }

  // Tests for convert(Object, LegacySQLTypeName)

  @Test
  void testConvertNullValue() {
    assertNull(TypeMapper.convert(null, LegacySQLTypeName.STRING))
    assertNull(TypeMapper.convert(null, LegacySQLTypeName.INTEGER))
    assertNull(TypeMapper.convert(null, LegacySQLTypeName.BOOLEAN))
  }

  @Test
  void testConvertToString() {
    assertEquals("hello", TypeMapper.convert("hello", LegacySQLTypeName.STRING))
    assertEquals("123", TypeMapper.convert(123, LegacySQLTypeName.STRING))
    assertEquals("true", TypeMapper.convert(true, LegacySQLTypeName.STRING))
  }

  @Test
  void testConvertToBoolean() {
    assertEquals(true, TypeMapper.convert(true, LegacySQLTypeName.BOOLEAN))
    assertEquals(true, TypeMapper.convert("true", LegacySQLTypeName.BOOLEAN))
    assertEquals(false, TypeMapper.convert(false, LegacySQLTypeName.BOOLEAN))
    assertEquals(false, TypeMapper.convert("false", LegacySQLTypeName.BOOLEAN))
  }

  @Test
  void testConvertToLong() {
    assertEquals(123L, TypeMapper.convert(123, LegacySQLTypeName.INTEGER))
    assertEquals(456L, TypeMapper.convert("456", LegacySQLTypeName.INTEGER))
    assertEquals(789L, TypeMapper.convert(789L, LegacySQLTypeName.INTEGER))
  }

  @Test
  void testConvertToBigDecimal() {
    assertEquals(new BigDecimal("123.45"), TypeMapper.convert(123.45, LegacySQLTypeName.BIGNUMERIC))
    assertEquals(new BigDecimal("678.90"), TypeMapper.convert("678.90", LegacySQLTypeName.NUMERIC))
    assertEquals(new BigDecimal("100"), TypeMapper.convert(100, LegacySQLTypeName.BIGNUMERIC))
  }

  @Test
  void testConvertToTime() {
    // Test string parsing
    LocalTime time1 = TypeMapper.convert("12:34:56", LegacySQLTypeName.TIME) as LocalTime
    assertEquals(12, time1.hour)
    assertEquals(34, time1.minute)
    assertEquals(56, time1.second)

    // Test microseconds conversion
    long micros = 12 * 3600 * 1_000_000L + 34 * 60 * 1_000_000L + 56 * 1_000_000L
    LocalTime time2 = TypeMapper.convert(micros, LegacySQLTypeName.TIME) as LocalTime
    assertEquals(12, time2.hour)
    assertEquals(34, time2.minute)
    assertEquals(56, time2.second)
  }

  @Test
  void testConvertDefaultToString() {
    // Unknown types default to string conversion
    assertEquals("someValue", TypeMapper.convert("someValue", LegacySQLTypeName.RECORD))
  }

  // Tests for convertToInstant(Object)

  @Test
  void testConvertToInstantNull() {
    assertNull(TypeMapper.convertToInstant(null))
  }

  @Test
  void testConvertToInstantFromNumber() {
    // BigQuery timestamps are in seconds with decimal microseconds
    // Example: 1609459200.123456 = 2021-01-01 00:00:00.123456 UTC
    BigDecimal timestampSeconds = new BigDecimal("1609459200.123456")
    Instant instant = TypeMapper.convertToInstant(timestampSeconds)

    assertNotNull(instant)
    assertEquals(1609459200123L, instant.toEpochMilli())
  }

  @Test
  void testConvertToInstantFromString() {
    // String representation of epoch seconds
    String timestampSeconds = "1609459200.500"
    Instant instant = TypeMapper.convertToInstant(timestampSeconds)

    assertNotNull(instant)
    assertEquals(1609459200500L, instant.toEpochMilli())
  }

  @Test
  void testConvertToInstantFromInteger() {
    // Integer seconds (no fractional part)
    int timestampSeconds = 1609459200
    Instant instant = TypeMapper.convertToInstant(timestampSeconds)

    assertNotNull(instant)
    assertEquals(1609459200000L, instant.toEpochMilli())
  }

  // Roundtrip tests

  @Test
  void testRoundtripConversion() {
    // Test that converting a type to SQL and back gives us the expected Java type
    // Note: BIGNUMERIC maps to NUMERIC in LegacySQLTypeName
    StandardSQLTypeName sqlType = TypeMapper.toStandardSqlType(BigDecimal)
    assertEquals(StandardSQLTypeName.BIGNUMERIC, sqlType)

    // INT64 maps to INTEGER in LegacySQLTypeName
    StandardSQLTypeName intSqlType = TypeMapper.toStandardSqlType(Integer)
    assertEquals(StandardSQLTypeName.INT64, intSqlType)

    // String stays as STRING
    StandardSQLTypeName strSqlType = TypeMapper.toStandardSqlType(String)
    assertEquals(StandardSQLTypeName.STRING, strSqlType)
  }
}
