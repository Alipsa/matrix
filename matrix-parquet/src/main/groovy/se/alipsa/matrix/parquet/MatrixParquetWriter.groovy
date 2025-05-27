package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.hadoop.example.GroupWriteSupport
import org.apache.parquet.io.api.Binary
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
    def schema = buildSchema(matrix, inferPrecisionAndScale)
    def conf = new Configuration()
    /*

    conf.set("parquet.example.schema", schema.toString())
    conf.set("matrix.columnTypes", matrix.types().collect { it.simpleName }.join(','))
    GroupWriteSupport.setSchema(schema, conf)

    def writeSupport = new GroupWriteSupportWithMetadata()
    writeSupport.init(conf)

    def writer = new ParquetWriter<Group>(
        new Path(file.toURI()),
        writeSupport,
        ParquetWriter.DEFAULT_COMPRESSION_CODEC_NAME,
        ParquetWriter.DEFAULT_BLOCK_SIZE,
        ParquetWriter.DEFAULT_PAGE_SIZE,
        ParquetWriter.DEFAULT_PAGE_SIZE,
        ParquetWriter.DEFAULT_IS_DICTIONARY_ENABLED,
        ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED,
        ParquetWriter.DEFAULT_WRITER_VERSION,
        conf
    )*/
    def extraMeta = new HashMap<String, String>()
    extraMeta.put("matrix.columnTypes", matrix.types().collect { it.name }.join(','))

    def writer = ExampleParquetWriter.builder(new Path(file.toURI()))
        .withConf(conf)
        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
        .withType(schema)
        .withExtraMetaData(extraMeta)
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

          switch (value.class) {
            case Integer, int -> group.append(col, (int) value)
            case Long, long, BigInteger -> group.append(col, ((Number)value).longValue())
            case Float, float -> group.append(col, ((Number)value).floatValue())
            case Double, double -> group.append(col, ((Number)value).doubleValue())
            case BigDecimal -> {
              def field = schema.getType(col).asPrimitiveType()
              def logical = field.getLogicalTypeAnnotation()
              if (field.primitiveTypeName == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY &&
                  logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
                def bd = (BigDecimal) value
                def scale = logical.scale
                def unscaled = bd.setScale(scale).unscaledValue()
                def bytes = unscaled.toByteArray()
                def size = field.typeLength
                def padded = new byte[size]
                System.arraycopy(bytes, 0, padded, size - bytes.length, bytes.length)
                group.add(col, Binary.fromConstantByteArray(padded))
              } else {
                def bd = (BigDecimal) value
                group.append(col, bd.doubleValue())
              }
            }
            case Boolean, boolean -> group.append(col, (boolean)value)
            case LocalDate -> group.append(col, (int) value.toEpochDay())
            case java.sql.Date -> group.append(col, (int) value.toLocalDate().toEpochDay())
            case Time -> group.append(col, (int) value.toLocalTime().toSecondOfDay())
            case LocalDateTime -> {
              def micros = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000
              group.append(col, (long) micros)
            }
            case Date -> group.append(col, ((Date)value).time)
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
      if (decimalMeta != null) {
        builder.addField(buildParquetField(col, type, decimalMeta[col]))
      } else {
        builder.addField(buildParquetField(col, type))
      }

    }
    return builder.named("MatrixSchema")
  }

  static PrimitiveType buildParquetField(String name, Class clazz, int[] decimalMeta = null) {
    if (clazz == BigDecimal) {
      if (decimalMeta != null) {
        int precision = decimalMeta[0] ?: 10
        int scale = decimalMeta[1] ?: 2
        return Types.optional(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
            .length(minBytesForPrecision(precision))
            .as(LogicalTypeAnnotation.decimalType(scale, precision))
            .named(name)
      } else {
        // fallback to double (discouraged, but allowed for backward compat)
        return Types.optional(PrimitiveTypeName.DOUBLE).named(name)
      }
    }

    def primitive
    def logical = null

    switch (clazz) {
      case Integer, int -> primitive = PrimitiveTypeName.INT32
      case Long, long, BigInteger -> primitive = PrimitiveTypeName.INT64
      case Float, float -> primitive = PrimitiveTypeName.FLOAT
      case Double, double -> primitive = PrimitiveTypeName.DOUBLE
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
    Map<String, int[]> result = [:] // columnName -> [precision, scale]

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
        result[col] = [maxPrecision, maxScale] as int[]
      }
    }
    return result
  }

  static int minBytesForPrecision(int precision) {
    // According to the Parquet spec
    return (Math.ceil((Math.log(Math.pow(10, precision)) / Math.log(2) + 1) / 8) as int)
  }
}

