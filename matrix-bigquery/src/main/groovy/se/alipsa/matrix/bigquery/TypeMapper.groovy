package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.StandardSQLTypeName
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ValueConverter

import java.nio.ByteBuffer
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@CompileStatic
class TypeMapper {

  static StandardSQLTypeName toStandardSqlType(Class cls) {
    switch (cls) {
      case BigDecimal -> StandardSQLTypeName.BIGNUMERIC
      case BigInteger, Integer, Long, Short -> StandardSQLTypeName.INT64
      case Boolean -> StandardSQLTypeName.BOOL
      case Byte -> StandardSQLTypeName.INT64
      case Byte[], byte[] -> StandardSQLTypeName.BYTES
      case Double, Float -> StandardSQLTypeName.FLOAT64
      // Check more specific types before general types (Timestamp extends Date, Time extends Date)
      case LocalTime, Time -> StandardSQLTypeName.TIME
      case Timestamp, Instant -> StandardSQLTypeName.TIMESTAMP
      case LocalDate, Date -> StandardSQLTypeName.DATE
      case LocalDateTime -> StandardSQLTypeName.DATETIME
      case String -> StandardSQLTypeName.STRING
      case ZonedDateTime -> StandardSQLTypeName.STRING // There is no timezone aware type in BQ so we store it as a string
      default -> StandardSQLTypeName.STRING
    }
  }

  static Class convertType(LegacySQLTypeName typeName) {
    return switch (typeName) {
      case LegacySQLTypeName.BIGNUMERIC -> BigDecimal
      case LegacySQLTypeName.BOOLEAN -> Boolean
      case LegacySQLTypeName.BYTES -> byte[]
      case LegacySQLTypeName.DATE -> LocalDate
      case LegacySQLTypeName.DATETIME -> LocalDateTime
      case LegacySQLTypeName.FLOAT -> Double
        //case LegacySQLTypeName.GEOGRAPHY -> Object
      case LegacySQLTypeName.INTEGER -> Long
        //case LegacySQLTypeName.INTERVAL -> Duration
        //case LegacySQLTypeName.JSON ->
      case LegacySQLTypeName.NUMERIC -> BigDecimal
        //case LegacySQLTypeName.RANGE -> Range
        //case LegacySQLTypeName.RECORD
      case LegacySQLTypeName.STRING -> String
      case LegacySQLTypeName.TIMESTAMP -> Instant
      case LegacySQLTypeName.TIME -> LocalTime
      default -> Object
    }
  }

  static Object convertFieldValue(FieldValue fv, LegacySQLTypeName colType) {
    if (fv.isNull()) return null

    if (colType == LegacySQLTypeName.BYTES) {
      try {
        // Preferred: typed accessor
        return fv.getBytesValue()
      } catch (Throwable ignore) {
        // Fallbacks depending on client version/driver
        def v = fv.getValue()
        if (v instanceof byte[]) return (byte[]) v
        if (v instanceof CharSequence) return Base64.decoder.decode(v.toString())
        if (v instanceof ByteBuffer) {
          ByteBuffer bb = (ByteBuffer) v
          byte[] out = new byte[bb.remaining()]
          bb.get(out)
          return out
        }
        throw new IllegalStateException("Unsupported BYTES value type: ${v?.getClass()?.name}")
      }
    }
    convert(fv.getValue(), colType)
  }

  static def convert(Object value, LegacySQLTypeName type) {
    if (value == null) return null
    switch (type) {
      case LegacySQLTypeName.BIGNUMERIC -> ValueConverter.asBigDecimal(value)
      case LegacySQLTypeName.BOOLEAN -> ValueConverter.asBoolean(value)
        //case LegacySQLTypeName.GEOGRAPHY -> Object
      case LegacySQLTypeName.INTEGER -> ValueConverter.asLong(value)
        //case LegacySQLTypeName.INTERVAL -> Duration
        //case LegacySQLTypeName.JSON ->
      case LegacySQLTypeName.NUMERIC -> ValueConverter.asBigDecimal(value)
        //case LegacySQLTypeName.RANGE -> Range
        //case LegacySQLTypeName.RECORD todo: this could probably be converted to a Map
      case LegacySQLTypeName.STRING -> ValueConverter.asString(value)
      case LegacySQLTypeName.TIMESTAMP -> convertToInstant(value)
      case LegacySQLTypeName.TIME ->
        if (value instanceof CharSequence) {
          LocalTime.parse(value.toString(), Bq.bqTimeFormatter)
        } else if (value instanceof Number) {
          long micros = ((Number) value).longValue()
          LocalTime.ofNanoOfDay(micros * 1_000L)
        }
      default -> ValueConverter.asString(value)
    }
  }

  static Instant convertToInstant(Object value) {
    if (value == null) return null
    def epochMillis = (ValueConverter.asBigDecimal(value) * 1000).longValue()
    Instant.ofEpochMilli(epochMillis)
  }
}
