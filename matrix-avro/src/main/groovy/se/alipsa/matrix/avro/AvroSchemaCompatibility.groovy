package se.alipsa.matrix.avro

import groovy.transform.PackageScope

import org.apache.avro.Schema
import org.apache.avro.generic.GenericFixed
import org.apache.avro.generic.GenericRecord

import java.nio.ByteBuffer
import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Locates the nested value that prevents an otherwise selected Avro schema from being written.
 */
@PackageScope
final class AvroSchemaCompatibility {

  static boolean isCompatible(Schema schema, Object value) {
    findIncompatibleValue(schema, value, null) == null
  }

  static CompatibilityFailure findIncompatibleValue(Schema schema, Object value, String path) {
    if (value == null) {
      return null
    }
    if (schema.type == Schema.Type.UNION) {
      CompatibilityFailure firstFailure = null
      for (Schema candidate : schema.types) {
        if (candidate.type == Schema.Type.NULL) {
          continue
        }
        CompatibilityFailure failure = findIncompatibleValue(candidate, value, path)
        if (failure == null) {
          return null
        }
        if (firstFailure == null || (firstFailure.path == path && failure.path != path)) {
          firstFailure = failure
        }
      }
      if (firstFailure != null && firstFailure.path != path) {
        return firstFailure
      }
      new CompatibilityFailure(path, firstFailure == null ? value : firstFailure.value, schema)
    }
    if (schema.logicalType != null) {
      return isLeafCompatible(schema, value) ? null : new CompatibilityFailure(path, value, schema)
    }
    if (schema.type == Schema.Type.ARRAY) {
      return findIncompatibleArrayValue(schema, value, path)
    }
    if (schema.type == Schema.Type.MAP) {
      return findIncompatibleMapValue(schema, value, path)
    }
    if (schema.type == Schema.Type.RECORD) {
      return findIncompatibleRecordValue(schema, value, path)
    }
    isLeafCompatible(schema, value) ? null : new CompatibilityFailure(path, value, schema)
  }

  private static boolean isLeafCompatible(Schema schema, Object value) {
    def logical = schema.logicalType
    if (logical != null) {
      return isLogicalTypeCompatible(logical.name, value)
    }
    isPlainTypeCompatible(schema, value)
  }

  private static boolean isLogicalTypeCompatible(String name, Object value) {
    switch (name) {
      case 'date' -> LocalDate.isInstance(value) || java.sql.Date.isInstance(value) || Number.isInstance(value)
      case 'time-millis', 'time-micros' -> LocalTime.isInstance(value) || Time.isInstance(value) || Number.isInstance(value)
      case 'timestamp-millis' ->
        Instant.isInstance(value) || Date.isInstance(value) || LocalDateTime.isInstance(value) || Number.isInstance(value)
      case 'timestamp-micros' ->
        Instant.isInstance(value) || Date.isInstance(value) || Number.isInstance(value)
      case 'local-timestamp-millis', 'local-timestamp-micros' -> LocalDateTime.isInstance(value) || Number.isInstance(value)
      case 'uuid' -> UUID.isInstance(value) || String.isInstance(value)
      case 'decimal' -> Number.isInstance(value) || byte[].isInstance(value) || ByteBuffer.isInstance(value)
      default -> false
    }
  }

  private static boolean isPlainTypeCompatible(Schema schema, Object value) {
    switch (schema.type) {
      case Schema.Type.STRING -> true // converted with toString() during writing
      case Schema.Type.BOOLEAN -> Boolean.isInstance(value)
      case Schema.Type.INT -> isExactIntCompatible(value)
      case Schema.Type.LONG -> isExactLongCompatible(value) || Date.isInstance(value) || Instant.isInstance(value)
      case Schema.Type.FLOAT -> Number.isInstance(value)
      case Schema.Type.DOUBLE -> Number.isInstance(value) || BigDecimal.isInstance(value)
      case Schema.Type.BYTES -> byte[].isInstance(value) || ByteBuffer.isInstance(value) || BigDecimal.isInstance(value)
      case Schema.Type.FIXED -> GenericFixed.isInstance(value)
      default -> false
    }
  }

  private static boolean isExactIntCompatible(Object value) {
    if (!Number.isInstance(value)) {
      return false
    }
    if (NumericKinds.isDirectInt(value)) {
      return true
    }
    try {
      decimalValue((Number) value).intValueExact()
      true
    } catch (ArithmeticException ignored) {
      false
    }
  }

  private static boolean isExactLongCompatible(Object value) {
    if (!Number.isInstance(value)) {
      return false
    }
    if (NumericKinds.isDirectLong(value)) {
      return true
    }
    try {
      decimalValue((Number) value).longValueExact()
      true
    } catch (ArithmeticException ignored) {
      false
    }
  }

  private static BigDecimal decimalValue(Number value) {
    if (NumericKinds.isNonFiniteFloating(value)) {
      throw new ArithmeticException('Non-finite number')
    }
    NumericKinds.toBigDecimal(value)
  }

  private static CompatibilityFailure findIncompatibleArrayValue(Schema schema, Object value, String path) {
    if (!List.isInstance(value)) {
      return new CompatibilityFailure(path, value, schema)
    }
    List values = (List) value
    for (int index = 0; index < values.size(); index++) {
      String elementPath = path == null ? null : "$path[$index]"
      CompatibilityFailure failure = findIncompatibleValue(schema.elementType, values[index], elementPath)
      if (failure != null) {
        return failure
      }
    }
    null
  }

  private static CompatibilityFailure findIncompatibleMapValue(Schema schema, Object value, String path) {
    if (!Map.isInstance(value)) {
      return new CompatibilityFailure(path, value, schema)
    }
    Map values = (Map) value
    for (Map.Entry entry : values.entrySet()) {
      String valuePath = path == null ? null : "$path['${entry.key}']"
      CompatibilityFailure failure = findIncompatibleValue(schema.valueType, entry.value, valuePath)
      if (failure != null) {
        return failure
      }
    }
    null
  }

  private static CompatibilityFailure findIncompatibleRecordValue(Schema schema, Object value, String path) {
    if (GenericRecord.isInstance(value)) {
      return null
    }
    if (!Map.isInstance(value)) {
      return new CompatibilityFailure(path, value, schema)
    }
    Map values = (Map) value
    for (Schema.Field field : schema.fields) {
      String fieldPath = path == null ? null : "$path.${field.name()}"
      CompatibilityFailure failure = findIncompatibleValue(field.schema(), values.get(field.name()), fieldPath)
      if (failure != null) {
        return failure
      }
    }
    null
  }

  static final class CompatibilityFailure {

    final String path
    final Object value
    final Schema schema

    private CompatibilityFailure(String path, Object value, Schema schema) {
      this.path = path
      this.value = value
      this.schema = schema
    }
  }
}
