package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads Apache Parquet files into {@link Matrix} objects.
 *
 * <p>This class provides static methods for deserializing Parquet files back into Matrix data,
 * reconstructing column types, nested structures (Lists, Maps), and preserving BigDecimal precision.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Read a Parquet file (matrix name derived from filename)
 * Matrix data = MatrixParquetReader.read(new File("sales.parquet"))
 *
 * // Read with a custom matrix name
 * Matrix data = MatrixParquetReader.read(new File("sales.parquet"), "quarterly_sales")
 *
 * // Read from a byte array
 * byte[] parquetContent = ...
 * Matrix data = MatrixParquetReader.read(parquetContent)
 *
 * // Read from an InputStream
 * Matrix data = MatrixParquetReader.read(inputStream)
 *
 * // Read from a URL
 * Matrix data = MatrixParquetReader.read(new URL("https://example.com/sales.parquet"))
 * }</pre>
 *
 * <h3>Type Preservation</h3>
 * <p>When reading files written by {@link MatrixParquetWriter}, column types are automatically
 * restored from the {@link #METADATA_COLUMN_TYPES} metadata. This includes:</p>
 * <ul>
 *   <li>Primitives: Integer, Long, Float, Double, Boolean</li>
 *   <li>Numeric: BigDecimal (with original precision), BigInteger</li>
 *   <li>Temporal: LocalDate, LocalDateTime, java.sql.Date, java.sql.Time, java.sql.Timestamp</li>
 *   <li>Text: String</li>
 *   <li>Nested: List, Map (reconstructed from Parquet LIST/MAP/STRUCT types)</li>
 * </ul>
 *
 * <h3>Reading External Parquet Files</h3>
 * <p>Files not created by MatrixParquetWriter can still be read. Column types will be
 * inferred from the Parquet schema's logical type annotations.</p>
 *
 * <h3>Timezone Handling</h3>
 * <p>UTC timestamps are converted to {@link LocalDateTime} using the system default
 * timezone. To use a different timezone, use the {@link #read(File, ZoneId)} method:</p>
 * <pre>{@code
 * // Read using a specific timezone
 * Matrix data = MatrixParquetReader.read(file, ZoneId.of("America/New_York"))
 *
 * // Read with custom name and timezone
 * Matrix data = MatrixParquetReader.read(file, "myData", ZoneId.of("Europe/London"))
 * }</pre>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>The entire file is loaded into memory (no streaming support)</li>
 * </ul>
 *
 * @see MatrixParquetWriter
 * @see Matrix
 */
class MatrixParquetReader {

  private static final Logger log = Logger.getLogger(MatrixParquetReader)

  /** Metadata key for storing Matrix column types in Parquet file */
  static final String METADATA_COLUMN_TYPES = 'matrix.columnTypes'

  /** Metadata key for storing Matrix index column names in Parquet file */
  static final String METADATA_INDEX_COLUMNS = 'matrix.indexColumns'

  /** Parquet schema field name for repeated list entries */
  private static final String FIELD_LIST = 'list'

  /** Parquet schema field name for map key-value pairs */
  private static final String FIELD_KEY_VALUE = 'key_value'

  /** Thread-local storage for timezone used during read operations */
  private static final ThreadLocal<ZoneId> ZONE_ID_HOLDER = new ThreadLocal<>()

  /** Cache for Class.forName() results to avoid repeated reflection calls */
  private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>()

  private static final String ERR_PATH_NULL = 'Path cannot be null'
  private static final String ERR_FILE_PATH_NULL = 'File path cannot be null'
  private static final String ERR_CONTENT_NULL = 'Content cannot be null'
  private static final String ERR_URL_NULL = 'URL cannot be null'
  private static final String ERR_URL_STRING_NULL = 'URL string cannot be null'
  private static final String ERR_ZONE_ID_NULL = 'ZoneId cannot be null'
  private static final String DOT = '.'
  private static final String COMMA = ','
  private static final long MILLIS_PER_SECOND = 1_000L
  private static final long MICROS_PER_SECOND = 1_000_000L
  private static final long NANOS_PER_SECOND = 1_000_000_000L
  private static final long NANOS_PER_MILLI = 1_000_000L
  private static final long NANOS_PER_MICRO = 1_000L

  /**
   * Creates a new builder for reading Parquet data into a Matrix.
   *
   * <h3>Usage</h3>
   * <pre>{@code
   * // Read from file
   * Matrix data = MatrixParquetReader.builder()
   *     .matrixName("myData")
   *     .zoneId("Europe/Stockholm")
   *     .read(new File("data.parquet"))
   *
   * // Read from byte array
   * Matrix data = MatrixParquetReader.builder()
   *     .matrixName("imported")
   *     .read(parquetBytes)
   *
   * // Read from URL
   * Matrix data = MatrixParquetReader.builder()
   *     .read(new URL("https://example.com/data.parquet"))
   * }</pre>
   *
   * @return a new ReaderBuilder
   */
  static ReaderBuilder builder() {
    new ReaderBuilder()
  }

  /**
   * Fluent builder for configuring and executing Parquet read operations.
   *
   * <p>Obtain an instance via {@link MatrixParquetReader#builder()}.</p>
   */
  static class ReaderBuilder {

    private final ParquetReadOptions options = new ParquetReadOptions()

    private ReaderBuilder() {}

    /**
     * Sets the name for the resulting Matrix.
     *
     * @param value the matrix name
     * @return this builder
     */
    ReaderBuilder matrixName(String value) {
      options.matrixName(value)
      this
    }

    /**
     * Sets the timezone for converting UTC timestamps to LocalDateTime values.
     *
     * @param value the timezone
     * @return this builder
     */
    ReaderBuilder zoneId(ZoneId value) {
      options.zoneId(value)
      this
    }

    /**
     * Sets the timezone for converting UTC timestamps to LocalDateTime values.
     *
     * @param value the timezone ID string (e.g. "Europe/Stockholm")
     * @return this builder
     */
    ReaderBuilder zoneId(String value) {
      options.zoneId(value)
      this
    }

    /**
     * Reads a Parquet file into a Matrix.
     *
     * @param file the Parquet file to read
     * @return a Matrix populated with the file contents
     */
    Matrix read(File file) {
      if (options.zoneId != null && options.matrixName != null) {
        return MatrixParquetReader.read(file, options.matrixName, options.zoneId)
      }
      if (options.zoneId != null) {
        return MatrixParquetReader.read(file, options.zoneId)
      }
      if (options.matrixName != null) {
        return MatrixParquetReader.read(file, options.matrixName)
      }
      MatrixParquetReader.read(file)
    }

    /**
     * Reads a Parquet file from a Path into a Matrix.
     *
     * @param path the Path to read
     * @return a Matrix populated with the file contents
     */
    Matrix read(java.nio.file.Path path) {
      if (path == null) {
        throw new IllegalArgumentException(ERR_PATH_NULL)
      }
      read(path.toFile())
    }

    /**
     * Reads Parquet content from a byte array into a Matrix.
     *
     * @param content byte array containing Parquet data
     * @return a Matrix populated with the byte array contents
     */
    Matrix read(byte[] content) {
      if (content == null) {
        throw new IllegalArgumentException(ERR_CONTENT_NULL)
      }
      read(new ByteArrayInputStream(content))
    }

    /**
     * Reads Parquet content from an InputStream into a Matrix.
     *
     * @param is input stream containing Parquet data
     * @return a Matrix populated with the stream contents
     */
    Matrix read(InputStream is) {
      if (options.zoneId != null && options.matrixName != null) {
        return MatrixParquetReader.read(is, options.matrixName, options.zoneId)
      }
      if (options.zoneId != null) {
        return MatrixParquetReader.read(is, options.zoneId)
      }
      if (options.matrixName != null) {
        return MatrixParquetReader.read(is, options.matrixName)
      }
      MatrixParquetReader.read(is)
    }

    /**
     * Reads Parquet content from a URL into a Matrix.
     *
     * @param url URL pointing to Parquet content
     * @return a Matrix populated with the URL contents
     */
    Matrix read(URL url) {
      if (options.zoneId != null && options.matrixName != null) {
        return MatrixParquetReader.read(url, options.matrixName, options.zoneId)
      }
      if (options.zoneId != null) {
        return MatrixParquetReader.read(url, options.zoneId)
      }
      if (options.matrixName != null) {
        url.openStream().withCloseable { InputStream is ->
          MatrixParquetReader.read(is, options.matrixName)
        }
      } else {
        MatrixParquetReader.read(url)
      }
    }

    /**
     * Reads a Parquet file from a file path string into a Matrix.
     *
     * @param filePath the file path to read
     * @return a Matrix populated with the file contents
     */
    Matrix readFile(String filePath) {
      if (filePath == null) {
        throw new IllegalArgumentException(ERR_FILE_PATH_NULL)
      }
      read(new File(filePath))
    }

    /**
     * Reads Parquet content from a URL string into a Matrix.
     *
     * @param urlString String URL pointing to Parquet content
     * @return a Matrix populated with the URL contents
     */
    Matrix readUrl(String urlString) {
      if (urlString == null) {
        throw new IllegalArgumentException(ERR_URL_STRING_NULL)
      }
      read(new URI(urlString).toURL())
    }
  }

  /**
   * Gets the current timezone for timestamp conversion.
   * Returns the thread-local value if set, otherwise the system default.
   */
  private static ZoneId getZoneId() {
    ZoneId zoneId = ZONE_ID_HOLDER.get()
    zoneId != null ? zoneId : ZoneId.systemDefault()
  }

  /**
   * Gets a cached Class object for the given class name.
   * Uses Thread.currentThread().contextClassLoader.loadClass() and caches the result for subsequent calls.
   */
  private static Class<?> getCachedClass(String className) {
    CLASS_CACHE.computeIfAbsent(className) { name ->
      Thread.currentThread().contextClassLoader.loadClass(name)
    }
  }

  /**
   * Validates the file parameter before reading.
   *
   * @param file the file to validate
   * @throws IllegalArgumentException if file is null, does not exist, or is a directory
   */
  private static void validateFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException('File cannot be null')
    }
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exist: ${file.absolutePath}")
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Expected a file but got a directory: ${file.absolutePath}")
    }
  }

  private static String defaultMatrixName(File file) {
    if (file.name.contains(DOT)) {
      return file.name.substring(0, file.name.lastIndexOf(DOT))
    }
    file.name
  }

  /**
   * Read the Parquet file into a {@link Matrix} using the supplied name.
   *
   * @param file the Parquet file to read
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, or is a directory
   */
  static Matrix read(File file, String matrixName) {
    read(file, null, false, null).withMatrixName(matrixName)
  }

  /**
   * Read the Parquet file from a Path into a {@link Matrix}.
   *
   * @param path the Path to read
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if path is null, does not exist, or is a directory
   */
  static Matrix read(java.nio.file.Path path) {
    if (path == null) {
      throw new IllegalArgumentException(ERR_PATH_NULL)
    }
    read(path.toFile())
  }

  /**
   * Read the Parquet file from a Path into a {@link Matrix} using the supplied name.
   *
   * @param path the Path to read
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if path is null, does not exist, or is a directory
   */
  static Matrix read(java.nio.file.Path path, String matrixName) {
    if (path == null) {
      throw new IllegalArgumentException(ERR_PATH_NULL)
    }
    read(path.toFile(), matrixName)
  }

  /**
   * Read the Parquet file from a file path string into a {@link Matrix}.
   *
   * @param filePath the file path to read
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if filePath is null, does not exist, or is a directory
   */
  static Matrix readFile(String filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException(ERR_FILE_PATH_NULL)
    }
    read(new File(filePath))
  }

  /**
   * Read the Parquet file from a file path string into a {@link Matrix} using the supplied name.
   *
   * @param filePath the file path to read
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if filePath is null, does not exist, or is a directory
   */
  static Matrix readFile(String filePath, String matrixName) {
    if (filePath == null) {
      throw new IllegalArgumentException(ERR_FILE_PATH_NULL)
    }
    read(new File(filePath), matrixName)
  }

  /**
   * Read Parquet content from a byte array into a {@link Matrix}.
   *
   * <p>Parquet requires a seekable input, so the content is copied to a temporary file.</p>
   *
   * @param content byte array containing Parquet data
   * @return a matrix populated with the byte array contents
   * @throws IllegalArgumentException if content is null
   */
  static Matrix read(byte[] content) {
    if (content == null) {
      throw new IllegalArgumentException(ERR_CONTENT_NULL)
    }
    read(new ByteArrayInputStream(content))
  }

  /**
   * Read Parquet content from a byte array into a {@link Matrix} using the supplied name.
   *
   * <p>Parquet requires a seekable input, so the content is copied to a temporary file.</p>
   *
   * @param content byte array containing Parquet data
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the byte array contents
   * @throws IllegalArgumentException if content is null
   */
  static Matrix read(byte[] content, String matrixName) {
    if (content == null) {
      throw new IllegalArgumentException(ERR_CONTENT_NULL)
    }
    read(new ByteArrayInputStream(content), matrixName)
  }

  /**
   * Read Parquet content from a byte array into a {@link Matrix} with a specific timezone.
   *
   * <p>Parquet requires a seekable input, so the content is copied to a temporary file.</p>
   *
   * @param content byte array containing Parquet data
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the byte array contents
   * @throws IllegalArgumentException if content is null or zoneId is null
   */
  static Matrix read(byte[] content, ZoneId zoneId) {
    if (content == null) {
      throw new IllegalArgumentException(ERR_CONTENT_NULL)
    }
    read(new ByteArrayInputStream(content), zoneId)
  }

  /**
   * Read Parquet content from a byte array into a {@link Matrix} with a specific timezone and name.
   *
   * <p>Parquet requires a seekable input, so the content is copied to a temporary file.</p>
   *
   * @param content byte array containing Parquet data
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the byte array contents
   * @throws IllegalArgumentException if content is null or zoneId is null
   */
  static Matrix read(byte[] content, String matrixName, ZoneId zoneId) {
    if (content == null) {
      throw new IllegalArgumentException(ERR_CONTENT_NULL)
    }
    read(new ByteArrayInputStream(content), matrixName, zoneId)
  }

  /**
   * Read Parquet content from an InputStream into a {@link Matrix}.
   *
   * <p>Parquet requires a seekable input, so the stream is copied to a temporary file.</p>
   *
   * @param is input stream containing Parquet data
   * @return a matrix populated with the stream contents
   * @throws IllegalArgumentException if input stream is null
   */
  static Matrix read(InputStream is) {
    readFromInputStream(is, null, null)
  }

  /**
   * Read Parquet content from an InputStream into a {@link Matrix} using the supplied name.
   *
   * <p>Parquet requires a seekable input, so the stream is copied to a temporary file.</p>
   *
   * @param is input stream containing Parquet data
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the stream contents
   * @throws IllegalArgumentException if input stream is null
   */
  static Matrix read(InputStream is, String matrixName) {
    readFromInputStream(is, matrixName, null)
  }

  /**
   * Read Parquet content from a URL into a {@link Matrix}.
   *
   * @param url URL pointing to Parquet content
   * @return a matrix populated with the URL contents
   * @throws IllegalArgumentException if url is null
   */
  static Matrix read(URL url) {
    if (url == null) {
      throw new IllegalArgumentException(ERR_URL_NULL)
    }
    url.openStream().withCloseable { InputStream is ->
      readFromInputStream(is, null, null)
    }
  }

  /**
   * Read Parquet content from a URL string into a {@link Matrix}.
   *
   * @param urlString String URL pointing to Parquet content
   * @return a matrix populated with the URL contents
   * @throws IllegalArgumentException if urlString is null
   */
  static Matrix readUrl(String urlString) {
    if (urlString == null) {
      throw new IllegalArgumentException(ERR_URL_STRING_NULL)
    }
    read(new URI(urlString).toURL())
  }

  /**
   * Read the Parquet file into a {@link Matrix} with a specific timezone for timestamp conversion.
   *
   * <p>By default, UTC timestamps are converted to {@link LocalDateTime} using the system
   * default timezone. Use this method to specify a different timezone for the conversion.</p>
   *
   * @param file the Parquet file to read
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, is a directory, or zoneId is null
   */
  static Matrix read(File file, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    read(file, file == null ? null : defaultMatrixName(file), false, zoneId)
  }

  /**
   * Read the Parquet file from a Path into a {@link Matrix} with a specific timezone.
   *
   * @param path the Path to read
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if path is null, does not exist, or is a directory, or zoneId is null
   */
  static Matrix read(java.nio.file.Path path, ZoneId zoneId) {
    if (path == null) {
      throw new IllegalArgumentException(ERR_PATH_NULL)
    }
    read(path.toFile(), zoneId)
  }

  /**
   * Read the Parquet file from a file path string into a {@link Matrix} with a specific timezone.
   *
   * @param filePath the file path to read
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if filePath is null or zoneId is null
   */
  static Matrix readFile(String filePath, ZoneId zoneId) {
    if (filePath == null) {
      throw new IllegalArgumentException(ERR_FILE_PATH_NULL)
    }
    read(new File(filePath), zoneId)
  }

  /**
   * Read Parquet content from an InputStream into a {@link Matrix} with a specific timezone.
   *
   * <p>Parquet requires a seekable input, so the stream is copied to a temporary file.</p>
   *
   * @param is input stream containing Parquet data
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the stream contents
   * @throws IllegalArgumentException if input stream is null or zoneId is null
   */
  static Matrix read(InputStream is, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    readFromInputStream(is, null, zoneId)
  }

  /**
   * Read Parquet content from a URL into a {@link Matrix} with a specific timezone.
   *
   * @param url URL pointing to Parquet content
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the URL contents
   * @throws IllegalArgumentException if url is null or zoneId is null
   */
  static Matrix read(URL url, ZoneId zoneId) {
    if (url == null) {
      throw new IllegalArgumentException(ERR_URL_NULL)
    }
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    url.openStream().withCloseable { InputStream is ->
      readFromInputStream(is, null, zoneId)
    }
  }

  /**
   * Read Parquet content from a URL string into a {@link Matrix} with a specific timezone.
   *
   * @param urlString String URL pointing to Parquet content
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the URL contents
   * @throws IllegalArgumentException if urlString is null or zoneId is null
   */
  static Matrix readUrl(String urlString, ZoneId zoneId) {
    if (urlString == null) {
      throw new IllegalArgumentException(ERR_URL_STRING_NULL)
    }
    read(new URI(urlString).toURL(), zoneId)
  }

  /**
   * Read the Parquet file into a {@link Matrix} with a specific timezone and matrix name.
   *
   * @param file the Parquet file to read
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, is a directory, or zoneId is null
   */
  static Matrix read(File file, String matrixName, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    read(file, null, false, zoneId).withMatrixName(matrixName)
  }

  /**
   * Read the Parquet file from a Path into a {@link Matrix} with a specific timezone and name.
   *
   * @param path the Path to read
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if path is null or zoneId is null
   */
  static Matrix read(java.nio.file.Path path, String matrixName, ZoneId zoneId) {
    if (path == null) {
      throw new IllegalArgumentException(ERR_PATH_NULL)
    }
    read(path.toFile(), matrixName, zoneId)
  }

  /**
   * Read the Parquet file from a file path string into a {@link Matrix} with a specific timezone and name.
   *
   * @param filePath the file path to read
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if filePath is null or zoneId is null
   */
  static Matrix readFile(String filePath, String matrixName, ZoneId zoneId) {
    if (filePath == null) {
      throw new IllegalArgumentException(ERR_FILE_PATH_NULL)
    }
    read(new File(filePath), matrixName, zoneId)
  }

  /**
   * Read Parquet content from an InputStream into a {@link Matrix} with a specific timezone and name.
   *
   * <p>Parquet requires a seekable input, so the stream is copied to a temporary file.</p>
   *
   * @param is input stream containing Parquet data
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the stream contents
   * @throws IllegalArgumentException if input stream is null or zoneId is null
   */
  static Matrix read(InputStream is, String matrixName, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    readFromInputStream(is, matrixName, zoneId)
  }

  /**
   * Read Parquet content from a URL into a {@link Matrix} with a specific timezone and name.
   *
   * @param url URL pointing to Parquet content
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the URL contents
   * @throws IllegalArgumentException if url is null or zoneId is null
   */
  static Matrix read(URL url, String matrixName, ZoneId zoneId) {
    if (url == null) {
      throw new IllegalArgumentException(ERR_URL_NULL)
    }
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    url.openStream().withCloseable { InputStream is ->
      readFromInputStream(is, matrixName, zoneId)
    }
  }

  /**
   * Read Parquet content from a URL string into a {@link Matrix} with a specific timezone and name.
   *
   * @param urlString String URL pointing to Parquet content
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the URL contents
   * @throws IllegalArgumentException if urlString is null or zoneId is null
   */
  static Matrix readUrl(String urlString, String matrixName, ZoneId zoneId) {
    if (urlString == null) {
      throw new IllegalArgumentException(ERR_URL_STRING_NULL)
    }
    read(new URI(urlString).toURL(), matrixName, zoneId)
  }

  /**
   * Read the Parquet file into a {@link Matrix}, reconstructing nested LIST/MAP/STRUCT values.
   *
   * @param file the Parquet file to read
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, or is a directory
   */
  static Matrix read(File file) {
    read(file, file == null ? null : defaultMatrixName(file), false, null)
  }

  private static Matrix read(File file, String fallbackMatrixName, boolean preferSchemaName, ZoneId zoneId) {
    if (zoneId == null) {
      return read(file, fallbackMatrixName, preferSchemaName)
    }
    try {
      ZONE_ID_HOLDER.set(zoneId)
      return read(file, fallbackMatrixName, preferSchemaName)
    } finally {
      ZONE_ID_HOLDER.remove()
    }
  }

  private static Matrix read(File file, String fallbackMatrixName, boolean preferSchemaName) {
    validateFile(file)
    log.debug("Reading Parquet file: ${file.absolutePath}")
    def path = new Path(file.toURI())
    def conf = new Configuration()
    // Open Parquet file metadata
    def footer = ParquetFileReader.readFooter(conf, path)
    def keyValueMetaData = footer.getFileMetaData().getKeyValueMetaData()
    def typeString = keyValueMetaData.get(METADATA_COLUMN_TYPES)
    def indexString = keyValueMetaData.get(METADATA_INDEX_COLUMNS)

    List<Class> fieldTypes
    if (typeString != null) {
      fieldTypes = parseTypeString(typeString)
    }

    ParquetReader.builder(new GroupReadSupport(), path).build().withCloseable { reader ->
      String schemaName = footer.getFileMetaData().getSchema().name
      String matrixName = fallbackMatrixName
      if (preferSchemaName && schemaName != null && !schemaName.trim().isEmpty()) {
        matrixName = schemaName
      }

      Group row = reader.read()
      GroupType schema = row != null ? row.getType() : footer.getFileMetaData().getSchema()
      List<String> fieldNames = schema.fields*.name
      if (fieldTypes == null) {
        fieldTypes = extractFieldTypes(schema)
      }
      if (row == null) {
        Matrix m = Matrix.builder(matrixName)
            .columnNames(fieldNames)
            .types(fieldTypes)
            .build()
        return restoreIndex(m, indexString)
      }

      def builder = Matrix.builder(matrixName)
      .columnNames(fieldNames)
      .types(fieldTypes)

      while (row != null) {
        def rowData = []
        fieldNames.eachWithIndex { name, i ->
          def type = fieldTypes[i]
          def field = schema.getType(name)
          def value = readValue(row, name, field, type)
          rowData << value
        }
        builder.addRow(rowData as Object[])
        row = reader.read()
      }
      Matrix m = builder.build()
      restoreIndex(m, indexString)
    }
  }

  /**
   * Restores the index on a Matrix from the serialized index column names string.
   *
   * @param matrix the matrix to restore the index on
   * @param indexString JSON array or legacy comma-separated index column names, or null if no index
   * @return the matrix with index restored (or unchanged if no index metadata)
   */
  private static Matrix restoreIndex(Matrix matrix, String indexString) {
    if (indexString != null && !indexString.trim().isEmpty()) {
      matrix.createIndex(parseIndexColumns(indexString))
    }
    matrix
  }

  private static String[] parseIndexColumns(String indexString) {
    String value = indexString.trim()
    if (value.startsWith('[')) {
      try {
        return parseJsonStringArray(value) as String[]
      } catch (IllegalArgumentException ignored) {
        return parseLegacyIndexColumns(value)
      }
    }
    parseLegacyIndexColumns(value)
  }

  private static String[] parseLegacyIndexColumns(String value) {
    value.split(COMMA)*.trim() as String[]
  }

  private static List<String> parseJsonStringArray(String json) {
    List<String> result = []
    int pos = skipWhitespace(json, 0)
    if (pos >= json.length() || json.charAt(pos) != '[' as char) {
      throw new IllegalArgumentException("Invalid index metadata JSON: $json")
    }
    pos = skipWhitespace(json, pos + 1)
    if (pos < json.length() && json.charAt(pos) == ']' as char) {
      return result
    }
    while (pos < json.length()) {
      if (json.charAt(pos) != '"' as char) {
        throw new IllegalArgumentException("Invalid index metadata JSON: $json")
      }
      StringBuilder value = new StringBuilder()
      pos++
      while (pos < json.length()) {
        char ch = json.charAt(pos)
        if (ch == '"' as char) {
          pos++
          break
        }
        if (ch == '\\' as char) {
          pos = appendEscapedJsonChar(json, pos + 1, value)
        } else {
          value.append(ch)
          pos++
        }
      }
      result << value.toString()
      pos = skipWhitespace(json, pos)
      if (pos < json.length() && json.charAt(pos) == ',' as char) {
        pos = skipWhitespace(json, pos + 1)
      } else if (pos < json.length() && json.charAt(pos) == ']' as char) {
        return result
      } else {
        throw new IllegalArgumentException("Invalid index metadata JSON: $json")
      }
    }
    throw new IllegalArgumentException("Invalid index metadata JSON: $json")
  }

  private static int appendEscapedJsonChar(String json, int pos, StringBuilder value) {
    if (pos >= json.length()) {
      throw new IllegalArgumentException("Invalid index metadata JSON: $json")
    }
    char escaped = json.charAt(pos)
    if (escaped == '"' as char || escaped == '\\' as char || escaped == '/' as char) {
      value.append(escaped)
      return pos + 1
    }
    if (escaped == 'b' as char) {
      value.append('\b')
      return pos + 1
    }
    if (escaped == 'f' as char) {
      value.append('\f')
      return pos + 1
    }
    if (escaped == 'n' as char) {
      value.append('\n')
      return pos + 1
    }
    if (escaped == 'r' as char) {
      value.append('\r')
      return pos + 1
    }
    if (escaped == 't' as char) {
      value.append('\t')
      return pos + 1
    }
    if (escaped == 'u' as char) {
      if (pos + 4 >= json.length()) {
        throw new IllegalArgumentException("Invalid index metadata JSON: $json")
      }
      value.append((char) Integer.parseInt(json.substring(pos + 1, pos + 5), 16))
      return pos + 5
    }
    throw new IllegalArgumentException("Invalid index metadata JSON: $json")
  }

  private static int skipWhitespace(String value, int pos) {
    int current = pos
    while (current < value.length() && Character.isWhitespace(value.charAt(current))) {
      current++
    }
    current
  }

  private static Matrix readFromInputStream(InputStream is, String matrixName, ZoneId zoneId) {
    if (is == null) {
      throw new IllegalArgumentException('InputStream cannot be null')
    }
    java.nio.file.Path tempFile = Files.createTempFile('matrix-parquet-', '.parquet')
    try {
      Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING)
      File file = tempFile.toFile()
      Matrix result = read(file, defaultMatrixName(file), true, zoneId)
      if (matrixName != null && !matrixName.trim().isEmpty()) {
        result = result.withMatrixName(matrixName)
      }
      result
    } finally {
      try {
        Files.deleteIfExists(tempFile)
      } catch (IOException ignored) {
      }
    }
  }

  private static List<Class> parseTypeString(String typeString) {
    typeString.split(COMMA).collect { className ->
      getCachedClass(className.trim())
    }
  }

  private static Class getJavaType(PrimitiveType.PrimitiveTypeName typeName) {
    switch (typeName) {
      case PrimitiveTypeName.INT32 -> Integer
      case PrimitiveTypeName.INT64 -> Long
      case PrimitiveTypeName.FLOAT -> Float
      case PrimitiveTypeName.DOUBLE -> Double
      case PrimitiveTypeName.BOOLEAN -> Boolean
      default -> String
    }
  }

  private static List<Class> extractFieldTypes(GroupType schema) {
    schema.fields.collect { field ->
      def logical = field.getLogicalTypeAnnotation()
      if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
        List
      } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
        Map
      } else if (!field.isPrimitive()) {
        Map
      } else {
        def primitive = field.asPrimitiveType().primitiveTypeName

        if (logical != null) {
          if (logical == LogicalTypeAnnotation.dateType()) {
            LocalDate
          } else if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
            LocalDateTime
          } else if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
            BigDecimal
          } else {
            getJavaType(primitive)
          }
        } else {
          getJavaType(primitive)
        }
      }
    }
  }

  private static Object readValue(Group group, String fieldName, Type fieldType, Class expectedType) {
    if (group.getFieldRepetitionCount(fieldName) == 0) {
      return null
    }

    if (fieldType.isPrimitive()) {
      return readPrimitive(group, fieldName, fieldType.asPrimitiveType(), expectedType)
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      readList(group, fieldName, groupType)
    } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      readMap(group, fieldName, groupType)
    } else {
      readStruct(group, fieldName, groupType)
    }
  }

  private static Object readPrimitive(Group group, String fieldName, PrimitiveType fieldType, Class expectedType) {
    def logical = fieldType.logicalTypeAnnotation
    switch (fieldType.primitiveTypeName) {
      case PrimitiveTypeName.INT32 -> {
        if (logical == LogicalTypeAnnotation.dateType()) {
          LocalDate.ofEpochDay(group.getInteger(fieldName, 0))
        } else if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
          int millis = group.getInteger(fieldName, 0)
          Time.valueOf(LocalTime.ofNanoOfDay(millis * MICROS_PER_SECOND))
        } else if (expectedType == java.sql.Date) {
          java.sql.Date.valueOf(LocalDate.ofEpochDay(group.getInteger(fieldName, 0)))
        } else {
          group.getInteger(fieldName, 0)
        }
      }
      case PrimitiveTypeName.INT64 -> {
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
          Instant instant = timestampInstant(group.getLong(fieldName, 0), logical.unit)
          if (expectedType == java.sql.Timestamp) {
            java.sql.Timestamp.from(instant)
          } else if (expectedType == Date) {
            Date.from(instant)
          } else {
            LocalDateTime.ofInstant(instant, getZoneId())
          }
        } else {
          def longValue = group.getLong(fieldName, 0)
          if (expectedType == BigInteger) {
            BigInteger.valueOf(longValue)
          } else if (expectedType == Date) {
            new Date(longValue)
          } else {
            longValue
          }
        }
      }
      case PrimitiveTypeName.FLOAT -> group.getFloat(fieldName, 0)
      case PrimitiveTypeName.DOUBLE -> group.getDouble(fieldName, 0)
      case PrimitiveTypeName.BOOLEAN -> group.getBoolean(fieldName, 0)
      case PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY, PrimitiveTypeName.BINARY -> {
        if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
          def scale = logical.scale
          def binary = group.getBinary(fieldName, 0)
          def unscaled = new BigInteger(binary.getBytes())
          if (expectedType == BigInteger) {
            unscaled
          } else {
            new BigDecimal(unscaled, scale)
          }
        } else if (expectedType == BigDecimal) {
          new BigDecimal(group.getBinary(fieldName, 0).toStringUsingUTF8())
        } else {
          group.getBinary(fieldName, 0).toStringUsingUTF8()
        }
      }
      default -> group.getString(fieldName, 0)
    }
  }

  private static Instant timestampInstant(long value, LogicalTypeAnnotation.TimeUnit unit) {
    if (unit == LogicalTypeAnnotation.TimeUnit.MILLIS) {
      return Instant.ofEpochSecond(
          Math.floorDiv(value, MILLIS_PER_SECOND),
          Math.floorMod(value, MILLIS_PER_SECOND) * NANOS_PER_MILLI)
    }
    if (unit == LogicalTypeAnnotation.TimeUnit.NANOS) {
      return Instant.ofEpochSecond(
          Math.floorDiv(value, NANOS_PER_SECOND),
          Math.floorMod(value, NANOS_PER_SECOND))
    }
    Instant.ofEpochSecond(
        Math.floorDiv(value, MICROS_PER_SECOND),
        Math.floorMod(value, MICROS_PER_SECOND) * NANOS_PER_MICRO)
  }

  private static List readList(Group group, String fieldName, GroupType groupType) {
    List result = []
    Group listGroup = group.getGroup(fieldName, 0)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    int elementCount = listGroup.getFieldRepetitionCount(FIELD_LIST)
    for (int j = 0; j < elementCount; j++) {
      Group elementGroup = listGroup.getGroup(FIELD_LIST, j)
      def element = readValue(elementGroup, elementType.name, elementType, null)
      result << element
    }
    result
  }

  private static Map readMap(Group group, String fieldName, GroupType groupType) {
    Map result = [:]
    Group mapGroup = group.getGroup(fieldName, 0)
    GroupType keyValueType = groupType.getType(0).asGroupType()
    Type keyType = keyValueType.getType(0)
    Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
    int keyValueCount = mapGroup.getFieldRepetitionCount(FIELD_KEY_VALUE)
    for (int j = 0; j < keyValueCount; j++) {
      Group kvGroup = mapGroup.getGroup(FIELD_KEY_VALUE, j)
      def key = readValue(kvGroup, keyType.name, keyType, null)
      def value = valueType == null ? null : readValue(kvGroup, valueType.name, valueType, null)
      result[key] = value
    }
    result
  }

  private static Map readStruct(Group group, String fieldName, GroupType groupType) {
    if (groupType.fields.isEmpty()) {
      [:]
    } else {
      Group structGroup = group.getGroup(fieldName, 0)
      Map<String, Object> result = [:]
      groupType.fields.each { Type field ->
        def value = readValue(structGroup, field.name, field, null)
        result[field.name] = value
      }
      result
    }
  }
}
