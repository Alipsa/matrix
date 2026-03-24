package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import org.apache.commons.csv.CSVFormat

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
  @SuppressWarnings('FieldName')
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

}
