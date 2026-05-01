package se.alipsa.matrix.avro

import org.apache.avro.Conversions
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.UnresolvedUnionException
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericFixed
import org.apache.avro.generic.GenericRecord

import se.alipsa.matrix.avro.exceptions.AvroConversionException
import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.avro.exceptions.AvroValidationException
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.file.Path
import java.sql.Time
import java.time.*

/**
 * Writes Matrix objects to Avro Object Container Files (OCF).
 *
 * <p>Supports writing to File, Path, OutputStream, and byte arrays.
 * Handles conversion of Java types to appropriate Avro types, including
 * support for logical types (date, time, timestamp, decimal, UUID).
 *
 * <p>Example usage:
 * <pre>{@code
 * Matrix m = Matrix.builder('data')
 *     .columnNames(['id', 'name', 'price'])
 *     .rows([[1, 'Alice', 10.50], [2, 'Bob', 20.75]])
 *     .types(Integer, String, BigDecimal)
 *     .build()
 *
 * // Write to file
 * MatrixAvroWriter.write(m, new File('data.avro'))
 *
 * // Write with decimal precision inference
 * MatrixAvroWriter.write(m, new File('data.avro'), true)
 *
 * // Write to byte array
 * byte[] bytes = MatrixAvroWriter.writeBytes(m)
 * }</pre>
 */
class MatrixAvroWriter {

  private static final String PATH_NULL_MESSAGE = 'Path cannot be null'
  private static final String OUTPUT_STREAM_NULL_MESSAGE = 'OutputStream cannot be null'
  private static final String OPTIONS_NULL_MESSAGE = 'Options cannot be null'
  private static final String NULL_TYPE_NAME = 'null'
  private static final Object NO_AVRO_VALUE = new Object()
  private static final long MICROS_PER_SECOND = 1_000_000L
  private static final long MILLIS_PER_SECOND = 1_000L
  private static final int NANOS_PER_MICRO = 1_000
  private static final int NANOS_PER_MILLI = 1_000_000
  /**
   * Write a Matrix to an Avro file.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param file the target file to write to (must not be null)
   * @param inferPrecisionAndScale if true, infer precision and scale for BigDecimal columns
   *        from the actual data; if false, BigDecimal columns are stored as doubles
   * @throws IllegalArgumentException if matrix is null, has no columns, or file is null
   * @throws IOException if an I/O error occurs or parent directory creation fails
   */
  static void write(Matrix matrix, File file, boolean inferPrecisionAndScale = false) {
    validateMatrix(matrix)
    validateFile(file)
    Schema schema = buildSchema(matrix, inferPrecisionAndScale)
    DataFileWriter<GenericRecord> dfw = new DataFileWriter<>(new GenericDatumWriter<GenericRecord>(schema))
    dfw.create(schema, file)
    try {
      writeRows(matrix, dfw, schema)
    } finally {
      dfw.close()
    }
  }
  /**
   * Write a Matrix to an Avro file at the specified Path.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param path the target path to write to (must not be null)
   * @param inferPrecisionAndScale if true, infer precision and scale for BigDecimal columns
   *        from the actual data; if false, BigDecimal columns are stored as doubles
   * @throws IllegalArgumentException if matrix is null, has no columns, or path is null
   * @throws IOException if an I/O error occurs or parent directory creation fails
   * @see #write(Matrix, File, boolean)
   */
  static void write(Matrix matrix, Path path, boolean inferPrecisionAndScale = false) {
    if (path == null) {
      throw new IllegalArgumentException(PATH_NULL_MESSAGE)
    }
    validateMatrix(matrix)
    write(matrix, path.toFile(), inferPrecisionAndScale)
  }
  /**
   * Write a Matrix to an OutputStream in Avro format.
   *
   * <p>The stream will NOT be closed by this method; the caller is responsible for closing it.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param out the OutputStream to write to (must not be null)
   * @param inferPrecisionAndScale if true, infer precision and scale for BigDecimal columns
   *        from the actual data; if false, BigDecimal columns are stored as doubles
   * @throws IllegalArgumentException if matrix is null, has no columns, or out is null
   * @throws IOException if an I/O error occurs
   */
  static void write(Matrix matrix, OutputStream out, boolean inferPrecisionAndScale = false) {
    validateMatrix(matrix)
    if (out == null) {
      throw new IllegalArgumentException(OUTPUT_STREAM_NULL_MESSAGE)
    }
    Schema schema = buildSchema(matrix, inferPrecisionAndScale)
    DataFileWriter<GenericRecord> dfw = new DataFileWriter<>(new GenericDatumWriter<GenericRecord>(schema))
    dfw.create(schema, new NonClosingOutputStream(out))
    try {
      writeRows(matrix, dfw, schema)
    } finally {
      dfw.close()
    }
  }
  /**
   * Write a Matrix to a byte array in Avro format.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param inferPrecisionAndScale if true, infer precision and scale for BigDecimal columns
   *        from the actual data; if false, BigDecimal columns are stored as doubles
   * @return byte array containing the Avro data
   * @throws IllegalArgumentException if matrix is null or has no columns
   */
  static byte[] writeBytes(Matrix matrix, boolean inferPrecisionAndScale = false) {
    validateMatrix(matrix)
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    Schema schema = buildSchema(matrix, inferPrecisionAndScale)
    DataFileWriter<GenericRecord> dfw = new DataFileWriter<>(new GenericDatumWriter<GenericRecord>(schema))
    dfw.create(schema, baos)
    try {
      writeRows(matrix, dfw, schema)
    } finally {
      dfw.close()
    }
    return baos.toByteArray()
  }
  // ----------------------------------------------------------------------
  // Methods accepting AvroWriteOptions
  // ----------------------------------------------------------------------
  /**
   * Write a Matrix to an Avro file with configurable options.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param file the target file to write to (must not be null)
   * @param options the write configuration options
   * @throws IllegalArgumentException if matrix is null, has no columns, file is null, or options is null
   * @throws IOException if an I/O error occurs or parent directory creation fails
   * @see AvroWriteOptions
   */
  static void write(Matrix matrix, File file, AvroWriteOptions options) {
    validateMatrix(matrix)
    validateFile(file)
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    Schema schema = buildSchema(matrix, options)
    DataFileWriter<GenericRecord> dfw = createDataFileWriter(schema, options)
    dfw.create(schema, file)
    try {
      writeRows(matrix, dfw, schema)
    } finally {
      dfw.close()
    }
  }
  /**
   * Write a Matrix to an Avro file at the specified Path with configurable options.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param path the target path to write to (must not be null)
   * @param options the write configuration options
   * @throws IllegalArgumentException if matrix is null, has no columns, path is null, or options is null
   * @throws IOException if an I/O error occurs or parent directory creation fails
   * @see AvroWriteOptions
   */
  static void write(Matrix matrix, Path path, AvroWriteOptions options) {
    if (path == null) {
      throw new IllegalArgumentException(PATH_NULL_MESSAGE)
    }
    write(matrix, path.toFile(), options)
  }
  /**
   * Write a Matrix to an OutputStream in Avro format with configurable options.
   *
   * <p>The stream will NOT be closed by this method; the caller is responsible for closing it.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param out the OutputStream to write to (must not be null)
   * @param options the write configuration options
   * @throws IllegalArgumentException if matrix is null, has no columns, out is null, or options is null
   * @throws IOException if an I/O error occurs
   * @see AvroWriteOptions
   */
  static void write(Matrix matrix, OutputStream out, AvroWriteOptions options) {
    validateMatrix(matrix)
    if (out == null) {
      throw new IllegalArgumentException(OUTPUT_STREAM_NULL_MESSAGE)
    }
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    Schema schema = buildSchema(matrix, options)
    DataFileWriter<GenericRecord> dfw = createDataFileWriter(schema, options)
    dfw.create(schema, new NonClosingOutputStream(out))
    try {
      writeRows(matrix, dfw, schema)
    } finally {
      dfw.close()
    }
  }
  /**
   * Write a Matrix to a byte array in Avro format with configurable options.
   *
   * @param matrix the Matrix to write (must not be null, must have at least one column)
   * @param options the write configuration options
   * @return byte array containing the Avro data
   * @throws IllegalArgumentException if matrix is null, has no columns, or options is null
   * @see AvroWriteOptions
   */
  static byte[] writeBytes(Matrix matrix, AvroWriteOptions options) {
    validateMatrix(matrix)
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    Schema schema = buildSchema(matrix, options)
    DataFileWriter<GenericRecord> dfw = createDataFileWriter(schema, options)
    dfw.create(schema, baos)
    try {
      writeRows(matrix, dfw, schema)
    } finally {
      dfw.close()
    }
    return baos.toByteArray()
  }
  /**
   * Write a Matrix to an Avro file with exact decimal logical types inferred from the data.
   *
   * @param matrix the Matrix to write
   * @param file the target file
   * @throws IOException if an I/O error occurs
   */
  static void writeExactDecimals(Matrix matrix, File file) {
    write(matrix, file, AvroWriteOptions.exactDecimals())
  }
  /**
   * Write a Matrix to an Avro file at the specified Path with exact decimal logical types inferred from the data.
   *
   * @param matrix the Matrix to write
   * @param path the target path
   * @throws IOException if an I/O error occurs
   */
  static void writeExactDecimals(Matrix matrix, Path path) {
    write(matrix, path, AvroWriteOptions.exactDecimals())
  }
  /**
   * Write a Matrix to an OutputStream with exact decimal logical types inferred from the data.
   *
   * <p>The stream will NOT be closed by this method; the caller is responsible for closing it.
   *
   * @param matrix the Matrix to write
   * @param out the OutputStream to write to
   * @throws IOException if an I/O error occurs
   */
  static void writeExactDecimals(Matrix matrix, OutputStream out) {
    write(matrix, out, AvroWriteOptions.exactDecimals())
  }
  /**
   * Write a Matrix to a byte array with exact decimal logical types inferred from the data.
   *
   * @param matrix the Matrix to write
   * @return byte array containing the Avro data
   */
  static byte[] writeExactDecimalBytes(Matrix matrix) {
    writeBytes(matrix, AvroWriteOptions.exactDecimals())
  }
  /**
   * Creates a DataFileWriter configured with the specified options.
   */
  private static DataFileWriter<GenericRecord> createDataFileWriter(Schema schema, AvroWriteOptions options) {
    DataFileWriter<GenericRecord> dfw = new DataFileWriter<>(new GenericDatumWriter<GenericRecord>(schema))
    dfw.setCodec(options.createCodecFactory())
    if (options.syncInterval > 0) {
      dfw.setSyncInterval(options.syncInterval)
    }
    return dfw
  }
  /**
   * Validates that the matrix is not null and has at least one column.
   *
   * @throws AvroValidationException if matrix is null or has no columns
   */
  private static void validateMatrix(Matrix matrix) {
    if (matrix == null) {
      throw AvroValidationException.nullParameter('matrix')
    }
    if (matrix.columnCount() == 0) {
      throw AvroValidationException.emptyMatrix()
    }
    int expectedRows = matrix.rowCount()
    for (String col : matrix.columnNames()) {
      List values = matrix.column(col)
      if (values.size() != expectedRows) {
        int rowNumber = Math.min(values.size(), expectedRows)
        throw AvroValidationException.columnSizeMismatch(col, rowNumber, values.size(), expectedRows)
      }
    }
  }
  /**
   * Validates the file parameter and ensures parent directory exists.
   *
   * @throws AvroValidationException if file is null
   * @throws IOException if parent directory creation fails
   */
  private static void validateFile(File file) {
    if (file == null) {
      throw AvroValidationException.nullParameter('file')
    }
    File parentDir = file.parentFile
    if (parentDir != null && !parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        throw new IOException("Failed to create parent directory: ${parentDir.absolutePath}. " +
            'Check that you have write permissions and the path is valid.')
      }
    }
  }
  // ----------------------------------------------------------------------
  // Schema building
  // ----------------------------------------------------------------------
  /**
   * Builds an Avro schema for the given Matrix.
   *
   * <p>The schema is a record type with one field per Matrix column. Each field
   * is wrapped in a nullable union [NULL_TYPE_NAME, T] to handle null values. The type
   * mapping follows these rules:
   * <ul>
   *   <li>Primitive types map directly (String, Boolean, Integer, Long, Float, Double)</li>
   *   <li>BigDecimal maps to decimal logical type (if inferPrecisionAndScale) or double</li>
   *   <li>Date/Time types map to appropriate Avro logical types</li>
   *   <li>List columns map to Avro arrays</li>
   *   <li>Map columns map to Avro maps or records (based on key consistency)</li>
   * </ul>
   *
   * @param matrix the Matrix to build a schema for
   * @param inferPrecisionAndScale if true, scan BigDecimal columns to determine precision/scale
   * @return an Avro record schema suitable for writing the Matrix
   */
  static Schema buildSchema(Matrix matrix, boolean inferPrecisionAndScale) {
    validateMatrix(matrix)
    return buildSchemaInternal(
        matrix,
        inferPrecisionAndScale,
        resolveSchemaName(matrix, null),
        AvroWriteOptions.DEFAULT_NAMESPACE,
        [:]
    )
  }
  /**
   * Builds an Avro schema for the given Matrix using options.
   *
   * @param matrix the Matrix to build a schema for
   * @param options the write options containing schema configuration
   * @return an Avro record schema suitable for writing the Matrix
   */
  static Schema buildSchema(Matrix matrix, AvroWriteOptions options) {
    validateMatrix(matrix)
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    return buildSchemaInternal(
        matrix,
        options.inferPrecisionAndScale,
        resolveSchemaName(matrix, options.schemaName),
        options.namespace,
        options.columnSchemas
    )
  }
  private static String resolveSchemaName(Matrix matrix, String configuredSchemaName) {
    if (configuredSchemaName != null && !configuredSchemaName.isBlank()) {
      return configuredSchemaName
    }
    String matrixName = matrix?.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      return matrixName
    }
    'MatrixSchema'
  }
  /**
   * Internal schema building with configurable name and namespace.
   */
  private static Schema buildSchemaInternal(Matrix matrix, boolean inferPrecisionAndScale,
                                            String schemaName, String namespace,
                                            Map<String, AvroSchemaDecl> columnSchemas) {
    Map<String, AvroSchemaDecl> declaredSchemas = columnSchemas ?: [:]
    validateDeclaredColumnSchemas(matrix, declaredSchemas)
    Schema record = Schema.createRecord(schemaName, 'Generated by MatrixAvroWriter', namespace, false)
    List<Schema.Field> fields = new ArrayList<>(matrix.columnCount())
    Map<String, ColumnProfile> profiles = analyzeColumns(matrix, inferPrecisionAndScale)
    for (String col : matrix.columnNames()) {
      AvroSchemaUtil.validateAvroFieldName(col, col)
      Schema fieldSchema
      AvroSchemaDecl declaredSchema = declaredSchemas.get(col)
      if (declaredSchema != null) {
        fieldSchema = declaredSchema.toAvroSchema(col, namespace)
      } else {
        ColumnProfile profile = profiles.get(col)
        Class<?> clazz = profile.effectiveType
        if (clazz == List) {
          Class<?> elemClass = profile.listElemClass ?: String
          Schema elemSchema = toFieldSchema(elemClass, null)
          Schema nullableElem = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), elemSchema))
          fieldSchema = Schema.createArray(nullableElem)
        } else if (clazz == Map) {
          if (profile.recordLike) {
            Map first = profile.recordSample
            def rec = Schema.createRecord(col + '_record', null, namespace, false)
            List<Schema.Field> flds = []
            for (def k : first.keySet()) {
              String fieldName = k
              AvroSchemaUtil.validateAvroFieldName(fieldName, "${col}.${fieldName}")
              def v = first.get(k)
              Class<?> vClazz = (v == null) ? String : v.getClass()
              Schema valueSchema = toFieldSchema(vClazz, null)
              Schema nullable = AvroSchemaUtil.nullableSchema(valueSchema)
              flds.add(new Schema.Field(fieldName, nullable, null as String, (Object) null))
            }
            rec.setFields(flds)
            fieldSchema = rec
          } else {
            Class<?> valClass = profile.mapValueClass ?: String
            Schema valueSchema = toFieldSchema(valClass, null)
            Schema nullableValue = AvroSchemaUtil.nullableSchema(valueSchema)
            fieldSchema = Schema.createMap(nullableValue)
          }
        } else {
          int[] decimalMeta = inferPrecisionAndScale && clazz == BigDecimal ? profile.decimalMeta() : null
          fieldSchema = toFieldSchema(clazz, decimalMeta)
        }
      }
      Schema nullable = AvroSchemaUtil.nullableSchema(fieldSchema)
      fields.add(new Schema.Field(col, nullable, null as String, (Object) null))
    }
    record.setFields(fields)
    return record
  }
  /**
   * Maps a Java class to the corresponding Avro field schema.
   *
   * <p>Handles primitive types, date/time types with logical types, and special cases:
   * <ul>
   *   <li>BigDecimal → decimal logical type (BYTES) if decimalMeta provided, else DOUBLE</li>
   *   <li>LocalDate, java.sql.Date → date logical type (INT)</li>
   *   <li>LocalTime, java.sql.Time → time-millis logical type (INT)</li>
   *   <li>Instant, java.util.Date → timestamp-millis logical type (LONG)</li>
   *   <li>LocalDateTime → local-timestamp-micros logical type (LONG)</li>
   *   <li>UUID → uuid logical type (STRING)</li>
   *   <li>Unknown types → STRING (fallback)</li>
   * </ul>
   *
   * @param clazz the Java class to map
   * @param decimalMeta optional [precision, scale] for BigDecimal columns; null uses double fallback
   * @return the corresponding Avro schema
   */
  private static Schema toFieldSchema(Class<?> clazz, int[] decimalMeta) {
    if (clazz == BigDecimal) {
      if (decimalMeta != null) {
        int precision = decimalMeta[0] > 0 ? decimalMeta[0] : 10
        int scale = decimalMeta[1] >= 0 ? decimalMeta[1] : 2
        Schema s = Schema.create(Schema.Type.BYTES)
        LogicalTypes.decimal(precision, scale).addToSchema(s)
        return s
      } else {
        return Schema.create(Schema.Type.DOUBLE) // fallback like Parquet writer
      }
    }
    if (clazz == String) {
      return Schema.create(Schema.Type.STRING)
    }
    if (clazz == Boolean || clazz == boolean.class) {
      return Schema.create(Schema.Type.BOOLEAN)
    }
    if (clazz == Integer || clazz == int.class) {
      return Schema.create(Schema.Type.INT)
    }
    if (clazz == Long || clazz == long.class || clazz == BigInteger) {
      return Schema.create(Schema.Type.LONG)
    }
    if (clazz == Float || clazz == float.class) {
      return Schema.create(Schema.Type.FLOAT)
    }
    if (clazz == Double || clazz == double.class) {
      return Schema.create(Schema.Type.DOUBLE)
    }
    if (clazz == byte[].class) {
      return Schema.create(Schema.Type.BYTES)
    }
    if (clazz == LocalDate || clazz == java.sql.Date) {
      Schema s = Schema.create(Schema.Type.INT)
      LogicalTypes.date().addToSchema(s)
      return s
    }
    if (clazz == LocalTime || clazz == Time) {
      Schema s = Schema.create(Schema.Type.INT)
      LogicalTypes.timeMillis().addToSchema(s)
      return s
    }
    if (clazz == Instant) {
      Schema s = Schema.create(Schema.Type.LONG)
      LogicalTypes.timestampMillis().addToSchema(s)
      return s
    }
    if (clazz == LocalDateTime) {
      Schema s = Schema.create(Schema.Type.LONG)
      LogicalTypes.localTimestampMicros().addToSchema(s)
      return s
    }
    if (clazz == Date) {
      Schema s = Schema.create(Schema.Type.LONG)
      LogicalTypes.timestampMillis().addToSchema(s)
      return s
    }
    if (clazz == UUID) {
      Schema s = Schema.create(Schema.Type.STRING)
      LogicalTypes.uuid().addToSchema(s)
      return s
    }
    // Fallback
    return Schema.create(Schema.Type.STRING)
  }
  // ----------------------------------------------------------------------
  // Row writing
  // ----------------------------------------------------------------------
  /**
   * Writes all Matrix rows to the Avro data file.
   *
   * <p>For each row in the Matrix, creates a GenericRecord and populates it
   * with converted Avro values for each column, then appends it to the writer.
   *
   * @param matrix the Matrix containing the data to write
   * @param dfw the Avro DataFileWriter to append records to
   * @param schema the Avro schema describing the record structure
   * @throws AvroConversionException if a value cannot be converted to its Avro type
   */
  private static void writeRows(Matrix matrix, DataFileWriter<GenericRecord> dfw, Schema schema) {
    GenericData.Record rec = new GenericData.Record(schema)
    Conversions.DecimalConversion decConv = new Conversions.DecimalConversion()
    // Unwrap nullable unions to actual field schema
    Map<String, Schema> fieldSchemas = [:]
    for (Schema.Field f : schema.getFields()) {
      Schema s = f.schema()
      if (s.getType() == Schema.Type.UNION) {
        for (Schema t : s.getTypes()) {
          if (t.getType() != Schema.Type.NULL) {
            s = t; break
          }
        }
      }
      fieldSchemas.put(f.name(), s)
    }
    List<String> cols = matrix.columnNames()
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      for (String col : cols) {
        Object v = matrix[r, col]
        Schema fs = fieldSchemas.get(col)
        try {
          if (!isCompatible(fs, v)) {
            throw new AvroSchemaException(
                'Value does not match schema type',
                col,
                schemaTypeLabel(fs),
                v?.getClass()?.simpleName ?: NULL_TYPE_NAME
            )
          }
          rec.put(col, toAvroValue(fs, v, decConv))
        } catch (AvroSchemaException e) {
          throw e
        } catch (Exception e) {
          throw new AvroConversionException(
              'Failed to convert value to Avro format',
              col,
              r,
              v?.getClass()?.simpleName ?: NULL_TYPE_NAME,
              fs.getType().name(),
              v,
              e
          )
        }
      }
      dfw.append(rec)
      rec = new GenericData.Record(schema) // fresh record per row
    }
  }
  /**
   * Converts a Java value to its Avro representation for writing.
   *
   * <p>This method handles the inverse of convertValue in MatrixAvroReader:
   * <ul>
   *   <li>null values pass through as null</li>
   *   <li>UNION types are unwrapped and the appropriate branch selected</li>
   *   <li>Logical types (date, time, timestamp, decimal) are converted to their storage format</li>
   *   <li>Complex types (ARRAY, MAP, RECORD) are recursively converted</li>
   *   <li>Primitives are converted or coerced as needed</li>
   * </ul>
   *
   * @param fieldSchema the Avro schema for the field
   * @param v the Java value to convert (may be null)
   * @param decConv the decimal conversion helper for BigDecimal values
   * @return the Avro-compatible value ready for writing
   * @throws UnresolvedUnionException if value cannot be matched to any union branch
   */
  private static Object toAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv) {
    if (v == null) {
      return null
    }
    if (fieldSchema.getType() == Schema.Type.UNION) {
      return toUnionAvroValue(fieldSchema, v, decConv)
    }
    Object logicalValue = toLogicalAvroValue(fieldSchema, v, decConv)
    if (!NO_AVRO_VALUE.is(logicalValue)) {
      return logicalValue
    }
    toPrimitiveAvroValue(fieldSchema, v, decConv)
  }
  private static Object toUnionAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv) {
    List<Schema> types = fieldSchema.getTypes()
    if (types.size() == 2 && (types[0].getType() == Schema.Type.NULL || types[1].getType() == Schema.Type.NULL)) {
      Schema nonNull = (types[0].getType() == Schema.Type.NULL) ? types[1] : types[0]
      return toAvroValue(nonNull, v, decConv)
    }
    Schema branch = types.find { Schema candidate ->
      candidate.getType() != Schema.Type.NULL && isCompatible(candidate, v)
    }
    if (branch != null) {
      return toAvroValue(branch, v, decConv)
    }
    throw new UnresolvedUnionException(fieldSchema, v)
  }
  private static Object toLogicalAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv) {
    def lt = fieldSchema.getLogicalType()
    if (lt == null) {
      return NO_AVRO_VALUE
    }
    switch (lt.getName()) {
      case 'date' -> toDateAvroValue(v)
      case 'time-millis' -> toTimeMillisAvroValue(v)
      case 'local-timestamp-micros' -> toLocalTimestampMicrosAvroValue(v)
      case 'timestamp-millis' -> toTimestampMillisAvroValue(v)
      case 'local-timestamp-millis' -> toLocalTimestampMillisAvroValue(v)
      case 'uuid' -> v.toString()
      case 'decimal' -> toDecimalAvroValue(fieldSchema, v, (LogicalTypes.Decimal) lt, decConv)
      default -> NO_AVRO_VALUE
    }
  }
  private static Object toDateAvroValue(Object v) {
    Object value = java.sql.Date.isInstance(v) ? ((java.sql.Date) v).toLocalDate() : v
    LocalDate.isInstance(value) ? (int) ((LocalDate) value).toEpochDay() : NO_AVRO_VALUE
  }
  private static Object toTimeMillisAvroValue(Object v) {
    Object value = Time.isInstance(v) ? ((Time) v).toLocalTime() : v
    if (!LocalTime.isInstance(value)) {
      return NO_AVRO_VALUE
    }
    int nanosMs = ((LocalTime) value).getNano().intdiv(NANOS_PER_MILLI)
    (int) (((LocalTime) value).toSecondOfDay() * MILLIS_PER_SECOND + nanosMs)
  }
  private static Object toLocalTimestampMicrosAvroValue(Object v) {
    if (!LocalDateTime.isInstance(v)) {
      return NO_AVRO_VALUE
    }
    int nanosUs = ((LocalDateTime) v).getNano().intdiv(NANOS_PER_MICRO)
    ((LocalDateTime) v).toEpochSecond(ZoneOffset.UTC) * MICROS_PER_SECOND + nanosUs
  }
  private static Object toTimestampMillisAvroValue(Object v) {
    if (Date.isInstance(v)) {
      return ((Date) v).getTime()
    }
    if (Instant.isInstance(v)) {
      return ((Instant) v).toEpochMilli()
    }
    if (!LocalDateTime.isInstance(v)) {
      return NO_AVRO_VALUE
    }
    ((LocalDateTime) v).toInstant(ZoneOffset.UTC).toEpochMilli()
  }
  private static Object toLocalTimestampMillisAvroValue(Object v) {
    if (!LocalDateTime.isInstance(v)) {
      return NO_AVRO_VALUE
    }
    int nanosMs = ((LocalDateTime) v).getNano().intdiv(NANOS_PER_MILLI)
    ((LocalDateTime) v).toEpochSecond(ZoneOffset.UTC) * MILLIS_PER_SECOND + nanosMs
  }
  private static Object toDecimalAvroValue(Schema fieldSchema, Object v, LogicalTypes.Decimal dec,
                                           Conversions.DecimalConversion decConv) {
    if (BigDecimal.isInstance(v)) {
      return decConv.toBytes((BigDecimal) v, fieldSchema, dec)
    }
    if (Double.isInstance(v) || Float.isInstance(v)) {
      BigDecimal bd = new BigDecimal(((Number) v).toString()).setScale(dec.getScale(), RoundingMode.HALF_UP)
      return decConv.toBytes(bd, fieldSchema, dec)
    }
    NO_AVRO_VALUE
  }
  private static Object toPrimitiveAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv) {
    switch (fieldSchema.getType()) {
      case Schema.Type.STRING -> v.toString()
      case Schema.Type.BOOLEAN -> (Boolean) v
      case Schema.Type.INT -> Number.isInstance(v) ? ((Number) v).intValue() : v.toString()
      case Schema.Type.LONG -> toLongAvroValue(v)
      case Schema.Type.FLOAT -> Number.isInstance(v) ? ((Number) v).floatValue() : v.toString()
      case Schema.Type.DOUBLE -> Number.isInstance(v) ? ((Number) v).doubleValue() : v.toString()
      case Schema.Type.BYTES -> toBytesAvroValue(v)
      case Schema.Type.ARRAY -> toArrayAvroValue(fieldSchema, (List) v, decConv)
      case Schema.Type.MAP -> toMapAvroValue(fieldSchema, (Map) v, decConv)
      case Schema.Type.RECORD -> toRecordAvroValue(fieldSchema, (Map) v, decConv)
      default -> v.toString()
    }
  }
  private static Object toLongAvroValue(Object v) {
    if (BigInteger.isInstance(v)) {
      return ((BigInteger) v).longValue()
    }
    if (Number.isInstance(v)) {
      return ((Number) v).longValue()
    }
    if (Date.isInstance(v)) {
      return ((Date) v).time
    }
    Instant.isInstance(v) ? ((Instant) v).toEpochMilli() : v.toString()
  }
  private static Object toBytesAvroValue(Object v) {
    if (byte[].isInstance(v)) {
      return ByteBuffer.wrap((byte[]) v)
    }
    if (ByteBuffer.isInstance(v)) {
      return v
    }
    BigDecimal.isInstance(v) ? ByteBuffer.wrap(((BigDecimal) v).unscaledValue().toByteArray()) : v.toString()
  }
  private static List toArrayAvroValue(Schema fieldSchema, List input, Conversions.DecimalConversion decConv) {
    Schema elem = fieldSchema.getElementType()
    input?.collect { Object e -> toAvroValue(elem, e, decConv) } ?: []
  }
  private static Map<String, Object> toMapAvroValue(Schema fieldSchema, Map input, Conversions.DecimalConversion decConv) {
    Schema vs = fieldSchema.getValueType()
    Map<String, Object> outMap = [:]
    input?.each { key, value ->
      outMap[key?.toString()] = toAvroValue(vs, value, decConv)
    }
    outMap
  }
  private static GenericData.Record toRecordAvroValue(Schema fieldSchema, Map input,
                                                      Conversions.DecimalConversion decConv) {
    GenericData.Record record = new GenericData.Record(fieldSchema)
    fieldSchema.getFields().each { Schema.Field field ->
      def value = input == null ? null : input.get(field.name())
      record.put(field.name(), toAvroValue(AvroSchemaUtil.nonNullSchema(field.schema()), value, decConv))
    }
    record
  }
  private static Map<String, ColumnProfile> analyzeColumns(Matrix matrix, boolean inferPrecisionAndScale) {
    Map<String, ColumnProfile> profiles = [:]
    for (String col : matrix.columnNames()) {
      profiles.put(col, analyzeColumn(matrix, col, inferPrecisionAndScale))
    }
    return profiles
  }
  private static ColumnProfile analyzeColumn(Matrix matrix, String col, boolean inferPrecisionAndScale) {
    Class<?> declared = normalizeType(matrix.type(col))
    ColumnProfile profile = new ColumnProfile(col, declared)
    if (declared != Object && declared != Number) {
      profile.effectiveType = declared
      if (declared == BigDecimal && inferPrecisionAndScale) {
        scanDecimalPrecision(matrix, col, profile)
      } else if (declared == List) {
        scanListElement(matrix, col, profile)
      } else if (declared == Map) {
        scanMapDetails(matrix, col, profile)
      }
      return profile
    }
    boolean sawBigDecimal = false
    boolean sawFloat = false
    boolean sawIntegral = false
    boolean needsLong = false
    boolean fixedType = false
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      Object v = matrix[r, col]
      if (v == null) {
        continue
      }
      if (!fixedType) {
        if (BigDecimal.isInstance(v)) {
          sawBigDecimal = true
          if (inferPrecisionAndScale) {
            updateDecimalMeta((BigDecimal) v, profile)
          }
          continue
        }
        if (Float.isInstance(v) || Double.isInstance(v)) {
          sawFloat = true
          continue
        }
        if (Byte.isInstance(v) || Short.isInstance(v) || Integer.isInstance(v)
            || Long.isInstance(v) || BigInteger.isInstance(v)) {
          sawIntegral = true
          long lv = (BigInteger.isInstance(v)) ? ((BigInteger) v).longValue() : ((Number) v).longValue()
          if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE || Long.isInstance(v) || BigInteger.isInstance(v)) {
            needsLong = true
          }
          continue
        }
        if (String.isInstance(v) || Boolean.isInstance(v) || byte[].isInstance(v)
            || java.sql.Date.isInstance(v) || Time.isInstance(v) || Date.isInstance(v)
            || LocalDate.isInstance(v) || LocalTime.isInstance(v)
            || Instant.isInstance(v) || LocalDateTime.isInstance(v)
            || UUID.isInstance(v)) {
          profile.effectiveType = v.getClass()
          fixedType = true
          break
        }
        if (List.isInstance(v)) {
          profile.effectiveType = List
          fixedType = true
          scanListElementValue((List) v, profile)
          if (profile.listElemClass != null) {
            break
          }
          continue
        }
        if (Map.isInstance(v)) {
          profile.effectiveType = Map
          fixedType = true
          scanMapValue((Map) v, profile)
          continue
        }
        profile.effectiveType = String
        fixedType = true
        break
      } else if (profile.effectiveType == Map) {
        if (Map.isInstance(v)) {
          scanMapValue((Map) v, profile)
        }
      } else if (profile.effectiveType == List) {
        if (List.isInstance(v) && profile.listElemClass == null) {
          scanListElementValue((List) v, profile)
          if (profile.listElemClass != null) {
            break
          }
        }
      }
    }
    if (!fixedType) {
      if (sawBigDecimal) {
        profile.effectiveType = BigDecimal
      } else if (sawFloat) {
        profile.effectiveType = Double
      } else if (sawIntegral) {
        profile.effectiveType = needsLong ? Long : Integer
      } else {
        profile.effectiveType = String
      }
    }
    return profile
  }
  private static void scanDecimalPrecision(Matrix matrix, String col, ColumnProfile profile) {
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      def v = matrix[r, col]
      if (BigDecimal.isInstance(v)) {
        updateDecimalMeta((BigDecimal) v, profile)
      }
    }
  }
  private static void scanListElement(Matrix matrix, String col, ColumnProfile profile) {
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      def v = matrix[r, col]
      if (List.isInstance(v)) {
        scanListElementValue((List) v, profile)
        if (profile.listElemClass != null) {
          return
        }
      }
    }
  }
  private static void scanMapDetails(Matrix matrix, String col, ColumnProfile profile) {
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      def v = matrix[r, col]
      if (Map.isInstance(v)) {
        scanMapValue((Map) v, profile)
      }
    }
  }
  private static void scanListElementValue(List list, ColumnProfile profile) {
    for (def e : list) {
      if (e != null) {
        profile.listElemClass = e.getClass()
        return
      }
    }
  }
  private static void scanMapValue(Map map, ColumnProfile profile) {
    if (!profile.recordSeen) {
      profile.recordSeen = true
      profile.recordLike = true
      profile.recordSample = map
      profile.recordKeys = new LinkedHashSet<>(map.keySet()*.toString())
    } else if (profile.recordLike) {
      Set<String> keys = new LinkedHashSet<>(map.keySet()*.toString())
      if (profile.recordKeys != keys) {
        profile.recordLike = false
      }
    }
    if (profile.mapValueClass == null) {
      for (def e : map.values()) {
        if (e != null) {
          profile.mapValueClass = e.getClass()
          break
        }
      }
    }
  }
  private static void updateDecimalMeta(BigDecimal value, ColumnProfile profile) {
    profile.sawDecimal = true
    int scale = value.scale()
    profile.maxScale = Math.max(profile.maxScale, scale)
    int integerDigits = value.precision() - scale
    if (integerDigits < 0) {
      integerDigits = 0
    }
    profile.maxIntegerDigits = Math.max(profile.maxIntegerDigits, integerDigits)
  }
  private static Class<?> normalizeType(Class<?> clazz) {
    return clazz == BigInteger ? Long : clazz
  }
  private static void validateDeclaredColumnSchemas(Matrix matrix, Map<String, AvroSchemaDecl> declaredSchemas) {
    Set<String> matrixColumns = matrix.columnNames() as Set<String>
    declaredSchemas.keySet().each { String columnName ->
      if (!matrixColumns.contains(columnName)) {
        throw new IllegalArgumentException("columnSchemas['$columnName'] does not match any Matrix column")
      }
    }
  }
  /**
   * Checks if a Java value is compatible with an Avro schema type.
   *
   * <p>Used for union type resolution to find the appropriate branch.
   * Compatibility rules are lenient for numeric types (any Number matches
   * LONG, FLOAT, DOUBLE) but strict for other types.
   *
   * @param s the Avro schema to check against
   * @param v the Java value to check (may be null)
   * @return true if the value can be serialized under this schema
   */
  private static boolean isCompatible(Schema s, Object v) {
    if (v == null) {
      return true
    }
    if (s.getType() == Schema.Type.UNION) {
      for (Schema branch : s.getTypes()) {
        if (isCompatible(branch, v)) {
          return true
        }
      }
      return false
    }
    def logical = s.getLogicalType()
    if (logical != null) {
      String name = logical.getName()
      return switch (name) {
        case 'date' -> LocalDate.isInstance(v) || java.sql.Date.isInstance(v) || Number.isInstance(v)
        case 'time-millis', 'time-micros' -> LocalTime.isInstance(v) || Time.isInstance(v) || Number.isInstance(v)
        case 'timestamp-millis', 'timestamp-micros' ->
          Instant.isInstance(v) || Date.isInstance(v) || LocalDateTime.isInstance(v) || Number.isInstance(v)
        case 'local-timestamp-millis', 'local-timestamp-micros' -> LocalDateTime.isInstance(v) || Number.isInstance(v)
        case 'uuid' -> UUID.isInstance(v) || String.isInstance(v)
        case 'decimal' -> BigDecimal.isInstance(v) || Double.isInstance(v) || Float.isInstance(v) ||
            byte[].isInstance(v) || ByteBuffer.isInstance(v)
        default -> false
      }
    }
    return switch (s.getType()) {
      case Schema.Type.STRING -> true // we'll toString() later
      case Schema.Type.BOOLEAN -> Boolean.isInstance(v)
      case Schema.Type.INT -> Byte.isInstance(v) || Short.isInstance(v) || Integer.isInstance(v)
      case Schema.Type.LONG -> Number.isInstance(v) || Date.isInstance(v) || Instant.isInstance(v)
      case Schema.Type.FLOAT -> Number.isInstance(v)
      case Schema.Type.DOUBLE -> Number.isInstance(v) || BigDecimal.isInstance(v)
      case Schema.Type.BYTES -> (byte[].isInstance(v)) || (ByteBuffer.isInstance(v)) || (BigDecimal.isInstance(v))
      case Schema.Type.ARRAY -> isArrayCompatible(s, v)
      case Schema.Type.MAP -> isMapCompatible(s, v)
      case Schema.Type.RECORD -> isRecordCompatible(s, v)
      case Schema.Type.FIXED -> GenericFixed.isInstance(v)
      default -> false
    }
  }
  private static boolean isArrayCompatible(Schema schema, Object value) {
    if (!(List.isInstance(value))) {
      return false
    }
    Schema elem = schema.getElementType()
    ((List) value).every { Object item -> isCompatible(elem, item) }
  }
  private static boolean isMapCompatible(Schema schema, Object value) {
    if (!(Map.isInstance(value))) {
      return false
    }
    Schema valueSchema = schema.getValueType()
    ((Map) value).entrySet().every { Map.Entry entry -> isCompatible(valueSchema, entry.value) }
  }
  private static boolean isRecordCompatible(Schema schema, Object value) {
    if (GenericRecord.isInstance(value)) {
      return true
    }
    if (!(Map.isInstance(value))) {
      return false
    }
    Map input = (Map) value
    schema.getFields().every { Schema.Field field -> isCompatible(field.schema(), input.get(field.name())) }
  }
  /**
   * Produces a human-readable label for a schema type, preferring logical types.
   */
  private static String schemaTypeLabel(Schema schema) {
    if (schema.getType() == Schema.Type.UNION) {
      List<String> parts = []
      for (Schema branch : schema.getTypes()) {
        parts.add(schemaTypeLabel(branch))
      }
      return 'UNION[' + String.join(', ', parts) + ']'
    }
    def logical = schema.getLogicalType()
    if (logical != null) {
      return logical.getName()
    }
    return schema.getType().name()
  }

  private static final class NonClosingOutputStream extends FilterOutputStream {

    private NonClosingOutputStream(OutputStream out) {
      super(out)
    }

    @Override
    void close() throws IOException {
      flush()
    }
  }

}
