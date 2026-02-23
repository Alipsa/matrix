package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.DuplicateHeaderMode
import org.apache.commons.io.input.ReaderInputStream
import se.alipsa.matrix.core.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Reads CSV files into Matrix objects using Apache Commons CSV.
 *
 * <p>Provides flexible CSV parsing with support for multiple input sources
 * (File, Path, URL, InputStream, Reader, String content) and configurable
 * format options through {@link CsvFormat} or the {@link CsvOption} enum.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * // Simple read with default format
 * Matrix m = CsvReader.read(new File("data.csv"))
 *
 * // Read from String content
 * String csvContent = "name,age\nAlice,30\nBob,25"
 * Matrix m = CsvReader.readString(csvContent)
 *
 * // Custom format with CsvFormat builder
 * CsvFormat format = CsvFormat.builder()
 *     .delimiter(';' as char)
 *     .quoteCharacter('"' as Character)
 *     .build()
 * Matrix m = CsvReader.read(new File("data.csv"), format)
 *
 * // Custom format with named arguments
 * Matrix m = CsvReader.read(Delimiter: ';', Quote: '"', new File("data.csv"))
 * </pre>
 *
 * <h3>Format Options</h3>
 * <p>Use the {@link CsvFormat} builder to customize parsing behavior:</p>
 * <ul>
 *   <li><b>delimiter</b> - Field separator (default: comma)</li>
 *   <li><b>quoteCharacter</b> - Quote character (default: double quote)</li>
 *   <li><b>escapeCharacter</b> - Escape character for special characters</li>
 *   <li><b>trim</b> - Trim whitespace from values (default: true)</li>
 *   <li><b>commentMarker</b> - Character marking comment lines (default: null)</li>
 *   <li><b>nullString</b> - String to convert to null values (default: null)</li>
 * </ul>
 *
 * @see CsvWriter
 * @see CsvFormat
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
      parse(tableName(url), parser, r.firstRowAsHeader as boolean)
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
    read(format, new URI(url).toURL())
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
    try (CSVParser parser = CSVParser.parse(is, r.charset as Charset, r.apacheFormat as CSVFormat)) {
      parse(r.tableName as String, parser, r.firstRowAsHeader as boolean)
    }
  }

  /**
   * Read a CSV file from a Reader with custom format options.
   *
   * @param format Map of format options using {@link CsvOption} enum keys
   * @param reader Reader to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, Reader reader) throws IOException {
    Map r = parseMap(format)
    Charset charset = r.charset as Charset
    try (ReaderInputStream readerInputStream = ReaderInputStream.builder()
        .setCharset(charset).setReader(reader).get()) {
      try (CSVParser parser = CSVParser.parse(readerInputStream, charset, r.apacheFormat as CSVFormat)) {
        parse(r.tableName as String, parser, r.firstRowAsHeader as boolean)
      }
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
      parse(tableName(file), parser, r.firstRowAsHeader as boolean)
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
    r
  }

  // ──────────────────────────────────────────────────────────────
  // CsvFormat-based API (new, preferred)
  // ──────────────────────────────────────────────────────────────

  /**
   * Read a CSV file from an InputStream using a {@link CsvFormat}.
   *
   * @param is InputStream to read the CSV data from
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param matrixName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading the stream fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix read(InputStream is, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (CSVParser parser = CSVParser.parse(is, charset, format.toCSVFormat())) {
      parse(matrixName, parser, firstRowAsHeader)
    }
  }

  /**
   * Read a CSV file from a Reader using a {@link CsvFormat}.
   *
   * @param reader Reader to read the CSV data from
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param matrixName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix read(Reader reader, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (ReaderInputStream readerInputStream = ReaderInputStream.builder()
        .setCharset(charset).setReader(reader).get()) {
      read(readerInputStream, format, firstRowAsHeader, charset, matrixName)
    }
  }

  /**
   * Read CSV content from a String using a {@link CsvFormat}.
   *
   * @param csvContent String containing CSV data
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @return Matrix containing the imported data
   * @throws IOException if parsing fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix readString(String csvContent, CsvFormat format, boolean firstRowAsHeader = true) throws IOException {
    read(new StringReader(csvContent), format, firstRowAsHeader, StandardCharsets.UTF_8, '')
  }

  /**
   * Read a CSV file from a URL using a {@link CsvFormat}.
   *
   * @param url URL pointing to the CSV file to read
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from URL
   * @throws IOException if reading the URL fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix read(URL url, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (CSVParser parser = CSVParser.parse(url, charset, format.toCSVFormat())) {
      parse(tableName(url), parser, firstRowAsHeader)
    }
  }

  /**
   * Read a CSV file from a URL string using a {@link CsvFormat}.
   *
   * @param url String URL pointing to the CSV file to read
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from URL
   * @throws IOException if reading the URL fails or URL is invalid
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix readUrl(String url, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(new URI(url).toURL(), format, firstRowAsHeader, charset)
  }

  /**
   * Read a CSV file from a Path using a {@link CsvFormat}.
   *
   * @param path Path to the CSV file to read
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from file name
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix read(Path path, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(path.toFile(), format, firstRowAsHeader, charset)
  }

  /**
   * Read a CSV file from a File using a {@link CsvFormat}.
   *
   * @param file File to read the CSV data from
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from file name
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix read(File file, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (CSVParser parser = CSVParser.parse(file, charset, format.toCSVFormat())) {
      parse(tableName(file), parser, firstRowAsHeader)
    }
  }

  /**
   * Read a CSV file from a file path string using a {@link CsvFormat}.
   *
   * @param filePath path to the CSV file as a String
   * @param format CsvFormat configuration (default: CsvFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with default settings
   * @throws IOException if reading the file fails or file not found
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix readFile(String filePath, CsvFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(new File(filePath), format, firstRowAsHeader, charset)
  }

  // ──────────────────────────────────────────────────────────────
  // CSVFormat-based API (deprecated — use CsvFormat overloads)
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
   * @deprecated Use {@link #read(InputStream, CsvFormat, boolean, Charset, String)} instead
   */
  @Deprecated
  static Matrix read(InputStream is, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (CSVParser parser = CSVParser.parse(is, charset, format)) {
      parse(matrixName, parser, firstRowAsHeader)
    }
  }

  /**
   * Read a CSV file from a Reader using Apache Commons CSV format.
   *
   * @param reader Reader to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param matrixName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link #read(Reader, CsvFormat, boolean, Charset, String)} instead
   */
  @Deprecated
  static Matrix read(Reader reader, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (ReaderInputStream readerInputStream = ReaderInputStream.builder()
        .setCharset(charset).setReader(reader).get()) {
      read(readerInputStream, format, firstRowAsHeader, charset, matrixName)
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
   * @deprecated Use {@link #readString(String, CsvFormat, boolean)} instead
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
   * @deprecated Use {@link #read(URL, CsvFormat, boolean, Charset)} instead
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
   * @deprecated Use {@link #readUrl(String, CsvFormat, boolean, Charset)} instead
   */
  @Deprecated
  static Matrix readUrl(String url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(new URI(url).toURL(), format, firstRowAsHeader, charset)
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
   * @deprecated Use {@link #read(Path, CsvFormat, boolean, Charset)} instead
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
   * @deprecated Use {@link #read(File, CsvFormat, boolean, Charset)} instead
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
   * @deprecated Use {@link #readFile(String, CsvFormat, boolean, Charset)} instead
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

    // DuplicateHeaderMode (string handling only — pass the enum name as a String)
    if (format.containsKey(CsvOption.DuplicateHeaderMode)) {
      def val = format.get(CsvOption.DuplicateHeaderMode)
      if (val instanceof String) {
        f.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(val))
      } else {
        throw new IllegalArgumentException("The value for DuplicateHeaderMode must be a String (e.g. 'ALLOW_ALL', 'ALLOW_EMPTY', 'DISALLOW') but was $val")
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
        m.put(CsvOption.valueOf(String.valueOf(k)), v)
      }
    }
    m
  }
}
