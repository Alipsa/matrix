package se.alipsa.matrix.parquet

import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import se.alipsa.matrix.core.Matrix

class MatrixParquetReader {

  static Matrix read(File file) {
    def path = new Path(file.toURI())
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

    MessageType schema = row.getType()
    List<String> fieldNames = schema.fields.collect { it.name }
    List<Class> fieldTypes = schema.fields.collect { getJavaType(it.asPrimitiveType().primitiveTypeName) }

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
            case Integer:
              value = row.getInteger(name, 0)
              break
            case Long:
              value = row.getLong(name, 0)
              break
            case Float:
              value = row.getFloat(name, 0)
              break
            case Double:
              value = row.getDouble(name, 0)
              break
            case Boolean:
              value = row.getBoolean(name, 0)
              break
            default:
              value = row.getString(name, 0)
          }
        }

        rowData << value
      }
      builder.addRow(rowData as Object[])
      row = reader.read()
    }
    return builder.build()
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
}
