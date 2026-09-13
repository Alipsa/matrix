package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.example.data.Group
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.math.RoundingMode
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

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

  /** Cache for PropertyDescriptor lists by class to avoid repeated reflection calls */
  private static final Map<Class<?>, List<PropertyDescriptor>> PROPERTY_DESCRIPTOR_CACHE =
      new ConcurrentHashMap<>()

  private static final int ERROR_VALUE_MAX_LENGTH = 100
  private static final long MICROS_PER_SECOND = 1_000_000L
  private static final long NANOS_PER_MILLI = 1_000_000L
  private static final long NANOS_PER_MICRO = 1_000L

  private ParquetValueWriter() {}

  /**
   * Runs the given write action with the supplied timezone pinned for the current thread,
   * clearing it afterwards. The pinned timezone is used when converting
   * {@link LocalDateTime} values to UTC timestamps.
   *
   * @param zoneId the timezone to pin, or null to use the system default
   * @param action the write action to execute
   * @return the action's result
   */
  static <T> T withZoneId(ZoneId zoneId, Closure<T> action) {
    ZONE_ID_HOLDER.set(zoneId)
    try {
      action.call()
    } finally {
      ZONE_ID_HOLDER.remove()
    }
  }

  /**
   * Gets the current timezone for timestamp conversion.
   * Returns the thread-local value if set, otherwise the system default.
   */
  private static ZoneId getZoneId() {
    ZoneId zoneId = ZONE_ID_HOLDER.get()
    return zoneId != null ? zoneId : ZoneId.systemDefault()
  }

  /**
   * Gets cached PropertyDescriptors for the given class.
   * Uses Introspector to get bean info and caches the result for subsequent calls.
   */
  static List<PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) {
    return PROPERTY_DESCRIPTOR_CACHE.computeIfAbsent(clazz) { c ->
      def beanInfo = Introspector.getBeanInfo(c, Object)
      return beanInfo.propertyDescriptors.findAll { it.readMethod != null }
    }
  }

  static void writeValue(Group group, String fieldName, Type fieldType, Object value, RoundingMode roundingMode) {
    if (value == null) {
      return
    }
    if (fieldType.isPrimitive()) {
      writePrimitiveValue(group, fieldName, fieldType.asPrimitiveType(), value, roundingMode)
      return
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      writeList(group, fieldName, groupType, value, roundingMode)
    } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      writeMap(group, fieldName, groupType, value, roundingMode)
    } else {
      writeStruct(group, fieldName, groupType, value, roundingMode)
    }
  }

  static void writePrimitiveValue(Group group, String fieldName, PrimitiveType field, Object value,
      RoundingMode roundingMode) {
    switch (value.class) {
      case Integer, int -> group.append(fieldName, (int) value)
      case Long, long -> group.append(fieldName, ((Number) value).longValue())
      case BigInteger -> group.add(fieldName, Binary.fromConstantByteArray(((BigInteger) value).toByteArray()))
      case Float, float -> group.append(fieldName, ((Number) value).floatValue())
      case Double, double -> group.append(fieldName, ((Number) value).doubleValue())
      case BigDecimal -> writeBigDecimalValue(group, fieldName, field, (BigDecimal) value, roundingMode)
      case Boolean, boolean -> group.append(fieldName, (boolean) value)
      case LocalDate -> group.append(fieldName, ((LocalDate) value).toEpochDay().intValue())
      case java.sql.Date -> group.append(fieldName, ((java.sql.Date) value).toLocalDate().toEpochDay().intValue())
      case Time -> group.append(fieldName, timeToMillis((Time) value))
      case LocalDateTime -> group.append(fieldName, instantToMicros(((LocalDateTime) value).atZone(getZoneId()).toInstant()))
      case Timestamp -> group.append(fieldName, instantToMicros(((Timestamp) value).toInstant()))
      case Date -> group.append(fieldName, ((Date) value).time)
      default -> group.append(fieldName, value.toString())
    }
  }

  private static int timeToMillis(Time time) {
    def localTime = Instant.ofEpochMilli(time.time).atZone(ZoneId.systemDefault()).toLocalTime()
    (int) (localTime.toNanoOfDay() / NANOS_PER_MILLI)
  }

  private static long instantToMicros(Instant instant) {
    instant.epochSecond * MICROS_PER_SECOND + (long) (instant.nano / NANOS_PER_MICRO)
  }

  private static void writeBigDecimalValue(Group group, String fieldName, PrimitiveType field, BigDecimal value,
      RoundingMode roundingMode) {
    def logical = field.getLogicalTypeAnnotation()
    if (field.primitiveTypeName == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY &&
        logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
      writeFixedLenBigDecimal(group, fieldName, field, value,
          (LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) logical, roundingMode)
    } else {
      group.append(fieldName, value.doubleValue())
    }
  }

  private static void writeFixedLenBigDecimal(Group group, String fieldName, PrimitiveType field, BigDecimal value,
      LogicalTypeAnnotation.DecimalLogicalTypeAnnotation logical, RoundingMode roundingMode) {
    int scale = logical.scale
    int precision = logical.precision
    BigDecimal rescaled
    try {
      rescaled = value.setScale(scale, roundingMode)
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "BigDecimal value '$value' for field '$fieldName' cannot be represented at scale $scale with roundingMode $roundingMode. " +
          'Use an exact value or explicitly select a rounding mode such as HALF_UP.', exception)
    }
    if (rescaled.precision() > precision) {
      throw new IllegalArgumentException(
          "BigDecimal value '$value' for field '$fieldName' exceeds DECIMAL($precision, $scale) after applying roundingMode $roundingMode. " +
          "Rescaled value '$rescaled' has precision ${rescaled.precision()}. Increase the precision or choose an appropriate rounding mode.")
    }
    def unscaled = rescaled.unscaledValue()
    def bytes = unscaled.toByteArray()
    int size = field.typeLength
    if (bytes.length > size) {
      throw new IllegalArgumentException(
          "BigDecimal value '$value' for field '$fieldName' cannot be encoded in DECIMAL($precision, $scale). " +
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

  private static void writeList(Group group, String fieldName, GroupType groupType, Object value, RoundingMode roundingMode) {
    if (!(value instanceof Collection)) {
      throw new IllegalArgumentException(
          "Cannot write field '$fieldName' as Parquet LIST: expected a Collection (List, Set, etc.) " +
          "but got ${value.class.simpleName}. Value: ${truncateForError(value)}")
    }
    Collection<?> collection = (Collection<?>) value
    Group listGroup = group.addGroup(fieldName)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    collection.each { Object element ->
      Group entry = listGroup.addGroup(FIELD_LIST)
      writeValue(entry, elementType.name, elementType, element, roundingMode)
    }
  }

  private static void writeMap(Group group, String fieldName, GroupType groupType, Object value, RoundingMode roundingMode) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException(
          "Cannot write field '$fieldName' as Parquet MAP: expected a Map " +
          "but got ${value.class.simpleName}. Value: ${truncateForError(value)}")
    }
    Map<?, ?> mapValue = (Map<?, ?>) value
    Group mapGroup = group.addGroup(fieldName)
    GroupType keyValueType = groupType.getType(0).asGroupType()
    PrimitiveType keyPrimitive = keyValueType.getType(0).asPrimitiveType()
    Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
    mapValue.each { Object k, Object v ->
      Group kvGroup = mapGroup.addGroup(FIELD_KEY_VALUE)
      writePrimitiveValue(kvGroup, keyPrimitive.name, keyPrimitive, k, roundingMode)
      if (valueType != null && v != null) {
        writeValue(kvGroup, valueType.name, valueType, v, roundingMode)
      }
    }
  }

  private static void writeStruct(Group group, String fieldName, GroupType groupType, Object value, RoundingMode roundingMode) {
    Map<String, Object> structValues = toStructMap(value)
    Group structGroup = group.addGroup(fieldName)
    groupType.fields.each { Type field ->
      def childValue = structValues.get(field.name)
      if (childValue != null) {
        writeValue(structGroup, field.name, field, childValue, roundingMode)
      }
    }
  }

  private static Map<String, Object> toStructMap(Object value) {
    if (value instanceof Map mapValue) {
      Map<String, Object> result = [:]
      mapValue.each { k, v -> result[String.valueOf(k)] = v }
      return result
    }
    Map<String, Object> map = [:]
    getPropertyDescriptors(value.class).each { PropertyDescriptor pd ->
      def method = pd.readMethod
      if (!method.accessible) {
        method.accessible = true
      }
      map[pd.name] = method.invoke(value)
    }
    return map
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
