package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.FieldValue
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

/**
 * Utility class for mapping between Java types and BigQuery SQL types.
 *
 * <p>This class provides bidirectional type mapping:</p>
 * <ul>
 *   <li>{@link #toStandardSqlType}: Java class → BigQuery StandardSQLTypeName (for schema creation)</li>
 *   <li>{@link #convertType}: BigQuery StandardSQLTypeName → Java class (for query results)</li>
 *   <li>{@link #convertFieldValue}: BigQuery FieldValue → Java object (for data conversion)</li>
 * </ul>
 *
 * <h2>Supported Type Mappings</h2>
 * <table border="1">
 *   <tr><th>Java Type</th><th>BigQuery Type</th><th>Notes</th></tr>
 *   <tr><td>BigDecimal</td><td>BIGNUMERIC</td><td>High-precision decimal</td></tr>
 *   <tr><td>BigInteger, Integer, Long, Short, Byte</td><td>INT64</td><td>64-bit integer</td></tr>
 *   <tr><td>Boolean</td><td>BOOL</td><td></td></tr>
 *   <tr><td>byte[]</td><td>BYTES</td><td>Binary data</td></tr>
 *   <tr><td>Double, Float</td><td>FLOAT64</td><td>64-bit floating point</td></tr>
 *   <tr><td>LocalTime, java.sql.Time</td><td>TIME</td><td>Time without date</td></tr>
 *   <tr><td>Timestamp, Instant</td><td>TIMESTAMP</td><td>Absolute point in time</td></tr>
 *   <tr><td>LocalDate, java.util.Date</td><td>DATE</td><td>Date without time</td></tr>
 *   <tr><td>LocalDateTime</td><td>DATETIME</td><td>Date and time without timezone</td></tr>
 *   <tr><td>String</td><td>STRING</td><td></td></tr>
 *   <tr><td>ZonedDateTime</td><td>STRING</td><td>Stored as ISO-8601 string (BQ has no timezone-aware type)</td></tr>
 * </table>
 *
 * <h2>Unsupported BigQuery Types</h2>
 * <p>The following BigQuery types are not yet supported:</p>
 * <ul>
 *   <li><b>GEOGRAPHY</b>: Geospatial data - would require GeoJSON parsing</li>
 *   <li><b>INTERVAL</b>: Duration type - could map to java.time.Duration</li>
 *   <li><b>JSON</b>: JSON documents - could map to Map or JsonNode</li>
 *   <li><b>RANGE</b>: Range of values - would require custom Range class</li>
 *   <li><b>STRUCT</b>: Nested structures - could map to Map&lt;String, Object&gt;</li>
 * </ul>
 *
 * @see Bq
 */
@CompileStatic
class TypeMapper {

  /**
   * Maps a Java class to a BigQuery StandardSQLTypeName.
   *
   * <p>Used when creating BigQuery table schemas from Matrix column types.
   * Unknown types default to STRING.</p>
   *
   * <h3>Type Mapping</h3>
   * <ul>
   *   <li>BigDecimal → BIGNUMERIC</li>
   *   <li>BigInteger, Integer, Long, Short, Byte → INT64</li>
   *   <li>Boolean → BOOL</li>
   *   <li>byte[] → BYTES</li>
   *   <li>Double, Float → FLOAT64</li>
   *   <li>LocalTime, java.sql.Time → TIME</li>
   *   <li>Timestamp, Instant → TIMESTAMP</li>
   *   <li>LocalDate, java.util.Date → DATE</li>
   *   <li>LocalDateTime → DATETIME</li>
   *   <li>String → STRING</li>
   *   <li>ZonedDateTime → STRING (BigQuery has no timezone-aware type)</li>
   *   <li>Other → STRING (default fallback)</li>
   * </ul>
   *
   * @param cls the Java class to map
   * @return the corresponding BigQuery StandardSQLTypeName
   */
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

  /**
   * Maps a BigQuery StandardSQLTypeName to a Java class.
   *
   * <p>Used when converting BigQuery query results to Matrix column types.
   * Unknown types default to Object.</p>
   *
   * <h3>Type Mapping</h3>
   * <ul>
   *   <li>BIGNUMERIC, NUMERIC → BigDecimal</li>
   *   <li>BOOL → Boolean</li>
   *   <li>BYTES → byte[]</li>
   *   <li>DATE → LocalDate</li>
   *   <li>DATETIME → LocalDateTime</li>
   *   <li>FLOAT64 → Double</li>
   *   <li>INT64 → Long</li>
   *   <li>STRING → String</li>
   *   <li>TIMESTAMP → Instant</li>
   *   <li>TIME → LocalTime</li>
   *   <li>Other → Object (default fallback)</li>
   * </ul>
   *
   * @param typeName the BigQuery StandardSQLTypeName to map
   * @return the corresponding Java class
   */
  static Class convertType(StandardSQLTypeName typeName) {
    switch (typeName) {
      case StandardSQLTypeName.BIGNUMERIC -> BigDecimal
      case StandardSQLTypeName.BOOL -> Boolean
      case StandardSQLTypeName.BYTES -> byte[]
      case StandardSQLTypeName.DATE -> LocalDate
      case StandardSQLTypeName.DATETIME -> LocalDateTime
      case StandardSQLTypeName.FLOAT64 -> Double
        // GEOGRAPHY: Not yet supported - would require GeoJSON parsing
      case StandardSQLTypeName.INT64 -> Long
        // INTERVAL: Not yet supported - could map to java.time.Duration
        // JSON: Not yet supported - could map to Map or JsonNode
      case StandardSQLTypeName.NUMERIC -> BigDecimal
        // RANGE: Not yet supported - would require custom Range class
        // STRUCT: Not yet supported - could map to Map<String, Object>
      case StandardSQLTypeName.STRING -> String
      case StandardSQLTypeName.TIMESTAMP -> Instant
      case StandardSQLTypeName.TIME -> LocalTime
      default -> Object
    }
  }

  /**
   * Converts a BigQuery FieldValue to a Java object based on the column type,
   * using typed accessors for accurate and efficient conversion.
   *
   * @param fv the FieldValue from BigQuery
   * @param colType the StandardSQLTypeName indicating the column's data type
   * @return the converted Java object, or null if the field value is null
   */
  static Object convertFieldValue(FieldValue fv, StandardSQLTypeName colType) {
    if (fv.isNull()) return null

    switch (colType) {
      case StandardSQLTypeName.BYTES -> {
        try {
          return fv.getBytesValue()
        } catch (IllegalStateException | UnsupportedOperationException | ClassCastException ignored) {
          // Fallbacks depending on client version/driver:
          // - IllegalStateException: when FieldValue state doesn't allow getBytesValue()
          // - UnsupportedOperationException: when the method isn't supported by driver version
          // - ClassCastException: when internal type doesn't match expected byte array
          Object v = fv.getValue()
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
      case StandardSQLTypeName.BIGNUMERIC, StandardSQLTypeName.NUMERIC -> fv.getNumericValue()
      case StandardSQLTypeName.BOOL -> fv.getBooleanValue()
      case StandardSQLTypeName.INT64 -> fv.getLongValue()
      case StandardSQLTypeName.FLOAT64 -> fv.getDoubleValue()
      case StandardSQLTypeName.STRING -> fv.getStringValue()
      case StandardSQLTypeName.TIMESTAMP -> fv.getTimestampInstant()
      case StandardSQLTypeName.DATE -> LocalDate.parse(fv.getStringValue())
      case StandardSQLTypeName.DATETIME -> LocalDateTime.parse(fv.getStringValue())
      case StandardSQLTypeName.TIME -> {
        Object v = fv.getValue()
        if (v instanceof CharSequence) {
          LocalTime.parse(v.toString(), Bq.bqTimeFormatter)
        } else if (v instanceof Number) {
          LocalTime.ofNanoOfDay(((Number) v).longValue() * 1_000L)
        }
      }
      default -> fv.getStringValue()
    }
  }

  /**
   * Converts a raw value from BigQuery to the appropriate Java type.
   *
   * <p>This method handles value conversion based on the BigQuery column type when
   * working with raw Object values rather than FieldValue instances.
   * It uses {@link se.alipsa.matrix.core.ValueConverter} for standard conversions and
   * custom logic for time-related types.</p>
   *
   * <h3>Conversion Rules</h3>
   * <ul>
   *   <li>BIGNUMERIC, NUMERIC → BigDecimal via ValueConverter</li>
   *   <li>BOOL → Boolean via ValueConverter</li>
   *   <li>FLOAT64 → Double</li>
   *   <li>INT64 → Long via ValueConverter</li>
   *   <li>STRING → String via ValueConverter</li>
   *   <li>TIMESTAMP → Instant via {@link #convertToInstant} (epoch seconds)</li>
   *   <li>DATE → LocalDate (parsed from ISO string)</li>
   *   <li>DATETIME → LocalDateTime (parsed from ISO string)</li>
   *   <li>TIME → LocalTime (parsed from string or converted from microseconds)</li>
   *   <li>Other types → String via ValueConverter (fallback)</li>
   * </ul>
   *
   * <h3>TIME Conversion Details</h3>
   * <p>BigQuery TIME values can be returned as:</p>
   * <ul>
   *   <li>String format "HH:mm:ss[.SSSSSS]" - parsed directly</li>
   *   <li>Numeric microseconds since midnight - converted to nanoseconds for LocalTime</li>
   * </ul>
   *
   * @param value the raw value from BigQuery
   * @param type the BigQuery column type
   * @return the converted Java object, or null if value is null
   * @see ValueConverter
   */
  static Object convert(Object value, StandardSQLTypeName type) {
    if (value == null) return null
    switch (type) {
      case StandardSQLTypeName.BIGNUMERIC -> ValueConverter.asBigDecimal(value)
      case StandardSQLTypeName.BOOL -> ValueConverter.asBoolean(value)
      case StandardSQLTypeName.FLOAT64 -> value as Double
        // GEOGRAPHY: Not yet supported - would require GeoJSON parsing
      case StandardSQLTypeName.INT64 -> ValueConverter.asLong(value)
        // INTERVAL: Not yet supported - could map to java.time.Duration
        // JSON: Not yet supported - could map to Map or JsonNode
      case StandardSQLTypeName.NUMERIC -> ValueConverter.asBigDecimal(value)
        // RANGE: Not yet supported - would require custom Range class
        // STRUCT: Not yet supported - could map to Map<String, Object>
      case StandardSQLTypeName.STRING -> ValueConverter.asString(value)
      case StandardSQLTypeName.TIMESTAMP -> convertToInstant(value)
      case StandardSQLTypeName.DATE -> LocalDate.parse(ValueConverter.asString(value))
      case StandardSQLTypeName.DATETIME -> LocalDateTime.parse(ValueConverter.asString(value))
      case StandardSQLTypeName.TIME -> {
        if (value instanceof CharSequence) {
          LocalTime.parse(value.toString(), Bq.bqTimeFormatter)
        } else if (value instanceof Number) {
          long micros = ((Number) value).longValue()
          LocalTime.ofNanoOfDay(micros * 1_000L)
        }
      }
      default -> ValueConverter.asString(value)
    }
  }

  /**
   * Converts a BigQuery TIMESTAMP value to a Java Instant.
   *
   * <p>BigQuery TIMESTAMP values are stored as fractional seconds since Unix epoch
   * (1970-01-01 00:00:00 UTC). This method converts them to Java Instant objects.</p>
   *
   * <h3>Conversion Process</h3>
   * <ol>
   *   <li>Convert value to BigDecimal (handles both numeric and string representations)</li>
   *   <li>Multiply by 1000 to convert from seconds to milliseconds</li>
   *   <li>Create Instant from epoch milliseconds</li>
   * </ol>
   *
   * <p><b>Note:</b> Precision is limited to milliseconds. Sub-millisecond precision
   * from BigQuery is truncated.</p>
   *
   * @param value the BigQuery timestamp value (seconds since epoch as decimal)
   * @return Instant representing the timestamp, or null if value is null
   */
  static Instant convertToInstant(Object value) {
    if (value == null) return null
    long epochMillis = (ValueConverter.asBigDecimal(value) * 1000).longValue()
    Instant.ofEpochMilli(epochMillis)
  }
}
