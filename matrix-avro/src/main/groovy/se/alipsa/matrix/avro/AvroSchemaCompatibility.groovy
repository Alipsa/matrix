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
      for (Schema candidate : schema.types) {
        if (findIncompatibleValue(candidate, value, path, compatible) == null) {
          return null
        }
      }
      Schema branch = schema.types.find { Schema candidate -> candidate.type != Schema.Type.NULL }
      if (branch == null) {
        return new CompatibilityFailure(path, value, schema)
      }
      CompatibilityFailure failure = findIncompatibleValue(branch, value, path, compatible)
      if (failure != null && failure.path?.toString() != path) {
        return failure
      }
      new CompatibilityFailure(path, failure?.value ?: value, schema)
    }
    if (schema.logicalType != null) {
      return compatible.call(schema, value) ? null : new CompatibilityFailure(path, value, schema)
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
    compatible.call(schema, value) ? null : new CompatibilityFailure(path, value, schema)
  }

  private static CompatibilityFailure findIncompatibleArrayValue(Schema schema, Object value, String path,
                                                                  Closure<Boolean> compatible) {
    if (!List.isInstance(value)) {
      return new CompatibilityFailure(path, value, schema)
    }
    List values = (List) value
    for (int index = 0; index < values.size(); index++) {
      String elementPath = path == null ? null : "$path[$index]"
      CompatibilityFailure failure = findIncompatibleValue(schema.elementType, values[index], elementPath, compatible)
      if (failure != null) {
        return failure
      }
    }
    null
  }

  private static CompatibilityFailure findIncompatibleMapValue(Schema schema, Object value, String path,
                                                                Closure<Boolean> compatible) {
    if (!Map.isInstance(value)) {
      return new CompatibilityFailure(path, value, schema)
    }
    Map values = (Map) value
    for (Map.Entry entry : values.entrySet()) {
      String valuePath = path == null ? null : "$path['${entry.key}']"
      CompatibilityFailure failure = findIncompatibleValue(schema.valueType, entry.value, valuePath, compatible)
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
      return new CompatibilityFailure(path, value, schema)
    }
    Map values = (Map) value
    for (Schema.Field field : schema.fields) {
      String fieldPath = path == null ? null : "$path.${field.name()}"
      CompatibilityFailure failure = findIncompatibleValue(field.schema(), values.get(field.name()), fieldPath, compatible)
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
