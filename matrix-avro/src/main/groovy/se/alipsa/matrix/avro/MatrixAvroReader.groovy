package se.alipsa.matrix.avro

import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericFixed
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import se.alipsa.matrix.avro.exceptions.AvroConversionException
import se.alipsa.matrix.avro.exceptions.AvroValidationException
import se.alipsa.matrix.core.Matrix
import java.nio.ByteBuffer
import java.nio.file.Path
import java.time.*

/**
 * Reads Avro Object Container Files (OCF) into Matrix objects.
 *
 * <p>Supports reading from File, Path, URL, InputStream, and byte arrays.
 * Handles Avro logical types including date, time, timestamp, decimal, and UUID.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Read from file
 * Matrix m = MatrixAvroReader.read(new File('data.avro'))
 *
 * // Read from file path string
 * Matrix m = MatrixAvroReader.readFile('/path/to/data.avro')
 *
 * // Read from URL
 * Matrix m = MatrixAvroReader.readUrl('https://example.com/data.avro')
 * }</pre>
 */
class MatrixAvroReader {

  private static final String DEFAULT_MATRIX_NAME = 'AvroMatrix'
  private static final String FILE_PARAMETER = 'file'
  private static final String OPTIONS_NULL_MESSAGE = 'Options cannot be null'
  private static final String INVALID_URL_STRING_MESSAGE = 'Invalid URL string: '
  private static final String INVALID_OCF_SUGGESTION = 'Ensure the file contains valid Avro OCF data'
  private static final String PATH_NULL_MESSAGE = 'Path cannot be null'
  private static final String URL_NULL_MESSAGE = 'URL cannot be null'
  private static final String CONTENT_NULL_MESSAGE = 'Content cannot be null'
  private static final String INPUT_STREAM_NULL_MESSAGE = 'InputStream cannot be null'
  private static final String DOT = '.'
  private static final String SLASH = '/'
  private static final long MILLIS_PER_SECOND = 1_000L
  private static final long NANOS_PER_MICRO = 1_000L
  private static final long NANOS_PER_MILLI = 1_000_000L
  /**
   * Read an Avro file from a File object.
   *
   * @param file the Avro file to read (must exist and be a file, not a directory)
   * @param name optional override name for the resulting Matrix
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if file is null or is a directory
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(File file, String name = null) {
    validateFile(file)
    InputStream is = new FileInputStream(file)
    try {
      return readInternal(is, name, defaultName(file), null)
    } catch (AvroConversionException | AvroValidationException e) {
      throw e
    } catch (Exception e) {
      throw new AvroValidationException(
          "Invalid or corrupt Avro file: ${file.absolutePath}",
          FILE_PARAMETER,
          INVALID_OCF_SUGGESTION,
          e
      )
    } finally {
      is.close()
    }
  }
  /**
   * Read an Avro file from a Path.
   *
   * @param path the path to the Avro file (must not be null)
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if path is null or points to a directory
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(Path path) {
    if (path == null) {
      throw new IllegalArgumentException(PATH_NULL_MESSAGE)
    }
    return read(path.toFile())
  }
  /**
   * Read Avro data from a URL.
   *
   * @param url the URL to read from (must not be null)
   * @param name optional override name for the resulting Matrix
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if url is null
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(URL url, String name = null) {
    if (url == null) {
      throw new IllegalArgumentException(URL_NULL_MESSAGE)
    }
    InputStream is = url.openStream()
    try {
      return readInternal(is, name, defaultName(url), null)
    } finally {
      is.close()
    }
  }
  /**
   * Read Avro data from a byte array.
   *
   * @param content the Avro content as a byte array (must not be null)
   * @param name optional override name for the resulting Matrix
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if content is null
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(byte[] content, String name = null) {
    if (content == null) {
      throw new IllegalArgumentException(CONTENT_NULL_MESSAGE)
    }
    return readInternal(new ByteArrayInputStream(content), name, DEFAULT_MATRIX_NAME, null)
  }
  /**
   * Read an Avro file from a file path string.
   *
   * @param filePath the path to the Avro file (must not be null)
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if filePath is null or points to a directory
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if an I/O error occurs
   * @see #read(File, String)
   */
  static Matrix readFile(String filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException('File path cannot be null')
    }
    return read(new File(filePath))
  }
  /**
   * Read Avro data from a URL string.
   *
   * @param urlString the URL string to read from (must not be null and must be a valid URL)
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if urlString is null or invalid
   * @throws IOException if an I/O error occurs
   * @see #read(URL, String)
   */
  static Matrix readUrl(String urlString) {
    if (urlString == null) {
      throw new IllegalArgumentException('URL string cannot be null')
    }
    try {
      return read(new URI(urlString).toURL())
    } catch (URISyntaxException | MalformedURLException e) {
      throw new IllegalArgumentException(INVALID_URL_STRING_MESSAGE + urlString, e)
    }
  }
  /**
   * Read Avro data from an InputStream.
   *
   * <p>The stream will NOT be closed by this method; the caller is responsible for closing it.
   *
   * @param input the InputStream to read from (must not be null)
   * @param name optional override name for the resulting Matrix
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if input is null
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(InputStream input, String name = null) {
    if (input == null) {
      throw new IllegalArgumentException(INPUT_STREAM_NULL_MESSAGE)
    }
    return readInternal(input, name, DEFAULT_MATRIX_NAME, null)
  }
  // ----------------------------------------------------------------------
  // Methods accepting AvroReadOptions
  // ----------------------------------------------------------------------
  /**
   * Read an Avro file from a File object with configurable options.
   *
   * @param file the Avro file to read (must exist and be a file, not a directory)
   * @param options the read configuration options
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if file is null, is a directory, or options is null
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if an I/O error occurs
   * @see AvroReadOptions
   */
  static Matrix read(File file, AvroReadOptions options) {
    validateFile(file)
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    InputStream is = new FileInputStream(file)
    try {
      return readInternal(is, options.matrixName, defaultName(file), options.readerSchema)
    } catch (AvroConversionException | AvroValidationException e) {
      throw e
    } catch (Exception e) {
      throw new AvroValidationException(
          "Invalid or corrupt Avro file: ${file.absolutePath}",
          FILE_PARAMETER,
          INVALID_OCF_SUGGESTION,
          e
      )
    } finally {
      is.close()
    }
  }
  /**
   * Read an Avro file from a Path with configurable options.
   *
   * @param path the path to the Avro file (must not be null)
   * @param options the read configuration options
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if path is null, points to a directory, or options is null
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if an I/O error occurs
   * @see AvroReadOptions
   */
  static Matrix read(Path path, AvroReadOptions options) {
    if (path == null) {
      throw new IllegalArgumentException(PATH_NULL_MESSAGE)
    }
    return read(path.toFile(), options)
  }
  /**
   * Read Avro data from a URL with configurable options.
   *
   * @param url the URL to read from (must not be null)
   * @param options the read configuration options
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if url is null or options is null
   * @throws IOException if an I/O error occurs
   * @see AvroReadOptions
   */
  static Matrix read(URL url, AvroReadOptions options) {
    if (url == null) {
      throw new IllegalArgumentException(URL_NULL_MESSAGE)
    }
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    InputStream is = url.openStream()
    try {
      return readInternal(is, options.matrixName, defaultName(url), options.readerSchema)
    } finally {
      is.close()
    }
  }
  /**
   * Read Avro data from a byte array with configurable options.
   *
   * @param content the Avro content as a byte array (must not be null)
   * @param options the read configuration options
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if content is null or options is null
   * @throws IOException if an I/O error occurs
   * @see AvroReadOptions
   */
  static Matrix read(byte[] content, AvroReadOptions options) {
    if (content == null) {
      throw new IllegalArgumentException(CONTENT_NULL_MESSAGE)
    }
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    return readInternal(new ByteArrayInputStream(content), options.matrixName, DEFAULT_MATRIX_NAME, options.readerSchema)
  }
  /**
   * Read Avro data from an InputStream with configurable options.
   *
   * <p>The stream will NOT be closed by this method; the caller is responsible for closing it.
   *
   * @param input the InputStream to read from (must not be null)
   * @param options the read configuration options
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if input is null or options is null
   * @throws IOException if an I/O error occurs
   * @see AvroReadOptions
   */
  static Matrix read(InputStream input, AvroReadOptions options) {
    if (input == null) {
      throw new IllegalArgumentException(INPUT_STREAM_NULL_MESSAGE)
    }
    if (options == null) {
      throw new IllegalArgumentException(OPTIONS_NULL_MESSAGE)
    }
    return readInternal(input, options.matrixName, DEFAULT_MATRIX_NAME, options.readerSchema)
  }
  /**
   * Internal read implementation supporting optional reader schema and name resolution.
   */
  private static Matrix readInternal(InputStream input, String overrideName, String fallbackName, Schema readerSchema) {
    GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<>()
    DataFileStream<GenericRecord> dfs = new DataFileStream<>(input, datumReader)
    try {
      if (readerSchema != null) {
        datumReader.setExpected(readerSchema)
      }
      Schema writerSchema = dfs.schema
      Schema effectiveSchema = readerSchema ?: writerSchema
      List<Schema.Field> fields = effectiveSchema.fields
      String matrixName = resolveMatrixName(overrideName, writerSchema, fallbackName)
      Map<String, List<Object>> columns = [:]
      for (Schema.Field f : fields) {
        columns.put(f.name(), [])
      }
      int rowNumber = 0
      for (GenericRecord rec : dfs) {
        appendRecordValues(rec, fields, columns, rowNumber)
        rowNumber++
      }
      return Matrix.builder(matrixName).columns(columns).build()
    } finally {
      dfs.close()
    }
  }
  private static void appendRecordValues(GenericRecord rec, List<Schema.Field> fields,
                                         Map<String, List<Object>> columns, int rowNumber) {
    fields.each { Schema.Field f ->
      Object raw = rec.get(f.name())
      try {
        columns.get(f.name()).add(convertValue(f.schema(), raw))
      } catch (Exception e) {
        throw conversionException(f, rowNumber, raw, e)
      }
    }
  }
  private static AvroConversionException conversionException(Schema.Field field, int rowNumber, Object raw, Exception e) {
    new AvroConversionException(
        'Failed to convert value',
        field.name(),
        rowNumber,
        raw?.getClass()?.simpleName ?: 'null',
        getTargetType(field.schema()),
        raw,
        e
    )
  }
  private static String resolveMatrixName(String overrideName, Schema writerSchema, String fallbackName) {
    if (overrideName != null && !overrideName.isBlank()) {
      return overrideName
    }
    String schemaName = writerSchema?.name
    if (schemaName != null && !schemaName.isBlank()) {
      return schemaName
    }
    fallbackName ?: DEFAULT_MATRIX_NAME
  }
  /**
   * Gets a human-readable target type name from an Avro schema.
   */
  private static String getTargetType(Schema schema) {
    if (schema.getType() == Schema.Type.UNION) {
      Schema nonNull = schema.getTypes().stream()
          .filter(s -> s.getType() != Schema.Type.NULL)
          .findFirst().orElse(schema)
      return getTargetType(nonNull)
    }
    def lt = schema.getLogicalType()
    if (lt != null) {
      return lt.getName()
    }
    return schema.getType().name()
  }
  /**
   * Converts an Avro-typed value to a suitable Java value for Matrix storage.
   *
   * <p>This method handles the conversion of Avro's type system to Java types:
   * <ul>
   *   <li>UNION types are unwrapped to their non-null component</li>
   *   <li>Logical types (date, time, timestamp, decimal, uuid) are converted to appropriate Java types</li>
   *   <li>Primitive types are converted directly (INT→Integer, LONG→Long, etc.)</li>
   *   <li>Complex types (ARRAY, MAP, RECORD) are recursively converted</li>
   *   <li>Avro's Utf8 strings are converted to Java Strings</li>
   * </ul>
   *
   * @param schema the Avro schema describing the value's type
   * @param v the raw Avro value to convert (may be null)
   * @return the converted Java value suitable for Matrix storage, or null if input is null
   */
  private static Object convertValue(Schema schema, Object v) {
    if (v == null) {
      return null
    }
    if (schema.getType() == Schema.Type.UNION) {
      return convertValue(nonNullSchema(schema), v)
    }
    LogicalType lt = schema.getLogicalType()
    if (lt != null) {
      Object logicalValue = convertLogicalValue(lt, schema, v)
      return logicalValue != null ? logicalValue : convertSchemaValue(schema, v)
    }
    convertSchemaValue(schema, v)
  }
  private static Schema nonNullSchema(Schema schema) {
    schema.getTypes().stream()
        .filter(s -> s.getType() != Schema.Type.NULL)
        .findFirst().orElse(schema)
  }
  private static Object convertLogicalValue(LogicalType lt, Schema schema, Object v) {
    if (LogicalTypes.Decimal.isInstance(lt)) {
      return toBigDecimal((LogicalTypes.Decimal) lt, schema, v)
    }
    switch (lt.name) {
      case 'date' -> toLocalDate(v)
      case 'time-millis' -> toLocalTimeMillis(v)
      case 'time-micros' -> toLocalTimeMicros(v)
      case 'timestamp-millis' -> toInstantMillis(v)
      case 'timestamp-micros' -> toInstantMicros(v)
      case 'local-timestamp-millis' -> toLocalDateTimeMillis(v)
      case 'local-timestamp-micros' -> toLocalDateTimeMicros(v)
      case 'uuid' -> v.toString()
      default -> null
    }
  }
  private static Object convertSchemaValue(Schema schema, Object v) {
    switch (schema.getType()) {
      case Schema.Type.NULL -> null
      case Schema.Type.BOOLEAN -> (Boolean) v
      case Schema.Type.INT -> (Integer) v
      case Schema.Type.LONG -> (Long) v
      case Schema.Type.FLOAT -> (Float) v
      case Schema.Type.DOUBLE -> (Double) v
      case Schema.Type.STRING -> (Utf8.isInstance(v)) ? v.toString() : (String) v
      case Schema.Type.BYTES -> byteBufferToArray((ByteBuffer) v)
      case Schema.Type.FIXED -> (v as GenericFixed).bytes().clone()
      case Schema.Type.ENUM -> v.toString()
      case Schema.Type.ARRAY -> convertArrayValue(schema, v)
      case Schema.Type.MAP -> convertMapValue(schema, v)
      case Schema.Type.RECORD -> convertRecordValue(schema, v)
      default -> v
    }
  }
  private static List<Object> convertArrayValue(Schema schema, Object v) {
    Schema elem = schema.getElementType()
    List<?> list = (List<?>) v
    list.collect { Object e -> convertValue(elem, e) }
  }
  private static Map<String, Object> convertMapValue(Schema schema, Object v) {
    Schema valueSchema = schema.getValueType()
    Map<Utf8, ?> map = (Map<Utf8, ?>) v
    map.collectEntries { Utf8 key, Object value ->
      [(key.toString()): convertValue(valueSchema, value)]
    } as Map<String, Object>
  }
  private static Map<String, Object> convertRecordValue(Schema schema, Object v) {
    GenericRecord record = (GenericRecord) v
    schema.getFields().collectEntries { Schema.Field field ->
      [(field.name()): convertValue(field.schema(), record.get(field.name()))]
    } as Map<String, Object>
  }
  /**
   * Converts a ByteBuffer to a byte array, extracting only the remaining bytes.
   *
   * @param buf the ByteBuffer to convert
   * @return a new byte array containing the buffer's remaining bytes
   */
  private static byte[] byteBufferToArray(ByteBuffer buf) {
    ByteBuffer slice = buf.slice()
    byte[] exact = new byte[slice.remaining()]
    slice.get(exact)
    return exact
  }
  /**
   * Converts an Avro date value (days since epoch) to a LocalDate.
   *
   * @param v the Avro value representing days since Unix epoch (1970-01-01)
   * @return the corresponding LocalDate
   */
  private static LocalDate toLocalDate(Object v) {
    int days = (Integer.isInstance(v)) ? (Integer) v : ((Number) v).intValue()
    return LocalDate.ofEpochDay(days)
  }
  /**
   * Converts an Avro time-millis value to a LocalTime.
   *
   * @param v the Avro value representing milliseconds since midnight
   * @return the corresponding LocalTime
   */
  private static LocalTime toLocalTimeMillis(Object v) {
    long ms = (Integer.isInstance(v)) ? ((Integer) v).longValue() : ((Number) v).longValue()
    return LocalTime.ofNanoOfDay(ms * NANOS_PER_MILLI)
  }
  /**
   * Converts an Avro time-micros value to a LocalTime.
   *
   * @param v the Avro value representing microseconds since midnight
   * @return the corresponding LocalTime
   */
  private static LocalTime toLocalTimeMicros(Object v) {
    long micros = ((Number) v).longValue()
    return LocalTime.ofNanoOfDay(micros * NANOS_PER_MICRO)
  }
  /**
   * Converts an Avro timestamp-millis value to an Instant.
   *
   * @param v the Avro value representing milliseconds since Unix epoch (UTC)
   * @return the corresponding Instant
   */
  private static Instant toInstantMillis(Object v) {
    long ms = ((Number) v).longValue()
    return Instant.ofEpochMilli(ms)
  }
  /**
   * Converts an Avro timestamp-micros value to an Instant.
   *
   * @param v the Avro value representing microseconds since Unix epoch (UTC)
   * @return the corresponding Instant
   */
  private static Instant toInstantMicros(Object v) {
    long micros = ((Number) v).longValue()
    long seconds = Math.floorDiv(micros, NANOS_PER_MILLI)
    long nanos   = Math.floorMod(micros, NANOS_PER_MILLI) * NANOS_PER_MICRO
    return Instant.ofEpochSecond(seconds, nanos)
  }
  /**
   * Converts an Avro local-timestamp-millis value to a LocalDateTime.
   *
   * <p>Note: local-timestamp has no timezone; UTC offset is used for conversion.
   *
   * @param v the Avro value representing milliseconds since epoch (no timezone)
   * @return the corresponding LocalDateTime
   */
  private static LocalDateTime toLocalDateTimeMillis(Object v) {
    long ms = ((Number) v).longValue()
    return LocalDateTime.ofEpochSecond(
        Math.floorDiv(ms, MILLIS_PER_SECOND),
        (int)((ms % MILLIS_PER_SECOND) * NANOS_PER_MILLI),
        ZoneOffset.UTC
    )
  }
  /**
   * Converts an Avro local-timestamp-micros value to a LocalDateTime.
   *
   * <p>Note: local-timestamp has no timezone; UTC offset is used for conversion.
   *
   * @param v the Avro value representing microseconds since epoch (no timezone)
   * @return the corresponding LocalDateTime
   */
  private static LocalDateTime toLocalDateTimeMicros(Object v) {
    long micros = ((Number) v).longValue()
    long seconds = Math.floorDiv(micros, NANOS_PER_MILLI)
    int nanos    = (int) (Math.floorMod(micros, NANOS_PER_MILLI) * NANOS_PER_MICRO)
    return LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
  }
  /**
   * Converts an Avro decimal logical type value to a BigDecimal.
   *
   * <p>Avro decimals are stored as unscaled byte arrays (big-endian two's complement).
   * The scale is obtained from the logical type metadata.
   *
   * @param dec the Avro Decimal logical type containing precision and scale
   * @param schema the Avro schema (must be BYTES or FIXED)
   * @param v the raw Avro value (ByteBuffer for BYTES, GenericFixed for FIXED)
   * @return the corresponding BigDecimal
   * @throws IllegalArgumentException if schema type is not BYTES or FIXED
   */
  private static BigDecimal toBigDecimal(LogicalTypes.Decimal dec, Schema schema, Object v) {
    int scale = dec.getScale()
    byte[] bytes
    if (schema.getType() == Schema.Type.BYTES) {
      bytes = byteBufferToArray((ByteBuffer) v)
    } else if (schema.getType() == Schema.Type.FIXED) {
      bytes = ((GenericFixed) v).bytes()
    } else {
      throw new IllegalArgumentException('Decimal logical type on non-bytes/fixed field')
    }
    return new BigDecimal(new BigInteger(bytes), scale)
  }
  /**
   * Validates that the file exists and is not a directory.
   *
   * @throws AvroValidationException if file is null, doesn't exist, or is a directory
   */
  private static void validateFile(File file) {
    if (file == null) {
      throw AvroValidationException.nullParameter(FILE_PARAMETER)
    }
    if (!file.exists()) {
      throw AvroValidationException.fileNotFound(file.absolutePath)
    }
    if (file.isDirectory()) {
      throw AvroValidationException.isDirectory(file.absolutePath)
    }
    if (file.length() == 0) {
      throw new AvroValidationException(
          "Avro file is empty: ${file.absolutePath}",
          FILE_PARAMETER,
          'Ensure the file contains Avro OCF data'
      )
    }
  }
  /**
   * Extracts a default name from a file (file name without extension).
   */
  private static String defaultName(File file) {
    String name = file.name
    if (name?.contains(DOT)) {
      name = name.substring(0, name.lastIndexOf(DOT))
    }
    return name ?: DEFAULT_MATRIX_NAME
  }
  /**
   * Extracts a default name from a URL (file name without extension).
   */
  private static String defaultName(URL url) {
    String name = url.getPath()
    if (name == null || name.isEmpty()) {
      name = url.getFile()
    }
    if (name == null || name.isEmpty()) {
      return DEFAULT_MATRIX_NAME
    }
    if (name.contains(SLASH)) {
      name = name.substring(name.lastIndexOf(SLASH) + 1)
    }
    if (name.contains(DOT)) {
      name = name.substring(0, name.lastIndexOf(DOT))
    }
    return name ?: DEFAULT_MATRIX_NAME
  }

}
