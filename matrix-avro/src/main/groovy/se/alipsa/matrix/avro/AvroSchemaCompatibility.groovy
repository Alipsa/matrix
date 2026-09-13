package se.alipsa.matrix.avro

import groovy.transform.PackageScope

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

/**
 * Locates the nested value that prevents an otherwise selected Avro schema from being written.
 */
@PackageScope
final class AvroSchemaCompatibility {

  static CompatibilityFailure findIncompatibleValue(Schema schema, Object value, String path,
                                                    Closure<Boolean> compatible) {
    if (value == null) {
      return null
    }
    if (schema.type == Schema.Type.UNION) {
      if (schema.types.any { Schema branch -> compatible.call(branch, value) }) {
        return null
      }
      Schema branch = schema.types.find { Schema candidate -> candidate.type != Schema.Type.NULL }
      return branch == null ? new CompatibilityFailure(path, value) : findIncompatibleValue(branch, value, path, compatible)
    }
    if (schema.logicalType != null) {
      return compatible.call(schema, value) ? null : new CompatibilityFailure(path, value)
    }
    if (schema.type == Schema.Type.ARRAY) {
      return findIncompatibleArrayValue(schema, value, path, compatible)
    }
    if (schema.type == Schema.Type.MAP) {
      return findIncompatibleMapValue(schema, value, path, compatible)
    }
    if (schema.type == Schema.Type.RECORD) {
      return findIncompatibleRecordValue(schema, value, path, compatible)
    }
    compatible.call(schema, value) ? null : new CompatibilityFailure(path, value)
  }

  private static CompatibilityFailure findIncompatibleArrayValue(Schema schema, Object value, String path,
                                                                  Closure<Boolean> compatible) {
    if (!List.isInstance(value)) {
      return new CompatibilityFailure(path, value)
    }
    List values = (List) value
    for (int index = 0; index < values.size(); index++) {
      CompatibilityFailure failure = findIncompatibleValue(schema.elementType, values[index], "$path[$index]", compatible)
      if (failure != null) {
        return failure
      }
    }
    null
  }

  private static CompatibilityFailure findIncompatibleMapValue(Schema schema, Object value, String path,
                                                                Closure<Boolean> compatible) {
    if (!Map.isInstance(value)) {
      return new CompatibilityFailure(path, value)
    }
    Map values = (Map) value
    for (Map.Entry entry : values.entrySet()) {
      CompatibilityFailure failure = findIncompatibleValue(schema.valueType, entry.value, "$path['${entry.key}']", compatible)
      if (failure != null) {
        return failure
      }
    }
    null
  }

  private static CompatibilityFailure findIncompatibleRecordValue(Schema schema, Object value, String path,
                                                                   Closure<Boolean> compatible) {
    if (GenericRecord.isInstance(value)) {
      return null
    }
    if (!Map.isInstance(value)) {
      return new CompatibilityFailure(path, value)
    }
    Map values = (Map) value
    for (Schema.Field field : schema.fields) {
      CompatibilityFailure failure = findIncompatibleValue(
          field.schema(), values.get(field.name()), "$path.${field.name()}", compatible
      )
      if (failure != null) {
        return failure
      }
    }
    null
  }

  static final class CompatibilityFailure {

    final String path
    final Object value

    private CompatibilityFailure(String path, Object value) {
      this.path = path
      this.value = value
    }
  }
}
