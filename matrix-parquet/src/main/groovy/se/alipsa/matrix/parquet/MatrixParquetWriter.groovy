package se.alipsa.matrix.parquet

import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Types

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

import java.math.RoundingMode
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Writes {@link Matrix} data to Apache Parquet files.
 *
 * <p>This class provides static methods for serializing Matrix data to the Parquet columnar format,
 * preserving column types, nested structures (Lists, Maps), and BigDecimal precision. See
 * {@link #builder(Matrix)} and the various {@code write}/{@code writeBytes} overloads for usage,
 * {@link #write(Matrix, File, int, int)} for uniform BigDecimal precision/scale control, and
 * {@link #write(Matrix, File, ZoneId)} for {@link LocalDateTime} timezone handling (system default
 * timezone is used otherwise).</p>
 *
 * <p>Supported data types: primitives (Integer, Long, Float, Double, Boolean); numeric
 * (BigDecimal with configurable precision, BigInteger); temporal (LocalDate, LocalDateTime,
 * java.sql.Date, java.sql.Time, java.sql.Timestamp); String; and nested List/Map, preserved as
 * Parquet LIST/MAP/STRUCT types.</p>
 *
 * <p>Column type information is stored in Parquet file metadata under the key
 * {@link #METADATA_COLUMN_TYPES}, enabling {@link MatrixParquetReader} to reconstruct
 * the original Matrix types when reading.</p>
 *
 * Timestamp precision defaults to MICROS with sub-microsecond truncation; use
 * {@link WriterBuilder#timestampUnit(String)} for full NANOS precision.
 *
 * @see MatrixParquetReader
 * @see Matrix
 */
class MatrixParquetWriter {

  private static final Logger log = Logger.getLogger(MatrixParquetWriter)

  /** Metadata key for canonical nested leaf types by logical path. */
  static final String METADATA_NESTED_TYPES = 'matrix.nestedTypes'

  /** Metadata key for storing Matrix column types in Parquet file. */
  static final String METADATA_COLUMN_TYPES = 'matrix.columnTypes'

  /** Metadata key for storing Matrix index column names in Parquet file */
  static final String METADATA_INDEX_COLUMNS = 'matrix.indexColumns'

  /** Parquet schema field name for list elements */
  private static final String FIELD_ELEMENT = 'element'

  /** Parquet schema field name for map keys */
  private static final String FIELD_KEY = 'key'

  /** Parquet schema field name for map values */
  private static final String FIELD_VALUE = 'value'

  private static final String ERR_MATRIX_NULL = 'Matrix cannot be null'
  private static final String ERR_MATRIX_EMPTY = 'Matrix must have at least one column'
  private static final String ERR_FILE_OR_DIR_NULL = 'File or directory cannot be null'
  private static final String ERR_ZONE_ID_NULL = 'ZoneId cannot be null'
  private static final String ERR_OPTIONS_NULL = 'Options cannot be null'
  private static final String COMMA = ','
  private static final String ZERO = '0'
  private static final String DEFAULT_MATRIX_NAME = 'matrix'
  private static final int DECIMAL_META_LENGTH = 2

  /**
   * Creates a new builder for writing a Matrix to Parquet format.
   *
   * <h3>Usage</h3>
   * <pre>{@code
   * // Write to file with inferred precision
   * MatrixParquetWriter.builder(matrix)
   *     .write(new File("output.parquet"))
   *
   * // Write with explicit precision and timezone
   * MatrixParquetWriter.builder(matrix)
   *     .precision(38)
   *     .scale(18)
   *     .zoneId("Europe/Stockholm")
   *     .write(outputDir)
   *
   * // Write to byte array
   * byte[] bytes = MatrixParquetWriter.builder(matrix)
   *     .inferPrecisionAndScale(false).precision(38).scale(18)
   *     .writeBytes()
   * }</pre>
   *
   * @param matrix the matrix to write
   * @return a new WriterBuilder
   * @throws IllegalArgumentException if matrix is null or has no columns
   */
  static WriterBuilder builder(Matrix matrix) {
    validateInput(matrix)
    new WriterBuilder(matrix)
  }

  /**
   * Fluent builder for configuring and executing Parquet write operations.
   *
   * <p>Obtain an instance via {@link MatrixParquetWriter#builder(Matrix)}.</p>
   */
  static class WriterBuilder {

    private final Matrix matrix
    /**
     * Timestamp unit, default MICROS: truncates sub-microsecond digits downward in time for
     * LocalDateTime, Timestamp and Instant. NANOS preserves digits but Spark-family readers
     * may not support it. NANOS range: 1677-09-21T00:12:43.145224192Z to
     * 2262-04-11T23:47:16.854775807Z; out-of-range values fail. MICROS covers about ±292,000 years.
     * @param value MICROS or NANOS
     * @return this builder
     */
    WriterBuilder timestampUnit(String value) { options.timestampUnit(value); this }

    /** Sets rowGroupSize; see ParquetWriteOptions for defaults. */
    WriterBuilder rowGroupSize(long value) { options.rowGroupSize(value); this }

    /** Sets pageSize; see ParquetWriteOptions for defaults. */
    WriterBuilder pageSize(int value) { options.pageSize(value); this }

    /** Sets dictionaryEncoding; see ParquetWriteOptions for defaults. */
    WriterBuilder dictionaryEncoding(boolean value) { options.dictionaryEncoding(value); this }

    /** Sets writerVersion; see ParquetWriteOptions for defaults. */
    WriterBuilder writerVersion(String value) { options.writerVersion(value); this }

    /** Sets columnIndexTruncateLength; see ParquetWriteOptions for defaults. */
    WriterBuilder columnIndexTruncateLength(int value) { options.columnIndexTruncateLength(value); this }

    /** Sets statisticsTruncateLength; see ParquetWriteOptions for defaults. */
    WriterBuilder statisticsTruncateLength(int value) { options.statisticsTruncateLength(value); this }

    /** Sets bloomFilterEnabled; see ParquetWriteOptions for defaults. */
    WriterBuilder bloomFilterEnabled(Map<String, Boolean> value) { options.bloomFilterEnabled(value); this }

    /** Sets bloomFilterNDV; see ParquetWriteOptions for defaults. */
    WriterBuilder bloomFilterNDV(Map<String, Long> value) { options.bloomFilterNDV(value); this }

    /** Sets sizeStatisticsEnabled; see ParquetWriteOptions for defaults. */
    WriterBuilder sizeStatisticsEnabled(boolean value) { options.sizeStatisticsEnabled(value); this }

    private final ParquetWriteOptions options = new ParquetWriteOptions()

    private WriterBuilder(Matrix matrix) {
      this.matrix = matrix
    }

    /**
     * Sets whether to infer precision and scale for BigDecimal columns (default: {@code true}).
     *
     * @param value {@code true} to infer, {@code false} to require explicit precision/scale or decimalMeta
     * @return this builder
     */
    WriterBuilder inferPrecisionAndScale(boolean value) {
      options.inferPrecisionAndScale(value)
      this
    }

    /**
     * Sets uniform precision for all BigDecimal columns.
     * Must be used together with {@link #scale(int)}.
     *
     * @param value the decimal precision
     * @return this builder
     */
    WriterBuilder precision(int value) {
      options.precision(value)
      this
    }

    /**
     * Sets uniform scale for all BigDecimal columns.
     * Must be used together with {@link #precision(int)}.
     *
     * @param value the decimal scale
     * @return this builder
     */
    WriterBuilder scale(int value) {
      options.scale(value)
      this
    }

    /**
     * Sets per-column decimal precision and scale metadata.
     *
     * @param value map of column names to {@code [precision, scale]} arrays
     * @return this builder
     */
    WriterBuilder decimalMeta(Map<String, int[]> value) {
      options.decimalMeta(value)
      this
    }

    /**
     * Sets the timezone for converting LocalDateTime values to UTC timestamps.
     *
     * @param value the timezone
     * @return this builder
     */
    WriterBuilder zoneId(ZoneId value) {
      options.zoneId(value)
      this
    }

    /**
     * Sets the timezone for converting LocalDateTime values to UTC timestamps.
     *
     * @param value the timezone ID string (e.g. "Europe/Stockholm")
     * @return this builder
     */
    WriterBuilder zoneId(String value) {
      options.zoneId(value)
      this
    }

    /**
     * Sets the compression codec for the Parquet file (default: {@code SNAPPY}).
     *
     * @param value the compression codec
     * @return this builder
     */
    WriterBuilder compressionCodec(CompressionCodecName value) {
      options.compressionCodec(value)
      this
    }

    /**
     * Sets the compression codec for the Parquet file (default: {@code SNAPPY}).
     *
     * @param value the compression codec name, e.g. "GZIP", "SNAPPY", "ZSTD", "UNCOMPRESSED"
     * @return this builder
     */
    WriterBuilder compressionCodec(String value) {
      options.compressionCodec(value)
      this
    }

    /**
     * Sets the rounding mode used when BigDecimal values require scale conversion.
     * The default {@link RoundingMode#UNNECESSARY} rejects lossy conversion.
     *
     * @param value the rounding mode
     * @return this builder
     */
    WriterBuilder roundingMode(RoundingMode value) {
      options.roundingMode(value)
      this
    }

    /**
     * Sets the rounding mode by its trimmed, case-insensitive enum name.
     *
     * @param value the rounding mode name, for example {@code "HALF_UP"}
     * @return this builder
     */
    WriterBuilder roundingMode(String value) {
      options.roundingMode(value)
      this
    }

    /**
     * Writes the matrix to the specified file or directory.
     *
     * @param fileOrDir the target file or directory; if a directory, the filename is derived from the matrix name
     * @return the file that was written
     * @throws IllegalArgumentException if fileOrDir is null
     */
    File write(File fileOrDir) {
      if (fileOrDir == null) {
        throw new IllegalArgumentException(ERR_FILE_OR_DIR_NULL)
      }
      options.validate()
      MatrixParquetWriter.write(matrix, fileOrDir, options)
    }

    /**
     * Writes the matrix to a byte array in Parquet format.
     *
     * @return byte array containing Parquet data
     */
    byte[] writeBytes() {
      options.validate()
      MatrixParquetWriter.writeBytes(matrix, options)
    }

    File write(java.nio.file.Path path) {
      if (path == null) {
        throw new IllegalArgumentException('Path cannot be null')
      }
      write(path.toFile())
    }

    void write(OutputStream os) {
      if (os == null) {
        throw new IllegalArgumentException('OutputStream cannot be null')
      }
      os.write(writeBytes())
    }
  }

  /**
   * Validates the matrix and file parameters before writing.
   *
   * @param matrix the matrix to validate
   * @param fileOrDir the file or directory to validate
   * @throws IllegalArgumentException if matrix is null, has no columns, or fileOrDir is null
   */
  private static void validateInput(Matrix matrix, File fileOrDir) {
    validateInput(matrix)
    if (fileOrDir == null) {
      throw new IllegalArgumentException(ERR_FILE_OR_DIR_NULL)
    }
  }

  private static void validateInput(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException(ERR_MATRIX_NULL)
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException(ERR_MATRIX_EMPTY)
    }
  }

  /**
   * Writes a Matrix to a Parquet file, optionally inferring precision and scale for BigDecimal columns.
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory. If directory the file name will be based on the matrix name.
   * @param inferPrecisionAndScale whether to infer precision and scale for BigDecimal columns. If false,
   * BigDecimal fields require explicit precision/scale settings.
   * Nested Groovy {@link List} and {@link Map} structures are preserved as Parquet LIST/MAP/STRUCT types.
   * @return the target file
   * @throws IllegalArgumentException if matrix is null, has no columns, or fileOrDir is null
   */
  static File write(Matrix matrix, File fileOrDir, boolean inferPrecisionAndScale = true) {
    validateInput(matrix, fileOrDir)
    WriteSchema schema
    if (inferPrecisionAndScale) {
      schema = buildSchema(matrix,true)
    } else {
      schema = buildSchema(matrix, false)
    }
    log.debug("Write, inferPrecisionAndScale = $inferPrecisionAndScale, schema = $schema")
    File file = determineTargetFile(matrix, fileOrDir)

    return writeInternal(matrix, file, schema)
  }

  /**
   * Writes a Matrix to a Parquet file with a specific timezone for timestamp conversion.
   *
   * <p>By default, {@link LocalDateTime} values are converted to UTC timestamps using the system
   * default timezone. Use this method to specify a different timezone for the conversion.</p>
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory. If directory the file name will be based on the matrix name.
   * @param zoneId the timezone to use for converting LocalDateTime values to UTC timestamps
   * @return the target file
   * @throws IllegalArgumentException if matrix is null, has no columns, fileOrDir is null, or zoneId is null
   */
  static File write(Matrix matrix, File fileOrDir, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    ParquetValueWriter.withZoneId(zoneId) { write(matrix, fileOrDir, true) }
  }

  /**
   * Writes a Matrix to a Parquet file using uniform decimal precision
   * and scale for all BigDecimal columns instead of inferring settings.
   * Choose settings large enough for the data (e.g., [38, 18] for typical financial calculations).
   * Values that require rounding fail unless a rounding mode is explicitly selected.
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory
   * @param precision the decimal precision to use for all BigDecimal columns
   * @param scale the decimal scale to use for all BigDecimal columns
   * @return the target file
   * @throws IllegalArgumentException if matrix is null, has no columns, or fileOrDir is null
   */
  static File write(Matrix matrix, File fileOrDir, int precision, int scale) {
    write(matrix, fileOrDir, precision, scale, RoundingMode.UNNECESSARY)
  }

  /**
   * Writes a Matrix using uniform BigDecimal precision, scale, and rounding mode.
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory
   * @param precision the decimal precision to use for all BigDecimal columns
   * @param scale the decimal scale to use for all BigDecimal columns
   * @param roundingMode the scale-conversion rounding mode
   * @return the target file
   */
  static File write(Matrix matrix, File fileOrDir, int precision, int scale, RoundingMode roundingMode) {
    ParquetWriteOptions options = new ParquetWriteOptions()
        .precision(precision)
        .scale(scale)
        .roundingMode(roundingMode)
    write(matrix, fileOrDir, options)
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
   * Unmatched BigDecimal paths infer exact settings; metadata for unknown/non-decimal paths is rejected.
   * @return the target file
   * @throws IllegalArgumentException if matrix is null, has no columns, or fileOrDir is null
   */
  static File write(Matrix matrix, File fileOrDir, Map<String, int[]> decimalMeta) {
    validateInput(matrix, fileOrDir)
    File file = determineTargetFile(matrix, fileOrDir)

    def schema = buildSchema(matrix, ParquetWriteOptions.normalizeDecimalMeta(decimalMeta ?: [:]))
    return writeInternal(matrix, file, schema)
  }

  /**
   * Writes a Matrix to a Parquet file using a typed options object.
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory
   * @param options the write options to apply
   * @return the target file
   */
  static File write(Matrix matrix, File fileOrDir, ParquetWriteOptions options) {
    validateInput(matrix, fileOrDir)
    if (options == null) {
      throw new IllegalArgumentException(ERR_OPTIONS_NULL)
    }
    options.validate()
    File file = determineTargetFile(matrix, fileOrDir)
    if (options.zoneId != null) {
      return ParquetValueWriter.withZoneId(options.zoneId) {
        writeInternal(matrix, file, createSchema(matrix, options), options.compressionCodec, options.roundingMode, options)
      }
    }
    return writeInternal(matrix, file, createSchema(matrix, options), options.compressionCodec, options.roundingMode, options)
  }

  /**
   * Writes a Matrix to a byte array in Parquet format, optionally inferring precision and scale for BigDecimal columns.
   *
   * @param matrix the matrix to write
   * @param inferPrecisionAndScale whether to infer precision and scale for BigDecimal columns. If false,
   * BigDecimal fields require explicit precision/scale settings.
   * @return byte array containing Parquet data
   * @throws IllegalArgumentException if matrix is null or has no columns
   */
  static byte[] writeBytes(Matrix matrix, boolean inferPrecisionAndScale = true) {
    validateInput(matrix)
    WriteSchema schema = buildSchema(matrix, inferPrecisionAndScale)
    return writeBytesInternal(matrix, schema)
  }

  /**
   * Writes a Matrix to a byte array in Parquet format with a specific timezone for timestamp conversion.
   *
   * @param matrix the matrix to write
   * @param zoneId the timezone to use for converting LocalDateTime values to UTC timestamps
   * @return byte array containing Parquet data
   * @throws IllegalArgumentException if matrix is null, has no columns, or zoneId is null
   */
  static byte[] writeBytes(Matrix matrix, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException(ERR_ZONE_ID_NULL)
    }
    ParquetValueWriter.withZoneId(zoneId) { writeBytes(matrix, true) }
  }

  /**
   * Writes a Matrix to a byte array in Parquet format using explicitly provided decimal precision and scale metadata.
   *
   * @param matrix the matrix to write
   * @param decimalMeta a Map where keys are column names (String) and values are int arrays [precision, scale]
   * @return byte array containing Parquet data
   * @throws IllegalArgumentException if matrix is null or has no columns
   */
  static byte[] writeBytes(Matrix matrix, Map<String, int[]> decimalMeta) {
    validateInput(matrix)
    WriteSchema schema = buildSchema(matrix, ParquetWriteOptions.normalizeDecimalMeta(decimalMeta ?: [:]))
    return writeBytesInternal(matrix, schema)
  }

  /**
   * Writes a Matrix to a byte array using a typed options object.
   *
   * @param matrix the matrix to write
   * @param options the write options to apply
   * @return byte array containing Parquet data
   */
  static byte[] writeBytes(Matrix matrix, ParquetWriteOptions options) {
    validateInput(matrix)
    if (options == null) {
      throw new IllegalArgumentException(ERR_OPTIONS_NULL)
    }
    options.validate()
    if (options.zoneId != null) {
      return ParquetValueWriter.withZoneId(options.zoneId) {
        writeBytesInternal(matrix, createSchema(matrix, options), options.compressionCodec, options.roundingMode, options)
      }
    }
    return writeBytesInternal(matrix, createSchema(matrix, options), options.compressionCodec, options.roundingMode, options)
  }

  private static File determineTargetFile(Matrix matrix, File fileOrDir) {
    if (fileOrDir.isDirectory()) {
      java.nio.file.Path directory = fileOrDir.toPath().toAbsolutePath().normalize()
      String filename = normalizedDirectoryFilename(matrix.matrixName)
      java.nio.file.Path target = directory.resolve(filename).normalize()
      if (target.parent != directory) {
        throw new IllegalArgumentException("Normalized Matrix filename '$filename' is not a direct child of '$directory'")
      }
      File file = target.toFile()
      log.debug("Writing to ${file.absolutePath}")
      return file
    }
    fileOrDir
  }

  private static String normalizedDirectoryFilename(String matrixName) {
    String normalized = (matrixName ?: DEFAULT_MATRIX_NAME)
        .replaceAll(/[\\\/:<>"|?*\p{Cntrl}]/, '_')
        .replaceAll(/[. ]+$/, '')
    if (normalized.trim().isEmpty() || normalized.replace(DOT, '').trim().isEmpty()) {
      normalized = DEFAULT_MATRIX_NAME
    }
    normalized.toLowerCase(Locale.ROOT).endsWith('.parquet') ? normalized : "${normalized}.parquet"
  }

  private static WriteSchema createSchema(Matrix matrix, ParquetWriteOptions options) {
    buildSchema(matrix, options.decimalMeta,
        new DecimalSchemaPolicy(options.decimalMeta, options.inferPrecisionAndScale, options.precision, options.scale, '', [] as Set<String>,
            LogicalTypeAnnotation.TimeUnit.valueOf(options.timestampUnit)))
  }

  /**
   * Internal helper method that handles the core logic of writing matrix rows
   * to a Parquet file, given an already constructed schema.
   * @param matrix the matrix to write
   * @param file the target file
   * @param resolved the validated physical schema and column policies
   * @return the target file
   */
  private static File writeInternal(Matrix matrix, File file, WriteSchema resolved,
      CompressionCodecName compressionCodec = CompressionCodecName.SNAPPY, RoundingMode roundingMode = RoundingMode.UNNECESSARY, ParquetWriteOptions options = new ParquetWriteOptions()) {
    MessageType schema = resolved.schema
    java.nio.file.Path target = resolveOutputTarget(file.toPath().toAbsolutePath())
    java.nio.file.Path temporary = target.resolveSibling(".matrix-parquet-${UUID.randomUUID()}.tmp")
    java.nio.file.Files.createFile(temporary)
    try {
      if (java.nio.file.Files.exists(target) && java.nio.file.Files.getFileStore(target).supportsFileAttributeView('posix')) {
        java.nio.file.Files.setPosixFilePermissions(temporary, java.nio.file.Files.getPosixFilePermissions(target))
      }
      def conf = new org.apache.parquet.conf.PlainParquetConfiguration()
      def writerBuilder = ExampleParquetWriter.builder(new org.apache.parquet.io.LocalOutputFile(temporary))
          .withConf(conf).withWriteMode(ParquetFileWriter.Mode.OVERWRITE).withType(schema)
          .withCompressionCodec(compressionCodec).withExtraMetaData(buildExtraMeta(matrix, resolved))
      def writer = configureWriter(writerBuilder, schema, options).build()
      writer.withCloseable { parquetWriter -> writeRows(parquetWriter, matrix, schema, roundingMode) }
      // Fail safely if the filesystem cannot atomically replace the destination.
      java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
          java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      file
    } finally { java.nio.file.Files.deleteIfExists(temporary) }
  }

  private static java.nio.file.Path resolveOutputTarget(java.nio.file.Path path) {
    java.nio.file.Path target = path
    Set<java.nio.file.Path> visited = [] as Set
    while (java.nio.file.Files.isSymbolicLink(target)) {
      if (!visited.add(target.toAbsolutePath().normalize())) {
        throw new java.nio.file.FileSystemLoopException(path.toString())
      }
      target = target.resolveSibling(java.nio.file.Files.readSymbolicLink(target))
    }
    java.nio.file.Files.exists(target) ? target.toRealPath() : target
  }

  private static final String FORMAT_VERSION = '2'
  private static final String DOT = '.'

  private static ExampleParquetWriter.Builder configureWriter(ExampleParquetWriter.Builder builder, MessageType schema, ParquetWriteOptions options) {
    options.validate()
    Set<String> paths = schema.columns.collect { it.path.join(DOT) }.toSet()
    (options.bloomFilterEnabled.keySet() + options.bloomFilterNDV.keySet()).each { String path ->
      if (!paths.contains(path)) { throw new IllegalArgumentException("Unknown primitive column '$path'") }
    }
    builder.withRowGroupSize(options.rowGroupSize).withPageSize(options.pageSize)
        .withDictionaryEncoding(options.dictionaryEncoding)
        .withWriterVersion(options.writerVersion == 'V1' ? org.apache.parquet.column.ParquetProperties.WriterVersion.PARQUET_1_0 : org.apache.parquet.column.ParquetProperties.WriterVersion.PARQUET_2_0)
        .withColumnIndexTruncateLength(options.columnIndexTruncateLength)
        .withStatisticsTruncateLength(options.statisticsTruncateLength)
        .withSizeStatisticsEnabled(options.sizeStatisticsEnabled)
    options.bloomFilterEnabled.each { String path, Boolean enabled -> builder.withBloomFilterEnabled(path, enabled) }
    options.bloomFilterNDV.each { String path, Long ndv -> builder.withBloomFilterNDV(path, ndv) }
    builder
  }

  private static Map<String, String> buildExtraMeta(Matrix matrix, WriteSchema resolved) {
    Map<String, String> extraMeta = ['matrix.parquet.version': FORMAT_VERSION]
    extraMeta.put(METADATA_COLUMN_TYPES, matrix.columnNames().collect { String col -> resolved.types[col].metadata.name }.join(COMMA))
    if (!resolved.nestedTypes.isEmpty()) {
      extraMeta[METADATA_NESTED_TYPES] = '{' + resolved.nestedTypes.collect { String path, String type ->
        "\"${escapeJson(path)}\":\"${escapeJson(type)}\""
      }.join(COMMA) + '}'
    }
    if (matrix.hasIndex()) {
      extraMeta.put(METADATA_INDEX_COLUMNS, toJsonStringArray(matrix.indexedColumns()))
    }
    extraMeta
  }

  private static void writeRows(ParquetWriter<Group> parquetWriter, Matrix matrix, MessageType schema, RoundingMode roundingMode) {
    def factory = new SimpleGroupFactory(schema)
    def rowCount = matrix.rowCount()
    def colNames = matrix.columnNames()

    List<List> columns = colNames.collect { (List) matrix.column(it) }
    List<Type> fieldTypes = colNames.collect { schema.getType(it) }
    // Indexed loops avoid per-cell column-name scans, coercion and closure allocation in this hot path.
    for (int row = 0; row < rowCount; row++) {
      Group group = factory.newGroup()
      for (int column = 0; column < colNames.size(); column++) {
        ParquetValueWriter.writeValue(group, colNames[column], fieldTypes[column], columns[column][row], roundingMode, colNames[column], row)
      }
      parquetWriter.write(group)
    }
  }

  /**
   * Internal helper method that handles the core logic of writing matrix rows
   * to a byte array, given an already constructed schema.
   *
   * <p>This method writes directly to memory using {@link InMemoryOutputFile} without creating
   * temporary files. This is more efficient than the previous temp file approach.</p>
   *
   * <p><strong>Performance Note:</strong> For very large matrices (&gt;100MB), consider using
   * {@link #write(Matrix, File)} directly to avoid memory pressure. The in-memory approach
   * requires approximately 2-3x the final file size in heap memory.</p>
   *
   * <p><strong>Size Limitation:</strong> Maximum file size is ~2GB due to ByteArrayOutputStream
   * constraints (Integer.MAX_VALUE).</p>
   *
   * @param matrix the matrix to write
   * @param resolved the validated physical schema and column policies
   * @return byte array containing Parquet data
   */
  private static byte[] writeBytesInternal(Matrix matrix, WriteSchema resolved,
      CompressionCodecName compressionCodec = CompressionCodecName.SNAPPY, RoundingMode roundingMode = RoundingMode.UNNECESSARY, ParquetWriteOptions options = new ParquetWriteOptions()) {
    MessageType schema = resolved.schema
    def conf = new org.apache.parquet.conf.PlainParquetConfiguration()
    InMemoryOutputFile outputFile = new InMemoryOutputFile()
    def writerBuilder = ExampleParquetWriter.builder(outputFile)
        .withConf(conf)
        .withType(schema)
        .withCompressionCodec(compressionCodec)
        .withExtraMetaData(buildExtraMeta(matrix, resolved))
    def writer = configureWriter(writerBuilder, schema, options).build()
    writer.withCloseable { parquetWriter -> writeRows(parquetWriter, matrix, schema, roundingMode) }
    return outputFile.getBytes()
  }

  private static String toJsonStringArray(List<String> values) {
    "[${values.collect { String value -> "\"${escapeJson(value)}\"" }.join(COMMA)}]"
  }

  private static String escapeJson(String value) {
    StringBuilder escaped = new StringBuilder()
    String source = String.valueOf(value)
    for (int i = 0; i < source.length(); i++) {
      char ch = source.charAt(i)
      if (ch == '\\' as char) {
        escaped.append('\\\\')
      } else if (ch == '"' as char) {
        escaped.append('\\"')
      } else if (ch == '\b' as char) {
        escaped.append('\\b')
      } else if (ch == '\f' as char) {
        escaped.append('\\f')
      } else if (ch == '\n' as char) {
        escaped.append('\\n')
      } else if (ch == '\r' as char) {
        escaped.append('\\r')
      } else if (ch == '\t' as char) {
        escaped.append('\\t')
      } else if (Character.isISOControl(ch)) {
        escaped.append('\\u').append(Integer.toHexString((int) ch).padLeft(4, ZERO))
      } else {
        escaped.append(ch)
      }
    }
    escaped.toString()
  }

  /**
   * Builds the Parquet schema based on the matrix and an optional map of explicit decimal metadata.
   * This overload is the core schema builder.
   *
   * @param matrix the matrix
   * @param explicitDecimalMeta map of column name to [precision, scale] array for BigDecimal columns.
   * @return validated physical schema and metadata policies.
   */
  private static WriteSchema buildSchema(Matrix matrix, Map<String, int[]> explicitDecimalMeta, DecimalSchemaPolicy decimals = null) {
    DecimalSchemaPolicy policy = decimals ?: new DecimalSchemaPolicy(explicitDecimalMeta ?: [:])
    Map<String, ColumnTypePolicy> types = matrix.columnNames().collectEntries { String name ->
      [(name): ColumnTypePolicy.resolve(name, matrix.column(name), matrix.type(name), policy.timestampUnit)]
    }
    Map<String, int[]> decimalMeta = withBigIntegerPrecision(matrix, explicitDecimalMeta, types)
    def builder = Types.buildMessage()
    matrix.columnNames().each { col ->
      def type = matrix.type(col)
      def meta = decimalMeta?.get(col)
      log.debug("Building schema for column '$col' of type $type with decimal meta: $meta")
      builder.addField(buildParquetType(col, matrix.column(col), type, meta, false, policy.child(col), types[col]))
    }
    Set<String> unknown = new HashSet<>(policy.settings.keySet())
    unknown.removeAll(policy.used)
    if (!unknown.empty) { throw new IllegalArgumentException("Unknown/non-decimal metadata paths: $unknown") }
    new WriteSchema(builder.named(matrix.matrixName ?: 'MatrixSchema'), types, policy.nestedTypes)
  }

  /**
   * Ensures every BigInteger column has decimal metadata {@code [precision, 0]}.
   * Explicit caller-supplied metadata wins after validating its scale is 0
   * and that its precision is large enough to hold every value in the column.
   */
  private static Map<String, int[]> withBigIntegerPrecision(Matrix matrix, Map<String, int[]> explicitDecimalMeta, Map<String, ColumnTypePolicy> types) {
    Map<String, int[]> merged = [:] + (explicitDecimalMeta ?: [:])
    matrix.columnNames().each { col ->
      if (types[col].encoding != BigInteger) {
        return
      }
      int[] existing = merged[col]
      if (existing != null) {
        if (existing[1] != 0) {
          throw new IllegalArgumentException(
              "decimalMeta['$col'] scale must be 0 for BigInteger columns but was ${existing[1]}")
        }
        int requiredPrecision = inferBigIntegerPrecision(matrix.column(col))
        if (existing[0] < requiredPrecision) {
          throw new IllegalArgumentException(
              "decimalMeta['$col'] precision (${existing[0]}) is too small to hold the data in this " +
              "column (requires at least $requiredPrecision digits)")
        }
      } else {
        merged[col] = [inferBigIntegerPrecision(matrix.column(col)), 0] as int[]
      }
    }
    merged
  }

  private static int inferBigIntegerPrecision(List<?> values) {
    int maxDigits = 1
    values.each { value ->
      if (value instanceof BigInteger) {
        int digits = value.abs().toString().length()
        maxDigits = Math.max(maxDigits, digits)
      }
    }
    maxDigits
  }

  /**
   * Builds the Parquet schema based on the matrix, inferring decimal precision/scale if requested.
   * This is for backward compatibility and delegates to the explicit map builder.
   * @param matrix the matrix
   * @param inferPrecisionAndScale if true, infers decimal precision and scale. If false, BigDecimal fields require
   * explicit precision/scale settings.
   * @return validated physical schema and metadata policies.
   */
  private static WriteSchema buildSchema(Matrix matrix, boolean inferPrecisionAndScale = false) {
    buildSchema(matrix, [:], new DecimalSchemaPolicy([:], inferPrecisionAndScale))
  }

  /**
   * Builds a Parquet {@link Type} for the supplied column, recursively handling nested collections and maps.
   *
   * @param name the field name in the Parquet schema
   * @param values the column values used to infer nested shapes
   * @param clazz the declared Matrix column type
   * @param decimalMeta optional decimal precision/scale metadata for {@link BigDecimal} columns
   * @return a Parquet {@link Type} describing the column
   */
  private static Type buildParquetType(String name, List<?> values, Class clazz, int[] decimalMeta = null, boolean preferStructForMaps = false, DecimalSchemaPolicy decimals = new DecimalSchemaPolicy([:]), ColumnTypePolicy resolved = null) {
    boolean nested = resolved == null
    resolved = resolved ?: ColumnTypePolicy.resolve(decimals.path ?: name, values, clazz, decimals.timestampUnit)
    if (resolved.encoding == List) {
      List<?> nestedValues = extractListElements(values)
      Type elementType = buildParquetType(FIELD_ELEMENT, nestedValues, Object, null, true, decimals.child(FIELD_ELEMENT))
      return Types.optionalList().element(elementType).named(name)
    }

    if (resolved.encoding == Map) {
      if (preferStructForMaps && values.findAll { it != null }.every { ((Map) it).keySet().every { key -> key instanceof String } }) {
        return buildStructType(name, values, decimals)
      }
      return buildMapType(name, values, decimals)
    }

    Class<?> effectiveClass = resolved.encoding
    int[] effectiveDecimalMeta = decimalMeta
    if (effectiveClass == BigDecimal) { effectiveDecimalMeta = decimals.resolve(values, effectiveDecimalMeta) }
    if (effectiveClass == BigInteger && decimals.settings.containsKey(decimals.path)) { decimals.used.add(decimals.path) }
    if (effectiveClass == BigInteger && effectiveDecimalMeta == null) {
      effectiveDecimalMeta = [inferBigIntegerPrecision(values), 0] as int[]
    }
    if (nested) { recordNestedType(decimals, resolved) }
    return buildPrimitiveType(name, effectiveClass, effectiveDecimalMeta, false, decimals.path ?: name, decimals.timestampUnit)
  }

  @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity']) // Exhaustive physical/logical type dispatch.
  private static PrimitiveType buildPrimitiveType(String name, Class clazz, int[] decimalMeta = null, boolean required = false, String path = name, LogicalTypeAnnotation.TimeUnit timestampUnit = LogicalTypeAnnotation.TimeUnit.MICROS) {
    if (clazz == BigInteger) {
      if (decimalMeta == null || decimalMeta.length != DECIMAL_META_LENGTH || decimalMeta[0] <= 0 || decimalMeta[1] != 0) {
        throw new IllegalArgumentException("BigInteger field '$path' requires decimal metadata [precision, 0]")
      }
      int precision = decimalMeta[0]
      def builder = required ? Types.required(PrimitiveTypeName.BINARY) : Types.optional(PrimitiveTypeName.BINARY)
      return builder.as(LogicalTypeAnnotation.decimalType(0, precision)).named(name)
    }

    if (clazz == BigDecimal) {
      if (decimalMeta != null && decimalMeta.length == DECIMAL_META_LENGTH) {
        int precision = decimalMeta[0]
        int scale = decimalMeta[1]

        def builder = required
            ? Types.required(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
            : Types.optional(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
        builder = builder
            .length(minBytesForPrecision(precision))
            .as(LogicalTypeAnnotation.decimalType(scale, precision))
        return builder.named(name)
      }
      throw new IllegalArgumentException("BigDecimal field '$path' requires precision/scale")
    }

    if (clazz == UUID) {
      def uuidBuilder = required ? Types.required(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) : Types.optional(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
      return uuidBuilder.length(16).as(LogicalTypeAnnotation.uuidType()).named(name)
    }

    def primitive
    def logical = null

    switch (clazz) {
      case Byte, Short -> {
        primitive = PrimitiveTypeName.INT32
        logical = LogicalTypeAnnotation.intType(clazz == Byte ? 8 : 16, true)
      }
      case byte[] -> primitive = PrimitiveTypeName.BINARY
      case String, Character -> {
        primitive = PrimitiveTypeName.BINARY
        logical = LogicalTypeAnnotation.stringType()
      }
      case LocalTime -> {
        primitive = PrimitiveTypeName.INT64
        logical = LogicalTypeAnnotation.timeType(false, LogicalTypeAnnotation.TimeUnit.NANOS)
      }
      case Integer, int -> primitive = PrimitiveTypeName.INT32
      case Long, long -> primitive = PrimitiveTypeName.INT64
      case Float, float -> primitive = PrimitiveTypeName.FLOAT
      case Double, double -> primitive = PrimitiveTypeName.DOUBLE
      case Boolean, boolean -> primitive = PrimitiveTypeName.BOOLEAN
      case LocalDate, java.sql.Date -> {
        primitive = PrimitiveTypeName.INT32
        logical = LogicalTypeAnnotation.dateType()
      }
      case LocalDateTime, Timestamp, Instant -> {
        primitive = PrimitiveTypeName.INT64
        logical = LogicalTypeAnnotation.timestampType(true, timestampUnit)
      }
      case Time -> {
        primitive = PrimitiveTypeName.INT32
        logical = LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      case Date -> {
        primitive = PrimitiveTypeName.INT64
        logical = LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      default -> throw new IllegalArgumentException("Unsupported primitive $clazz for field $path")
    }

    def builder = required ? Types.required(primitive) : Types.optional(primitive)
    if (logical != null) {
      builder = builder.as(logical)
    }
    return builder.named(name)
  }

  private static Type buildMapType(String name, List<?> values, DecimalSchemaPolicy decimals) {
    def mapBuilder = Types.optionalMap()
    List<Map> maps = values.findAll { it instanceof Map } as List<Map>
    if (maps.any { it.keySet().any { key -> key == null } }) { throw new IllegalArgumentException("Map field '${decimals.path ?: name}' cannot contain null keys") }

    List<Object> keySamples = maps.collectMany { it?.keySet() ?: [] } as List<Object>
    ColumnTypePolicy keyPolicy = ColumnTypePolicy.resolve(decimals.child(FIELD_KEY).path, keySamples, Object, decimals.timestampUnit)
    Class<?> keyClass = keyPolicy.encoding
    recordNestedType(decimals.child(FIELD_KEY), keyPolicy)
    int[] keyDecimalMeta = keyClass == BigInteger ? [inferBigIntegerPrecision(keySamples), 0] as int[] : keyClass == BigDecimal ? decimals.child(FIELD_KEY).resolve(keySamples, null) : null
    PrimitiveType keyType = buildPrimitiveType(FIELD_KEY, keyClass ?: String, keyDecimalMeta, true, decimals.child(FIELD_KEY).path, decimals.timestampUnit)
    mapBuilder.key(keyType)

    List<Object> valueSamples = maps.collectMany { it?.values() ?: [] } as List<Object>
    Set<Class> valueClasses = valueSamples.findAll { it != null }.collect { ColumnTypePolicy.policyType(it.getClass()) }.toSet()
    if (valueClasses.size() > 1 || valueClasses.contains(null)) {
      return buildStructType(name, values, decimals)
    }
    Type valueType
    if (valueSamples.isEmpty()) {
      valueType = buildPrimitiveType(FIELD_VALUE, String, null, false)
    } else {
      valueType = buildParquetType(FIELD_VALUE, valueSamples, Object, null, false, decimals.child(FIELD_VALUE))
    }
    mapBuilder.value(valueType)
    return mapBuilder.named(name)
  }

  private static void recordNestedType(DecimalSchemaPolicy decimals, ColumnTypePolicy policy) {
    if (policy.metadata in [Instant, Date, Timestamp, java.sql.Date, Byte, Short, BigInteger]) {
      String existing = decimals.nestedTypes[decimals.path]
      if (existing != null && existing != policy.metadata.name) {
        // Same path recorded with different types; drop it rather than silently keeping one.
        decimals.nestedTypes.remove(decimals.path)
      } else {
        decimals.nestedTypes[decimals.path] = policy.metadata.name
      }
    }
  }

  private static Type buildStructType(String name, List<?> values, DecimalSchemaPolicy decimals) {
    def groupBuilder = Types.optionalGroup()
    Set<String> keys = new LinkedHashSet<>()
    values.each { value ->
      if (value instanceof Map) {
        value.keySet().each { key ->
          if (!(key instanceof String)) { throw new IllegalArgumentException("Struct field '${decimals.path ?: name}' requires String keys; got ${key?.getClass()?.name}") }
          keys.add(key)
        }
      }
    }
    keys.each { String key ->
      List collected = values.collect { value -> value == null ? null : ((Map) value)[key] }
      groupBuilder.addField(buildParquetType(key, collected, Object, null, false, decimals.child(key)))
    }
    groupBuilder.named(name)
  }

  private static List<?> extractListElements(List<?> values) {
    List<Object> elements = []
    values.each { val ->
      if (val instanceof Collection coll) {
        coll.each { elements.add(it) }
      }
    }
    return elements
  }

  private static int minBytesForPrecision(int precision) {
    int bitsNeeded = BigInteger.TEN.pow(precision).subtract(BigInteger.ONE).bitLength() + 1
    Math.ceil(bitsNeeded / 8d) as int
  }

  private static final class WriteSchema {
    final MessageType schema
    final Map<String, ColumnTypePolicy> types
    final Map<String, String> nestedTypes

    WriteSchema(MessageType schema, Map<String, ColumnTypePolicy> types, Map<String, String> nestedTypes) {
      this.nestedTypes = nestedTypes
      this.schema = schema
      this.types = types
    }
  }
}
