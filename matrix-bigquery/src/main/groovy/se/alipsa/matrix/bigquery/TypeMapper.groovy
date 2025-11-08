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
import java.util.Date

@CompileStatic
class TypeMapper {

  /**
   * Translate a Java/Groovy type into the closest matching BigQuery standard SQL type.
   *
   * @param cls the runtime class that should be mapped
   * @return the {@link StandardSQLTypeName} that can represent the provided class
   */
  static StandardSQLTypeName toStandardSqlType(Class cls) {
    switch (cls) {
      case BigDecimal:
        return StandardSQLTypeName.BIGNUMERIC
      case BigInteger:
      case Integer:
      case Long:
      case Short:
        return StandardSQLTypeName.INT64
      case Boolean:
        return StandardSQLTypeName.BOOL
      case Byte:
        return StandardSQLTypeName.INT64
      case Byte[]:
      case byte[]:
        return StandardSQLTypeName.BYTES
      case Double:
      case Float:
        return StandardSQLTypeName.FLOAT64
      case LocalTime:
      case Time:
        return StandardSQLTypeName.TIME
      case LocalDate:
      case Date:
        return StandardSQLTypeName.DATE
      case LocalDateTime:
        return StandardSQLTypeName.DATETIME
      case String:
        return StandardSQLTypeName.STRING
      case Timestamp:
      case Instant:
        return StandardSQLTypeName.TIMESTAMP
      case ZonedDateTime: // There is no timezone aware type in BigQuery so zoned date-times are stored as strings.
        return StandardSQLTypeName.STRING
      default:
        return StandardSQLTypeName.STRING
    }
  }

  /**
   * Resolve the local runtime class that corresponds to a BigQuery legacy SQL type.
   *
   * @param typeName the BigQuery legacy SQL type
   * @return the local JVM class that should be used when materialising field values
   */
  static Class convertType(LegacySQLTypeName typeName) {
    switch (typeName) {
      case LegacySQLTypeName.BIGNUMERIC:
        return BigDecimal
      case LegacySQLTypeName.BOOLEAN:
        return Boolean
      case LegacySQLTypeName.BYTES:
        return byte[]
      case LegacySQLTypeName.DATE:
        return LocalDate
      case LegacySQLTypeName.DATETIME:
        return LocalDateTime
      case LegacySQLTypeName.FLOAT:
        return Double
      //case LegacySQLTypeName.GEOGRAPHY -> Object
      case LegacySQLTypeName.INTEGER:
        return Long
      //case LegacySQLTypeName.INTERVAL -> Duration
      //case LegacySQLTypeName.JSON ->
      case LegacySQLTypeName.NUMERIC:
        return BigDecimal
      //case LegacySQLTypeName.RANGE -> Range
      //case LegacySQLTypeName.RECORD
      case LegacySQLTypeName.STRING:
        return String
      case LegacySQLTypeName.TIMESTAMP:
        return Instant
      case LegacySQLTypeName.TIME:
        return LocalTime
      default:
        return Object
    }
  }

  /**
   * Convert a BigQuery {@link FieldValue} into a JVM value using the provided column type.
   *
   * @param fv the field value returned from a BigQuery query
   * @param colType the column's legacy SQL type
   * @return a converted value compatible with the Matrix data structures
   */
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

  /**
   * Convert a raw BigQuery value into an appropriate JVM representation.
   *
   * @param value the value to convert
   * @param type the BigQuery legacy SQL type describing the value
   * @return the converted value ready for use in the application
   */
  static def convert(Object value, LegacySQLTypeName type) {
    if (value == null) return null
    switch (type) {
      case LegacySQLTypeName.BIGNUMERIC:
        return ValueConverter.asBigDecimal(value)
      case LegacySQLTypeName.BOOLEAN:
        return ValueConverter.asBoolean(value)
      //case LegacySQLTypeName.GEOGRAPHY -> Object
      case LegacySQLTypeName.INTEGER:
        return ValueConverter.asLong(value)
      //case LegacySQLTypeName.INTERVAL -> Duration
      //case LegacySQLTypeName.JSON ->
      case LegacySQLTypeName.NUMERIC:
        return ValueConverter.asBigDecimal(value)
      //case LegacySQLTypeName.RANGE -> Range
      //case LegacySQLTypeName.RECORD todo: this could probably be converted to a Map
      case LegacySQLTypeName.STRING:
        return ValueConverter.asString(value)
      case LegacySQLTypeName.TIMESTAMP:
        return convertToInstant(value)
      case LegacySQLTypeName.TIME:
        if (value instanceof CharSequence) {
          return LocalTime.parse(value.toString(), Bq.bqTimeFormatter)
        }
        if (value instanceof Number) {
          long micros = ((Number) value).longValue()
          return LocalTime.ofNanoOfDay(micros * 1_000L)
        }
        return ValueConverter.asString(value)
      default:
        return ValueConverter.asString(value)
    }
  }

  static Instant convertToInstant(Object value) {
    if (value == null) return null
    def epochMillis = (ValueConverter.asBigDecimal(value) * 1000).longValue()
    Instant.ofEpochMilli(epochMillis)
  }
}
