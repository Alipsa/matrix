package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.DuplicateHeaderMode
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
    Map r = parseMap(format)
    try (CSVParser parser = CSVParser.parse(url, r.charset as Charset, r.apacheFormat as CSVFormat)) {
      convertIfNeeded(parse(tableName(url), parser, r.firstRowAsHeader as boolean),
          r.types as List<Class>, r.dateTimeFormat as String, r.numberFormat as NumberFormat)
    }
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
      read(format, new URI(url).toURL())
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
    Map r = parseMap(format)
    try (CSVParser parser = CSVParser.parse(CloseShieldInputStream.wrap(is), r.charset as Charset, r.apacheFormat as CSVFormat)) {
      convertIfNeeded(parse(r.tableName as String, parser, r.firstRowAsHeader as boolean),
          r.types as List<Class>, r.dateTimeFormat as String, r.numberFormat as NumberFormat)
    }
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
    Map r = parseMap(format)
    try (CSVParser parser = CSVParser.parse(CloseShieldReader.wrap(reader), r.apacheFormat as CSVFormat)) {
      convertIfNeeded(parse(r.tableName as String, parser, r.firstRowAsHeader as boolean),
          r.types as List<Class>, r.dateTimeFormat as String, r.numberFormat as NumberFormat)
    }
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
    Map r = parseMap(format)
    try (CSVParser parser = CSVParser.parse(file, r.charset as Charset, r.apacheFormat as CSVFormat)) {
      convertIfNeeded(parse(tableName(file), parser, r.firstRowAsHeader as boolean),
          r.types as List<Class>, r.dateTimeFormat as String, r.numberFormat as NumberFormat)
    }
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
    read(format, path.toFile())
  }

  private static Map parseMap(Map format) {
    format = convertKeysToOptions(format)
    Map r = [:]
    r.charset = format.getOrDefault(CsvOption.Charset, StandardCharsets.UTF_8) as Charset
    r.tableName = format.getOrDefault(CsvOption.TableName, '')
    r.apacheFormat = createFormatBuilder(format).build()
    boolean firstRowAsHeader
    if (format.containsKey(CsvOption.Header)) {
      firstRowAsHeader = false
    } else {
      firstRowAsHeader = format.getOrDefault(CsvOption.FirstRowAsHeader, true)
    }
    r.firstRowAsHeader = firstRowAsHeader
    r.types = format.get(CsvOption.Types) as List<Class>
    r.dateTimeFormat = format.get(CsvOption.DateTimeFormat) as String
    r.numberFormat = format.get(CsvOption.NumberFormat) as NumberFormat
    r
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
    try (CSVParser parser = CSVParser.parse(CloseShieldInputStream.wrap(is), charset, format)) {
      parse(matrixName, parser, firstRowAsHeader)
    }
  }

  /**
   * Read a CSV file from a Reader using Apache Commons CSV format.
   * The caller is responsible for closing the Reader.
   *
   * @param reader Reader to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param matrixName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@code CsvReader.read().from(reader)} instead
   */
  @Deprecated
  static Matrix read(Reader reader, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (CSVParser parser = CSVParser.parse(CloseShieldReader.wrap(reader), format)) {
      parse(matrixName, parser, firstRowAsHeader)
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
    try (CSVParser parser = CSVParser.parse(url, charset, format)) {
      parse(tableName(url), parser, firstRowAsHeader)
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
    try (CSVParser parser = CSVParser.parse(file, charset, format)) {
      parse(tableName(file), parser, firstRowAsHeader)
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
   * @return Matrix containing the parsed data with all values as Strings
   * @throws IllegalArgumentException if rows have inconsistent column counts
   */
  private static Matrix parse(String matrixName, CSVParser parser, boolean firstRowAsHeader) {
    List<List<String>> rows = parser.records*.toList()

    // Handle empty CSV file
    if (rows.isEmpty()) {
      return Matrix.builder()
          .matrixName(matrixName ?: 'matrix')
          .build()
    }

    int rowCount = 0
    int ncols = rows[0].size()
    for (List<String> row in rows) {
      if (row.size() != ncols) {
        throw new IllegalArgumentException("This csv file does not have an equal number of columns on each row, error on row $rowCount: expected $ncols but was ${row.size()}")
      }
      rowCount++
    }
    List<String> headerRow = []
    if (parser.headerNames != null && parser.headerNames.size() > 0) {
      headerRow = parser.headerNames
    } else if (firstRowAsHeader) {
      headerRow = rows.remove(0)
    } else {
      for (int i = 0; i < ncols; i++) {
        headerRow << "c" + i
      }
    }
    def types = [String] * ncols
    Matrix.builder()
        .matrixName(matrixName)
        .columnNames(headerRow)
        .rows(rows)
        .types(types)
        .build()
  }

  /**
   * Extracts a table name from a URL by removing the path and file extension.
   *
   * @param url URL to extract the name from
   * @return file name without extension, or the path if no file name is present
   */
  @groovy.transform.PackageScope
  static String tableName(URL url) {
    def name = url.getFile() == null ? url.getPath() : url.getFile()
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1, name.length())
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
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
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    name
  }

  /**
   * Builds a {@link CSVFormat.Builder} from Map-based format options.
   * Used internally by the Map-based API to support Header and DuplicateHeaderMode options
   * that interact with the CSVParser.
   */
  private static CSVFormat.Builder createFormatBuilder(Map format) {
    CSVFormat.Builder f = CSVFormat.Builder.create()

    // Boolean format options
    setTrim(f, format)
    setIgnoreEmptyLines(f, format)
    setIgnoreSurroundingSpaces(f, format)

    // Character format options
    setDelimiter(f, format)
    setQuote(f, format)
    setCommentMarker(f, format)
    setEscape(f, format)

    // String format options
    setNullString(f, format)
    setRecordSeparator(f, format)

    // Header format option (special handling)
    if (format.containsKey(CsvOption.Header)) {
      def val = format.get(CsvOption.Header)
      if (val instanceof String[]) {
        f.setHeader(val as String[])
        format.put(CsvOption.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof List) {
        f.setHeader(val as String[])
        format.put(CsvOption.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof String) {
        f.setHeader([val] as String[])
        format.put(CsvOption.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else {
        throw new IllegalArgumentException("The value for Header must be a List or array of Strings but was $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    // DuplicateHeaderMode (accepts String name or DuplicateHeaderMode enum value)
    if (format.containsKey(CsvOption.DuplicateHeaderMode)) {
      def val = format.get(CsvOption.DuplicateHeaderMode)
      if (val instanceof String) {
        f.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(val))
      } else if (val instanceof DuplicateHeaderMode) {
        f.setDuplicateHeaderMode(val as DuplicateHeaderMode)
      } else {
        throw new IllegalArgumentException("The value for DuplicateHeaderMode must be a String or DuplicateHeaderMode enum (e.g. 'ALLOW_ALL', 'ALLOW_EMPTY', 'DISALLOW') but was $val")
      }
    } else {
      f.setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
    }

    // FirstRowAsHeader (special boolean with header interaction)
    if (format.containsKey(CsvOption.FirstRowAsHeader)) {
      def val = format.get(CsvOption.FirstRowAsHeader)
      if (val instanceof Boolean) {
        if (!format.containsKey(CsvOption.Header)) {
          if (val) {
            f.setHeader()
            f.setSkipHeaderRecord(true)
          } else {
            f.setHeader((String) null)
            f.setSkipHeaderRecord(false)
          }
        }
      } else {
        throw new IllegalArgumentException("The value for FirstRowAsHeader must be a Boolean but was ${val.class} = $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    f
  }

  // Boolean format options
  private static void setTrim(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.Trim)) {
      def val = format.get(CsvOption.Trim)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for Trim must be a Boolean but was $val")
      }
      builder.setTrim(val as Boolean)
    } else {
      builder.setTrim(true)
    }
  }

  private static void setIgnoreEmptyLines(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.IgnoreEmptyLines)) {
      def val = format.get(CsvOption.IgnoreEmptyLines)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for IgnoreEmptyLines must be a Boolean but was $val")
      }
      builder.setIgnoreEmptyLines(val as Boolean)
    } else {
      builder.setIgnoreEmptyLines(true)
    }
  }

  private static void setIgnoreSurroundingSpaces(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.IgnoreSurroundingSpaces)) {
      def val = format.get(CsvOption.IgnoreSurroundingSpaces)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for IgnoreSurroundingSpaces must be a Boolean but was $val")
      }
      builder.setIgnoreSurroundingSpaces(val as Boolean)
    } else {
      builder.setIgnoreSurroundingSpaces(true)
    }
  }

  // Character format options
  private static void setDelimiter(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.Delimiter)) {
      def val = format.get(CsvOption.Delimiter)
      if (val instanceof String || val instanceof Character) {
        builder.setDelimiter(String.valueOf(val))
      } else {
        throw new IllegalArgumentException("The value for Delimiter must be a String or Character but was $val")
      }
    } else {
      builder.setDelimiter(',')
    }
  }

  private static void setQuote(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.Quote)) {
      def val = format.get(CsvOption.Quote)
      if (val instanceof String) {
        builder.setQuote(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        builder.setQuote(val as Character)
      } else {
        throw new IllegalArgumentException("The value for Quote must be a String or Character but was $val")
      }
    } else {
      builder.setQuote('"' as Character)
    }
  }

  private static void setCommentMarker(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.CommentMarker)) {
      def val = format.get(CsvOption.CommentMarker)
      if (val instanceof String) {
        builder.setCommentMarker(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        builder.setCommentMarker(val as Character)
      } else {
        throw new IllegalArgumentException("The value for CommentMarker must be a String or Character but was $val")
      }
    } else {
      builder.setCommentMarker(null)
    }
  }

  private static void setEscape(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.Escape)) {
      def val = format.get(CsvOption.Escape)
      if (val instanceof String) {
        builder.setEscape(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        builder.setEscape(val as Character)
      } else {
        throw new IllegalArgumentException("The value for Escape must be a String or Character but was $val")
      }
    } else {
      builder.setEscape(null)
    }
  }

  // String format options
  private static void setNullString(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.NullString)) {
      def val = format.get(CsvOption.NullString)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for NullString must be a String but was $val")
      }
      builder.setNullString(val as String)
    } else {
      builder.setNullString(null)
    }
  }

  private static void setRecordSeparator(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvOption.RecordSeparator)) {
      def val = format.get(CsvOption.RecordSeparator)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for RecordSeparator must be a String but was $val")
      }
      builder.setRecordSeparator(val as String)
    } else {
      builder.setRecordSeparator('\n')
    }
  }

  /**
   * Converts map keys to {@link CsvOption} enum values.
   * Accepts String keys (for Groovy named arguments), {@link CsvOption} keys,
   * and deprecated {@link CsvImporter.Format} keys (converted by name).
   */
  private static Map convertKeysToOptions(Map map) {
    Map m = [:]
    map.each { k, v ->
      if (k instanceof CsvOption) {
        m.put(k, v)
      } else if (k instanceof CsvImporter.Format) {
        // Backward compatibility: convert deprecated CsvImporter.Format to CsvOption by name
        m.put(CsvOption.valueOf(k.name()), v)
      } else {
        String key = String.valueOf(k)
        CsvOption option = CsvOption.values().find { it.name().equalsIgnoreCase(key) }
        if (option == null) {
          throw new IllegalArgumentException("Unknown CsvOption: '${key}'")
        }
        m.put(option, v)
      }
    }
    m
  }

  /**
   * Applies type conversion to a Matrix if types are specified.
   * Used by the Map-based API methods.
   */
  private static Matrix convertIfNeeded(Matrix matrix, List<Class> types, String dtf, NumberFormat nf) {
    if (types != null) {
      matrix.convert(types, dtf, nf)
    } else {
      matrix
    }
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
    private char _delimiter = ',' as char
    private Character _quoteCharacter = '"' as Character
    private Character _escapeCharacter = null
    private Character _commentMarker = null
    private boolean _trim = true
    private boolean _ignoreEmptyLines = true
    private boolean _ignoreSurroundingSpaces = true
    private String _nullString = null
    private String _recordSeparator = '\n'
    private boolean _firstRowAsHeader = true
    private List<String> _header = null
    private Charset _charset = StandardCharsets.UTF_8
    private String _matrixName = ''
    private List<Class> _types = null
    private String _dateTimeFormat = null
    private NumberFormat _numberFormat = null

    // ── Format configuration methods ──────────────────────────

    /** Sets the field delimiter character. */
    ReadBuilder delimiter(char c) { _delimiter = c; this }

    /** Sets the field delimiter as a single-character string. */
    ReadBuilder delimiter(String s) {
      if (s == null || s.isEmpty()) {
        throw new IllegalArgumentException("Delimiter string must not be null or empty")
      }
      _delimiter = s.charAt(0)
      this
    }

    /** Sets the quote character for enclosing fields. */
    ReadBuilder quoteCharacter(Character c) { _quoteCharacter = c; this }

    /** Sets the quote character as a single-character string, or {@code null}/empty to disable. */
    ReadBuilder quoteCharacter(String s) { _quoteCharacter = (s == null || s.isEmpty()) ? null : s.charAt(0); this }

    /** Sets the escape character. */
    ReadBuilder escapeCharacter(Character c) { _escapeCharacter = c; this }

    /** Sets the escape character as a single-character string, or {@code null}/empty to disable. */
    ReadBuilder escapeCharacter(String s) { _escapeCharacter = (s == null || s.isEmpty()) ? null : s.charAt(0); this }

    /** Sets the comment marker character. */
    ReadBuilder commentMarker(Character c) { _commentMarker = c; this }

    /** Sets the comment marker as a single-character string, or {@code null}/empty to disable. */
    ReadBuilder commentMarker(String s) { _commentMarker = (s == null || s.isEmpty()) ? null : s.charAt(0); this }

    /** Sets whether to trim whitespace from values. */
    ReadBuilder trim(boolean b) { _trim = b; this }

    /** Sets whether to ignore empty lines. */
    ReadBuilder ignoreEmptyLines(boolean b) { _ignoreEmptyLines = b; this }

    /** Sets whether to ignore spaces around quoted values. */
    ReadBuilder ignoreSurroundingSpaces(boolean b) { _ignoreSurroundingSpaces = b; this }

    /** Sets the string to interpret as null when reading. */
    ReadBuilder nullString(String s) { _nullString = s; this }

    /** Sets the record separator string. */
    ReadBuilder recordSeparator(String s) { _recordSeparator = s; this }

    // ── Reader-specific methods ───────────────────────────────

    /** Sets whether the first row contains column names (default: true). */
    ReadBuilder firstRowAsHeader(boolean b) { _firstRowAsHeader = b; this }

    /**
     * Sets an explicit header; also sets firstRowAsHeader to false.
     *
     * @param names the column names to use
     * @return this builder
     */
    ReadBuilder header(List<String> names) {
      _header = names
      _firstRowAsHeader = false
      this
    }

    /** Sets the character encoding (default: UTF-8). */
    ReadBuilder charset(Charset cs) { _charset = cs; this }

    /** Sets the name for the resulting Matrix (default: empty string). */
    ReadBuilder matrixName(String name) { _matrixName = name; this }

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
    ReadBuilder types(List<Class> types) { _types = types; this }

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
    ReadBuilder types(Class... types) { _types = types.toList(); this }

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
      _header = columnMap.keySet().toList()
      _types = columnMap.values().toList()
      _firstRowAsHeader = false
      this
    }

    /**
     * Sets the date/time parse pattern for type conversion (e.g. {@code 'yyyy-MM-dd'}).
     *
     * @param pattern date/time format pattern
     * @return this builder
     */
    ReadBuilder dateTimeFormat(String pattern) { _dateTimeFormat = pattern; this }

    /**
     * Sets the NumberFormat for locale-aware number parsing during type conversion.
     *
     * @param nf the NumberFormat instance
     * @return this builder
     */
    ReadBuilder numberFormat(NumberFormat nf) { _numberFormat = nf; this }

    // ── Preset methods ────────────────────────────────────────

    /** Configures Excel-compatible CSV format with CRLF record separators. */
    ReadBuilder excel() { _recordSeparator = '\r\n'; this }

    /** Configures tab-delimited format (TSV). */
    ReadBuilder tsv() { _delimiter = '\t' as char; this }

    /** Configures RFC 4180 compliant format with CRLF record separators. */
    ReadBuilder rfc4180() { _recordSeparator = '\r\n'; this }

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
      try (CSVParser parser = CSVParser.parse(file, _charset, apacheFormat)) {
        convertIfNeeded(parse(tableName(file), parser, _firstRowAsHeader))
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
      try (CSVParser parser = CSVParser.parse(url, _charset, apacheFormat)) {
        convertIfNeeded(parse(tableName(url), parser, _firstRowAsHeader))
      }
    }

    /**
     * Reads CSV data from an InputStream. The caller is responsible for closing the InputStream.
     *
     * @param is the InputStream to read from
     * @return Matrix containing the imported data
     * @throws IOException if reading the stream fails
     */
    Matrix from(InputStream is) throws IOException {
      CSVFormat apacheFormat = buildCSVFormat()
      try (CSVParser parser = CSVParser.parse(CloseShieldInputStream.wrap(is), _charset, apacheFormat)) {
        convertIfNeeded(parse(_matrixName, parser, _firstRowAsHeader))
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
      try (CSVParser parser = CSVParser.parse(CloseShieldReader.wrap(reader), apacheFormat)) {
        convertIfNeeded(parse(_matrixName, parser, _firstRowAsHeader))
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
      if (_types != null) {
        matrix.convert(_types, _dateTimeFormat, _numberFormat)
      } else {
        matrix
      }
    }

    private CSVFormat buildCSVFormat() {
      CsvFormat format = CsvFormat.builder()
          .delimiter(_delimiter)
          .quoteCharacter(_quoteCharacter)
          .escapeCharacter(_escapeCharacter)
          .commentMarker(_commentMarker)
          .trim(_trim)
          .ignoreEmptyLines(_ignoreEmptyLines)
          .ignoreSurroundingSpaces(_ignoreSurroundingSpaces)
          .nullString(_nullString)
          .recordSeparator(_recordSeparator)
          .build()
      CSVFormat csvFormat = format.toCSVFormat()
      if (_header != null) {
        csvFormat = CSVFormat.Builder.create(csvFormat)
            .setHeader(_header as String[])
            .setSkipHeaderRecord(false)
            .build()
      }
      csvFormat
    }
  }
}
