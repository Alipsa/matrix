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
 * format options through the {@link CsvImporter.Format} enum.</p>
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
 * // Custom format with semicolon delimiter (beautiful named arguments)
 * Matrix m = CsvReader.read(Delimiter: ';', Quote: '"', new File("data.csv"))
 *
 * // Or with explicit Map
 * Matrix m = CsvReader.read([
 *   (CsvImporter.Format.Delimiter): ';',
 *   (CsvImporter.Format.Quote): '"'
 * ], new File("data.csv"))
 * </pre>
 *
 * <h3>Format Options</h3>
 * <p>Use the {@link CsvImporter.Format} enum to customize parsing behavior:</p>
 * <ul>
 *   <li><b>Delimiter</b> - Field separator (default: comma)</li>
 *   <li><b>Quote</b> - Quote character (default: double quote)</li>
 *   <li><b>Escape</b> - Escape character for special characters</li>
 *   <li><b>Trim</b> - Trim whitespace from values (default: true)</li>
 *   <li><b>FirstRowAsHeader</b> - Use first row as column names (default: true)</li>
 *   <li><b>CommentMarker</b> - Character marking comment lines (default: null)</li>
 *   <li><b>NullString</b> - String to convert to null values (default: null)</li>
 * </ul>
 *
 * @see CsvWriter
 * @see CsvImporter.Format
 */
@CompileStatic
class CsvReader {

  /**
   * Read a CSV file from a URL with custom format options.
   *
   * @param format Map of format options using {@link CsvImporter.Format} enum keys
   * @param url URL pointing to the CSV file to read
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the URL fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, URL url) throws IOException {
    Map r = parseMap(format)
    read(url, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  /**
   * Read a CSV file from a URL string with custom format options.
   *
   * @param format Map of format options using {@link CsvImporter.Format} enum keys
   * @param url String URL pointing to the CSV file to read
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the URL fails or URL is invalid
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, String url) throws IOException {
    Map r = parseMap(format)
    readUrl(url, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  /**
   * Read a CSV file from an InputStream with custom format options.
   *
   * @param format Map of format options using {@link CsvImporter.Format} enum keys
   * @param is InputStream to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the stream fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, InputStream is) throws IOException {
    Map r = parseMap(format)
    read(is, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset, r.tableName as String)
  }

  /**
   * Read a CSV file from a Reader with custom format options.
   *
   * @param format Map of format options using {@link CsvImporter.Format} enum keys
   * @param reader Reader to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, Reader reader) throws IOException {
    Map r = parseMap(format)
    read(reader, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset, r.tableName as String)
  }

  /**
   * Read a CSV file from a File with custom format options.
   *
   * @param format Map of format options using {@link CsvImporter.Format} enum keys
   * @param file File to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, File file) throws IOException {
    Map r = parseMap(format)
    read(file, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  /**
   * Read a CSV file from a Path with custom format options.
   *
   * @param format Map of format options using {@link CsvImporter.Format} enum keys
   * @param path Path to the CSV file to read
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   */
  static Matrix read(Map format, Path path) throws IOException {
    Map r = parseMap(format)
    read(path, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  private static Map parseMap(Map format) {
    Map r = [:]
    r.charset = format.getOrDefault(CsvImporter.Format.Charset, StandardCharsets.UTF_8) as Charset
    r.tableName = format.getOrDefault(CsvImporter.Format.TableName, '')
    r.builder = createFormatBuilder(format).build()
    boolean firstRowAsHeader
    if (format.containsKey(CsvImporter.Format.Header)) {
      firstRowAsHeader = false
    } else {
      firstRowAsHeader = format.getOrDefault(CsvImporter.Format.FirstRowAsHeader, true)
    }
    r.firstRowAsHeader = firstRowAsHeader
    r
  }

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
   */
  static Matrix read(InputStream is, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try (CSVParser parser = CSVParser.parse(is, charset, format)) {
      return parse(matrixName, parser, firstRowAsHeader)
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
   */
  static Matrix read(Reader reader, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String matrixName = '') throws IOException {
    try(ReaderInputStream readerInputStream = ReaderInputStream.builder()
    .setCharset(charset).setReader(reader).get()) {
      return read(readerInputStream, format, firstRowAsHeader, charset, matrixName)
    }
  }

  /**
   * Read CSV content from a String.
   *
   * <p>This method provides a convenient way to parse CSV data directly from a String,
   * useful for testing and processing CSV data from non-file sources.</p>
   *
   * @param csvContent String containing CSV data
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @return Matrix containing the imported data
   * @throws IOException if parsing fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix readString(String csvContent, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true) throws IOException {
    return read(new StringReader(csvContent), format, firstRowAsHeader, StandardCharsets.UTF_8, '')
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
   */
  static Matrix read(URL url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (CSVParser parser = CSVParser.parse(url, charset, format)) {
      return parse(CsvImporter.tableName(url), parser, firstRowAsHeader)
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
   */
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
   */
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
   */
  static Matrix read(File file, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (CSVParser parser = CSVParser.parse(file, charset, format)) {
      return parse(CsvImporter.tableName(file), parser, firstRowAsHeader)
    }
  }

  /**
   * Read a CSV file from a file path string (convenience method).
   *
   * <p>This method provides a convenient way to read a CSV file using a String file path
   * instead of creating a File object. It uses default settings (comma delimiter, UTF-8 encoding,
   * first row as header).</p>
   *
   * @param filePath path to the CSV file as a String
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with default settings
   * @throws IOException if reading the file fails or file not found
   * @throws IllegalArgumentException if columns are mismatched between rows
   */
  static Matrix readFile(String filePath, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    read(new File(filePath), format, firstRowAsHeader, charset)
  }

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
    return Matrix.builder()
        .matrixName(matrixName)
        .columnNames(headerRow)
        .rows(rows)
        .types(types)
        .build()
  }

  /**
   * Extracts a table name from a URL by removing the path and file extension.
   *
   * <p>For example: "http://example.com/data/sales.csv" becomes "sales"</p>
   *
   * @param url URL to extract the name from
   * @return file name without extension, or the path if no file name is present
   */
  private static String tableName(URL url) {
    def name = url.getFile() == null ? url.getPath() : url.getFile()
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1, name.length())
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name
  }

  /**
   * Extracts a table name from a File by removing the file extension.
   *
   * <p>For example: "sales.csv" becomes "sales"</p>
   *
   * @param file File to extract the name from
   * @return file name without extension
   */
  private static String tableName(File file) {
    def name = file.getName()
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name
  }

  private static CSVFormat.Builder createFormatBuilder(Map format) {
    format = convertKeysToEnums(format)
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
    if (format.containsKey(CsvImporter.Format.Header)) {
      def val = format.get(CsvImporter.Format.Header)
      if (val instanceof String[]) {
        f.setHeader(val as String[])
        format.put(CsvImporter.Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof List) {
        f.setHeader(val as String[])
        format.put(CsvImporter.Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof String) {
        f.setHeader([val] as String[])
        format.put(CsvImporter.Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else {
        throw new IllegalArgumentException("The value for Header must be a List or array of Strings but was $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    // DuplicateHeaderMode (enum/string handling)
    if (format.containsKey(CsvImporter.Format.DuplicateHeaderMode)) {
      def val = format.get(CsvImporter.Format.DuplicateHeaderMode)
      if (val instanceof DuplicateHeaderMode) {
        f.setDuplicateHeaderMode(val)
      } else if (val instanceof String) {
        f.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(val))
      } else {
        throw new IllegalArgumentException("The value for DuplicateHeaderMode must be a String or DuplicateHeaderMode enum but was $val")
      }
    } else {
      f.setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
    }

    // FirstRowAsHeader (special boolean with header interaction)
    if (format.containsKey(CsvImporter.Format.FirstRowAsHeader)) {
      def val = format.get(CsvImporter.Format.FirstRowAsHeader)
      if (val instanceof Boolean) {
        if (!format.containsKey(CsvImporter.Format.Header)) {
          if (val) {
            f.setHeader()
            f.setSkipHeaderRecord(true)
          } else {
            f.setHeader((String) null)
            f.setSkipHeaderRecord(false)
          }
        } else {
          // will be set in the Header section
        }
      } else {
        throw new IllegalArgumentException("The value for FirstRowAsHeader must be a Boolean but was ${val.class} = $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    return f
  }

  // Boolean format options
  private static void setTrim(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvImporter.Format.Trim)) {
      def val = format.get(CsvImporter.Format.Trim)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for Trim must be a Boolean but was $val")
      }
      builder.setTrim(val as Boolean)
    } else {
      builder.setTrim(true)
    }
  }

  private static void setIgnoreEmptyLines(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvImporter.Format.IgnoreEmptyLines)) {
      def val = format.get(CsvImporter.Format.IgnoreEmptyLines)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for IgnoreEmptyLines must be a Boolean but was $val")
      }
      builder.setIgnoreEmptyLines(val as Boolean)
    } else {
      builder.setIgnoreEmptyLines(true)
    }
  }

  private static void setIgnoreSurroundingSpaces(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvImporter.Format.IgnoreSurroundingSpaces)) {
      def val = format.get(CsvImporter.Format.IgnoreSurroundingSpaces)
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
    if (format.containsKey(CsvImporter.Format.Delimiter)) {
      def val = format.get(CsvImporter.Format.Delimiter)
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
    if (format.containsKey(CsvImporter.Format.Quote)) {
      def val = format.get(CsvImporter.Format.Quote)
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
    if (format.containsKey(CsvImporter.Format.CommentMarker)) {
      def val = format.get(CsvImporter.Format.CommentMarker)
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
    if (format.containsKey(CsvImporter.Format.Escape)) {
      def val = format.get(CsvImporter.Format.Escape)
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
    if (format.containsKey(CsvImporter.Format.NullString)) {
      def val = format.get(CsvImporter.Format.NullString)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for NullString must be a String but was $val")
      }
      builder.setNullString(val as String)
    } else {
      builder.setNullString(null)
    }
  }

  private static void setRecordSeparator(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(CsvImporter.Format.RecordSeparator)) {
      def val = format.get(CsvImporter.Format.RecordSeparator)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for RecordSeparator must be a String but was $val")
      }
      builder.setRecordSeparator(val as String)
    } else {
      builder.setRecordSeparator('\n')
    }
  }

  private static Map convertKeysToEnums(Map map) {
    Map m = [:]
    map.each {k,v ->
      if (k instanceof CsvImporter.Format) {
        m.put(k, v)
      } else {
        def key = CsvImporter.Format.valueOf(String.valueOf(k))
        m.put(key, v)
      }
    }
    return m
  }
}
