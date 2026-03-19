package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.ToString

import org.apache.avro.LogicalTypes
import org.apache.avro.Schema

@PackageScope
@CompileStatic
@EqualsAndHashCode
@ToString(includeNames = true)
class DecimalAvroSchemaDecl extends AvroSchemaDecl {
  final int precision
  final int scale

  DecimalAvroSchemaDecl(int precision, int scale) {
    this.precision = precision
    this.scale = scale
  }

  @Override
  Map<String, ?> toMap() {
    [kind: 'decimal', precision: precision, scale: scale]
  }

  @Override
  @PackageScope
  Schema toAvroSchema(String defaultName, String namespace) {
    Schema schema = Schema.create(Schema.Type.BYTES)
    LogicalTypes.decimal(precision, scale).addToSchema(schema)
    schema
  }
}
