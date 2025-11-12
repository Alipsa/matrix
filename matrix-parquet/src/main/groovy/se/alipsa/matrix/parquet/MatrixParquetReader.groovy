package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type
import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MatrixParquetReader {

  /**
   * Read the Parquet file into a {@link Matrix} using the supplied name.
   *
   * @param file the Parquet file to read
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the file contents
   */
  static Matrix read(File file, String matrixName) {
    read(file).withMatrixName(matrixName)
  }

  /**
   * Read the Parquet file into a {@link Matrix}, reconstructing nested LIST/MAP/STRUCT values.
   *
   * @param file the Parquet file to read
   * @return a matrix populated with the file contents
   */
  static Matrix read(File file) {
    def path = new Path(file.toURI())
    def conf = new Configuration()
    // Open Parquet file metadata
    def footer = ParquetFileReader.readFooter(conf, path)
    def keyValueMetaData = footer.getFileMetaData().getKeyValueMetaData()
    def typeString = keyValueMetaData.get("matrix.columnTypes")

    //println("typeString: $typeString")
    List<Class> fieldTypes
    if (typeString != null) {
      fieldTypes = parseTypeString(typeString)
    }

    def reader = ParquetReader.builder(new GroupReadSupport(), path).build()

    String matrixName
    if (file.name.contains('.')) {
      matrixName = file.name.substring(0, file.name.lastIndexOf('.'))
    } else {
      matrixName = file.name
    }

    Group row = reader.read()
    if (row == null) {
      return Matrix.builder(matrixName).build()
    }

    def schema = row.getType()
    List<String> fieldNames = schema.fields.collect { it.name }
    if (fieldTypes == null) {
      fieldTypes = extractFieldTypes(schema)
    }

    def builder = Matrix.builder(matrixName)
    .columnNames(fieldNames)
    .types(fieldTypes)

    while (row != null) {
      def rowData = []
      fieldNames.eachWithIndex { name, i ->
        def type = fieldTypes[i]
        def field = schema.getType(name)
        def value = readValue(row, name, field, type)
        rowData << value
      }
      builder.addRow(rowData as Object[])
      row = reader.read()
    }
    return builder.build()
  }

  static List<Class> parseTypeString(String typeString) {
    return typeString.split(',').collect { className ->
      return Class.forName(className.trim())
    }
  }

  private static Class getJavaType(PrimitiveType.PrimitiveTypeName typeName) {
    switch (typeName) {
      case PrimitiveTypeName.INT32: return Integer
      case PrimitiveTypeName.INT64: return Long
      case PrimitiveTypeName.FLOAT: return Float
      case PrimitiveTypeName.DOUBLE: return Double
      case PrimitiveTypeName.BOOLEAN: return Boolean
      default: return String
    }
  }

  static List<Class> extractFieldTypes(GroupType schema) {
    schema.fields.collect { field ->
      def logical = field.getLogicalTypeAnnotation()
      if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
        return List
      }
      if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
        return Map
      }

      if (!field.isPrimitive()) {
        return Map
      }

      def primitive = field.asPrimitiveType().primitiveTypeName

      if (logical != null) {
        if (logical == LogicalTypeAnnotation.dateType()) return LocalDate
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) return LocalDateTime
        if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) return BigDecimal
      }

      return getJavaType(primitive)
    }
  }

  private static Object readValue(Group group, String fieldName, Type fieldType, Class expectedType) {
    if (group.getFieldRepetitionCount(fieldName) == 0) {
      return null
    }

    if (fieldType.isPrimitive()) {
      return readPrimitive(group, fieldName, fieldType.asPrimitiveType(), expectedType)
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      return readList(group, fieldName, groupType)
    }
    if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      return readMap(group, fieldName, groupType)
    }
    return readStruct(group, fieldName, groupType)
  }

  private static Object readPrimitive(Group group, String fieldName, PrimitiveType fieldType, Class expectedType) {
    def logical = fieldType.logicalTypeAnnotation
    switch (fieldType.primitiveTypeName) {
      case PrimitiveTypeName.INT32:
        if (logical == LogicalTypeAnnotation.dateType()) {
          return LocalDate.ofEpochDay(group.getInteger(fieldName, 0))
        }
        if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
          return Time.valueOf(LocalTime.ofSecondOfDay(group.getInteger(fieldName, 0)))
        }
        if (expectedType == java.sql.Date) {
          return java.sql.Date.valueOf(LocalDate.ofEpochDay(group.getInteger(fieldName, 0)))
        }
        return group.getInteger(fieldName, 0)
      case PrimitiveTypeName.INT64:
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
          def micros = group.getLong(fieldName, 0)
          def millis = (long) (micros / 1000)
          return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        }
        def longValue = group.getLong(fieldName, 0)
        if (expectedType == BigInteger) {
          return BigInteger.valueOf(longValue)
        }
        return longValue
      case PrimitiveTypeName.FLOAT:
        return group.getFloat(fieldName, 0)
      case PrimitiveTypeName.DOUBLE:
        return group.getDouble(fieldName, 0)
      case PrimitiveTypeName.BOOLEAN:
        return group.getBoolean(fieldName, 0)
      case PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY:
      case PrimitiveTypeName.BINARY:
        if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
          def scale = logical.scale
          def binary = group.getBinary(fieldName, 0)
          def unscaled = new BigInteger(binary.getBytes())
          return new BigDecimal(unscaled, scale)
        }
        if (expectedType == BigDecimal) {
          return BigDecimal.valueOf(group.getDouble(fieldName, 0))
        }
        return group.getBinary(fieldName, 0).toStringUsingUTF8()
      default:
        return group.getString(fieldName, 0)
    }
  }

  private static List readList(Group group, String fieldName, GroupType groupType) {
    List result = []
    Group listGroup = group.getGroup(fieldName, 0)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    int elementCount = listGroup.getFieldRepetitionCount('list')
    for (int j = 0; j < elementCount; j++) {
      Group elementGroup = listGroup.getGroup('list', j)
      def element = readValue(elementGroup, elementType.name, elementType, null)
      result << element
    }
    return result
  }

  private static Map readMap(Group group, String fieldName, GroupType groupType) {
    LinkedHashMap result = new LinkedHashMap()
    int repetition = group.getFieldRepetitionCount(fieldName)
    for (int i = 0; i < repetition; i++) {
      Group mapGroup = group.getGroup(fieldName, i)
      GroupType keyValueType = groupType.getType(0).asGroupType()
      Type keyType = keyValueType.getType(0)
      Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
      int keyValueCount = mapGroup.getFieldRepetitionCount('key_value')
      for (int j = 0; j < keyValueCount; j++) {
        Group kvGroup = mapGroup.getGroup('key_value', j)
        def key = readValue(kvGroup, keyType.name, keyType, null)
        def value = valueType == null ? null : readValue(kvGroup, valueType.name, valueType, null)
        result[key] = value
      }
    }
    return result
  }

  private static Map readStruct(Group group, String fieldName, GroupType groupType) {
    if (groupType.fields.isEmpty()) {
      return [:]
    }
    Group structGroup = group.getGroup(fieldName, 0)
    LinkedHashMap<String, Object> result = new LinkedHashMap<>()
    groupType.fields.each { Type field ->
      def value = readValue(structGroup, field.name, field, null)
      result[field.name] = value
    }
    return result
  }
}
