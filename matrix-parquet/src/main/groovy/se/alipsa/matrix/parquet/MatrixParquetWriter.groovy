package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.hadoop.example.GroupWriteSupport
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Types

import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MatrixParquetWriter {

  static void write(Matrix matrix, File file, boolean inferPrecisionAndScale = false) {
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
          def fieldType = schema.getType(col)
          if (!fieldType.isPrimitive()) {
            throw new IllegalArgumentException("Column '$col' is not a primitive field in schema: $fieldType")
          }

          switch (value) {
            case Integer, int -> group.append(col, (int) value)
            case Long, long, BigInteger -> group.append(col, ((Number)value).longValue())
            case Float, float -> group.append(col, ((Number)value).floatValue())
            case Double, double, BigDecimal -> group.append(col, ((Number)value).doubleValue())
            case Boolean, boolean -> group.append(col, (boolean)value)
            case LocalDate -> group.append(col, (int) value.toEpochDay())
            case java.sql.Date -> group.append(col, (int) value.toLocalDate().toEpochDay())
            case Time -> group.append(col, (int) value.toLocalTime().toSecondOfDay())
            case LocalDateTime -> {
              def micros = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000
              group.append(col, (long) micros)
            }
            case Date -> group.append(col, value.time)
            default -> group.append(col, value.toString())
          }
        }
      }
      writer.write(group)
    }

    writer.close()
  }

  static MessageType buildSchema(Matrix matrix, boolean inferPrecisionAndScale = false) {
    def builder = Types.buildMessage()
    Map<String, int[]> decimalMeta = null
    if (inferPrecisionAndScale) {
      decimalMeta = inferDecimalPrecisionAndScale(matrix)
    }
    matrix.columnNames().each { col ->
      def type = matrix.type(col)
      if (decimalMeta != null && type == BigDecimal) {
        builder.addField(buildParquetField(col, type, decimalMeta[col]))
      } else {
        builder.addField(buildParquetField(col, type))
      }

    }
    return builder.named("MatrixSchema")
  }

  static PrimitiveType buildParquetField(String name, Class clazz, int[] decimalMeta = null) {
    def primitive
    def logical = null

    switch (clazz) {
      case Integer, int -> primitive = PrimitiveTypeName.INT32
      case Long, long, BigInteger -> primitive = PrimitiveTypeName.INT64
      case Float, float -> primitive = PrimitiveTypeName.FLOAT
      case Double, double -> primitive = PrimitiveTypeName.DOUBLE
      case BigDecimal -> {
        if (decimalMeta != null) {
          int precision = decimalMeta ? decimalMeta[0] : 10
          int scale = decimalMeta ? decimalMeta[1] : 2
          primitive = PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY
          logical = LogicalTypeAnnotation.decimalType(scale, precision)
          Types.optional(primitive)
              .length(minBytesForPrecision(precision))
              .as(logical)
              .named(name)
        } else {
          primitive = PrimitiveTypeName.DOUBLE
          logical = null
        }
      }
      case Boolean, boolean -> primitive = PrimitiveTypeName.BOOLEAN
      case LocalDate, java.sql.Date -> {
        primitive = PrimitiveTypeName.INT32
        logical = LogicalTypeAnnotation.dateType()
      }
      case LocalDateTime, Timestamp -> {
        primitive = PrimitiveTypeName.INT64
        logical = LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      case Time -> {
        primitive = PrimitiveTypeName.INT32
        logical = LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.SECONDS)
      }
      default -> primitive = PrimitiveTypeName.BINARY
    }

    def builder = Types.optional(primitive)
    if (logical != null) {
      builder = builder.as(logical)
    }
    return builder.named(name)
  }

  static Map<String, int[]> inferDecimalPrecisionAndScale(Matrix matrix) {
    def result = [:] // columnName -> [precision, scale]

    matrix.columnNames().each { col ->
      if (matrix.type(col) == BigDecimal) {
        int maxPrecision = 0
        int maxScale = 0
        (0..<matrix.rowCount()).each { i ->
          def value = matrix[i, col]
          if (value instanceof BigDecimal) {
            maxPrecision = Math.max(maxPrecision, value.precision())
            maxScale = Math.max(maxScale, value.scale())
          }
        }
        result[col] = [maxPrecision, maxScale]
      }
    }

    return result
  }

  static int minBytesForPrecision(int precision) {
    // According to the Parquet spec
    return (Math.ceil((Math.log(Math.pow(10, precision)) / Math.log(2) + 1) / 8) as int)
  }
}

