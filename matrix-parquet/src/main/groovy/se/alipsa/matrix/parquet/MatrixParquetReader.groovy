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
import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MatrixParquetReader {

  static Matrix read(File file) {
    def path = new Path(file.toURI())
    def conf = new Configuration()
    // Open Parquet file metadata
    def footer = ParquetFileReader.readFooter(conf, path)
    def keyValueMetaData = footer.getFileMetaData().getKeyValueMetaData()
    def typeString = keyValueMetaData.get("matrix.columnTypes")

    println("typeString: $typeString")
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
        def value = null

        if (row.getFieldRepetitionCount(name) > 0) {
          switch (type) {
            case Integer     -> value = row.getInteger(name, 0)
            case Long        -> value = row.getLong(name, 0)
            case Float       -> value = row.getFloat(name, 0)
            case Double      -> value = row.getDouble(name, 0)
            case Boolean     -> value = row.getBoolean(name, 0)
            case BigDecimal  -> {
              def field = schema.getType(name)
              def logical = field.getLogicalTypeAnnotation()
              if (field.asPrimitiveType().primitiveTypeName == PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY &&
                  logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
                def scale = logical.scale
                def binary = row.getBinary(name, 0)
                def unscaled = new BigInteger(binary.getBytes())
                value = new BigDecimal(unscaled, scale)
              } else {
                // fallback (when not using inferredType i.e. FIXED_LEN_BYTE_ARRAY)
                value = BigDecimal.valueOf(row.getDouble(name, 0))
              }
            }
            case BigInteger  -> value = BigInteger.valueOf(row.getLong(name, 0))
            case LocalDate   -> value = LocalDate.ofEpochDay(row.getInteger(name, 0))
            case java.sql.Date ->
              value = java.sql.Date.valueOf(LocalDate.ofEpochDay(row.getInteger(name, 0)))
            case Time ->
              value = Time.valueOf(LocalTime.ofSecondOfDay(row.getInteger(name, 0)))
            case Date ->
              value = new Date(row.getLong(name, 0))
            case LocalDateTime -> {
              def micros = (long) row.getLong(name, 0)
              def millis = (long) (micros / 1000)
              value = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            }
            default -> value = row.getString(name, 0)
          }
        }

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
      case PrimitiveType.PrimitiveTypeName.INT32: return Integer
      case PrimitiveType.PrimitiveTypeName.INT64: return Long
      case PrimitiveType.PrimitiveTypeName.FLOAT: return Float
      case PrimitiveType.PrimitiveTypeName.DOUBLE: return Double
      case PrimitiveType.PrimitiveTypeName.BOOLEAN: return Boolean
      default: return String
    }
  }

  static List<Class> extractFieldTypes(GroupType schema) {
    schema.fields.collect { field ->
      def logical = field.getLogicalTypeAnnotation()
      def primitive = field.asPrimitiveType().primitiveTypeName

      if (logical != null) {
        if (logical == LogicalTypeAnnotation.dateType()) return LocalDate
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) return LocalDateTime
        if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) return BigDecimal
      }

      return getJavaType(primitive)
    }
  }
}
