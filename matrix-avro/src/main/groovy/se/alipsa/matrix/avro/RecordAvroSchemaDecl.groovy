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
class RecordAvroSchemaDecl extends AvroSchemaDecl {
  final String recordName
  final Map<String, AvroSchemaDecl> fields

  RecordAvroSchemaDecl(String recordName, Map<String, AvroSchemaDecl> fields) {
    this.recordName = recordName
    this.fields = fields.asImmutable()
  }

  @Override
  Map<String, ?> toMap() {
    Map<String, Object> result = [kind: 'record', fields: AvroSchemaDecl.columnSchemasToMap(fields)]
    if (recordName != null) {
      result.recordName = recordName
    }
    result
  }

  @Override
  @PackageScope
  Schema toAvroSchema(String defaultName, String namespace) {
    String effectiveRecordName = recordName ?: "${defaultName}_record"
    AvroSchemaUtil.validateAvroFieldName(effectiveRecordName, defaultName)
    Schema recordSchema = Schema.createRecord(effectiveRecordName, null, namespace, false)
    List<Schema.Field> recordFields = []
    fields.each { String fieldName, AvroSchemaDecl fieldDecl ->
      AvroSchemaUtil.validateAvroFieldName(fieldName, "${defaultName}.${fieldName}")
      Schema valueSchema = fieldDecl.toAvroSchema("${defaultName}_${fieldName}", namespace)
      recordFields << new Schema.Field(fieldName, AvroSchemaUtil.nullableSchema(valueSchema), null as String, (Object) null)
    }
    recordSchema.setFields(recordFields)
    recordSchema
  }
}
