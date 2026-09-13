package se.alipsa.matrix.avro

import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.ToString

import org.apache.avro.LogicalTypes
import org.apache.avro.Schema

/**
 * Avro schema declaration for lossless {@link BigInteger} values.
 */
@PackageScope
@EqualsAndHashCode
@ToString(includeNames = true)
class BigIntegerAvroSchemaDecl extends AvroSchemaDecl {

  final int precision

  BigIntegerAvroSchemaDecl(int precision) {
    this.precision = precision
  }

  @Override
  Map<String, ?> toMap() {
    [kind: 'bigInteger', precision: precision]
  }

  @Override
  @PackageScope
  Schema toAvroSchema(String defaultName, String namespace) {
    Schema schema = Schema.create(Schema.Type.BYTES)
    LogicalTypes.decimal(precision, 0).addToSchema(schema)
    schema.addProp(AvroSchemaUtil.JAVA_TYPE_PROPERTY, AvroSchemaUtil.BIG_INTEGER_JAVA_TYPE)
    schema
  }
}
