package test.alipsa.matrix.avro

import groovy.transform.PackageScope

import org.apache.avro.Schema

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
}
