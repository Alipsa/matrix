package se.alipsa.matrix.avro

import org.apache.avro.Conversions
import org.apache.avro.LogicalType
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
import se.alipsa.matrix.core.util.DecimalColumnProfile

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
      fieldSchema = declaredSchema != null
          ? declaredSchema.toAvroSchema(col, namespace)
          : inferredFieldSchema(profiles.get(col), col, inferPrecisionAndScale, namespace)
      Schema nullable = AvroSchemaUtil.nullableSchema(fieldSchema)
      fields.add(new Schema.Field(col, nullable, null as String, (Object) null))
    }
    record.setFields(fields)
    return record
  }
  private static Schema inferredFieldSchema(ColumnProfile profile, String columnName,
                                             boolean inferPrecisionAndScale, String namespace) {
    Class<?> clazz = profile.effectiveType
    if (clazz == List) {
      return listFieldSchema(profile)
    }
    if (clazz == Map) {
      return profile.recordLike ? recordFieldSchema(profile, columnName, namespace) : mapFieldSchema(profile)
    }
    int[] metadata = (clazz == BigInteger || (clazz == BigDecimal && (inferPrecisionAndScale || profile.forceDecimal)))
        ? profile.decimalMeta() : null
    toFieldSchema(clazz, metadata)
  }
  private static Schema listFieldSchema(ColumnProfile profile) {
    Class<?> elementClass = profile.listElemClass ?: String
    NestedNumericProfile numericProfile = profile.listNumericProfile
    boolean needsDecimal = (profile.inferPrecisionAndScale || numericProfile?.hasBigInteger) &&
        numericProfile?.decimalProfile?.hasValues
    Schema elementSchema = toFieldSchema(elementClass,
        needsDecimal || elementClass == BigInteger ? decimalMeta(numericProfile?.decimalProfile, elementClass == BigInteger) : null)
    Schema.createArray(AvroSchemaUtil.nullableSchema(elementSchema))
  }
  private static Schema mapFieldSchema(ColumnProfile profile) {
    Class<?> valueClass = profile.mapValueClass ?: String
    NestedNumericProfile numericProfile = profile.mapValueNumericProfile
    boolean needsDecimal = (profile.inferPrecisionAndScale || numericProfile?.hasBigInteger) &&
        numericProfile?.decimalProfile?.hasValues
    Schema valueSchema = toFieldSchema(valueClass,
        needsDecimal || valueClass == BigInteger ? decimalMeta(numericProfile?.decimalProfile, valueClass == BigInteger) : null)
    Schema.createMap(AvroSchemaUtil.nullableSchema(valueSchema))
  }
  private static Schema recordFieldSchema(ColumnProfile profile, String columnName, String namespace) {
    Schema record = Schema.createRecord(columnName + '_record', null, namespace, false)
    List<Schema.Field> fields = []
    profile.recordSample.keySet().each { Object key ->
      String fieldName = String.valueOf(key)
      AvroSchemaUtil.validateAvroFieldName(fieldName, "${columnName}.${fieldName}")
      Class<?> valueClass = profile.recordFieldClasses[fieldName] ?: String
      NestedNumericProfile numericProfile = profile.recordNumericProfiles[fieldName]
      boolean needsDecimal = (profile.inferPrecisionAndScale || numericProfile?.hasBigInteger) &&
          numericProfile?.decimalProfile?.hasValues
      Schema valueSchema = toFieldSchema(valueClass,
          needsDecimal || valueClass == BigInteger ? decimalMeta(numericProfile?.decimalProfile, valueClass == BigInteger) : null)
      fields << new Schema.Field(fieldName, AvroSchemaUtil.nullableSchema(valueSchema), null as String, (Object) null)
    }
    record.setFields(fields)
    record
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
    if (clazz == BigInteger) {
      return bigIntegerFieldSchema(decimalMeta)
    }
    if (clazz == BigDecimal) {
      return decimalMeta != null ? decimalFieldSchema(decimalMeta) : Schema.create(Schema.Type.DOUBLE) // fallback like Parquet writer
    }
    Schema primitive = primitiveFieldSchema(clazz)
    if (primitive != null) {
      return primitive
    }
    Schema logical = logicalFieldSchema(clazz)
    // Fallback
    logical != null ? logical : Schema.create(Schema.Type.STRING)
  }
  private static Schema decimalFieldSchema(int[] decimalMeta) {
    int precision = decimalMeta[0] > 0 ? decimalMeta[0] : 10
    int scale = decimalMeta[1] >= 0 ? decimalMeta[1] : 2
    Schema s = Schema.create(Schema.Type.BYTES)
    LogicalTypes.decimal(precision, scale).addToSchema(s)
    s
  }
  private static Schema bigIntegerFieldSchema(int[] decimalMeta) {
    int precision = decimalMeta != null && decimalMeta[0] > 0 ? decimalMeta[0] : 10
    Schema schema = decimalFieldSchema([precision, 0] as int[])
    schema.addProp(AvroSchemaUtil.JAVA_TYPE_PROPERTY, AvroSchemaUtil.BIG_INTEGER_JAVA_TYPE)
    schema
  }
  private static int[] decimalMeta(DecimalColumnProfile profile, boolean scaleZero) {
    if (profile == null || !profile.hasValues) {
      return [10, 0] as int[]
    }
    [profile.precision, scaleZero ? 0 : profile.scale] as int[]
  }
  private static Schema primitiveFieldSchema(Class<?> clazz) {
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
    null
  }
  private static Schema logicalFieldSchema(Class<?> clazz) {
    if (clazz == LocalDate || clazz == java.sql.Date) {
      return withLogicalType(Schema.Type.INT, LogicalTypes.date())
    }
    if (clazz == LocalTime || clazz == Time) {
      return withLogicalType(Schema.Type.INT, LogicalTypes.timeMillis())
    }
    if (clazz == Instant || clazz == Date) {
      return withLogicalType(Schema.Type.LONG, LogicalTypes.timestampMillis())
    }
    if (clazz == LocalDateTime) {
      return withLogicalType(Schema.Type.LONG, LogicalTypes.localTimestampMicros())
    }
    if (clazz == UUID) {
      return withLogicalType(Schema.Type.STRING, LogicalTypes.uuid())
    }
    null
  }
  private static Schema withLogicalType(Schema.Type type, LogicalType logicalType) {
    Schema s = Schema.create(type)
    logicalType.addToSchema(s)
    s
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
    Map<String, Schema> fieldSchemas = [:]
    for (Schema.Field f : schema.getFields()) {
      fieldSchemas.put(f.name(), f.schema())
    }
    List<String> cols = matrix.columnNames()
    int[] colIndexes = new int[cols.size()]
    for (int i = 0; i < cols.size(); i++) {
      colIndexes[i] = matrix.columnIndex(cols.get(i))
    }
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols.size(); c++) {
        String col = cols.get(c)
        Object v = matrix.get(r, colIndexes[c])
        Schema fs = fieldSchemas.get(col)
        try {
          if (!isCompatible(fs, v)) {
            throw new AvroSchemaException(
                'Value does not match schema type',
                col,
              AvroSchemaUtil.schemaTypeLabel(fs),
                v?.getClass()?.simpleName ?: NULL_TYPE_NAME
            )
          }
          rec.put(col, toAvroValue(fs, v, decConv, col))
        } catch (AvroSchemaException e) {
          throw e.withRowNumber(r)
        } catch (Exception e) {
          throw new AvroConversionException(
              'Failed to convert value to Avro format',
              col,
              r,
              v?.getClass()?.simpleName ?: NULL_TYPE_NAME,
              AvroSchemaUtil.schemaTypeLabel(fs),
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
  private static Object toAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv, String columnName) {
    if (v == null) {
      return null
    }
    if (fieldSchema.getType() == Schema.Type.UNION) {
      return toUnionAvroValue(fieldSchema, v, decConv, columnName)
    }
    Object logicalValue = toLogicalAvroValue(fieldSchema, v, decConv, columnName)
    if (!NO_AVRO_VALUE.is(logicalValue)) {
      return logicalValue
    }
    toPrimitiveAvroValue(fieldSchema, v, decConv, columnName)
  }
  private static Object toUnionAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv,
                                         String columnName) {
    List<Schema> types = fieldSchema.getTypes()
    if (types.size() == 2 && (types[0].getType() == Schema.Type.NULL || types[1].getType() == Schema.Type.NULL)) {
      Schema nonNull = (types[0].getType() == Schema.Type.NULL) ? types[1] : types[0]
      return toAvroValue(nonNull, v, decConv, columnName)
    }
    Schema branch = types.find { Schema candidate ->
      candidate.getType() != Schema.Type.NULL && isCompatible(candidate, v)
    }
    if (branch != null) {
      return toAvroValue(branch, v, decConv, columnName)
    }
    throw new UnresolvedUnionException(fieldSchema, v)
  }
  private static Object toLogicalAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv,
                                           String columnName) {
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
      case 'decimal' -> toDecimalAvroValue(fieldSchema, v, (LogicalTypes.Decimal) lt, decConv, columnName)
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
                                           Conversions.DecimalConversion decConv, String columnName) {
    if (Number.isInstance(v)) {
      BigDecimal value = decimalValue((Number) v, columnName).setScale(dec.getScale(), RoundingMode.HALF_UP)
      return decConv.toBytes(value, fieldSchema, dec)
    }
    NO_AVRO_VALUE
  }
  private static Object toPrimitiveAvroValue(Schema fieldSchema, Object v, Conversions.DecimalConversion decConv,
                                             String columnName) {
    switch (fieldSchema.getType()) {
      case Schema.Type.STRING -> v.toString()
      case Schema.Type.BOOLEAN -> (Boolean) v
      case Schema.Type.INT -> Number.isInstance(v) ? ((Number) v).intValue() : v.toString()
      case Schema.Type.LONG -> toLongAvroValue(v, columnName)
      case Schema.Type.FLOAT -> Number.isInstance(v) ? ((Number) v).floatValue() : v.toString()
      case Schema.Type.DOUBLE -> Number.isInstance(v) ? ((Number) v).doubleValue() : v.toString()
      case Schema.Type.BYTES -> toBytesAvroValue(v)
      case Schema.Type.ARRAY -> toArrayAvroValue(fieldSchema, (List) v, decConv, columnName)
      case Schema.Type.MAP -> toMapAvroValue(fieldSchema, (Map) v, decConv, columnName)
      case Schema.Type.RECORD -> toRecordAvroValue(fieldSchema, (Map) v, decConv, columnName)
      default -> v.toString()
    }
  }
  private static Object toLongAvroValue(Object v, String columnName) {
    if (Number.isInstance(v)) {
      if (NumericKinds.isDirectLong(v)) {
        return ((Number) v).longValue()
      }
      return decimalValue((Number) v, columnName).longValueExact()
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
  private static List toArrayAvroValue(Schema fieldSchema, List input, Conversions.DecimalConversion decConv,
                                       String columnName) {
    Schema elem = fieldSchema.getElementType()
    input?.collect { Object e -> toAvroValue(elem, e, decConv, columnName) } ?: []
  }
  private static Map<String, Object> toMapAvroValue(Schema fieldSchema, Map input, Conversions.DecimalConversion decConv,
                                                     String columnName) {
    Schema vs = fieldSchema.getValueType()
    Map<String, Object> outMap = [:]
    input?.each { key, value ->
      outMap[key?.toString()] = toAvroValue(vs, value, decConv, columnName)
    }
    outMap
  }
  private static GenericData.Record toRecordAvroValue(Schema fieldSchema, Map input,
                                                      Conversions.DecimalConversion decConv, String columnName) {
    GenericData.Record record = new GenericData.Record(fieldSchema)
    fieldSchema.getFields().each { Schema.Field field ->
      def value = input == null ? null : input.get(field.name())
      record.put(field.name(), toAvroValue(field.schema(), value, decConv, columnName))
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
    profile.inferPrecisionAndScale = inferPrecisionAndScale
    if (declared != Object && declared != Number) {
      applyDeclaredType(matrix, col, declared, profile)
      profileDecimalColumn(matrix, col, profile)
      finalizeNestedProfiles(profile)
      return profile
    }
    scanUntypedColumn(matrix, col, profile)
    profileDecimalColumn(matrix, col, profile)
    finalizeNestedProfiles(profile)
    return profile
  }
  private static void applyDeclaredType(Matrix matrix, String col, Class<?> declared, ColumnProfile profile) {
    profile.effectiveType = declared
    if (declared == List) {
      scanListElement(matrix, col, profile)
    } else if (declared == Map) {
      scanMapDetails(matrix, col, profile)
    }
  }
  private static void scanUntypedColumn(Matrix matrix, String col, ColumnProfile profile) {
    TypeScanState state = new TypeScanState()
    int colIndex = matrix.columnIndex(col)
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      Object v = matrix.get(r, colIndex)
      if (v == null) {
        continue
      }
      boolean stop = state.fixedType
          ? continueFixedTypeScan(v, profile)
          : scanUntypedValue(v, profile, state)
      if (stop) {
        break
      }
    }
    if (!state.fixedType) {
      profile.effectiveType = state.numerics.hasValues() ? state.numerics.schemaClass() : String
    }
  }
  private static boolean scanUntypedValue(Object v, ColumnProfile profile, TypeScanState state) {
    if (BigDecimal.isInstance(v)) {
      state.numerics.include((Number) v, (BigDecimal) v)
      return false
    }
    if (Float.isInstance(v) || Double.isInstance(v)) {
      BigDecimal decimal = decimalValue((Number) v, profile.name)
      state.numerics.include((Number) v, decimal)
      return false
    }
    if (NumericKinds.isIntegral(v)) {
      state.numerics.include((Number) v, decimalValue((Number) v, profile.name))
      return false
    }
    if (isFixedScalarValue(v)) {
      profile.effectiveType = v.getClass()
      state.fixedType = true
      return true
    }
    if (List.isInstance(v)) {
      profile.effectiveType = List
      state.fixedType = true
      scanListElementValue((List) v, profile)
      return false
    }
    if (Map.isInstance(v)) {
      profile.effectiveType = Map
      state.fixedType = true
      scanMapValue((Map) v, profile)
      return false
    }
    profile.effectiveType = String
    state.fixedType = true
    true
  }
  private static boolean continueFixedTypeScan(Object v, ColumnProfile profile) {
    if (profile.effectiveType == Map) {
      if (Map.isInstance(v)) {
        scanMapValue((Map) v, profile)
      }
      return false
    }
    if (profile.effectiveType == List && List.isInstance(v)) {
      scanListElementValue((List) v, profile)
      return false
    }
    false
  }
  private static boolean isFixedScalarValue(Object v) {
    String.isInstance(v) || Boolean.isInstance(v) || byte[].isInstance(v)
        || java.sql.Date.isInstance(v) || Time.isInstance(v) || Date.isInstance(v)
        || LocalDate.isInstance(v) || LocalTime.isInstance(v)
        || Instant.isInstance(v) || LocalDateTime.isInstance(v)
        || UUID.isInstance(v)
  }
  private static final class TypeScanState {
    final NestedNumericProfile numerics = new NestedNumericProfile()
    boolean fixedType
  }
  private static void profileDecimalColumn(Matrix matrix, String col, ColumnProfile profile) {
    if (profile.effectiveType == BigDecimal || profile.effectiveType == BigInteger) {
      List<Number> sourceValues = matrix.column(col).findAll { Number.isInstance(it) } as List<Number>
      List<Number> values = sourceValues.collect { Number value ->
        decimalValue(value, col)
      } as List<Number>
      profile.decimalProfile = DecimalColumnProfile.profile(values)
      profile.forceDecimal = profile.effectiveType == BigInteger || sourceValues.any { BigInteger.isInstance(it) }
    }
  }
  private static void scanListElement(Matrix matrix, String col, ColumnProfile profile) {
    int colIndex = matrix.columnIndex(col)
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      def v = matrix.get(r, colIndex)
      if (List.isInstance(v)) {
        scanListElementValue((List) v, profile)
      }
    }
  }
  private static void scanMapDetails(Matrix matrix, String col, ColumnProfile profile) {
    int colIndex = matrix.columnIndex(col)
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      def v = matrix.get(r, colIndex)
      if (Map.isInstance(v)) {
        scanMapValue((Map) v, profile)
      }
    }
  }
  private static void scanListElementValue(List list, ColumnProfile profile) {
    for (def e : list) {
      if (e != null) {
        if (profile.listElemClass == null) {
          profile.listElemClass = e.getClass()
        }
        if (Number.isInstance(e)) {
          BigDecimal decimal = decimalValue((Number) e, "${profile.name} list element")
          if (profile.listNumericProfile == null) {
            profile.listNumericProfile = new NestedNumericProfile()
          }
          profile.listNumericProfile.include((Number) e, decimal)
        } else {
          profile.listHasNonNumeric = true
        }
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
    map.each { key, value ->
      String fieldName = String.valueOf(key)
      if (value != null && !profile.recordFieldClasses.containsKey(fieldName)) {
        profile.recordFieldClasses[fieldName] = value.getClass()
      }
      if (Number.isInstance(value)) {
        BigDecimal decimal = decimalValue((Number) value, "${profile.name}.$fieldName")
        NestedNumericProfile numericProfile = profile.recordNumericProfiles[fieldName]
        if (numericProfile == null) {
          numericProfile = new NestedNumericProfile()
          profile.recordNumericProfiles[fieldName] = numericProfile
        }
        numericProfile.include((Number) value, decimal)
      } else if (value != null) {
        profile.recordHasNonNumeric[fieldName] = true
      }
    }
  }
  private static void finalizeNestedProfiles(ColumnProfile profile) {
    if (profile.listNumericProfile != null && !profile.listHasNonNumeric) {
      profile.listElemClass = profile.listNumericProfile.schemaClass()
    }
    if (profile.recordLike) {
      // record schemas use the per-field profiles; the whole-map profile is never read
      profile.mapValueNumericProfile = null
      profile.recordNumericProfiles.each { String fieldName, NestedNumericProfile numericProfile ->
        if (!profile.recordHasNonNumeric[fieldName]) {
          profile.recordFieldClasses[fieldName] = numericProfile.schemaClass()
        }
      }
    } else {
      profile.mapValueNumericProfile = NestedNumericProfile.merge(profile.recordNumericProfiles.values())
      if (profile.mapValueNumericProfile != null && !profile.mapValuesHaveNonNumeric) {
        profile.mapValueClass = profile.mapValueNumericProfile.schemaClass()
      }
    }
  }
  private static Class<?> normalizeType(Class<?> clazz) {
    clazz
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
      return isUnionCompatible(s, v)
    }
    def logical = s.getLogicalType()
    if (logical != null) {
      return isLogicalTypeCompatible(logical.getName(), v)
    }
    isPlainTypeCompatible(s, v)
  }
  private static boolean isUnionCompatible(Schema s, Object v) {
    for (Schema branch : s.getTypes()) {
      if (isCompatible(branch, v)) {
        return true
      }
    }
    false
  }
  private static boolean isLogicalTypeCompatible(String name, Object v) {
    switch (name) {
      case 'date' -> LocalDate.isInstance(v) || java.sql.Date.isInstance(v) || Number.isInstance(v)
      case 'time-millis', 'time-micros' -> LocalTime.isInstance(v) || Time.isInstance(v) || Number.isInstance(v)
      case 'timestamp-millis' ->
        Instant.isInstance(v) || Date.isInstance(v) || LocalDateTime.isInstance(v) || Number.isInstance(v)
      case 'timestamp-micros' ->
        Instant.isInstance(v) || Date.isInstance(v) || Number.isInstance(v)
      case 'local-timestamp-millis', 'local-timestamp-micros' -> LocalDateTime.isInstance(v) || Number.isInstance(v)
      case 'uuid' -> UUID.isInstance(v) || String.isInstance(v)
      case 'decimal' -> Number.isInstance(v) || byte[].isInstance(v) || ByteBuffer.isInstance(v)
      default -> false
    }
  }
  private static boolean isPlainTypeCompatible(Schema s, Object v) {
    switch (s.getType()) {
      case Schema.Type.STRING -> true // we'll toString() later
      case Schema.Type.BOOLEAN -> Boolean.isInstance(v)
      case Schema.Type.INT -> Byte.isInstance(v) || Short.isInstance(v) || Integer.isInstance(v)
      case Schema.Type.LONG -> isExactLongCompatible(v) || Date.isInstance(v) || Instant.isInstance(v)
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
  private static boolean isExactLongCompatible(Object value) {
    if (!Number.isInstance(value)) {
      return false
    }
    if (NumericKinds.isDirectLong(value)) {
      return true
    }
    try {
      decimalValue((Number) value, null).longValueExact()
      true
    } catch (ArithmeticException | AvroSchemaException ignored) {
      false
    }
  }
  private static BigDecimal decimalValue(Number value, String location) {
    if ((Double.isInstance(value) && !Double.isFinite((Double) value)) ||
        (Float.isInstance(value) && !Float.isFinite((Float) value))) {
      throw new AvroSchemaException('Non-finite decimal value', location, 'finite Number', String.valueOf(value))
    }
    BigDecimal.isInstance(value) ? (BigDecimal) value : new BigDecimal(value.toString())
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
