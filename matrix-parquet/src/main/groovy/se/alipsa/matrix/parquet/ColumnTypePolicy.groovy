package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import java.lang.reflect.Modifier
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Validated encoding and metadata types shared by schema construction and metadata emission. */
@PackageScope
final class ColumnTypePolicy {

  static final List<Class> SUPPORTED = [String, Integer, Long, Float, Double, Boolean, Byte, Short,
      BigInteger, BigDecimal, Character, byte[], LocalDate, LocalDateTime, LocalTime, Instant,
      UUID, Date, java.sql.Date, Time, Timestamp, List, Map]
  static final Map<String, Class> METADATA_TYPES = SUPPORTED.collectEntries { Class type -> [(type.name): type] }

  final Class encoding
  final Class metadata

  private ColumnTypePolicy(Class encoding, Class metadata) {
    this.encoding = encoding
    this.metadata = metadata
  }

  static ColumnTypePolicy resolve(String name, List<?> values, Class declaration, org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit unit = org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit.MICROS) {
    Class declared = box(declaration ?: Object)
    boolean infer = declared == Object || declared.interface || Modifier.isAbstract(declared.modifiers)
    Class policy = null
    values.eachWithIndex { value, int row ->
      if (value == null) {
        return
      }
      if (!declared.isInstance(value) && !(declared == String && value instanceof CharSequence)) {
        throw new IllegalArgumentException("Column '$name', row $row: declared ${declared.name}, got ${value.getClass().name}")
      }
      if (value instanceof Instant) {
        try { TemporalUnits.epochUnits(value, unit) }
        catch (ArithmeticException failure) { throw new IllegalArgumentException("Column '$name', row $row: Instant overflows epoch $unit", failure) }
      } else if (value instanceof LocalDateTime) {
        try { TemporalUnits.epochUnits(((LocalDateTime) value).atZone(ParquetValueWriter.getZoneId()).toInstant(), unit) }
        catch (ArithmeticException failure) { throw new IllegalArgumentException("Column '$name', row $row: LocalDateTime overflows epoch $unit", failure) }
      } else if (value instanceof Timestamp) {
        try { TemporalUnits.epochUnits(((Timestamp) value).toInstant(), unit) }
        catch (ArithmeticException failure) { throw new IllegalArgumentException("Column '$name', row $row: Timestamp overflows epoch $unit", failure) }
      }
      Class current = policyType(value.getClass())
      if (current == null || (policy != null && policy != current)) {
        throw new IllegalArgumentException("Column '$name', row $row: unsupported/mixed ${value.getClass().name}; expected ${policy?.name ?: 'supported encoding'}")
      }
      policy = current
    }
    if (policy == null) {
      if (infer && !(declared in [List, Map])) {
        if (!declared.isAssignableFrom(String)) {
          throw new IllegalArgumentException("All-null column '$name' declared ${declared.name} requires explicit retyping")
        }
        policy = String
      } else {
        policy = policyType(declared)
      }
    }
    if (policy == null) {
      throw new IllegalArgumentException("Unsupported type ${declared.name} for column '$name'; convert explicitly")
    }
    Class metadata = infer || policy in [String, List, Map] ? policy : declared
    if (!METADATA_TYPES.containsKey(metadata.name)) {
      throw new IllegalArgumentException("Unsupported declared type ${declared.name} for column '$name'")
    }
    new ColumnTypePolicy(policy, metadata)
  }

  static Class policyType(Class type) {
    if (type == Character || Enum.isAssignableFrom(type) || CharSequence.isAssignableFrom(type)) {
      return String
    }
    if (Collection.isAssignableFrom(type)) {
      return List
    }
    if (Map.isAssignableFrom(type)) {
      return Map
    }
    SUPPORTED.contains(type) ? type : null
  }

  static Class box(Class type) {
    if (!type.primitive) {
      return type
    }
    [ (int): Integer, (long): Long, (float): Float, (double): Double,
      (boolean): Boolean, (byte): Byte, (short): Short, (char): Character ][type]
  }

  static int[] decimal(List<?> values) {
    int scale = 0
    int left = 1
    values.each { value ->
      if (value instanceof BigDecimal) {
        scale = Math.max(scale, value.scale())
        left = Math.max(left, value.precision() - value.scale())
      }
    }
    [left + scale, scale] as int[]
  }
}
