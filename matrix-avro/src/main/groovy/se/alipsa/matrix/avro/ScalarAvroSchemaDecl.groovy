package se.alipsa.matrix.avro

import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.apache.avro.Schema

/**
 * Avro schema declaration for scalar column values.
 */
@PackageScope
@EqualsAndHashCode
@ToString(includeNames = true)
class ScalarAvroSchemaDecl extends AvroSchemaDecl {

  final AvroScalarTypeDecl scalarType
  ScalarAvroSchemaDecl(AvroScalarTypeDecl scalarType) {
    this.scalarType = scalarType
  }
  @Override
  Map<String, ?> toMap() {
    [kind: 'scalar', scalarType: scalarType.name()]
  }
  @Override
  @PackageScope
  Schema toAvroSchema(String defaultName, String namespace) {
    AvroSchemaUtil.scalarSchema(scalarType)
  }

}
