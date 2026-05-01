package se.alipsa.matrix.avro

import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.apache.avro.Schema

/**
 * Avro schema declaration for array columns.
 */
@PackageScope
@EqualsAndHashCode
@ToString(includeNames = true)
class ArrayAvroSchemaDecl extends AvroSchemaDecl {

  final AvroSchemaDecl elementType
  ArrayAvroSchemaDecl(AvroSchemaDecl elementType) {
    this.elementType = elementType
  }
  @Override
  Map<String, ?> toMap() {
    [kind: 'array', elementType: elementType.toMap()]
  }
  @Override
  @PackageScope
  Schema toAvroSchema(String defaultName, String namespace) {
    Schema elementSchema = elementType.toAvroSchema("${defaultName}_item", namespace)
    Schema.createArray(AvroSchemaUtil.nullableSchema(elementSchema))
  }

}
