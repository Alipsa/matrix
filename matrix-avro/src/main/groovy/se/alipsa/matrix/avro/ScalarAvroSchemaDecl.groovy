package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.apache.avro.Schema

@PackageScope
@CompileStatic
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
