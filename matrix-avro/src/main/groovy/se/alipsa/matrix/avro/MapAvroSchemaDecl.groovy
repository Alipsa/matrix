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
class MapAvroSchemaDecl extends AvroSchemaDecl {
  final AvroSchemaDecl valueType

  MapAvroSchemaDecl(AvroSchemaDecl valueType) {
    this.valueType = valueType
  }

  @Override
  Map<String, ?> toMap() {
    [kind: 'map', valueType: valueType.toMap()]
  }

  @Override
  @PackageScope
  Schema toAvroSchema(String defaultName, String namespace) {
    Schema valueSchema = valueType.toAvroSchema("${defaultName}_value", namespace)
    Schema.createMap(AvroSchemaUtil.nullableSchema(valueSchema))
  }
}
