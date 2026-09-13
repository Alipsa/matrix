package se.alipsa.matrix.avro

import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

import se.alipsa.matrix.avro.exceptions.AvroSchemaException

/**
 * Shared helpers for translating explicit schema declarations to Avro types.
 */
final class AvroSchemaUtil {

  static final String JAVA_TYPE_PROPERTY = 'se.alipsa.matrix.javaType'
  static final String BIG_INTEGER_JAVA_TYPE = 'java.math.BigInteger'

  private AvroSchemaUtil() {
  }
  static Schema nullableSchema(Schema schema) {
    Schema.createUnion([Schema.create(Schema.Type.NULL), schema])
  }
  /**
   * Resolves a non-null union datum using Avro's own branch-selection rules.
   *
   * @param unionSchema an Avro union schema
   * @param datum the non-null datum to resolve
   * @return the selected union branch
   */
  static Schema resolveUnionBranch(Schema unionSchema, Object datum) {
    if (unionSchema.getType() != Schema.Type.UNION) {
      return unionSchema
    }
    unionSchema.getTypes().get(GenericData.get().resolveUnion(unionSchema, datum))
  }

  /**
   * Produces a human-readable label for a schema type, preferring logical types.
   *
   * @param schema schema to label
   * @return uppercase physical type or logical type label
   */
  static String schemaTypeLabel(Schema schema) {
    if (schema.getType() == Schema.Type.UNION) {
      List<String> parts = schema.getTypes().collect { Schema branch -> schemaTypeLabel(branch) }
      return 'UNION[' + String.join(', ', parts) + ']'
    }
    def logical = schema.getLogicalType()
    logical != null ? logical.getName() : schema.getType().name()
  }
  static Schema scalarSchema(AvroScalarTypeDecl scalarType) {
    switch (scalarType) {
      case AvroScalarTypeDecl.STRING -> Schema.create(Schema.Type.STRING)
      case AvroScalarTypeDecl.BOOLEAN -> Schema.create(Schema.Type.BOOLEAN)
      case AvroScalarTypeDecl.INT -> Schema.create(Schema.Type.INT)
      case AvroScalarTypeDecl.LONG -> Schema.create(Schema.Type.LONG)
      case AvroScalarTypeDecl.FLOAT -> Schema.create(Schema.Type.FLOAT)
      case AvroScalarTypeDecl.DOUBLE -> Schema.create(Schema.Type.DOUBLE)
      case AvroScalarTypeDecl.BYTES -> Schema.create(Schema.Type.BYTES)
      case AvroScalarTypeDecl.DATE -> createDateSchema()
      case AvroScalarTypeDecl.TIME_MILLIS -> createTimeMillisSchema()
      case AvroScalarTypeDecl.TIMESTAMP_MILLIS -> createTimestampMillisSchema()
      case AvroScalarTypeDecl.LOCAL_TIMESTAMP_MICROS -> createLocalTimestampMicrosSchema()
      case AvroScalarTypeDecl.UUID -> createUuidSchema()
      default -> throw new IllegalArgumentException("Unsupported Avro scalar type declaration $scalarType")
    }
  }
  static void validateAvroFieldName(String fieldName, String location) {
    if (!isValidAvroName(fieldName)) {
      throw new AvroSchemaException(
          'Invalid Avro field name',
          location,
          'Avro field name (A-Za-z_ followed by A-Za-z0-9_)',
          fieldName
      )
    }
  }
  private static Schema createDateSchema() {
    Schema dateSchema = Schema.create(Schema.Type.INT)
    LogicalTypes.date().addToSchema(dateSchema)
    dateSchema
  }
  private static Schema createTimeMillisSchema() {
    Schema timeSchema = Schema.create(Schema.Type.INT)
    LogicalTypes.timeMillis().addToSchema(timeSchema)
    timeSchema
  }
  private static Schema createTimestampMillisSchema() {
    Schema timestampSchema = Schema.create(Schema.Type.LONG)
    LogicalTypes.timestampMillis().addToSchema(timestampSchema)
    timestampSchema
  }
  private static Schema createLocalTimestampMicrosSchema() {
    Schema localTimestampSchema = Schema.create(Schema.Type.LONG)
    LogicalTypes.localTimestampMicros().addToSchema(localTimestampSchema)
    localTimestampSchema
  }
  private static Schema createUuidSchema() {
    Schema uuidSchema = Schema.create(Schema.Type.STRING)
    LogicalTypes.uuid().addToSchema(uuidSchema)
    uuidSchema
  }
  private static boolean isValidAvroName(String name) {
    name != null && !name.isEmpty() && name ==~ /[A-Za-z_][A-Za-z0-9_]*/
  }

}
