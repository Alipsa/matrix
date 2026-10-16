package test.alipsa.matrix.avro

import groovy.transform.PackageScope

import org.apache.avro.Schema

import se.alipsa.matrix.avro.AvroSchemaCompatibility

/**
 * Shared schema assertions for Matrix Avro tests.
 */
@PackageScope
final class AvroSchemaTestSupport {

  static Schema nonNullFieldSchema(Schema record, String fieldName) {
    nonNullSchema(record.getField(fieldName).schema())
  }

  static Schema nonNullSchema(Schema schema) {
    schema.type == Schema.Type.UNION
        ? schema.types.find { Schema type -> type.type != Schema.Type.NULL }
        : schema
  }
  static boolean isCompatible(Schema schema, Object value) {
    def method = AvroSchemaCompatibility
        .getDeclaredMethod('isCompatible', Schema, Object)
    method.accessible = true
    method.invoke(null, schema, value) as boolean
  }
}
