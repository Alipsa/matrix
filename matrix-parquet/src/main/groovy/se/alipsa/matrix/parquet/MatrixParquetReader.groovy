package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.example.data.Group
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Time
import java.sql.Timestamp
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

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
  private static final String OPEN_BRACKET = '['
  private static final String EXTENSION = '.parquet'


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
  private static final ThreadLocal<Boolean> BINARY_AS_STRING = new ThreadLocal<>()

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
  private static final long MILLIS_PER_DAY = 86_400_000L
  private static final long MICROS_PER_DAY = MILLIS_PER_DAY * NANOS_PER_MICRO
  private static final long NANOS_PER_DAY = MICROS_PER_DAY * NANOS_PER_MICRO
  private static final int UNICODE_HEX_DIGITS = 4

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

  /** Creates a reader from a validated snapshot of typed options. */
  static ReaderBuilder builder(ParquetReadOptions options) {
    if (options == null) { throw new IllegalArgumentException('options cannot be null') }
    new ReaderBuilder(options)
  }

  /**
   * Fluent builder for configuring and executing Parquet read operations.
   *
   * <p>Obtain an instance via {@link MatrixParquetReader#builder()}.</p>
   */
  static class ReaderBuilder {

    private final ParquetReadOptions options

    private ReaderBuilder() { this(new ParquetReadOptions()) }

    private ReaderBuilder(ParquetReadOptions value) {
      options = ParquetReadOptions.fromMap(value.toMap())
      options.hubToken = value.hubToken
    }

    /** Sets columns; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder columns(List<String> value) { options.columns = value; this }

    /** Sets filter; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder filter(ParquetPredicate value) { options.filter = value; this }

    /** Sets binaryAsString; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder binaryAsString(Boolean value) { options.binaryAsString = value; this }

    /** Sets useColumnIndexFilter; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder useColumnIndexFilter(boolean value) { options.useColumnIndexFilter = value; this }

    /** Sets limit; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder limit(long value) { options.limit = value; this }

    /** Sets rowGroups; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder rowGroups(List<Integer> value) { options.rowGroups = value; this }

    /** Sets maxBatchRows; see ParquetReadOptions for defaults and validation. */
    ReaderBuilder maxBatchRows(int value) { options.maxBatchRows = value; this }

    /** Transport: DOWNLOAD, AUTO or RANGE; default 'DOWNLOAD'. */
    ReaderBuilder remoteReadMode(String value) { options.remoteReadMode = value; this }

    /** HTTP connection timeout; default 10000. */
    ReaderBuilder connectTimeoutMillis(int value) { options.connectTimeoutMillis = value; this }

    /** HTTP read timeout; default 60000. */
    ReaderBuilder readTimeoutMillis(int value) { options.readTimeoutMillis = value; this }

    /** Maximum HTTP range cache bytes; default 8388608. */
    ReaderBuilder rangeCacheBytes(int value) { options.rangeCacheBytes = value; this }

    /** Optional local source path column; default null. */
    ReaderBuilder sourceFileColumn(String value) { options.sourceFileColumn = value; this }

    /** Bounded dataset decoder workers; default 1. */
    ReaderBuilder parallelism(int value) { options.parallelism = value; this }

    /** Reads shards in caller order after checking every schema. */
    Matrix read(List<File> files) {
      batches(files).withCloseable { iterator -> collectBatches(iterator) }
    }

    /** Opens ordered shard batches; prefetch is bounded by parallelism. */
    MatrixBatchIterator batches(List<File> files) { new DatasetBatches(files, options) }

    /** Reads explicit file globs in normalized path order. */
    Matrix readGlob(String pattern) { read(globFiles(pattern)) }

    /** Selects original footer groups per shard, keyed by canonical local file path. */
    ReaderBuilder rowGroupsByFile(Map<String, List<Integer>> value) { options.rowGroupsByFile = value; this }

    /** Opens closeable batches; close explicitly when stopping before exhaustion. */
    MatrixBatchIterator batches(File file) {
      if (file?.isDirectory()) { return batches(directoryFiles(file)) }
      validateFile(file)
      new ParquetBatches(new org.apache.parquet.io.LocalInputFile(file.toPath()), options, defaultMatrixName(file))
    }


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
      if (file?.isDirectory()) { return read(directoryFiles(file)) }
      validateFile(file)
      readInput(new org.apache.parquet.io.LocalInputFile(file.toPath()), options, defaultMatrixName(file))
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
      readInput(new InMemoryInputFile(content), options, null)
    }

    /**
     * Reads Parquet content from an InputStream into a Matrix.
     *
     * @param is input stream containing Parquet data
     * @return a Matrix populated with the stream contents
     */
    Matrix read(InputStream is) {
      if (is == null) { throw new IllegalArgumentException('InputStream cannot be null') }
      File temp = File.createTempFile('matrix-parquet-', EXTENSION)
      try {
        Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING)
        readInput(new org.apache.parquet.io.LocalInputFile(temp.toPath()), options, null)
      } finally { Files.deleteIfExists(temp.toPath()) }
    }

    /**
     * Reads Parquet content from a URL into a Matrix.
     *
     * @param url URL pointing to Parquet content
     * @return a Matrix populated with the URL contents
     */
    Matrix read(URL url) {
      if (url == null) { throw new IllegalArgumentException(ERR_URL_NULL) }
      options.validate()
      if (options.remoteReadMode != 'DOWNLOAD') {
        HttpRangeInputFile input
        try { input = new HttpRangeInputFile(url, options) } catch (IOException failure) {
          if (options.remoteReadMode == 'RANGE') { throw failure }
        }
        if (input != null) { return readInput(input, options, null) }
      }
      if (url.protocol in ['http', 'https']) {
        return HttpTransport.download(url, options).withCloseable { InputStream stream -> read(stream) }
      }
      url.openStream().withCloseable { InputStream stream -> read(stream) }
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
      if (urlString.startsWith('hf://')) {
        options.hubToken = System.getenv('HF_TOKEN')
        return read(HubUrl.resolve(urlString))
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
    readInput(new InMemoryInputFile(content), new ParquetReadOptions(), null)
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
    readInput(new InMemoryInputFile(content), new ParquetReadOptions().matrixName(matrixName), null)
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
    readInput(new InMemoryInputFile(content), new ParquetReadOptions().zoneId(zoneId), null)
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
    readInput(new InMemoryInputFile(content), new ParquetReadOptions().matrixName(matrixName).zoneId(zoneId), null)
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
    builder().readUrl(urlString)
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
    if (file?.isDirectory()) { return builder().read(file) }
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
    readInput(new org.apache.parquet.io.LocalInputFile(file.toPath()), new ParquetReadOptions(), preferSchemaName ? null : fallbackMatrixName)
  }

  /** Reads a list of shards in the supplied order, with strict schema compatibility. */
  static Matrix read(List<File> files) { builder().read(files) }

  /** Reads an explicit glob; ordinary file strings are not interpreted as patterns. */
  static Matrix readGlob(String pattern) { builder().readGlob(pattern) }

  private static List<File> directoryFiles(File directory) {
    (directory.listFiles() ?: new File[0]).findAll { it.isFile() && it.name.endsWith(EXTENSION) }
        .sort { it.canonicalPath }.toList()
  }

  private static List<File> globFiles(String pattern) {
    java.nio.file.Path path = java.nio.file.Path.of(pattern).toAbsolutePath().normalize()
    String text = path as String
    int wildcard = text.findIndexOf { it in ['*', '?', OPEN_BRACKET, '{'] }
    if (wildcard < 0) { return [path.toFile()] }
    int slash = text.lastIndexOf(File.separator, wildcard)
    java.nio.file.Path root = java.nio.file.Path.of(text.substring(0, Math.max(1, slash)))
    def matcher = java.nio.file.FileSystems.default.getPathMatcher("glob:$text")
    Files.walk(root).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) && matcher.matches(it) }.sorted().map { it.toFile() }.toList()
    }
  }

  private static Matrix collectBatches(MatrixBatchIterator iterator) {
    Matrix prototype = iterator.emptyMatrix()
    List<List> columns = []
    prototype.columnNames().each { columns.add([]) }
    while (iterator.hasNext()) {
      Matrix batch = iterator.next()
      batch.columnNames().eachWithIndex { String name, int index -> columns[index].addAll(batch.column(name)) }
    }
    Matrix.builder(prototype.matrixName).columnNames(prototype.columnNames()).types(prototype.types()).columns(columns).build()
  }

  @PackageScope
  static <T> T withReadZone(ZoneId zone, Closure<T> action) {
    ZoneId previous = ZONE_ID_HOLDER.get()
    if (zone != null) { ZONE_ID_HOLDER.set(zone) }
    try { action.call() } finally {
      if (previous == null) { ZONE_ID_HOLDER.remove() } else { ZONE_ID_HOLDER.set(previous) }
    }
  }

  @PackageScope
  static Matrix readInput(org.apache.parquet.io.InputFile input, ParquetReadOptions options, String name) {
    log.debug("Reading Parquet input: $name")
    ZoneId previous = ZONE_ID_HOLDER.get()
    if (options.zoneId != null) { ZONE_ID_HOLDER.set(options.zoneId) }
    try {
      new ParquetBatches(input, options, name).withCloseable { batches ->
        Matrix empty = batches.emptyMatrix()
        List<List> columns = empty.columnNames().collect { [] }
        while (batches.hasNext()) {
          Matrix batch = batches.next()
          batch.columnNames().eachWithIndex { String column, int index -> columns[index].addAll(batch.column(column)) }
        }
        Matrix result = Matrix.builder(empty.matrixName).columnNames(empty.columnNames()).types(empty.types()).columns(columns).build()
        if (empty.hasIndex()) { result.createIndex(empty.indexedColumns() as String[]) }
        result
      }
    } finally {
      if (previous == null) { ZONE_ID_HOLDER.remove() } else { ZONE_ID_HOLDER.set(previous) }
    }
  }

  /**
   * Restores the index on a Matrix from the serialized index column names string.
   *
   * @param matrix the matrix to restore the index on
   * @param indexString JSON array or legacy comma-separated index column names, or null if no index
   * @return the matrix with index restored (or unchanged if no index metadata)
   */
  @PackageScope
  static Matrix restoreIndex(Matrix matrix, String indexString) {
    if (indexString != null && !indexString.trim().isEmpty()) {
      String[] indexes = parseIndexColumns(indexString)
      if (indexes.every { matrix.columnNames().contains(it) }) { matrix.createIndex(indexes) }
    }
    matrix
  }

  private static String[] parseIndexColumns(String indexString) {
    String value = indexString.trim()
    if (value.startsWith(OPEN_BRACKET)) {
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
    if (pos >= json.length() || json.charAt(pos) != OPEN_BRACKET as char) {
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
      int hexEnd = pos + 1 + UNICODE_HEX_DIGITS
      if (hexEnd > json.length()) {
        throw new IllegalArgumentException("Invalid index metadata JSON: $json")
      }
      value.append((char) Integer.parseInt(json.substring(pos + 1, hexEnd), 16))
      return hexEnd
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
    java.nio.file.Path tempFile = Files.createTempFile('matrix-parquet-', EXTENSION)
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



  private static Class getJavaType(PrimitiveType.PrimitiveTypeName typeName) {
    switch (typeName) {
      case PrimitiveTypeName.INT32 -> Integer
      case PrimitiveTypeName.INT64 -> Long
      case PrimitiveTypeName.FLOAT -> Float
      case PrimitiveTypeName.DOUBLE -> Double
      case PrimitiveTypeName.BOOLEAN -> Boolean
      case PrimitiveTypeName.BINARY, PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY -> byte[]
      default -> String
    }
  }

  @PackageScope
  static List<Class> extractFieldTypes(GroupType schema) {
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
          } else if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
            Time
          } else if (logical instanceof LogicalTypeAnnotation.StringLogicalTypeAnnotation) {
            String
          } else if (logical instanceof LogicalTypeAnnotation.UUIDLogicalTypeAnnotation) {
            UUID
          } else {
            getJavaType(primitive)
          }
        } else {
          getJavaType(primitive)
        }
      }
    }
  }

  @PackageScope
  static Object readValue(Group group, String fieldName, Type fieldType, Class expectedType) {
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
    Object raw = switch (fieldType.primitiveTypeName) {
      case PrimitiveTypeName.INT32 -> group.getInteger(fieldName, 0)
      case PrimitiveTypeName.INT64 -> group.getLong(fieldName, 0)
      case PrimitiveTypeName.FLOAT -> group.getFloat(fieldName, 0)
      case PrimitiveTypeName.DOUBLE -> group.getDouble(fieldName, 0)
      case PrimitiveTypeName.BOOLEAN -> group.getBoolean(fieldName, 0)
      case PrimitiveTypeName.BINARY, PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY -> group.getBinary(fieldName, 0)
      default -> throw new IllegalArgumentException("Unsupported ${fieldType.primitiveTypeName} field $fieldName")
    }
    decodePrimitive(raw, fieldType, expectedType, BINARY_AS_STRING.get() == Boolean.TRUE)
  }

  @PackageScope
  static Object decodePrimitive(Object raw, PrimitiveType field, Class expected, boolean binaryAsString) {
    if (field.primitiveTypeName == PrimitiveTypeName.INT96) {
      throw new IllegalArgumentException("Unsupported INT96 field '${field.name}'; convert legacy timestamps explicitly")
    }
    def logical = field.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
      BigInteger unscaled = raw instanceof org.apache.parquet.io.api.Binary ? new BigInteger(raw.bytes) : BigInteger.valueOf(((Number) raw).longValue())
      return expected == BigInteger ? unscaled : new BigDecimal(unscaled, logical.scale)
    }
    if (logical instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
      LocalDate date = LocalDate.ofEpochDay(((Number) raw).longValue())
      return expected in [java.sql.Date, Date] ? java.sql.Date.valueOf(date) : date
    }
    if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
      Instant instant = timestampInstant(((Number) raw).longValue(), logical.unit)
      if (expected == Instant) { return instant }
      if (expected == Timestamp) { return Timestamp.from(instant) }
      if (expected == Date) { return Date.from(instant) }
      return LocalDateTime.ofInstant(instant, logical.isAdjustedToUTC() ? getZoneId() : ZoneOffset.UTC)
    }
    if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
      long value = ((Number) raw).longValue()
      if (expected == LocalTime || (expected == null && !logical.isAdjustedToUTC())) {
        long multiplier = logical.unit == LogicalTypeAnnotation.TimeUnit.MILLIS ? NANOS_PER_MILLI :
            logical.unit == LogicalTypeAnnotation.TimeUnit.MICROS ? NANOS_PER_MICRO : 1L
        return LocalTime.ofNanoOfDay(Math.multiplyExact(value, multiplier))
      }
      return readTime(field.name, value, logical)
    }
    if (logical instanceof LogicalTypeAnnotation.UUIDLogicalTypeAnnotation) {
      ByteBuffer buffer = ByteBuffer.wrap(((org.apache.parquet.io.api.Binary) raw).bytes)
      return new UUID(buffer.getLong(), buffer.getLong())
    }
    if (logical instanceof LogicalTypeAnnotation.Float16LogicalTypeAnnotation) {
      throw new IllegalArgumentException("Unsupported FLOAT16 field '${field.name}'")
    }
    if (raw instanceof org.apache.parquet.io.api.Binary) {
      if (expected == BigDecimal) { return new BigDecimal(raw.toStringUsingUTF8()) }
      if (logical instanceof LogicalTypeAnnotation.StringLogicalTypeAnnotation || expected == String ||
          (logical == null && field.primitiveTypeName == PrimitiveTypeName.BINARY && binaryAsString && expected != byte[])) {
        return raw.toStringUsingUTF8()
      }
      return raw.bytes
    }
    narrowInteger(raw, field.name, expected)
  }

  private static Object narrowInteger(Object raw, String fieldName, Class expected) {
    if (expected == Byte) {
      long value = ((Number) raw).longValue()
      if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) { throw new IllegalArgumentException("Byte field '$fieldName' is out of range") }
      return ((Number) raw).byteValue()
    }
    if (expected == Short) {
      long value = ((Number) raw).longValue()
      if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) { throw new IllegalArgumentException("Short field '$fieldName' is out of range") }
      return ((Number) raw).shortValue()
    }
    raw
  }

  private static Time readTime(String fieldName, long value,
      LogicalTypeAnnotation.TimeLogicalTypeAnnotation logical) {
    long unitsPerDay
    long nanosPerUnit
    switch (logical.unit) {
      case LogicalTypeAnnotation.TimeUnit.MILLIS -> {
        unitsPerDay = MILLIS_PER_DAY
        nanosPerUnit = NANOS_PER_MILLI
      }
      case LogicalTypeAnnotation.TimeUnit.MICROS -> {
        unitsPerDay = MICROS_PER_DAY
        nanosPerUnit = NANOS_PER_MICRO
      }
      case LogicalTypeAnnotation.TimeUnit.NANOS -> {
        unitsPerDay = NANOS_PER_DAY
        nanosPerUnit = 1L
      }
      default -> throw new IllegalArgumentException("Unsupported TIME unit '${logical.unit}' for field '$fieldName'")
    }
    if (value < 0 || value >= unitsPerDay) {
      throw new IllegalArgumentException("TIME value $value for field '$fieldName' is outside one day")
    }
    try {
      LocalTime localTime = LocalTime.ofNanoOfDay(value * nanosPerUnit)
      Time time = Time.valueOf(localTime)
      long millisecondRemainder = (long) localTime.nano.intdiv(NANOS_PER_MILLI)
      time.setTime(time.time + millisecondRemainder)
      time
    } catch (DateTimeException exception) {
      // Guard defensively in case a future unit bypasses the one-day range check.
      throw new IllegalArgumentException("Invalid TIME value $value for field '$fieldName'", exception)
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
