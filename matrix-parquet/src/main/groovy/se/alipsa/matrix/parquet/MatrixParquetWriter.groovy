package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.example.ExampleParquetWriter
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

@CompileStatic
class MatrixParquetWriter {

  /**
   * Writes a Matrix to a Parquet file, optionally inferring precision and scale for BigDecimal columns.
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory. If directory the file name will be based on the matrix name.
   * @param inferPrecisionAndScale whether to infer precision and scale for BigDecimal columns. If false,
   * BigDecimal columns default to double storage (which leads to some loss of precision).
   * @return the target file
   */
  static File write(Matrix matrix, File fileOrDir, boolean inferPrecisionAndScale = true) {
    MessageType schema
    if (inferPrecisionAndScale) {
      schema = buildSchema(matrix,true)
    } else {
      schema = buildSchema(matrix, false)
    }
    //println "Write, inferPrecisionAndScale = $inferPrecisionAndScale, schema = $schema"
    File file = determineTargetFile(matrix, fileOrDir)

    return writeInternal(matrix, file, schema)
  }

  /**
   * Writes a Matrix to a Parquet file using uniform decimal precision
   * and scale for all BigDecimal columns. This is useful if you don't
   * want to infer precision/scale but also don't want to store them as double
   * so you can set the precision and scale to a sufficiently large value (e.g., [38, 18] for typical financial calculations)
   * so there will be no loss of precision.
   *
   * @param matrix
   * @param fileOrDir
   * @param precision
   * @param scale
   * @return
   */
  static File write(Matrix matrix, File fileOrDir, int precision, int scale) {
    Map<String, int[]> decimalMeta = [:]
    matrix.columnNames().each { col ->
      if (matrix.type(col) == BigDecimal) {
        decimalMeta[col] = [precision, scale] as int[]
      }
    }
    return write(matrix, fileOrDir, decimalMeta)
  }

  /**
   * Writes a Matrix to a Parquet file using explicitly provided decimal precision and scale metadata.
   * This allows for precise control over how BigDecimal values are stored.
   * Note that precision affects storage but not the precision and
   * scale of the BigDecimal values themselves. This means that if the precision is too small to hold a value,
   * an exception will be thrown during writing. BigDecimal automatically adjust precision and scale. E.g.
   * if you specified precision as 5 and scale as two and then retrieve a value of 12.1
   * the precision of that value will be 3 and scale 1. Example usage:
   * <code><pre>
   * Matrix data = Dataset.mtcars()
   * File file = new File("mtcars.parquet")
   * MatrixParquetWriter.write(data, file, [wt: [4, 3]])
   * </pre></code>
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory. If directory the file name will be based on the matrix name.
   * @param decimalMeta a Map where keys are column names (String) and values are int arrays [precision, scale].
   * if set to null or there is no matching entry for a BigDecimal column, that column will default to double storage
   * @return the target file
   */
  static File write(Matrix matrix, File fileOrDir, Map<String, int[]> decimalMeta) {
    File file = determineTargetFile(matrix, fileOrDir)

    def schema = buildSchema(matrix, decimalMeta)
    return writeInternal(matrix, file, schema)
  }

  private static File determineTargetFile(Matrix matrix, File fileOrDir) {
    String name = matrix.matrixName ?: 'matrix'
    File file
    if (fileOrDir.isDirectory()) {
      file = new File(fileOrDir, "${name}.parquet")
      println "Writing to ${file.absolutePath}"
    } else {
      file = fileOrDir
    }
    file
  }

  /**
   * Internal helper method that handles the core logic of writing matrix rows
   * to a Parquet file, given an already constructed schema.
   * @param matrix the matrix to write
   * @param file the target file
   * @param schema the Parquet MessageType schema
   * @return the target file
   */
  private static File writeInternal(Matrix matrix, File file, MessageType schema) {
    def conf = new Configuration()
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
                // Parquet stores the unscaled value padded to the field length
                def unscaled = bd.setScale(scale, BigDecimal.ROUND_HALF_UP).unscaledValue()
                def bytes = unscaled.toByteArray()
                def size = field.typeLength
                def padded = new byte[size]
                // Copy bytes into the padded array, aligning them to the right
                System.arraycopy(bytes, 0, padded, size - bytes.length, bytes.length)
                group.add(col, Binary.fromConstantByteArray(padded))
              } else {
                def bd = (BigDecimal) value
                group.append(col, bd.doubleValue())
              }
            }
            case Boolean, boolean -> group.append(col, (boolean)value)
            case LocalDate -> group.append(col, ((LocalDate)value).toEpochDay().intValue())
            case java.sql.Date -> group.append(col, ((java.sql.Date)value).toLocalDate().toEpochDay().intValue())
            case Time -> group.append(col, ((Time)value).toLocalTime().toSecondOfDay())
            case LocalDateTime -> {
              // Convert LocalDateTime to microseconds since epoch (Parquet TIMESTAMP_MILLIS logical type)
              def micros = ((LocalDateTime)value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000
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
    return file
  }

  /**
   * Builds the Parquet schema based on the matrix and an optional map of explicit decimal metadata.
   * This overload is the core schema builder.
   *
   * @param matrix the matrix
   * @param explicitDecimalMeta map of column name to [precision, scale] array for BigDecimal columns.
   * @return the Parquet MessageType schema.
   */
  static MessageType buildSchema(Matrix matrix, Map<String, int[]> explicitDecimalMeta) {
    def builder = Types.buildMessage()
    matrix.columnNames().each { col ->
      def type = matrix.type(col)
      def meta = explicitDecimalMeta?.get(col)
      //println "Building schema for column '$col' of type $type with decimal meta: $meta"
      builder.addField(buildParquetField(col, type, meta))
    }
    return builder.named("MatrixSchema")
  }

  /**
   * Builds the Parquet schema based on the matrix, inferring decimal precision/scale if requested.
   * This is for backward compatibility and delegates to the explicit map builder.
   * @param matrix the matrix
   * @param inferPrecisionAndScale if true, infers decimal precision and scale. If false, BigDecimal columns default to
   * double storage (which leads to some loss of precision).
   * @return the Parquet MessageType schema.
   */
  static MessageType buildSchema(Matrix matrix, boolean inferPrecisionAndScale = false) {
    Map<String, int[]> decimalMeta = null
    if (inferPrecisionAndScale) {
      decimalMeta = inferDecimalPrecisionAndScale(matrix)
    }
    return buildSchema(matrix, decimalMeta)
  }

  static PrimitiveType buildParquetField(String name, Class clazz, int[] decimalMeta = null) {
    if (clazz == BigDecimal) {
      if (decimalMeta != null && decimalMeta.length == 2) {
        // int precision = decimalMeta[0] ?: 10 // BUGGY: what if 0 is passed? (though inference prevents this)
        // int scale = decimalMeta[1] ?: 2     // BUGGY: 0 ?: 2 becomes 2

        int precision = decimalMeta[0]
        int scale = decimalMeta[1]

        // Add safeguards for invalid (0 or less) precision, which can be passed manually
        // The inference method already ensures precision is at least 1.
        if (precision <= 0) {
          precision = 10 // Fallback to a default precision
        }

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
        // Use LogicalTypeAnnotation.TimeUnit.MICROS for better precision, but current code used MILLIS
        // Sticking to MILLIS to avoid breaking existing behavior, but writing microseconds in _writeInternal
        logical = LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      case Time -> {
        primitive = PrimitiveTypeName.INT32
        // Time in milliseconds, but writing seconds in _writeInternal
        logical = LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      default -> primitive = PrimitiveTypeName.BINARY
    }

    def builder = Types.optional(primitive)
    if (logical != null) {
      builder = builder.as(logical)
    }
    return builder.named(name)
  }

  /**
   * Infers the required maximum precision and scale for all BigDecimal columns in the matrix.
   * It performs the calculation in a single pass:
   * 1. It tracks the running maximum scale (S).
   * 2. It tracks the running maximum number of digits to the LEFT of the decimal (L).
   * 3. The final precision P = L + S.
   * This guarantees that the resulting schema is large enough to contain all values.
   * @param matrix the matrix
   * @return a Map of column name to [precision, scale] array.
   */
  static Map<String, int[]> inferDecimalPrecisionAndScale(Matrix matrix) {
    Map<String, int[]> result = [:] // columnName -> [precision, scale]

    matrix.columnNames().each { col ->
      if (matrix.type(col) == BigDecimal) {
        int maxScale = 0
        int maxLeftDigits = 0 // Max digits to the left of the decimal

        (0..<matrix.rowCount()).each { i ->
          def value = matrix[i, col]
          if (value instanceof BigDecimal) {
            int valScale = value.scale()
            maxScale = Math.max(maxScale, valScale)

            // Calculate digits to the left of the decimal
            // precision() - scale() works for most numbers (e.g., 123.45 -> p=5, s=2 -> 3)
            // but fails for numbers < 1 (e.g., 0.45 -> p=2, s=2 -> 0)
            // We must ensure at least one digit (for the '0')
            int leftDigits = Math.max(1, value.precision() - value.scale())
            maxLeftDigits = Math.max(maxLeftDigits, leftDigits)
          }
        }

        // Total precision = max digits left + max digits right (max scale)
        int maxPrecision = maxLeftDigits + maxScale

        // Ensure precision is at least 1 (e.g., for a column of all 0s)
        maxPrecision = Math.max(1, maxPrecision)

        result[col] = [maxPrecision, maxScale] as int[]
      }
    }
    //println "Inferred decimal precision/scale: $result"
    return result
  }


  static int minBytesForPrecision(int precision) {
    // According to the Parquet spec
    def t = (Math.log(Math.pow(10, precision)) / Math.log(2) + 1) as double
    return Math.ceil( t / 8d) as int
  }
}

