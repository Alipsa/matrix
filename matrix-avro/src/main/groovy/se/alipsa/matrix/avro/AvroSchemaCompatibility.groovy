package se.alipsa.matrix.avro

import groovy.transform.PackageScope

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

/**
 * Locates the nested value that prevents an otherwise selected Avro schema from being written.
 */
@PackageScope
final class AvroSchemaCompatibility {

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
        if (firstFailure == null) {
          firstFailure = failure
        }
      }
      if (firstFailure != null && firstFailure.path != path) {
        return firstFailure
      }
      new CompatibilityFailure(path, firstFailure == null ? value : firstFailure.value, schema)
    }
    if (schema.logicalType != null) {
      return MatrixAvroWriter.isLeafCompatible(schema, value) ? null : new CompatibilityFailure(path, value, schema)
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
    MatrixAvroWriter.isLeafCompatible(schema, value) ? null : new CompatibilityFailure(path, value, schema)
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
