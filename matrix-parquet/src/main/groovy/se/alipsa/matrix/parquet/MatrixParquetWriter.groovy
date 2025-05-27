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

import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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
          if (value instanceof BigDecimal || value instanceof BigInteger) {
            group.append(col, value.toDouble())
          } else if (value instanceof LocalDate) {
            group.append(col, (int) value.toEpochDay())
          } else if (value instanceof java.sql.Date) {
            group.append(col, (int) (value.toLocalDate().toEpochDay()))
          } else if (value instanceof Time) {
            group.append(col, (int) (value.toLocalTime().toSecondOfDay()))
          } else if (value instanceof LocalDateTime) {
            def micros = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000
            group.append(col, micros as long) // Or use INT96 logic
          } else if (value instanceof Date) {
            group.append(col, value.time) // milliseconds since epoch
          } else {
            group.append(col, value.toString())
          }
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
    switch (clazz) {
      case Integer, int -> PrimitiveTypeName.INT32
      case Long, long, BigInteger -> PrimitiveTypeName.INT64
      case Float, float -> PrimitiveTypeName.FLOAT
      case Double, double, BigDecimal -> PrimitiveTypeName.DOUBLE
      case Boolean, boolean -> PrimitiveTypeName.BOOLEAN
      case LocalDate, java.sql.Date -> PrimitiveTypeName.INT32  // logicalType: DATE
      case LocalDateTime, Timestamp -> PrimitiveTypeName.INT96 // legacy support
      case Time -> PrimitiveTypeName.INT32 // logicalType: TIME
      default -> PrimitiveTypeName.BINARY
    }
  }
}

