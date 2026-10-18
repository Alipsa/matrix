package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.example.data.Group
import org.apache.parquet.schema.*
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName

/** Test-only baseline for the former Group materialization path. */
class GroupValueReader {
  private static final String FIELD_LIST = 'list'
  private static final String FIELD_KEY_VALUE = 'key_value'
  @PackageScope
  static Object readValue(Group group, String fieldName, Type fieldType, Class expectedType) {
    if (group.getFieldRepetitionCount(fieldName) == 0) {
      return null
    }

    if (fieldType.isPrimitive()) {
      return readPrimitive(group, fieldName, fieldType.asPrimitiveType(), expectedType)
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      readList(group, fieldName, groupType)
    } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      readMap(group, fieldName, groupType)
    } else {
      readStruct(group, fieldName, groupType)
    }
  }

  private static Object readPrimitive(Group group, String fieldName, PrimitiveType fieldType, Class expectedType) {
    Object raw = switch (fieldType.primitiveTypeName) {
      case PrimitiveTypeName.INT32 -> group.getInteger(fieldName, 0)
      case PrimitiveTypeName.INT64 -> group.getLong(fieldName, 0)
      case PrimitiveTypeName.FLOAT -> group.getFloat(fieldName, 0)
      case PrimitiveTypeName.DOUBLE -> group.getDouble(fieldName, 0)
      case PrimitiveTypeName.BOOLEAN -> group.getBoolean(fieldName, 0)
      case PrimitiveTypeName.BINARY, PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY, PrimitiveTypeName.INT96 -> group.getBinary(fieldName, 0)
      default -> throw new IllegalArgumentException("Unsupported ${fieldType.primitiveTypeName} field $fieldName")
    }
    MatrixParquetReader.decodePrimitive(raw, fieldType, expectedType, false)
  }

  private static List readList(Group group, String fieldName, GroupType groupType) {
    List result = []
    Group listGroup = group.getGroup(fieldName, 0)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    int elementCount = listGroup.getFieldRepetitionCount(FIELD_LIST)
    for (int j = 0; j < elementCount; j++) {
      Group elementGroup = listGroup.getGroup(FIELD_LIST, j)
      def element = readValue(elementGroup, elementType.name, elementType, null)
      result << element
    }
    result
  }

  private static Map readMap(Group group, String fieldName, GroupType groupType) {
    Map result = [:]
    Group mapGroup = group.getGroup(fieldName, 0)
    GroupType keyValueType = groupType.getType(0).asGroupType()
    Type keyType = keyValueType.getType(0)
    Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
    int keyValueCount = mapGroup.getFieldRepetitionCount(FIELD_KEY_VALUE)
    for (int j = 0; j < keyValueCount; j++) {
      Group kvGroup = mapGroup.getGroup(FIELD_KEY_VALUE, j)
      def key = readValue(kvGroup, keyType.name, keyType, null)
      def value = valueType == null ? null : readValue(kvGroup, valueType.name, valueType, null)
      result[key] = value
    }
    result
  }

  private static Map readStruct(Group group, String fieldName, GroupType groupType) {
    if (groupType.fields.isEmpty()) {
      [:]
    } else {
      Group structGroup = group.getGroup(fieldName, 0)
      Map<String, Object> result = [:]
      groupType.fields.each { Type field ->
        def value = readValue(structGroup, field.name, field, null)
        result[field.name] = value
      }
      result
    }
  }
}
