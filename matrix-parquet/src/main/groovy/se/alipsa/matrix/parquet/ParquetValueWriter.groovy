package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.example.data.Group
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type

import java.math.RoundingMode
import java.nio.ByteBuffer
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Writes individual Matrix cell values into Parquet {@link Group} records.
 *
 * <p>Package-internal helper for {@link MatrixParquetWriter}: converts Java values into the
 * primitive, LIST, MAP, and STRUCT representations declared by the Parquet schema, including
 * BigDecimal scale conversion with the configured {@link RoundingMode}.</p>
 *
 * <p>The timezone used to convert {@link LocalDateTime} values to UTC timestamps is pinned per
 * thread via {@link #withZoneId(ZoneId, Closure)} by the enclosing write operation.</p>
 */
@PackageScope
final class ParquetValueWriter {

  /** Parquet schema field name for repeated list entries */
  private static final String FIELD_LIST = 'list'

  /** Parquet schema field name for map key-value pairs */
  private static final String FIELD_KEY_VALUE = 'key_value'

  /** Thread-local storage for timezone used during write operations */
  private static final ThreadLocal<ZoneId> ZONE_ID_HOLDER = new ThreadLocal<>()

  private static final int ERROR_VALUE_MAX_LENGTH = 100
  private static final long NANOS_PER_MILLI = 1_000_000L

  private ParquetValueWriter() {}

  /**
   * Runs the given write action with the supplied timezone pinned for the current thread,
   * restoring the previously pinned timezone (if any) afterwards. The pinned timezone is
   * used when converting {@link LocalDateTime} values to UTC timestamps.
   *
   * @param zoneId the timezone to pin, or null to use the system default
   * @param action the write action to execute
   * @return the action's result
   */
  static <T> T withZoneId(ZoneId zoneId, Closure<T> action) {
    ZoneId previous = ZONE_ID_HOLDER.get()
    ZONE_ID_HOLDER.set(zoneId)
    try {
      action.call()
    } finally {
      if (previous == null) {
        ZONE_ID_HOLDER.remove()
      } else {
        ZONE_ID_HOLDER.set(previous)
      }
    }
  }

  /**
   * Gets the current timezone for timestamp conversion.
   * Returns the thread-local value if set, otherwise the system default.
   * Package scope so schema-time validation ({@link ColumnTypePolicy}) sees the same zone as value writing.
   */
  static ZoneId getZoneId() {
    ZoneId zoneId = ZONE_ID_HOLDER.get()
    return zoneId != null ? zoneId : ZoneId.systemDefault()
  }

  static void writeValue(Group group, String fieldName, Type fieldType, Object value, RoundingMode roundingMode, String path, int row) {
    if (value == null) {
      return
    }
    if (fieldType.isPrimitive()) {
      try { writePrimitiveValue(group, fieldName, fieldType.asPrimitiveType(), value, roundingMode, path, row) }
      catch (ArithmeticException failure) { throw new IllegalArgumentException("Column '$path', row $row: timestamp overflows epoch units", failure) }
      return
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      writeList(group, fieldName, groupType, value, roundingMode, path, row)
    } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      writeMap(group, fieldName, groupType, value, roundingMode, path, row)
    } else {
      writeStruct(group, fieldName, groupType, value, roundingMode, path, row)
    }
  }

  static void writePrimitiveValue(Group group, String fieldName, PrimitiveType field, Object value,
      RoundingMode roundingMode, String path, int row) {
    switch (value.class) {
      case Byte, Short, Integer, int -> group.append(fieldName, (int) value)
      case Long, long -> group.append(fieldName, ((Number) value).longValue())
      case BigInteger -> group.add(fieldName, Binary.fromConstantByteArray(((BigInteger) value).toByteArray()))
      case Float, float -> group.append(fieldName, ((Number) value).floatValue())
      case Double, double -> group.append(fieldName, ((Number) value).doubleValue())
      case BigDecimal -> writeBigDecimalValue(group, fieldName, field, (BigDecimal) value, roundingMode, path, row)
      case Boolean, boolean -> group.append(fieldName, (boolean) value)
      case LocalDate -> group.append(fieldName, ((LocalDate) value).toEpochDay().intValue())
      case java.sql.Date -> group.append(fieldName, ((java.sql.Date) value).toLocalDate().toEpochDay().intValue())
      case Time -> group.append(fieldName, timeToMillis((Time) value))
      case LocalDateTime -> group.append(fieldName, TemporalUnits.epochUnits(((LocalDateTime) value).atZone(getZoneId()).toInstant(), ((LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) field.logicalTypeAnnotation).unit))
      case Timestamp -> group.append(fieldName, TemporalUnits.epochUnits(((Timestamp) value).toInstant(), ((LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) field.logicalTypeAnnotation).unit))
      case byte[] -> group.add(fieldName, Binary.fromConstantByteArray((byte[]) value))
      case LocalTime -> group.append(fieldName, ((LocalTime) value).toNanoOfDay())
      case Instant -> group.append(fieldName, TemporalUnits.epochUnits((Instant) value, ((LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) field.logicalTypeAnnotation).unit))
      case UUID -> {
        UUID uuid = (UUID) value
        group.add(fieldName, Binary.fromConstantByteArray(ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()))
      }
      case String, Character, CharSequence -> group.append(fieldName, value.toString())
      case Enum -> group.append(fieldName, ((Enum) value).name())
      case Date -> group.append(fieldName, ((Date) value).time)
      default -> throw new IllegalArgumentException("Unsupported ${value.getClass().name} for field $path, row $row")
    }
  }

  private static int timeToMillis(Time time) {
    def localTime = Instant.ofEpochMilli(time.time).atZone(ZoneId.systemDefault()).toLocalTime()
    (int) (localTime.toNanoOfDay().intdiv(NANOS_PER_MILLI))
  }

  private static void writeBigDecimalValue(Group group, String fieldName, PrimitiveType field, BigDecimal value,
      RoundingMode roundingMode, String path, int row) {
    def logical = field.getLogicalTypeAnnotation()
    if (field.primitiveTypeName == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY &&
        logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
      writeFixedLenBigDecimal(group, fieldName, field, value,
          (LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) logical, roundingMode, path, row)
    } else {
      throw new IllegalArgumentException("Missing DECIMAL annotation for $path, row $row")
    }
  }

  @SuppressWarnings('ParameterCount') // Encoding context includes logical path and row for nested diagnostics.
  private static void writeFixedLenBigDecimal(Group group, String fieldName, PrimitiveType field, BigDecimal value,
      LogicalTypeAnnotation.DecimalLogicalTypeAnnotation logical, RoundingMode roundingMode, String path, int row) {
    int scale = logical.scale
    int precision = logical.precision
    BigDecimal rescaled
    try {
      rescaled = value.setScale(scale, roundingMode)
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "BigDecimal value '$value' for field '$path', row $row cannot be represented at scale $scale with roundingMode $roundingMode. " +
          'Use an exact value or explicitly select a rounding mode such as HALF_UP.', exception)
    }
    if (rescaled.precision() > precision) {
      throw new IllegalArgumentException(
          "BigDecimal value '$value' for field '$path', row $row exceeds DECIMAL($precision, $scale) after applying roundingMode $roundingMode. " +
          "Rescaled value '$rescaled' has precision ${rescaled.precision()}. Increase the precision or choose an appropriate rounding mode.")
    }
    def unscaled = rescaled.unscaledValue()
    def bytes = unscaled.toByteArray()
    int size = field.typeLength
    if (bytes.length > size) {
      throw new IllegalArgumentException(
          "BigDecimal value '$value' for field '$path', row $row cannot be encoded in DECIMAL($precision, $scale). " +
          "The value requires ${bytes.length} bytes but schema allows only $size bytes " +
          "after applying roundingMode $roundingMode. Increase the precision or use inferPrecisionAndScale=true.")
    }
    def padded = new byte[size]
    if (unscaled.signum() < 0) {
      Arrays.fill(padded, (byte) 0xFF)
    }
    System.arraycopy(bytes, 0, padded, size - bytes.length, bytes.length)
    group.add(fieldName, Binary.fromConstantByteArray(padded))
  }

  private static void writeList(Group group, String fieldName, GroupType groupType, Object value, RoundingMode roundingMode, String path, int row) {
    if (!(value instanceof Collection)) {
      throw new IllegalArgumentException(
          "Cannot write field '$path', row $row as Parquet LIST: expected a Collection (List, Set, etc.) " +
          "but got ${value.class.simpleName}. Value: ${truncateForError(value)}")
    }
    Collection<?> collection = (Collection<?>) value
    Group listGroup = group.addGroup(fieldName)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    collection.each { Object element ->
      Group entry = listGroup.addGroup(FIELD_LIST)
      writeValue(entry, elementType.name, elementType, element, roundingMode, NestedPaths.child(path, 'element'), row)
    }
  }

  private static void writeMap(Group group, String fieldName, GroupType groupType, Object value, RoundingMode roundingMode, String path, int row) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException(
          "Cannot write field '$path', row $row as Parquet MAP: expected a Map " +
          "but got ${value.class.simpleName}. Value: ${truncateForError(value)}")
    }
    Map<?, ?> mapValue = (Map<?, ?>) value
    Group mapGroup = group.addGroup(fieldName)
    GroupType keyValueType = groupType.getType(0).asGroupType()
    PrimitiveType keyPrimitive = keyValueType.getType(0).asPrimitiveType()
    Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
    mapValue.each { Object k, Object v ->
      Group kvGroup = mapGroup.addGroup(FIELD_KEY_VALUE)
      writeValue(kvGroup, keyPrimitive.name, keyPrimitive, k, roundingMode, NestedPaths.child(path, 'key'), row)
      if (valueType != null && v != null) {
        writeValue(kvGroup, valueType.name, valueType, v, roundingMode, NestedPaths.child(path, 'value'), row)
      }
    }
  }

  private static void writeStruct(Group group, String fieldName, GroupType groupType, Object value, RoundingMode roundingMode, String path, int row) {
    Map structValues = (Map) value
    Group structGroup = group.addGroup(fieldName)
    groupType.fields.each { Type field ->
      def childValue = structValues.get(field.name)
      if (childValue != null) {
        writeValue(structGroup, field.name, field, childValue, roundingMode, NestedPaths.child(path, field.name), row)
      }
    }
  }

  /**
   * Truncates a value's string representation for inclusion in error messages.
   * Prevents excessively long error messages from large objects.
   */
  private static String truncateForError(Object value) {
    if (value == null) {
      return 'null'
    }
    String str = String.valueOf(value)
    if (str.length() > ERROR_VALUE_MAX_LENGTH) {
      return str.substring(0, ERROR_VALUE_MAX_LENGTH) + '...'
    }
    return str
  }
}
