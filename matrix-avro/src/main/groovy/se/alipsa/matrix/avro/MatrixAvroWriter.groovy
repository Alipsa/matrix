package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
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
import java.util.Collections
import java.util.WeakHashMap

/**
 * Writes Matrix objects to Avro Object Container Files (OCF).
 *
 * <p>Supports writing to File, Path, OutputStream, and byte arrays.
 * Handles conversion of Java types to appropriate Avro types, including
 * support for logical types (date, time, timestamp, decimal, UUID).
 *
 * <p>Example usage:
 * <pre>{@code
 * Matrix m = Matrix.builder("data")
 *     .columnNames(["id", "name", "price"])
 *     .rows([[1, "Alice", 10.50], [2, "Bob", 20.75]])
 *     .types(Integer, String, BigDecimal)
 *     .build()
 *
 * // Write to file
 * MatrixAvroWriter.write(m, new File("data.avro"))
 *
 * // Write with decimal precision inference
 * MatrixAvroWriter.write(m, new File("data.avro"), true)
 *
 * // Write to byte array
 * byte[] bytes = MatrixAvroWriter.writeBytes(m)
 * }</pre>
 */
@CompileStatic
class MatrixAvroWriter {

  private static final Map<Matrix, Map<SchemaCacheKey, Schema>> SCHEMA_CACHE =
      Collections.synchronizedMap(new WeakHashMap<Matrix, Map<SchemaCacheKey, Schema>>())

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
      throw new IllegalArgumentException("Path cannot be null")
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
      throw new IllegalArgumentException("OutputStream cannot be null")
    }
    Schema schema = buildSchema(matrix, inferPrecisionAndScale)
    DataFileWriter<GenericRecord> dfw = new DataFileWriter<>(new GenericDatumWriter<GenericRecord>(schema))
    dfw.create(schema, out)
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
      throw new IllegalArgumentException("Options cannot be null")
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
      throw new IllegalArgumentException("Path cannot be null")
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
      throw new IllegalArgumentException("OutputStream cannot be null")
    }
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null")
    }
    Schema schema = buildSchema(matrix, options)
    DataFileWriter<GenericRecord> dfw = createDataFileWriter(schema, options)
    dfw.create(schema, out)
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
      throw new IllegalArgumentException("Options cannot be null")
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
      throw AvroValidationException.nullParameter("matrix")
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
      throw AvroValidationException.nullParameter("file")
    }
    File parentDir = file.parentFile
    if (parentDir != null && !parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        throw new IOException("Failed to create parent directory: ${parentDir.absolutePath}. " +
            "Check that you have write permissions and the path is valid.")
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
   * is wrapped in a nullable union ["null", T] to handle null values. The type
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
    return buildSchemaInternal(matrix, inferPrecisionAndScale, "MatrixSchema", "se.alipsa.matrix.avro")
  }

  /**
   * Builds an Avro schema for the given Matrix using options.
   *
   * @param matrix the Matrix to build a schema for
   * @param options the write options containing schema configuration
   * @return an Avro record schema suitable for writing the Matrix
   */
  static Schema buildSchema(Matrix matrix, AvroWriteOptions options) {
    return buildSchemaInternal(matrix, options.inferPrecisionAndScale, options.schemaName, options.namespace)
  }

  /**
   * Internal schema building with configurable name and namespace.
   */
  private static Schema buildSchemaInternal(Matrix matrix, boolean inferPrecisionAndScale,
                                            String schemaName, String namespace) {
    SchemaCacheKey cacheKey = new SchemaCacheKey(
        schemaName,
        namespace,
        inferPrecisionAndScale,
        matrix.rowCount(),
        matrix.columnNames(),
        matrix.types()
    )
    Schema cached = getCachedSchema(matrix, cacheKey)
    if (cached != null) {
      return cached
    }

    Schema record = Schema.createRecord(schemaName, "Generated by MatrixAvroWriter", namespace, false)
    List<Schema.Field> fields = new ArrayList<>(matrix.columnCount())

    Map<String, ColumnProfile> profiles = analyzeColumns(matrix, inferPrecisionAndScale)

    for (String col : matrix.columnNames()) {
      validateAvroFieldName(col, col)
      ColumnProfile profile = profiles.get(col)
      Class<?> clazz = profile.effectiveType
      Schema fieldSchema

      if (clazz == List) {
        Class<?> elemClass = profile.listElemClass ?: String
        Schema elemSchema = toFieldSchema(elemClass, null)
        // allow null elements in arrays
        Schema nullableElem = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), elemSchema))
        fieldSchema = Schema.createArray(nullableElem)
      } else if (clazz == Map) {
        if (profile.recordLike) {
          // RECORD: fixed fields from the first non-null row
          Map first = profile.recordSample
          def rec = Schema.createRecord(col + "_record", null, "se.alipsa.matrix.avro", false)
          List<Schema.Field> flds = new ArrayList<>()
          for (def k : first.keySet()) {
            String fieldName = k.toString()
            validateAvroFieldName(fieldName, "${col}.${fieldName}")
            def v = first.get(k)
            Class<?> vClazz = (v == null) ? String : v.getClass()
            Schema valueSchema = toFieldSchema(vClazz, null)
            // nullable union for each field
            Schema nullable = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), valueSchema))
            flds.add(new Schema.Field(fieldName, nullable, null as String, (Object) null))
          }
          rec.setFields(flds)
          fieldSchema = rec
        } else {
          Class<?> valClass = profile.mapValueClass ?: String
          Schema valueSchema = toFieldSchema(valClass, null)
          Schema nullableValue = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), valueSchema))
          fieldSchema = Schema.createMap(nullableValue)
        }
      } else {
        fieldSchema = toFieldSchema(clazz, profile.decimalMeta(inferPrecisionAndScale))
      }
      Schema nullable = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), fieldSchema))
      fields.add(new Schema.Field(col, nullable, null as String, (Object) null))
    }

    record.setFields(fields)
    cacheSchema(matrix, cacheKey, record)
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

    if (clazz == String) return Schema.create(Schema.Type.STRING)
    if (clazz == Boolean || clazz == boolean.class) return Schema.create(Schema.Type.BOOLEAN)
    if (clazz == Integer || clazz == int.class) return Schema.create(Schema.Type.INT)
    if (clazz == Long || clazz == long.class || clazz == BigInteger) return Schema.create(Schema.Type.LONG)
    if (clazz == Float || clazz == float.class) return Schema.create(Schema.Type.FLOAT)
    if (clazz == Double || clazz == double.class) return Schema.create(Schema.Type.DOUBLE)
    if (clazz == byte[].class) return Schema.create(Schema.Type.BYTES)

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
    Map<String, Schema> fieldSchemas = new LinkedHashMap<>()
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
                "Value does not match schema type",
                col,
                schemaTypeLabel(fs),
                v?.getClass()?.simpleName ?: "null"
            )
          }
          rec.put(col, toAvroValue(fs, v, decConv))
        } catch (AvroSchemaException e) {
          throw e
        } catch (Exception e) {
          throw new AvroConversionException(
              "Failed to convert value to Avro format",
              col,
              r,
              v?.getClass()?.simpleName ?: "null",
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
    if (v == null) return null

    // Handle UNIONs (including nested unions for array items and map values)
    if (fieldSchema.getType() == Schema.Type.UNION) {
      List<Schema> types = fieldSchema.getTypes()

      // Common case: ["null", T]
      if (types.size() == 2 && (types[0].getType() == Schema.Type.NULL || types[1].getType() == Schema.Type.NULL)) {
        Schema nonNull = (types[0].getType() == Schema.Type.NULL) ? types[1] : types[0]
        return (v == null) ? null : toAvroValue(nonNull, v, decConv)
      }

      // More general unions: pick the first compatible branch and serialize with it
      for (Schema branch : types) {
        if (branch.getType() == Schema.Type.NULL && v == null) return null
        if (branch.getType() == Schema.Type.NULL) continue
        if (isCompatible(branch, v)) {
          return toAvroValue(branch, v, decConv)
        }
      }

      // Could not resolve union — let Avro complain in a predictable way
      throw new UnresolvedUnionException(fieldSchema, v)
    }

    def lt = fieldSchema.getLogicalType()
    if (lt != null) {
      String name = lt.getName()
      switch (name) {
        case "date":
          if (v instanceof java.sql.Date) v = ((java.sql.Date) v).toLocalDate()
          if (v instanceof LocalDate) return (int) ((LocalDate) v).toEpochDay()
          break

        case "time-millis":
          if (v instanceof Time) v = ((Time) v).toLocalTime()
          if (v instanceof LocalTime) {
            int nanosMs = ((LocalTime) v).getNano().intdiv(1_000_000) // nanos -> millis
            long ms = ((LocalTime) v).toSecondOfDay() * 1000L + nanosMs
            return (int) ms
          }
          break

        case "local-timestamp-micros":
          if (v instanceof LocalDateTime) {
            int nanosUs = ((LocalDateTime) v).getNano().intdiv(1_000) // nanos -> micros
            long micros = ((LocalDateTime) v).toEpochSecond(ZoneOffset.UTC) * 1_000_000L + nanosUs
            return micros
          }
          break

        case "timestamp-millis":
          if (v instanceof Date) return ((Date) v).getTime()
          if (v instanceof Instant) return ((Instant) v).toEpochMilli()
          if (v instanceof LocalDateTime) {
            long ms = ((LocalDateTime) v)
                .toInstant(ZoneOffset.systemDefault().getRules().getOffset((LocalDateTime) v))
                .toEpochMilli()
            return ms
          }
          break

        case "local-timestamp-millis":
          if (v instanceof LocalDateTime) {
            int nanosMs = ((LocalDateTime) v).getNano().intdiv(1_000_000) // nanos -> millis
            long ms = ((LocalDateTime) v).toEpochSecond(ZoneOffset.UTC) * 1_000L + nanosMs
            return ms
          }
          break

        case "uuid":
          return v.toString()

        case "decimal":
          if (v instanceof BigDecimal) {
            LogicalTypes.Decimal dec = (LogicalTypes.Decimal) lt
            return decConv.toBytes((BigDecimal) v, fieldSchema, dec)
          } else if (v instanceof Double || v instanceof Float) {
            LogicalTypes.Decimal dec = (LogicalTypes.Decimal) lt
            BigDecimal bd = new BigDecimal(((Number) v).toString())
                .setScale(dec.getScale(), RoundingMode.HALF_UP)
            return decConv.toBytes(bd, fieldSchema, dec)
          }
          break
      }
    }

    // Primitive fallback based on schema type
    switch (fieldSchema.getType()) {
      case Schema.Type.STRING:
        return v.toString()
      case Schema.Type.BOOLEAN:
        return (Boolean) v
      case Schema.Type.INT:
        if (v instanceof Number) return ((Number) v).intValue()
        break
      case Schema.Type.LONG:
        if (v instanceof BigInteger) return ((BigInteger) v).longValue()
        if (v instanceof Number) return ((Number) v).longValue()
        if (v instanceof Date) return ((Date) v).time
        if (v instanceof Instant) return ((Instant) v).toEpochMilli()
        break
      case Schema.Type.FLOAT:
        if (v instanceof Number) return ((Number) v).floatValue()
        break
      case Schema.Type.DOUBLE:
        if (v instanceof BigDecimal) return ((BigDecimal) v).doubleValue()
        if (v instanceof Number) return ((Number) v).doubleValue()
        break
      case Schema.Type.BYTES:
        if (v instanceof byte[]) return ByteBuffer.wrap((byte[]) v)
        if (v instanceof ByteBuffer) return v
        if (v instanceof BigDecimal) {
          // fallback: unscaled bytes (only if schema is plain BYTES w/o decimal)
          return ByteBuffer.wrap(((BigDecimal) v).unscaledValue().toByteArray())
        }
        break
      case Schema.Type.ARRAY:
        Schema elem = fieldSchema.getElementType()
        List input = (List) v
        List out = new ArrayList( input == null ? 0 : input.size() )
        if ( input != null ) {
          for (def e: input ) out.add(toAvroValue(elem, e, decConv))
        }
        return out

      case Schema.Type.MAP:
        Schema vs = fieldSchema.getValueType()
        Map inMap = (Map) v
        Map<String, Object> outMap = new LinkedHashMap<>()
        if (inMap != null) {
          for (def e : inMap.entrySet()) {
            outMap.put(e.key?.toString(), toAvroValue(vs, e.value, decConv))
          }
        }
        return outMap

      case Schema.Type.RECORD:
        GenericData.Record gr = new GenericData.Record(fieldSchema)
        Map inRec = (Map) v
        for (Schema.Field f : fieldSchema.getFields()) {
          def fv = (inRec == null) ? null : inRec.get(f.name())
          // unwrap nullable union in field
          Schema fs = f.schema()
          if (fs.getType() == Schema.Type.UNION) {
            for (Schema t : fs.getTypes()) {
              if (t.getType() != Schema.Type.NULL) {
                fs = t; break
              }
            }
          }
          gr.put(f.name(), toAvroValue(fs, fv, decConv))
        }
        return gr
    }

    // Last resort
    return v.toString()
  }

  /**
   * Returns a cached schema for the given matrix and cache key, if available.
   *
   * @param matrix the matrix used as a cache namespace
   * @param key the cache key describing schema inputs
   * @return a cached schema instance, or null if none exists
   */
  private static Schema getCachedSchema(Matrix matrix, SchemaCacheKey key) {
    synchronized (SCHEMA_CACHE) {
      Map<SchemaCacheKey, Schema> perMatrix = SCHEMA_CACHE.get(matrix)
      return perMatrix == null ? null : perMatrix.get(key)
    }
  }

  private static void cacheSchema(Matrix matrix, SchemaCacheKey key, Schema schema) {
    synchronized (SCHEMA_CACHE) {
      Map<SchemaCacheKey, Schema> perMatrix = SCHEMA_CACHE.get(matrix)
      if (perMatrix == null) {
        perMatrix = new LinkedHashMap<>()
        SCHEMA_CACHE.put(matrix, perMatrix)
      }
      perMatrix.put(key, schema)
    }
  }

  private static Map<String, ColumnProfile> analyzeColumns(Matrix matrix, boolean inferPrecisionAndScale) {
    Map<String, ColumnProfile> profiles = new LinkedHashMap<>()
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
      if (v == null) continue

      if (!fixedType) {
        if (v instanceof BigDecimal) {
          sawBigDecimal = true
          if (inferPrecisionAndScale) {
            updateDecimalMeta((BigDecimal) v, profile)
          }
          continue
        }
        if (v instanceof Float || v instanceof Double) {
          sawFloat = true
          continue
        }
        if (v instanceof Byte || v instanceof Short || v instanceof Integer
            || v instanceof Long || v instanceof BigInteger) {
          sawIntegral = true
          long lv = (v instanceof BigInteger) ? ((BigInteger) v).longValue() : ((Number) v).longValue()
          if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE || v instanceof Long || v instanceof BigInteger) {
            needsLong = true
          }
          continue
        }
        if (v instanceof String || v instanceof Boolean || v instanceof byte[]
            || v instanceof java.sql.Date || v instanceof Time || v instanceof Date
            || v instanceof LocalDate || v instanceof LocalTime
            || v instanceof Instant || v instanceof LocalDateTime
            || v instanceof UUID) {
          profile.effectiveType = v.getClass()
          fixedType = true
          break
        }
        if (v instanceof List) {
          profile.effectiveType = List
          fixedType = true
          scanListElementValue((List) v, profile)
          if (profile.listElemClass != null) {
            break
          }
          continue
        }
        if (v instanceof Map) {
          profile.effectiveType = Map
          fixedType = true
          scanMapValue((Map) v, profile)
          continue
        }
        profile.effectiveType = String
        fixedType = true
        break
      } else if (profile.effectiveType == Map) {
        if (v instanceof Map) {
          scanMapValue((Map) v, profile)
        }
      } else if (profile.effectiveType == List) {
        if (v instanceof List && profile.listElemClass == null) {
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
      if (v instanceof BigDecimal) {
        updateDecimalMeta((BigDecimal) v, profile)
      }
    }
  }

  private static void scanListElement(Matrix matrix, String col, ColumnProfile profile) {
    int rows = matrix.rowCount()
    for (int r = 0; r < rows; r++) {
      def v = matrix[r, col]
      if (v instanceof List) {
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
      if (v instanceof Map) {
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
      profile.recordKeys = new LinkedHashSet<>(map.keySet().collect { it?.toString() })
    } else if (profile.recordLike) {
      Set<String> keys = new LinkedHashSet<>(map.keySet().collect { it?.toString() })
      if (!profile.recordKeys.equals(keys)) {
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

  private static final class SchemaCacheKey {
    private final String schemaName
    private final String namespace
    private final boolean inferPrecisionAndScale
    private final int rowCount
    private final List<String> columnNames
    private final List<Class<?>> columnTypes

    private SchemaCacheKey(String schemaName, String namespace, boolean inferPrecisionAndScale,
                           int rowCount, List<String> columnNames, List<Class<?>> columnTypes) {
      this.schemaName = schemaName
      this.namespace = namespace
      this.inferPrecisionAndScale = inferPrecisionAndScale
      this.rowCount = rowCount
      this.columnNames = Collections.unmodifiableList(new ArrayList<>(columnNames))
      this.columnTypes = Collections.unmodifiableList(new ArrayList<>(columnTypes))
    }

    @Override
    boolean equals(Object other) {
      if (this.is(other)) return true
      if (!(other instanceof SchemaCacheKey)) return false
      SchemaCacheKey that = (SchemaCacheKey) other
      return inferPrecisionAndScale == that.inferPrecisionAndScale &&
          rowCount == that.rowCount &&
          schemaName == that.schemaName &&
          namespace == that.namespace &&
          columnNames == that.columnNames &&
          columnTypes == that.columnTypes
    }

    @Override
    int hashCode() {
      int result = schemaName.hashCode()
      result = 31 * result + namespace.hashCode()
      result = 31 * result + (inferPrecisionAndScale ? 1 : 0)
      result = 31 * result + rowCount
      result = 31 * result + columnNames.hashCode()
      result = 31 * result + columnTypes.hashCode()
      return result
    }
  }

  private static final class ColumnProfile {
    final String name
    final Class<?> declaredType
    Class<?> effectiveType
    Class<?> listElemClass
    Class<?> mapValueClass
    boolean recordLike = false
    boolean recordSeen = false
    Map recordSample
    Set<String> recordKeys
    boolean sawDecimal = false
    int maxIntegerDigits = 0
    int maxScale = 0

    private ColumnProfile(String name, Class<?> declaredType) {
      this.name = name
      this.declaredType = declaredType
    }

    int[] decimalMeta(boolean inferPrecisionAndScale) {
      if (!inferPrecisionAndScale || effectiveType != BigDecimal) {
        return null
      }
      if (!sawDecimal) {
        return [10, 0] as int[]
      }
      int scale = Math.max(0, maxScale)
      int precision = Math.max(1, maxIntegerDigits + scale)
      return [precision, scale] as int[]
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
    if (v == null) return true
    if (s.getType() == Schema.Type.UNION) {
      for (Schema branch : s.getTypes()) {
        if (isCompatible(branch, v)) return true
      }
      return false
    }
    def logical = s.getLogicalType()
    if (logical != null) {
      String name = logical.getName()
      switch (name) {
        case "date":
          return v instanceof LocalDate || v instanceof java.sql.Date || v instanceof Number
        case "time-millis":
        case "time-micros":
          return v instanceof LocalTime || v instanceof Time || v instanceof Number
        case "timestamp-millis":
        case "timestamp-micros":
          return v instanceof Instant || v instanceof Date || v instanceof Number
        case "local-timestamp-millis":
        case "local-timestamp-micros":
          return v instanceof LocalDateTime || v instanceof Number
        case "uuid":
          return v instanceof UUID || v instanceof String
        case "decimal":
          return v instanceof BigDecimal || v instanceof Double || v instanceof Float ||
              v instanceof byte[] || v instanceof ByteBuffer
      }
    }
    switch (s.getType()) {
      case Schema.Type.STRING: return true // we'll toString() later
      case Schema.Type.BOOLEAN: return v instanceof Boolean
      case Schema.Type.INT: return v instanceof Byte || v instanceof Short || v instanceof Integer
      case Schema.Type.LONG: return v instanceof Number || v instanceof Date || v instanceof Instant
      case Schema.Type.FLOAT: return v instanceof Number
      case Schema.Type.DOUBLE: return v instanceof Number || v instanceof BigDecimal
      case Schema.Type.BYTES: return (v instanceof byte[]) || (v instanceof ByteBuffer) || (v instanceof BigDecimal)
      case Schema.Type.ARRAY:
        if (!(v instanceof List)) return false
        Schema elem = s.getElementType()
        for (def e : (List) v) {
          if (!isCompatible(elem, e)) return false
        }
        return true
      case Schema.Type.MAP:
        if (!(v instanceof Map)) return false
        Schema vs = s.getValueType()
        for (def e : ((Map) v).entrySet()) {
          if (!isCompatible(vs, e.value)) return false
        }
        return true
      case Schema.Type.RECORD:
        if (v instanceof GenericRecord) return true
        if (!(v instanceof Map)) return false
        Map inRec = (Map) v
        for (Schema.Field f : s.getFields()) {
          if (!isCompatible(f.schema(), inRec.get(f.name()))) return false
        }
        return true
      case Schema.Type.FIXED: return v instanceof GenericFixed
      default: return false
    }
  }

  /**
   * Validates that a field name conforms to Avro's naming rules.
   */
  private static void validateAvroFieldName(String fieldName, String columnName) {
    if (!isValidAvroName(fieldName)) {
      throw new AvroSchemaException(
          "Invalid Avro field name",
          columnName,
          "Avro field name (A-Za-z_ followed by A-Za-z0-9_)",
          fieldName
      )
    }
  }

  /**
   * Avro names must start with [A-Za-z_] and subsequently contain [A-Za-z0-9_].
   */
  private static boolean isValidAvroName(String name) {
    if (name == null || name.isEmpty()) return false
    return name ==~ /[A-Za-z_][A-Za-z0-9_]*/
  }

  /**
   * Produces a human-readable label for a schema type, preferring logical types.
   */
  private static String schemaTypeLabel(Schema schema) {
    if (schema.getType() == Schema.Type.UNION) {
      List<String> parts = new ArrayList<>()
      for (Schema branch : schema.getTypes()) {
        parts.add(schemaTypeLabel(branch))
      }
      return "UNION[" + String.join(", ", parts) + "]"
    }
    def logical = schema.getLogicalType()
    if (logical != null) {
      return logical.getName()
    }
    return schema.getType().name()
  }
}
