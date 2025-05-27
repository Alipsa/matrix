package se.alipsa.matrix.parquet

import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import se.alipsa.matrix.core.Matrix

class MatrixParquetReader {

  static Matrix read(File file) {
    def path = new org.apache.hadoop.fs.Path(file.toURI())
    def reader = ParquetReader.builder(new GroupReadSupport(), path).build()

    Group row = reader.read()
    if (row == null) {
      return new Matrix("EmptyParquet")
    }

    MessageType schema = row.getType()
    List<String> fieldNames = schema.fields.collect { it.name }
    List<Class> fieldTypes = schema.fields.collect { getJavaType(it.asPrimitiveType().primitiveTypeName) }

    def builder = Matrix.builder("ParquetData")
    .columnNames(fieldNames)
    .types(fieldTypes)

    while (row != null) {
      def rowData = fieldNames.collectWithIndex { name, i ->
        def type = fieldTypes[i]
        if (row.getFieldRepetitionCount(name) == 0) {
          return null
        }
        return switch (type) {
          case Integer -> row.getInteger(name, 0)
          case Long    -> row.getLong(name, 0)
          case Float   -> row.getFloat(name, 0)
          case Double  -> row.getDouble(name, 0)
          case Boolean -> row.getBoolean(name, 0)
          default      -> row.getString(name, 0)
        }
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
