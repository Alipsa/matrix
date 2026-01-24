package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
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
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
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
 * Matrix m = MatrixAvroReader.read(new File("data.avro"))
 *
 * // Read from file path string
 * Matrix m = MatrixAvroReader.readFile("/path/to/data.avro")
 *
 * // Read from URL
 * Matrix m = MatrixAvroReader.readUrl("https://example.com/data.avro")
 * }</pre>
 */
@CompileStatic
class MatrixAvroReader {

  /**
   * Read an Avro file from a File object.
   *
   * @param file the Avro file to read (must exist and be a file, not a directory)
   * @param name optional name for the resulting Matrix; defaults to the file name
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if file is null or is a directory
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(File file, String name = null) {
    validateFile(file)
    name = name ?: defaultName(file)
    InputStream is = new FileInputStream(file)
    try {
      return read(is, name)
    } catch (AvroConversionException | AvroValidationException e) {
      throw e
    } catch (Exception e) {
      throw new AvroValidationException(
          "Invalid or corrupt Avro file: ${file.absolutePath}",
          "file",
          "Ensure the file contains valid Avro OCF data",
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
      throw new IllegalArgumentException("Path cannot be null")
    }
    return read(path.toFile())
  }

  /**
   * Read Avro data from a URL.
   *
   * @param url the URL to read from (must not be null)
   * @param name optional name for the resulting Matrix; defaults to the URL file name
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if url is null
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(URL url, String name = null) {
    if (url == null) {
      throw new IllegalArgumentException("URL cannot be null")
    }
    name = name ?: defaultName(url)
    InputStream is = url.openStream()
    try {
      return read(is, name)
    } finally {
      is.close()
    }
  }

  /**
   * Read Avro data from a byte array.
   *
   * @param content the Avro content as a byte array (must not be null)
   * @param name optional name for the resulting Matrix; defaults to "AvroMatrix"
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if content is null
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(byte[] content, String name = "AvroMatrix") {
    if (content == null) {
      throw new IllegalArgumentException("Content cannot be null")
    }
    return read(new ByteArrayInputStream(content), name)
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
      throw new IllegalArgumentException("File path cannot be null")
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
      throw new IllegalArgumentException("URL string cannot be null")
    }
    try {
      return read(new URI(urlString).toURL())
    } catch (URISyntaxException | MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL string: " + urlString, e)
    }
  }

  /**
   * Read Avro data from an InputStream.
   *
   * <p>The stream will NOT be closed by this method; the caller is responsible for closing it.
   *
   * @param input the InputStream to read from (must not be null)
   * @param name optional name for the resulting Matrix; defaults to "AvroMatrix"
   * @return a Matrix containing the Avro data
   * @throws IllegalArgumentException if input is null
   * @throws IOException if an I/O error occurs
   */
  static Matrix read(InputStream input, String name = "AvroMatrix") {
    if (input == null) {
      throw new IllegalArgumentException("InputStream cannot be null")
    }
    return readInternal(input, name, null)
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
      throw new IllegalArgumentException("Options cannot be null")
    }
    String name = options.matrixName ?: defaultName(file)
    InputStream is = new FileInputStream(file)
    try {
      return readInternal(is, name, options.readerSchema)
    } catch (AvroConversionException | AvroValidationException e) {
      throw e
    } catch (Exception e) {
      throw new AvroValidationException(
          "Invalid or corrupt Avro file: ${file.absolutePath}",
          "file",
          "Ensure the file contains valid Avro OCF data",
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
      throw new IllegalArgumentException("Path cannot be null")
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
      throw new IllegalArgumentException("URL cannot be null")
    }
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null")
    }
    String name = options.matrixName ?: defaultName(url)
    InputStream is = url.openStream()
    try {
      return readInternal(is, name, options.readerSchema)
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
      throw new IllegalArgumentException("Content cannot be null")
    }
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null")
    }
    String name = options.matrixName ?: "AvroMatrix"
    return readInternal(new ByteArrayInputStream(content), name, options.readerSchema)
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
      throw new IllegalArgumentException("InputStream cannot be null")
    }
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null")
    }
    String name = options.matrixName ?: "AvroMatrix"
    return readInternal(input, name, options.readerSchema)
  }

  /**
   * Internal read implementation supporting optional reader schema.
   */
  private static Matrix readInternal(InputStream input, String name, Schema readerSchema) {
    GenericDatumReader<GenericRecord> datumReader = readerSchema != null
        ? new GenericDatumReader<>(readerSchema)
        : new GenericDatumReader<>()
    DataFileStream<GenericRecord> dfs = new DataFileStream<>(input, datumReader)
    try {
      Schema schema = dfs.schema
      List<Schema.Field> fields = schema.fields

      LinkedHashMap<String, List<Object>> columns = new LinkedHashMap<>()
      for (Schema.Field f : fields) {
        columns.put(f.name(), new ArrayList<>())
      }

      int rowNumber = 0
      for (GenericRecord rec : dfs) {
        for (Schema.Field f : fields) {
          Object raw = rec.get(f.name())
          try {
            Object val = convertValue(f.schema(), raw)
            columns.get(f.name()).add(val)
          } catch (Exception e) {
            throw new AvroConversionException(
                "Failed to convert value",
                f.name(),
                rowNumber,
                raw?.getClass()?.simpleName ?: "null",
                getTargetType(f.schema()),
                raw,
                e
            )
          }
        }
        rowNumber++
      }

      return Matrix.builder(name).columns(columns).build()
    } finally {
      dfs.close()
    }
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
    if (v == null) return null

    // Unwrap UNIONs (commonly ["null", T])
    if (schema.getType() == Schema.Type.UNION) {
      Schema nonNull = schema.getTypes().stream()
          .filter(s -> s.getType() != Schema.Type.NULL)
          .findFirst().orElse(schema)
      return convertValue(nonNull, v)
    }

    // Logical types: switch on the NAME to avoid nested-class access issues
    def lt = schema.getLogicalType()
    if (lt != null) {
      // decimal needs instanceof to read scale; the rest can switch on the name string
      if (lt instanceof LogicalTypes.Decimal) {
        return toBigDecimal((LogicalTypes.Decimal) lt, schema, v)
      }

      String name = lt.getName() // e.g. "date", "time-millis", "uuid", ...
      switch (name) {
        case "date":                    // int days since epoch
          return toLocalDate(v)
        case "time-millis":             // int millis since midnight
          return toLocalTimeMillis(v)
        case "time-micros":             // long micros since midnight
          return toLocalTimeMicros(v)
        case "timestamp-millis":        // long epoch millis UTC
          return toInstantMillis(v)
        case "timestamp-micros":        // long epoch micros UTC
          return toInstantMicros(v)
        case "local-timestamp-millis":  // long millis, no zone
          return toLocalDateTimeMillis(v)
        case "local-timestamp-micros":  // long micros, no zone
          return toLocalDateTimeMicros(v)
        case "uuid":
          return v.toString()           // or UUID.fromString(v.toString())
        default:
          // fall through to primitive/complex handling
          break
      }
    }

    switch (schema.getType()) {
      case Schema.Type.NULL:    return null
      case Schema.Type.BOOLEAN: return (Boolean) v
      case Schema.Type.INT:     return (Integer) v
      case Schema.Type.LONG:    return (Long) v
      case Schema.Type.FLOAT:   return (Float) v
      case Schema.Type.DOUBLE:  return (Double) v

      case Schema.Type.STRING:
        return (v instanceof Utf8) ? v.toString() : (String) v

      case Schema.Type.BYTES:
        return byteBufferToArray((ByteBuffer) v)

      case Schema.Type.FIXED:
        return (v as GenericFixed).bytes().clone()

      case Schema.Type.ENUM:
        return v.toString()

      case Schema.Type.ARRAY:
        Schema elem = schema.getElementType()
        List<?> list = (List<?>) v
        List<Object> out = new ArrayList<>(list.size())
        for (Object e : list) out.add(convertValue(elem, e))
        return out

      case Schema.Type.MAP:
        Schema vs = schema.getValueType()
        Map<Utf8, ?> m = (Map<Utf8, ?>) v
        Map<String, Object> outMap = new LinkedHashMap<>(m.size())
        for (Map.Entry<Utf8, ?> e : m.entrySet()) {
          outMap.put(e.getKey().toString(), convertValue(vs, e.getValue()))
        }
        return outMap

      case Schema.Type.RECORD:
        GenericRecord gr = (GenericRecord) v
        Map<String,Object> recMap = new LinkedHashMap<>(schema.getFields().size())
        for (Schema.Field f : schema.getFields()) {
          recMap.put(f.name(), convertValue(f.schema(), gr.get(f.name())))
        }
        return recMap

      default:
        return v
    }
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
    int days = (v instanceof Integer) ? (Integer) v : ((Number) v).intValue()
    return LocalDate.ofEpochDay(days)
  }

  /**
   * Converts an Avro time-millis value to a LocalTime.
   *
   * @param v the Avro value representing milliseconds since midnight
   * @return the corresponding LocalTime
   */
  private static LocalTime toLocalTimeMillis(Object v) {
    long ms = (v instanceof Integer) ? ((Integer) v).longValue() : ((Number) v).longValue()
    return LocalTime.ofNanoOfDay(ms * 1_000_000L)
  }

  /**
   * Converts an Avro time-micros value to a LocalTime.
   *
   * @param v the Avro value representing microseconds since midnight
   * @return the corresponding LocalTime
   */
  private static LocalTime toLocalTimeMicros(Object v) {
    long micros = ((Number) v).longValue()
    return LocalTime.ofNanoOfDay(micros * 1_000L)
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
    long seconds = Math.floorDiv(micros, 1_000_000L)
    long nanos   = Math.floorMod(micros, 1_000_000L) * 1_000L
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
        Math.floorDiv(ms, 1000L),
        (int)((ms % 1000L) * 1_000_000L),
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
    long seconds = Math.floorDiv(micros, 1_000_000L)
    int nanos    = (int) (Math.floorMod(micros, 1_000_000L) * 1_000L)
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
      throw new IllegalArgumentException("Decimal logical type on non-bytes/fixed field")
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
      throw AvroValidationException.nullParameter("file")
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
          "file",
          "Ensure the file contains Avro OCF data"
      )
    }
  }

  /**
   * Extracts a default name from a file (file name without extension).
   */
  private static String defaultName(File file) {
    String name = file.name
    if (name != null && name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name ?: "AvroMatrix"
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
      return "AvroMatrix"
    }
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1)
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name ?: "AvroMatrix"
  }
}
