package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.commons.csv.DuplicateHeaderMode
import org.apache.commons.csv.QuoteMode
import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.input.BOMInputStream
import org.apache.commons.io.input.CloseShieldInputStream
import org.apache.commons.io.input.CloseShieldReader

import se.alipsa.matrix.core.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.text.NumberFormat

/**
 * Reads CSV files into Matrix objects using Apache Commons CSV.
 *
 * <p>Provides flexible CSV parsing with support for multiple input sources
 * (File, Path, URL, InputStream, Reader, String content) and configurable
 * format options through the fluent API or the {@link CsvOption} enum.</p>
 *
 * <h3>Fluent API (recommended)</h3>
 * <pre>
 * // Simple read with defaults
 * Matrix m = CsvReader.read().from(file)
 *
 * // Read with custom delimiter
 * Matrix m = CsvReader.read().delimiter(';').from(file)
 *
 * // Read from String content
 * Matrix m = CsvReader.read().fromString(csvContent)
 *
 * // Read TSV
 * Matrix m = CsvReader.read().tsv().from(file)
 *
 * // Read with explicit header
 * Matrix m = CsvReader.read().header(['id', 'name', 'amount']).from(file)
 * </pre>
 *
 * <h3>Map-based API</h3>
 * <pre>
 * // Named arguments (camelCase, case-insensitive)
 * Matrix m = CsvReader.read(delimiter: ';', quote: '"', file)
 *
 * // With type conversion
 * Matrix m = CsvReader.read(
 *     types: [Integer, String, LocalDate, BigDecimal],
 *     dateTimeFormat: 'yyyy-MM-dd',
 *     file)
 * </pre>
 *
 * @see CsvWriter
 * @see CsvOption
 */
@CompileStatic
class CsvReader {

  private static final String PATH_SEPARATOR = '/'
  private static final String EXTENSION_SEPARATOR = '.'
  private static final String DEFAULT_MATRIX_NAME = 'matrix'
  private static final Charset UTF_32LE = Charset.forName('UTF-32LE')
  private static final Charset UTF_32BE = Charset.forName('UTF-32BE')

  // ──────────────────────────────────────────────────────────────
  // Map-based API (uses CsvOption enum as keys)
  // ──────────────────────────────────────────────────────────────

  /**
   * Read a CSV file from a URL with custom format options.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param url URL pointing to the CSV file to read
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the URL fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, URL url) throws IOException {
    read(url, CsvReadOptions.fromMap(format))
  }

  /**
   * Read a CSV file from a URL string with custom format options.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param url String URL pointing to the CSV file to read
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the URL fails or URL is invalid
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, String url) throws IOException {
    try {
      read(new URI(url).toURL(), CsvReadOptions.fromMap(format))
    } catch (URISyntaxException e) {
      throw new IOException("Invalid URL: ${url}", e)
    }
  }

  /**
   * Read a CSV file from an InputStream with custom format options.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param is InputStream to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the stream fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, InputStream is) throws IOException {
    read(is, CsvReadOptions.fromMap(format))
  }

  /**
   * Read a CSV file from a Reader with custom format options.
   * The caller is responsible for closing the Reader.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param reader Reader to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, Reader reader) throws IOException {
    read(reader, CsvReadOptions.fromMap(format))
  }

  /**
   * Read a CSV file from a File with custom format options.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param file File to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, File file) throws IOException {
    read(file, CsvReadOptions.fromMap(format))
  }

  /**
   * Read a CSV file from a Path with custom format options.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param path Path to the CSV file to read
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, Path path) throws IOException {
    read(path, CsvReadOptions.fromMap(format))
  }

  // ──────────────────────────────────────────────────────────────
  // Fluent builder API (recommended)
  // ──────────────────────────────────────────────────────────────

  /**
   * Entry point for the fluent CSV reading API.
   *
   * <p>Returns a {@link ReadBuilder} that allows chaining format configuration
   * methods before triggering I/O with a terminal {@code from*()} method.</p>
   *
   * <pre>
   * Matrix m = CsvReader.read().from(file)
   * Matrix m = CsvReader.read().delimiter(';').from(file)
   * Matrix m = CsvReader.read().tsv().from(file)
   * </pre>
   *
   * @return a new {@link ReadBuilder}
   */
  static ReadBuilder read() {
    new ReadBuilder()
  }

  /**
   * Reads CSV data from a File using typed read options.
   *
   * <p>If {@code options.tableName} is set it wins. Otherwise the Matrix name is derived
   * from the file basename. For {@code .tsv} and {@code .tab} files, tab-delimited parsing
   * is auto-selected when the delimiter was not explicitly configured.</p>
   *
   * @param file the file to read
   * @param options typed CSV read options
   * @return the parsed Matrix
   * @throws IOException if reading fails
   */
  static Matrix read(File file, CsvReadOptions options) throws IOException {
    buildReadBuilder(options, file.name, false).from(file)
  }

  /**
   * Reads CSV data from a Path using typed read options.
   *
   * @param path the path to read
   * @param options typed CSV read options
   * @return the parsed Matrix
   * @throws IOException if reading fails
   */
  static Matrix read(Path path, CsvReadOptions options) throws IOException {
    read(path.toFile(), options)
  }

  /**
   * Reads CSV data from a URL using typed read options.
   *
   * <p>If {@code options.tableName} is set it wins. Otherwise the Matrix name is derived
   * from the URL basename. For {@code .tsv} and {@code .tab} URLs, tab-delimited parsing
   * is auto-selected when the delimiter was not explicitly configured.</p>
   *
   * @param url the URL to read
   * @param options typed CSV read options
   * @return the parsed Matrix
   * @throws IOException if reading fails
   */
  static Matrix read(URL url, CsvReadOptions options) throws IOException {
    buildReadBuilder(options, url.path, false).from(url)
  }

  /**
   * Reads CSV data from an InputStream using typed read options.
   *
   * <p>The charset option applies to this byte-based source. A matching UTF-8,
   * UTF-16LE, UTF-16BE, UTF-32LE, or UTF-32BE byte-order mark is stripped. If no explicit
   * table name is configured, the resulting Matrix uses the fallback name {@code matrix}.</p>
   *
   * @param is the input stream to read
   * @param options typed CSV read options
   * @return the parsed Matrix
   * @throws IOException if reading fails
   */
  static Matrix read(InputStream is, CsvReadOptions options) throws IOException {
    buildReadBuilder(options, null, true).from(is)
  }

  /**
   * Reads CSV data from a Reader using typed read options.
   *
   * <p>The charset option is ignored for Reader-based input because the Reader already
   * supplies decoded characters. If no explicit table name is configured, the resulting
   * Matrix uses the fallback name {@code matrix}.</p>
   *
   * @param reader the Reader to read
   * @param options typed CSV read options
   * @return the parsed Matrix
   * @throws IOException if reading fails
   */
  static Matrix read(Reader reader, CsvReadOptions options) throws IOException {
    buildReadBuilder(options, null, true).from(reader)
  }

  /**
   * Reads CSV data from an in-memory String using typed read options.
   *
   * <p>The charset option is ignored for String content because the content is already
   * decoded. If no explicit table name is configured, the resulting Matrix uses the
   * fallback name {@code matrix}.</p>
   *
   * @param csvContent the CSV content to parse
   * @param options typed CSV read options
   * @return the parsed Matrix
   * @throws IOException if parsing fails
   */
  static Matrix readString(String csvContent, CsvReadOptions options) throws IOException {
    buildReadBuilder(options, null, true).fromString(csvContent)
  }

  // ──────────────────────────────────────────────────────────────
  // CSVFormat-based API (deprecated — use fluent API)
  // ──────────────────────────────────────────────────────────────

  /**
   * Read a CSV file from an InputStream using Apache Commons CSV format.
   *
   * @param is InputStream to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param matrixName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading the stream fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().from(inputStream)} instead
   */
  @Deprecated
  static Matrix read(InputStream is, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    InputStream input = byteInput(is, charset, true)
    try (CSVParser parser = CSVParser.parse(input, charset, parserFormat(format))) {
      parse(matrixName, parser, firstRowAsHeader, format)
    }
  }

  /**
   * Read a CSV file from a Reader using Apache Commons CSV format.
   * The caller is responsible for closing the Reader.
   *
   * @param reader Reader to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8); deprecated because it has no effect for Reader-based input
   * @param matrixName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading from the Reader fails
  * @throws IllegalArgumentException if columns are mismatched between rows
  * @deprecated Use {@code CsvReader.read().from(reader)} instead. The {@code charset} parameter has no effect when reading from a Reader because the Reader already handles character decoding.
  */
  @Deprecated
  @SuppressWarnings('UnusedMethodParameter')
  static Matrix read(Reader reader, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (CSVParser parser = CSVParser.parse(CloseShieldReader.wrap(reader), parserFormat(format))) {
      parse(matrixName, parser, firstRowAsHeader, format)
    }
  }

  /**
   * Read CSV content from a String.
   *
   * @param csvContent String containing CSV data
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @return Matrix containing the imported data
   * @throws IOException if parsing fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().fromString(csvContent)} instead
   */
  @Deprecated
  static Matrix readString(String csvContent, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true) throws IOException {
    read(new StringReader(csvContent), format, firstRowAsHeader, StandardCharsets.UTF_8, '')
  }

  /**
   * Read a CSV file from a URL using Apache Commons CSV format.
   *
   * @param url URL pointing to the CSV file to read
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from URL
   * @throws IOException if reading the URL fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().from(url)} instead
   */
  @Deprecated
  static Matrix read(URL url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (InputStream source = url.openStream()) {
      InputStream input = byteInput(source, charset, false)
      try (CSVParser parser = CSVParser.parse(input, charset, parserFormat(format))) {
        parse(tableName(url), parser, firstRowAsHeader, format)
      }
    }
  }

  /**
   * Read a CSV file from a URL string using Apache Commons CSV format.
   *
   * @param url String URL pointing to the CSV file to read
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from URL
   * @throws IOException if reading the URL fails or URL is invalid
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().fromUrl(url)} instead
   */
  @Deprecated
  static Matrix readUrl(String url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try {
      read(new URI(url).toURL(), format, firstRowAsHeader, charset)
    } catch (URISyntaxException e) {
      throw new IOException("Invalid URL: ${url}", e)
    }
  }

  /**
   * Read a CSV file from a Path using Apache Commons CSV format.
   *
   * @param path Path to the CSV file to read
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from file name
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().from(path)} instead
   */
  @Deprecated
  static Matrix read(Path path, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(path.toFile(), format, firstRowAsHeader, charset)
  }

  /**
   * Read a CSV file from a File using Apache Commons CSV format.
   *
   * @param file File to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from file name
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().from(file)} instead
   */
  @Deprecated
  static Matrix read(File file, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (InputStream source = new FileInputStream(file)) {
      InputStream input = byteInput(source, charset, false)
      try (CSVParser parser = CSVParser.parse(input, charset, parserFormat(format))) {
        parse(tableName(file), parser, firstRowAsHeader, format)
      }
    }
  }

  /**
   * Read a CSV file from a file path string (convenience method).
   *
   * @param filePath path to the CSV file as a String
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with default settings
   * @throws IOException if reading the file fails or file not found
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().fromFile(filePath)} instead
   */
  @Deprecated
  static Matrix readFile(String filePath, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(new File(filePath), format, firstRowAsHeader, charset)
  }

  // ──────────────────────────────────────────────────────────────
  // Internal parsing and helper methods
  // ──────────────────────────────────────────────────────────────

  /**
   * Core parsing logic that converts CSVParser records into a Matrix.
   *
   * <p>Handles column name extraction, validates that all rows have the same number
   * of columns, and generates auto-generated column names (c0, c1, ...) when needed.</p>
   *
   * @param matrixName name for the resulting Matrix (can be null or empty)
   * @param parser CSVParser containing the parsed CSV data
   * @param firstRowAsHeader if true, uses first row as column names; if false, generates c0, c1, etc.
   * @param format original CSV format whose null and inferred-header settings are applied to the result
   * @return Matrix containing the parsed data with all values as Strings
   * @throws IllegalArgumentException if rows have inconsistent column counts
   */
  private static Matrix parse(String matrixName, CSVParser parser, boolean firstRowAsHeader, CSVFormat format) {
    List<CSVRecord> records = parser.records
    boolean extractedHeader = inferredHeaderNullString(format) != null
    List<String> headerRow
    if (extractedHeader && !records.isEmpty()) {
      List<String> inferredHeader = records.remove(0).toList()
      boolean absentHeader = inferredHeader.every { String name -> name == null || name.isEmpty() }
          && !records.isEmpty()
          && inferredHeader.size() < records[0].size()
      if (absentHeader) {
        headerRow = []
      } else {
        validateInferredHeader(inferredHeader, format)
        headerRow = inferredHeader.collect { String name -> name == null ? '' : name }
      }
    } else {
      headerRow = parserHeaderRow(parser)
    }

    // Handle empty CSV file
    if (records.isEmpty()) {
      if (!headerRow.isEmpty()) {
        return Matrix.builder()
            .matrixName(matrixName ?: DEFAULT_MATRIX_NAME)
            .columnNames(headerRow)
            .types([String] * headerRow.size())
            .build()
      }
      return Matrix.builder()
          .matrixName(matrixName ?: DEFAULT_MATRIX_NAME)
          .build()
    }

    int ncols = headerRow.isEmpty() ? records[0].size() : headerRow.size()
    boolean consumedHeader = firstRowAsHeader && !headerRow.isEmpty()
    records.each { CSVRecord record ->
      if (record.size() != ncols) {
        long sourceRecord = record.recordNumber + (consumedHeader && !extractedHeader ? 1 : 0)
        throw new IllegalArgumentException("CSV record $sourceRecord has ${record.size()} columns; expected $ncols")
      }
    }
    List<List<String>> rows = records*.toList()
    if (headerRow.isEmpty() && firstRowAsHeader) {
      headerRow = rows.remove(0)
    } else if (headerRow.isEmpty()) {
      for (int i = 0; i < ncols; i++) {
        headerRow << 'c' + i
      }
    }
    def types = [String] * ncols
    Matrix.builder()
        .matrixName(matrixName ?: DEFAULT_MATRIX_NAME)
        .columnNames(headerRow)
        .rows(rows)
        .types(types)
        .build()
  }

  private static List<String> parserHeaderRow(CSVParser parser) {
    if (parser.headerNames == null || parser.headerNames.isEmpty()) {
      return []
    }
    List<String> headerNames = parser.headerNames.toList()
    Map<String, Integer> headerMap = parser.headerMap
    if (headerMap == null || headerMap.isEmpty()) {
      return headerNames
    }
    int headerWidth = headerMap.values().max() + 1
    if (headerNames.size() == headerWidth) {
      return headerNames
    }
    List<String> resolved = emptyHeader(headerWidth)
    headerMap.each { String name, Integer index ->
      resolved[index] = name
    }
    resolved
  }

  private static void validateInferredHeader(List<String> header, CSVFormat format) {
    boolean observedMissing = false
    Set<String> observed = format.ignoreHeaderCase
        ? new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)
        : new LinkedHashSet<String>()
    header.each { String name ->
      boolean blank = name == null || name.isBlank()
      if (blank && !format.allowMissingColumnNames) {
        throw new IllegalArgumentException("A header name is missing in ${header}")
      }
      boolean duplicate = blank ? observedMissing : observed.contains(name)
      DuplicateHeaderMode mode = format.duplicateHeaderMode
      if (duplicate && mode != DuplicateHeaderMode.ALLOW_ALL && !(blank && mode == DuplicateHeaderMode.ALLOW_EMPTY)) {
        throw new IllegalArgumentException("The header contains a duplicate name: \"${name}\" in ${header}. "
            + 'If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().')
      }
      observedMissing |= blank
      if (name != null) {
        observed << name
      }
    }
  }

  private static List<String> emptyHeader(int width) {
    Collections.nCopies(width, '').toList()
  }

  /**
   * Extracts a table name from a URL by removing the path and file extension.
   *
   * @param url URL to extract the name from
   * @return file name without extension, or the path if no file name is present
   */
  @groovy.transform.PackageScope
  static String tableName(URL url) {
    String name
    try {
      name = url.toURI().path
    } catch (URISyntaxException ignored) {
      // Fall through to URL accessors, which also work for non-RFC URL strings.
    }
    if (!name) {
      name = url.file ?: url.path ?: ''
      int cutIndex = name.indexOf('?')
      if (cutIndex < 0) {
        cutIndex = name.indexOf('#')
      }
      if (cutIndex >= 0) {
        name = name.substring(0, cutIndex)
      }
    }
    if (name.contains(PATH_SEPARATOR)) {
      name = name.substring(name.lastIndexOf(PATH_SEPARATOR) + 1, name.length())
    }
    if (name.contains(EXTENSION_SEPARATOR)) {
      name = name.substring(0, name.lastIndexOf(EXTENSION_SEPARATOR))
    }
    name
  }

  /**
   * Extracts a table name from a File by removing the file extension.
   *
   * @param file File to extract the name from
   * @return file name without extension
   */
  @groovy.transform.PackageScope
  static String tableName(File file) {
    def name = file.getName()
    if (name.contains(EXTENSION_SEPARATOR)) {
      name = name.substring(0, name.lastIndexOf(EXTENSION_SEPARATOR))
    }
    name
  }

  private static ReadBuilder buildReadBuilder(CsvReadOptions options, String sourceName, boolean useFallbackMatrixName) {
    if (options == null) {
      throw new IllegalArgumentException('CsvReadOptions must not be null')
    }
    CsvReadOptions readOptions = options
    ReadBuilder builder = read()
        .delimiter(resolveReadDelimiter(readOptions, sourceName))
        .quoteCharacter(readOptions.quote)
        .escapeCharacter(readOptions.escape)
        .commentMarker(readOptions.commentMarker)
        .trim(readOptions.trim)
        .ignoreEmptyLines(readOptions.ignoreEmptyLines)
        .ignoreSurroundingSpaces(readOptions.ignoreSurroundingSpaces)
        .nullString(readOptions.nullString)
        .recordSeparator(readOptions.recordSeparator)
        .firstRowAsHeader(readOptions.firstRowAsHeader)
        .header(readOptions.header)
        .charset(readOptions.charset)
        .duplicateHeaderMode(readOptions.duplicateHeaderMode)

    if (readOptions.tableName != null) {
      builder.matrixName(readOptions.tableName)
    } else if (useFallbackMatrixName) {
      builder.matrixName(DEFAULT_MATRIX_NAME)
    }
    if (readOptions.types != null) {
      builder.types(readOptions.types)
    }
    if (readOptions.dateTimeFormat != null) {
      builder.dateTimeFormat(readOptions.dateTimeFormat)
    }
    if (readOptions.numberFormat != null) {
      builder.numberFormat(readOptions.numberFormat)
    }
    builder
  }

  private static char resolveReadDelimiter(CsvReadOptions options, String sourceName) {
    if (!options.delimiterConfigured && CsvOptionUtil.isTsvFileName(sourceName)) {
      return '\t' as char
    }
    options.delimiter as char
  }

  private static String inferredHeaderNullString(CSVFormat format) {
    String[] header = format.header
    header != null && header.length == 0 ? format.nullString : null
  }

  private static CSVFormat parserFormat(CSVFormat format) {
    if (inferredHeaderNullString(format) == null) {
      return format
    }
    CSVFormat.Builder.create(format)
        .setHeader((String[]) null)
        .setSkipHeaderRecord(false)
        .build()
  }

  /**
   * Wraps a byte input with caller-ownership protection and matching BOM removal.
   *
   * @param input source byte stream
   * @param charset charset used to decode the source
   * @param callerOwned whether closing the returned stream must preserve the source stream
   * @return the stream to pass to Commons CSV
   * @throws IOException if the BOM wrapper cannot be created
   */
  private static InputStream byteInput(InputStream input, Charset charset, boolean callerOwned) throws IOException {
    InputStream source = callerOwned ? CloseShieldInputStream.wrap(input) : input
    ByteOrderMark mark = byteOrderMark(charset)
    if (mark == null) {
      return source
    }
    BOMInputStream.builder()
        .setInputStream(source)
        .setByteOrderMarks(mark)
        .get()
  }

  /**
   * Resolves the single BOM that may be stripped for a configured charset.
   *
   * @param charset configured source charset
   * @return the matching BOM, or null when the decoder must receive the original bytes
   */
  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  private static ByteOrderMark byteOrderMark(Charset charset) {
    if (charset == StandardCharsets.UTF_8) {
      return ByteOrderMark.UTF_8
    }
    if (charset == StandardCharsets.UTF_16LE) {
      return ByteOrderMark.UTF_16LE
    }
    if (charset == StandardCharsets.UTF_16BE) {
      return ByteOrderMark.UTF_16BE
    }
    if (charset == UTF_32LE) {
      return ByteOrderMark.UTF_32LE
    }
    if (charset == UTF_32BE) {
      return ByteOrderMark.UTF_32BE
    }
    null
  }

  // ──────────────────────────────────────────────────────────────
  // ReadBuilder
  // ──────────────────────────────────────────────────────────────

  /**
   * Fluent builder for reading CSV data into a Matrix.
   *
   * <p>Obtained via {@link CsvReader#read()}. Chain format configuration methods
   * and finish with a terminal {@code from*()} method to trigger I/O.</p>
   *
   * <pre>
   * Matrix m = CsvReader.read()
   *     .delimiter(';')
   *     .trim(true)
   *     .from(file)
   * </pre>
   */
  @CompileStatic
  static class ReadBuilder {
    private char delimiter = ',' as char
    private Character quoteCharacter = '"' as Character
    private Character escapeCharacter = null
    private Character commentMarker = null
    private boolean trimValue = true
    private boolean ignoreEmptyLines = true
    private boolean ignoreSurroundingSpaces = true
    private String nullString = null
    private String recordSeparator = '\n'
    private QuoteMode quoteMode = null
    private boolean allowMissingColumnNames = false
    private boolean firstRowAsHeader = true
    private List<String> header = null
    private Charset charset = StandardCharsets.UTF_8
    private String matrixName = null
    private List<Class> types = null
    private String dateTimeFormat = null
    private NumberFormat numberFormat = null
    private DuplicateHeaderMode duplicateHeaderMode = DuplicateHeaderMode.ALLOW_EMPTY

    // ── Format configuration methods ──────────────────────────

    /** Sets the field delimiter character. */
    ReadBuilder delimiter(char c) { delimiter = c; this }

    /** Sets the field delimiter as a single-character string. */
    ReadBuilder delimiter(String s) {
      if (s == null || s.length() != 1) {
        throw new IllegalArgumentException("Delimiter must be a single character string, got: ${s}")
      }
      delimiter = s.charAt(0)
      this
    }

    /** Sets the quote character for enclosing fields. */
    ReadBuilder quoteCharacter(Character c) { quoteCharacter = c; this }

    /** Sets the quote character as a single-character string, or {@code null}/empty to disable. */
    ReadBuilder quoteCharacter(String s) {
      if (s != null && s.length() > 1) {
        throw new IllegalArgumentException("Quote character must be a single character string, got: ${s}")
      }
      quoteCharacter = (s == null || s.isEmpty()) ? null : s.charAt(0)
      this
    }

    /** Sets the escape character. */
    ReadBuilder escapeCharacter(Character c) { escapeCharacter = c; this }

    /** Sets the escape character as a single-character string, or {@code null}/empty to disable. */
    ReadBuilder escapeCharacter(String s) {
      if (s != null && s.length() > 1) {
        throw new IllegalArgumentException("Escape character must be a single character string, got: ${s}")
      }
      escapeCharacter = (s == null || s.isEmpty()) ? null : s.charAt(0)
      this
    }

    /** Sets the comment marker character. */
    ReadBuilder commentMarker(Character c) { commentMarker = c; this }

    /** Sets the comment marker as a single-character string, or {@code null}/empty to disable. */
    ReadBuilder commentMarker(String s) {
      if (s != null && s.length() > 1) {
        throw new IllegalArgumentException("Comment marker must be a single character string, got: ${s}")
      }
      commentMarker = (s == null || s.isEmpty()) ? null : s.charAt(0)
      this
    }

    /** Sets whether to trim whitespace from values. */
    ReadBuilder trim(boolean b) { trimValue = b; this }

    /** Sets whether to ignore empty lines. */
    ReadBuilder ignoreEmptyLines(boolean b) { ignoreEmptyLines = b; this }

    /** Sets whether to ignore spaces around quoted values. */
    ReadBuilder ignoreSurroundingSpaces(boolean b) { ignoreSurroundingSpaces = b; this }

    /** Sets the string to interpret as null when reading. */
    ReadBuilder nullString(String s) { nullString = s; this }

    /**
     * Retains the legacy read-side record separator setting without changing parsing.
     *
     * @param s ignored record separator
     * @return this builder
     * @deprecated Commons CSV record separators affect output only; use
     * {@link CsvWriteOptions#recordSeparator(String)} when writing
     */
    @Deprecated
    ReadBuilder recordSeparator(String s) { recordSeparator = s; this }

    // ── Reader-specific methods ───────────────────────────────

    /** Sets whether the first row contains column names (default: true). */
    ReadBuilder firstRowAsHeader(boolean b) { firstRowAsHeader = b; this }

    /**
     * Sets an explicit header; also sets firstRowAsHeader to false.
     *
     * @param names the column names to use
     * @return this builder
     */
    ReadBuilder header(List<String> names) {
      header = names
      if (names != null) {
        firstRowAsHeader = false
      }
      this
    }

    /** Sets the character encoding (default: UTF-8). */
    ReadBuilder charset(Charset cs) { charset = cs; this }

    /** Sets the character encoding by name (default: UTF-8). */
    ReadBuilder charset(String cs) { charset = Charset.forName(cs); this }

    /**
     * Sets the name for the resulting Matrix.
     * Defaults to the source-derived name for file and URL reads, or {@code matrix}
     * for stream, reader, and string reads.
     */
    ReadBuilder matrixName(String name) { matrixName = name; this }

    /** Sets how duplicate header names are handled when header parsing is enabled. */
    ReadBuilder duplicateHeaderMode(DuplicateHeaderMode mode) {
      duplicateHeaderMode = mode == null ? DuplicateHeaderMode.ALLOW_EMPTY : mode
      this
    }

    /** Sets the duplicate header mode from its enum name. */
    ReadBuilder duplicateHeaderMode(String mode) {
      duplicateHeaderMode(CsvOptionUtil.duplicateHeaderMode(mode))
    }

    // ── Type conversion methods ─────────────────────────────

    /**
     * Sets column types by position for automatic conversion after reading.
     *
     * <pre>
     * Matrix m = CsvReader.read()
     *     .types([Integer, String, BigDecimal])
     *     .from(file)
     * </pre>
     *
     * @param types list of Class objects, one per column
     * @return this builder
     */
    ReadBuilder types(List<Class> types) { this.types = types; this }

    /**
     * Sets column types by position for automatic conversion after reading (varargs).
     *
     * <pre>
     * Matrix m = CsvReader.read()
     *     .types(Integer, String, BigDecimal)
     *     .from(file)
     * </pre>
     *
     * @param types Class objects, one per column
     * @return this builder
     */
    ReadBuilder types(Class... types) { this.types = types.toList(); this }

    /**
     * Sets both header and types from a map of column name to type.
     * The map must preserve insertion order (e.g. a Groovy map literal
     * or {@code LinkedHashMap}) so that column names and types stay aligned.
     *
     * <pre>
     * Matrix m = CsvReader.read()
     *     .columns(id: Integer, name: String, amount: BigDecimal)
     *     .from(file)
     * </pre>
     *
     * @param columnMap ordered map of column names to their types
     * @return this builder
     */
    ReadBuilder columns(Map<String, Class> columnMap) {
      header = columnMap.keySet().toList()
      types = columnMap.values().toList()
      firstRowAsHeader = false
      this
    }

    /**
     * Sets the date/time parse pattern for type conversion (e.g. {@code 'yyyy-MM-dd'}).
     *
     * @param pattern date/time format pattern
     * @return this builder
     */
    ReadBuilder dateTimeFormat(String pattern) { dateTimeFormat = pattern; this }

    /**
     * Sets the NumberFormat for locale-aware number parsing during type conversion.
     *
     * @param nf the NumberFormat instance
     * @return this builder
     */
    ReadBuilder numberFormat(NumberFormat nf) { numberFormat = nf; this }

    // ── Preset methods ────────────────────────────────────────

    /** Configures Excel-compatible CSV format, including missing-header tolerance. */
    ReadBuilder excel() { applyFormat(CsvFormat.EXCEL) }

    /** Configures tab-delimited format (TSV). */
    ReadBuilder tsv() { applyFormat(CsvFormat.TDF) }

    /** Configures RFC 4180 compliant format with CRLF record separators. */
    ReadBuilder rfc4180() { applyFormat(CsvFormat.RFC4180) }

    // ── Terminal operations ───────────────────────────────────

    /**
     * Reads CSV data from a File.
     *
     * @param file the file to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the file fails
     */
    Matrix from(File file) throws IOException {
      CSVFormat apacheFormat = buildCSVFormat()
      String name = matrixName ? matrixName : tableName(file)
      try (InputStream source = new FileInputStream(file)) {
        InputStream input = byteInput(source, charset, false)
        try (CSVParser parser = CSVParser.parse(input, charset, parserFormat(apacheFormat))) {
          convertIfNeeded(parse(name, parser, firstRowAsHeader, apacheFormat))
        }
      }
    }

    /**
     * Reads CSV data from a Path.
     *
     * @param path the path to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the file fails
     */
    Matrix from(Path path) throws IOException {
      from(path.toFile())
    }

    /**
     * Reads CSV data from a URL.
     *
     * @param url the URL to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the URL fails
     */
    Matrix from(URL url) throws IOException {
      CSVFormat apacheFormat = buildCSVFormat()
      String name = matrixName ? matrixName : tableName(url)
      try (InputStream source = url.openStream()) {
        InputStream input = byteInput(source, charset, false)
        try (CSVParser parser = CSVParser.parse(input, charset, parserFormat(apacheFormat))) {
          convertIfNeeded(parse(name, parser, firstRowAsHeader, apacheFormat))
        }
      }
    }

    /**
     * Reads CSV data from an InputStream. A matching UTF-8, UTF-16LE, UTF-16BE, UTF-32LE,
     * or UTF-32BE byte-order mark is stripped. The caller is responsible for closing the InputStream.
     *
     * @param is the InputStream to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the stream fails
     */
    Matrix from(InputStream is) throws IOException {
      CSVFormat apacheFormat = buildCSVFormat()
      InputStream input = byteInput(is, charset, true)
      try (CSVParser parser = CSVParser.parse(input, charset, parserFormat(apacheFormat))) {
        convertIfNeeded(parse(matrixName ?: DEFAULT_MATRIX_NAME, parser, firstRowAsHeader, apacheFormat))
      }
    }

    /**
     * Reads CSV data from a Reader. The caller is responsible for closing the Reader.
     *
     * @param reader the Reader to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading from the Reader fails
     */
    Matrix from(Reader reader) throws IOException {
      CSVFormat apacheFormat = buildCSVFormat()
      try (CSVParser parser = CSVParser.parse(CloseShieldReader.wrap(reader), parserFormat(apacheFormat))) {
        convertIfNeeded(parse(matrixName ?: DEFAULT_MATRIX_NAME, parser, firstRowAsHeader, apacheFormat))
      }
    }

    /**
     * Reads CSV data from a String containing CSV content.
     *
     * @param csvContent the CSV string to parse
     * @return Matrix containing the imported data
     * @throws IOException if parsing fails
     */
    Matrix fromString(String csvContent) throws IOException {
      try (Reader reader = new StringReader(csvContent)) {
        from(reader)
      }
    }

    /**
     * Reads CSV data from a URL specified as a String.
     *
     * @param url the URL string to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the URL fails
     */
    Matrix fromUrl(String url) throws IOException {
      try {
        from(new URI(url).toURL())
      } catch (URISyntaxException e) {
        throw new IOException("Invalid URL: ${url}", e)
      }
    }

    /**
     * Reads CSV data from a file path specified as a String.
     *
     * @param filePath the file path to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the file fails
     */
    Matrix fromFile(String filePath) throws IOException {
      from(new File(filePath))
    }

    private Matrix convertIfNeeded(Matrix matrix) {
      if (types != null) {
        matrix.convert(types, dateTimeFormat, numberFormat)
      } else {
        matrix
      }
    }

    private CSVFormat buildCSVFormat() {
      CsvFormat format = CsvFormat.builder()
          .delimiter(delimiter)
          .quoteCharacter(quoteCharacter)
          .escapeCharacter(escapeCharacter)
          .commentMarker(commentMarker)
          .trim(trimValue)
          .ignoreEmptyLines(ignoreEmptyLines)
          .ignoreSurroundingSpaces(ignoreSurroundingSpaces)
          .nullString(nullString)
          .recordSeparator(recordSeparator)
          .quoteMode(quoteMode)
          .allowMissingColumnNames(allowMissingColumnNames)
          .build()
      CSVFormat.Builder builder = CSVFormat.Builder.create(format.toCSVFormat())
          .setDuplicateHeaderMode(duplicateHeaderMode)
      if (header != null) {
        builder.setHeader(header as String[])
        builder.setSkipHeaderRecord(false)
      } else if (firstRowAsHeader) {
        builder.setHeader()
        builder.setSkipHeaderRecord(true)
      }
      builder.build()
    }

    private ReadBuilder applyFormat(CsvFormat format) {
      delimiter = format.delimiter
      quoteCharacter = format.quoteCharacter
      escapeCharacter = format.escapeCharacter
      commentMarker = format.commentMarker
      trimValue = format.trim
      ignoreEmptyLines = format.ignoreEmptyLines
      ignoreSurroundingSpaces = format.ignoreSurroundingSpaces
      nullString = format.nullString
      recordSeparator = format.recordSeparator
      quoteMode = format.quoteMode
      allowMissingColumnNames = format.allowMissingColumnNames
      this
    }
  }
}
