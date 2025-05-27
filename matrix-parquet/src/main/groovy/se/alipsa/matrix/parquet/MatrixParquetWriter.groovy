package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.hadoop.example.GroupWriteSupport
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Types

import se.alipsa.matrix.core.Matrix

class MatrixParquetWriter {

  static void write(Matrix matrix, File file) {
    def schema = buildSchema(matrix)
    def conf = new Configuration()
    GroupWriteSupport.setSchema(schema, conf)

    def writer = ExampleParquetWriter.builder(new Path(file.toURI()))
        .withConf(conf)
        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
        .withType(schema)
        .build()

    def factory = new SimpleGroupFactory(schema)
    def rowCount = matrix.rowCount()
    def colNames = matrix.columnNames()

    (0..<rowCount).each { i ->
      def group = factory.newGroup()
      colNames.each { col ->
        def value = matrix[i, col]
        if (value != null) {
          group.append(col, value.toString()) // TODO: type-specific appending
        }
      }
      writer.write(group)
    }

    writer.close()
  }

  static MessageType buildSchema(Matrix matrix) {
    def builder = Types.buildMessage()
    matrix.columnNames().each { col ->
      def type = matrix.type(col)
      builder.optional(getPrimitiveType(type)).named(col)
    }
    return builder.named("MatrixSchema")
  }

  static PrimitiveTypeName getPrimitiveType(Class clazz) {
    if (clazz == Integer || clazz == int) return PrimitiveTypeName.INT32
    if (clazz == Long || clazz == long) return PrimitiveTypeName.INT64
    if (clazz == Double || clazz == double) return PrimitiveTypeName.DOUBLE
    if (clazz == Float || clazz == float) return PrimitiveTypeName.FLOAT
    if (clazz == Boolean || clazz == boolean) return PrimitiveTypeName.BOOLEAN
    return PrimitiveTypeName.BINARY
  }
}

