package test.alipsa.matrix.bigquery

import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.Test

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*

import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.bigquery.TypeMapper

class TypeMapperTest {

  @Test
  void mapsStandardSqlTypesForCommonInputs() {
    assertEquals(StandardSQLTypeName.BIGNUMERIC, TypeMapper.toStandardSqlType(BigDecimal))
    assertEquals(StandardSQLTypeName.INT64, TypeMapper.toStandardSqlType(Long))
    assertEquals(StandardSQLTypeName.TIME, TypeMapper.toStandardSqlType(LocalTime))
    assertEquals(StandardSQLTypeName.STRING, TypeMapper.toStandardSqlType(String))
  }

  @Test
  void resolvesLegacyTypeMappings() {
    assertEquals(LocalDateTime, TypeMapper.convertType(LegacySQLTypeName.DATETIME))
    assertEquals(LocalTime, TypeMapper.convertType(LegacySQLTypeName.TIME))
    assertEquals(Instant, TypeMapper.convertType(LegacySQLTypeName.TIMESTAMP))
  }

  @Test
  void convertsTimeRepresentations() {
    def parsed = TypeMapper.convert("12:34:56.789123", LegacySQLTypeName.TIME)
    assertEquals(LocalTime.parse("12:34:56.789123", Bq.bqTimeFormatter), parsed)

    def fromMicros = TypeMapper.convert(123_456L, LegacySQLTypeName.TIME)
    assertEquals(LocalTime.ofNanoOfDay(123_456L * 1_000L), fromMicros)
  }

  @Test
  void fallsBackToStringsForUnsupportedTypes() {
    assertEquals("value", TypeMapper.convert("value", LegacySQLTypeName.STRING))
    assertNull(TypeMapper.convert(null, LegacySQLTypeName.STRING))
  }

  @Test
  void detectsValuesThatRequireConversionBeforeInsert() {
    assertTrue(Bq.needsConversion(LocalDate.now()))
    assertTrue(Bq.needsConversion(new Timestamp(System.currentTimeMillis())))
    assertFalse(Bq.needsConversion("plain"))
    assertFalse(Bq.needsConversion(null))
  }
}
