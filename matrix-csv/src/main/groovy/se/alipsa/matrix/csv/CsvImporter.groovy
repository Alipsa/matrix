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
 * Imports CSV files into Matrix objects using Apache Commons CSV.
 *
 * @deprecated Use {@link CsvReader} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>{@code CsvImporter.importCsv(file)} → {@code CsvReader.read(file)}</li>
 *   <li>{@code CsvImporter.importCsvString(content)} → {@code CsvReader.readString(content)}</li>
 *   <li>{@code CsvImporter.importCsvFromFile(path)} → {@code CsvReader.readFile(path)}</li>
 * </ul>
 *
 * <p>Provides flexible CSV parsing with support for multiple input sources
 * (File, URL, InputStream, Reader) and configurable format options through
 * the {@link Format} enum.</p>
 *
 * @see CsvReader
 * @see CsvExporter
 * @see Format
 */
@Deprecated
@CompileStatic
class CsvImporter {

  /**
   * Configuration options for CSV parsing.
   *
   * @deprecated Use {@link CsvOption} instead. This enum will be removed in v2.0.
   */
  @Deprecated
  enum Format {
    Trim,
    Delimiter,
    IgnoreEmptyLines,
    Quote,
    CommentMarker,
    Escape,
    Header,
    DuplicateHeaderMode,
    IgnoreSurroundingSpaces,
    NullString,
    RecordSeparator,
    FirstRowAsHeader,
    Charset,
    TableName
  }

  /**
   * Import a CSV file from a URL with custom format options.
   *
   * @param format Map of format options using {@link Format} enum keys
   * @param url URL pointing to the CSV file to import
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the URL fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   * @deprecated Use {@link CsvReader#read(Map, URL)} instead
   */
  @Deprecated
  static Matrix importCsv(Map format, URL url) throws IOException {
    CsvReader.read(format, url)
  }

  /**
   * Import a CSV file from a URL string with custom format options.
   *
   * @param format Map of format options using {@link Format} enum keys
   * @param url String URL pointing to the CSV file to import
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the URL fails or URL is invalid
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   * @deprecated Use {@link CsvReader#read(Map, String)} instead
   */
  @Deprecated
  static Matrix importCsv(Map format, String url) throws IOException {
    CsvReader.read(format, url)
  }

  /**
   * Import a CSV file from an InputStream with custom format options.
   *
   * @param format Map of format options using {@link Format} enum keys
   * @param is InputStream to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the stream fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   * @deprecated Use {@link CsvReader#read(Map, InputStream)} instead
   */
  @Deprecated
  static Matrix importCsv(Map format, InputStream is) throws IOException {
    CsvReader.read(format, is)
  }

  /**
   * Import a CSV file from a Reader with custom format options.
   *
   * @param format Map of format options using {@link Format} enum keys
   * @param reader Reader to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   * @deprecated Use {@link CsvReader#read(Map, Reader)} instead
   */
  @Deprecated
  static Matrix importCsv(Map format, Reader reader) throws IOException {
    CsvReader.read(format, reader)
  }

  /**
   * Import a CSV file from a File with custom format options.
   *
   * @param format Map of format options using {@link Format} enum keys
   * @param file File to read the CSV data from
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   * @deprecated Use {@link CsvReader#read(Map, File)} instead
   */
  @Deprecated
  static Matrix importCsv(Map format, File file) throws IOException {
    CsvReader.read(format, file)
  }

  /**
   * Import a CSV file from a Path with custom format options.
   *
   * @param format Map of format options using {@link Format} enum keys
   * @param path Path to the CSV file to import
   * @return Matrix containing the imported data with column names from the first row
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if format options are invalid or columns are mismatched
   * @deprecated Use {@link CsvReader#read(Map, Path)} instead
   */
  @Deprecated
  static Matrix importCsv(Map format, Path path) throws IOException {
    CsvReader.read(format, path)
  }

  private static Map parseMap(Map format) {
    Map r = [:]
    r.charset = format.getOrDefault(Format.Charset, StandardCharsets.UTF_8) as Charset
    r.tableName = format.getOrDefault(Format.TableName, '')
    r.builder = createFormatBuilder(format).build()
    boolean firstRowAsHeader
    if (format.containsKey(Format.Header)) {
      firstRowAsHeader = false
    } else {
      firstRowAsHeader = format.getOrDefault(Format.FirstRowAsHeader, true)
    }
    r.firstRowAsHeader = firstRowAsHeader
    r
  }

  /**
   * Import a CSV file from an InputStream using Apache Commons CSV format.
   *
   * @param is InputStream to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param tableName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading the stream fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#read(InputStream, CSVFormat, boolean, Charset, String)} instead
   */
  @Deprecated
  static Matrix importCsv(InputStream is, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String tableName = '') throws IOException {
    CsvReader.read(is, format, firstRowAsHeader, charset, tableName)
  }

  /**
   * Import a CSV file from a Reader using Apache Commons CSV format.
   *
   * @param reader Reader to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @param tableName name for the resulting Matrix (default: empty string)
   * @return Matrix containing the imported data
   * @throws IOException if reading from the Reader fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#read(Reader, CSVFormat, boolean, Charset, String)} instead
   */
  @Deprecated
  static Matrix importCsv(Reader reader, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String tableName = '') throws IOException {
    CsvReader.read(reader, format, firstRowAsHeader, charset, tableName)
  }

  /**
   * Import CSV content from a String (convenience method).
   *
   * <p>This method provides a convenient way to parse CSV data directly from a String,
   * useful for testing and processing CSV data from non-file sources.</p>
   *
   * @param csvContent String containing CSV data
   * @return Matrix containing the imported data with default settings (comma delimiter, first row as header)
   * @throws IOException if parsing fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#readString(String)} instead
   */
  @Deprecated
  static Matrix importCsvString(String csvContent) throws IOException {
    CsvReader.readString(csvContent)
  }

  /**
   * Import CSV content from a String with custom format.
   *
   * @param csvContent String containing CSV data
   * @param format CSVFormat configuration
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @return Matrix containing the imported data
   * @throws IOException if parsing fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#readString(String, CSVFormat, boolean)} instead
   */
  @Deprecated
  static Matrix importCsvString(String csvContent, CSVFormat format, boolean firstRowAsHeader = true) throws IOException {
    CsvReader.readString(csvContent, format, firstRowAsHeader)
  }

  /**
   * Import a CSV file from a URL using Apache Commons CSV format.
   *
   * @param url URL pointing to the CSV file to import
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from URL
   * @throws IOException if reading the URL fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#read(URL, CSVFormat, boolean, Charset)} instead
   */
  @Deprecated
  static Matrix importCsv(URL url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    CsvReader.read(url, format, firstRowAsHeader, charset)
  }

  /**
   * Import a CSV file from a URL string using Apache Commons CSV format.
   *
   * @param url String URL pointing to the CSV file to import
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from URL
   * @throws IOException if reading the URL fails or URL is invalid
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#readUrl(String, CSVFormat, boolean, Charset)} instead
   */
  @Deprecated
  static Matrix importCsv(String url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    CsvReader.readUrl(url, format, firstRowAsHeader, charset)
  }

  /**
   * Import a CSV file from a Path using Apache Commons CSV format.
   *
   * @param path Path to the CSV file to import
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from file name
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#read(Path, CSVFormat, boolean, Charset)} instead
   */
  @Deprecated
  static Matrix importCsv(Path path, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
   CsvReader.read(path, format, firstRowAsHeader, charset)
  }

  /**
   * Import a CSV file from a File using Apache Commons CSV format.
   *
   * @param file File to read the CSV data from
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param firstRowAsHeader whether the first row contains column names (default: true)
   * @param charset character encoding to use (default: UTF-8)
   * @return Matrix containing the imported data with name derived from file name
   * @throws IOException if reading the file fails
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#read(File, CSVFormat, boolean, Charset)} instead
   */
  @Deprecated
  static Matrix importCsv(File file, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    CsvReader.read(file, format, firstRowAsHeader, charset)
  }

  /**
   * Import a CSV file from a file path string (convenience method).
   *
   * <p>This method provides a convenient way to import a CSV file using a String file path
   * instead of creating a File object. It uses default settings (comma delimiter, UTF-8 encoding,
   * first row as header).</p>
   *
   * @param filePath path to the CSV file as a String
   * @return Matrix containing the imported data with default settings
   * @throws IOException if reading the file fails or file not found
   * @throws IllegalArgumentException if columns are mismatched between rows
   * @deprecated Use {@link CsvReader#readFile(String)} instead
   */
  @Deprecated
  static Matrix importCsvFromFile(String filePath) throws IOException {
    CsvReader.readFile(filePath)
  }

  /**
   * Core parsing logic that converts CSVParser records into a Matrix.
   *
   * <p>Handles column name extraction, validates that all rows have the same number
   * of columns, and generates auto-generated column names (c0, c1, ...) when needed.</p>
   *
   * @param tableName name for the resulting Matrix (can be null or empty)
   * @param parser CSVParser containing the parsed CSV data
   * @param firstRowAsHeader if true, uses first row as column names; if false, generates c0, c1, etc.
   * @return Matrix containing the parsed data with all values as Strings
   * @throws IllegalArgumentException if rows have inconsistent column counts
   */
  private static Matrix parse(String tableName, CSVParser parser, boolean firstRowAsHeader) {
    List<List<String>> rows = parser.records*.toList()

    // Handle empty CSV file
    if (rows.isEmpty()) {
      return Matrix.builder()
          .matrixName(tableName ?: 'matrix')
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
        .matrixName(tableName)
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
   * @deprecated Delegates to {@link CsvReader#tableName(URL)}
   */
  @Deprecated
  static String tableName(URL url) {
    return CsvReader.tableName(url)
  }

  /**
   * Extracts a table name from a File by removing the file extension.
   *
   * <p>For example: "sales.csv" becomes "sales"</p>
   *
   * @param file File to extract the name from
   * @return file name without extension
   * @deprecated Delegates to {@link CsvReader#tableName(File)}
   */
  @Deprecated
  static String tableName(File file) {
    return CsvReader.tableName(file)
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
    if (format.containsKey(Format.Header)) {
      def val = format.get(Format.Header)
      if (val instanceof String[]) {
        f.setHeader(val as String[])
        format.put(Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof List) {
        f.setHeader(val as String[])
        format.put(Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof String) {
        f.setHeader([val] as String[])
        format.put(Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else {
        throw new IllegalArgumentException("The value for Header must be a List or array of Strings but was $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    // DuplicateHeaderMode (enum/string handling)
    if (format.containsKey(Format.DuplicateHeaderMode)) {
      def val = format.get(Format.DuplicateHeaderMode)
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
    if (format.containsKey(Format.FirstRowAsHeader)) {
      def val = format.get(Format.FirstRowAsHeader)
      if (val instanceof Boolean) {
        if (!format.containsKey(Format.Header)) {
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
    if (format.containsKey(Format.Trim)) {
      def val = format.get(Format.Trim)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for Trim must be a Boolean but was $val")
      }
      builder.setTrim(val as Boolean)
    } else {
      builder.setTrim(true)
    }
  }

  private static void setIgnoreEmptyLines(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.IgnoreEmptyLines)) {
      def val = format.get(Format.IgnoreEmptyLines)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for IgnoreEmptyLines must be a Boolean but was $val")
      }
      builder.setIgnoreEmptyLines(val as Boolean)
    } else {
      builder.setIgnoreEmptyLines(true)
    }
  }

  private static void setIgnoreSurroundingSpaces(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.IgnoreSurroundingSpaces)) {
      def val = format.get(Format.IgnoreSurroundingSpaces)
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
    if (format.containsKey(Format.Delimiter)) {
      def val = format.get(Format.Delimiter)
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
    if (format.containsKey(Format.Quote)) {
      def val = format.get(Format.Quote)
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
    if (format.containsKey(Format.CommentMarker)) {
      def val = format.get(Format.CommentMarker)
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
    if (format.containsKey(Format.Escape)) {
      def val = format.get(Format.Escape)
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
    if (format.containsKey(Format.NullString)) {
      def val = format.get(Format.NullString)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for NullString must be a String but was $val")
      }
      builder.setNullString(val as String)
    } else {
      builder.setNullString(null)
    }
  }

  private static void setRecordSeparator(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.RecordSeparator)) {
      def val = format.get(Format.RecordSeparator)
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
      if (k instanceof Format) {
        m.put(k, v)
      } else {
        def key = Format.valueOf(String.valueOf(k))
        m.put(key, v)
      }
    }
    return m
  }
}
