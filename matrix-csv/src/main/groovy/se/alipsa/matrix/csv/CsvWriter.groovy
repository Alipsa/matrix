package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

/**
 * Writes Matrix data to CSV format using Apache Commons CSV.
 *
 * <p>Provides flexible CSV export with support for File, Path, Writer, PrintWriter
 * outputs, and String generation. Character-based format suitable for
 * human-readable data exchange.</p>
 *
 * <h3>Fluent API (recommended)</h3>
 * <pre>
 * // Write to file with defaults
 * CsvWriter.write(matrix).to(file)
 *
 * // Write to string
 * String csv = CsvWriter.write(matrix).asString()
 *
 * // Write with custom delimiter
 * CsvWriter.write(matrix).delimiter(';' as char).to(file)
 *
 * // Write Excel CSV
 * CsvWriter.write(matrix).excel().to(file)
 * </pre>
 *
 * @see CsvReader
 */
@CompileStatic
class CsvWriter {

  // ──────────────────────────────────────────────────────────────
  // Fluent builder API (recommended)
  // ──────────────────────────────────────────────────────────────

  /**
   * Entry point for the fluent CSV writing API.
   *
   * <p>Returns a {@link WriteBuilder} that allows chaining format configuration
   * methods before triggering I/O with a terminal {@code to*()} or {@code asString()} method.</p>
   *
   * <pre>
   * CsvWriter.write(matrix).to(file)
   * CsvWriter.write(matrix).delimiter(';' as char).to(file)
   * String csv = CsvWriter.write(matrix).asString()
   * </pre>
   *
   * @param matrix the matrix to write
   * @return a new {@link WriteBuilder}
   */
  static WriteBuilder write(Matrix matrix) {
    new WriteBuilder(matrix)
  }

  // ──────────────────────────────────────────────────────────────
  // CSVFormat-based API (deprecated — use fluent API)
  // ──────────────────────────────────────────────────────────────

  /**
   * Write a Matrix to a CSV file.
   * The out File is normally the destination file but out could also be a directory,
   * in which case the file will be composed of the directory + the matrix name + .csv
   *
   * @param matrix the matrix to write
   * @param out the file to write to or a directory to write to
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @deprecated Use {@code CsvWriter.write(matrix).to(file)} instead
   */
  @Deprecated
  static void write(Matrix matrix, File out, CSVFormat format = CSVFormat.DEFAULT, boolean withHeader = true) {
    validateMatrix(matrix)
    out = ensureFileOutput(matrix, out)
    write(matrix, format, new PrintWriter(out), withHeader)
  }

  /**
   * Write a Matrix to a CSV file specified by Path.
   *
   * @param matrix the matrix to write
   * @param path the path to write to
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @deprecated Use {@code CsvWriter.write(matrix).to(path)} instead
   */
  @Deprecated
  static void write(Matrix matrix, Path path, CSVFormat format = CSVFormat.DEFAULT, boolean withHeader = true) {
    write(matrix, path.toFile(), format, withHeader)
  }

  /**
   * Write a Matrix to a CSV file specified by String path.
   *
   * @param matrix the matrix to write
   * @param filePath the file path to write to
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @deprecated Use {@code CsvWriter.write(matrix).toFile(filePath)} instead
   */
  @Deprecated
  static void write(Matrix matrix, String filePath, CSVFormat format = CSVFormat.DEFAULT, boolean withHeader = true) {
    write(matrix, new File(filePath), format, withHeader)
  }

  /**
   * Write a Matrix as a CSV to the Writer specified.
   *
   * @param matrix the matrix to write
   * @param writer the Writer to write to
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @deprecated Use {@code CsvWriter.write(matrix).to(writer)} instead
   */
  @Deprecated
  static void write(Matrix matrix, Writer writer, CSVFormat format = CSVFormat.DEFAULT, boolean withHeader = true) {
    write(matrix, format, new PrintWriter(writer), withHeader)
  }

  /**
   * Write a Matrix as a CSV to the PrintWriter specified.
   *
   * @param matrix the matrix to write
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param printWriter the PrintWriter to write to
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @deprecated Use {@code CsvWriter.write(matrix).to(printWriter)} instead
   */
  @Deprecated
  static void write(Matrix matrix, CSVFormat format = CSVFormat.DEFAULT, PrintWriter printWriter, boolean withHeader = true) {
    try (CSVPrinter printer = new CSVPrinter(printWriter, format)) {
      if (format.header != null && format.header.length == matrix.columnCount()) {
        printer.printRecord(format.header)
      } else if (withHeader) {
        if (matrix.columnNames() != null) {
          printer.printRecord(matrix.columnNames())
        } else {
          printer.printRecord((1..matrix.columnCount()).collect { 'c' + it })
        }
      }
      printer.printRecords(matrix.rows())
    }
  }

  /**
   * Write a Matrix as a CSV to the CSVPrinter specified.
   *
   * @param matrix the matrix to write
   * @param printer the CSVPrinter to use
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @deprecated Use {@code CsvWriter.write(matrix).to(writer)} instead
   */
  @Deprecated
  static void write(Matrix matrix, CSVPrinter printer, boolean withHeader = true) {
    validateMatrix(matrix)
    if (withHeader) {
      printer.printRecord(matrix.columnNames())
    }
    printer.printRecords(matrix.rows())
  }

  /**
   * Write a Matrix to a String in CSV format.
   *
   * @param matrix the matrix to write
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @return String containing the CSV data
   * @deprecated Use {@code CsvWriter.write(matrix).asString()} instead
   */
  @Deprecated
  static String writeString(Matrix matrix, CSVFormat format = CSVFormat.DEFAULT, boolean withHeader = true) {
    StringWriter stringWriter = new StringWriter()
    write(matrix, stringWriter, format, withHeader)
    stringWriter.toString()
  }

  // ──────────────────────────────────────────────────────────────
  // Convenience format methods
  // ──────────────────────────────────────────────────────────────

  /**
   * Write a Matrix to an Excel-compatible CSV file.
   *
   * <p>Uses the Excel CSV format which is compatible with Microsoft Excel.</p>
   *
   * @param matrix the matrix to write
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row (default: true)
   */
  static void writeExcelCsv(Matrix matrix, File out, boolean withHeader = true) {
    write(matrix).excel().withHeader(withHeader).to(out)
  }

  /**
   * Write a Matrix to a String in Excel-compatible CSV format.
   *
   * @param matrix the matrix to write
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @return String containing the Excel CSV data
   */
  static String writeExcelCsvString(Matrix matrix, boolean withHeader = true) {
    write(matrix).excel().withHeader(withHeader).asString()
  }

  /**
   * Write a Matrix to a Tab-Separated Values (TSV) file.
   *
   * <p>Uses tab characters as delimiters instead of commas.</p>
   *
   * @param matrix the matrix to write
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row (default: true)
   */
  static void writeTsv(Matrix matrix, File out, boolean withHeader = true) {
    write(matrix).tsv().withHeader(withHeader).to(out)
  }

  /**
   * Write a Matrix to a String in Tab-Separated Values (TSV) format.
   *
   * @param matrix the matrix to write
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @return String containing the TSV data
   */
  static String writeTsvString(Matrix matrix, boolean withHeader = true) {
    write(matrix).tsv().withHeader(withHeader).asString()
  }

  // ──────────────────────────────────────────────────────────────
  // Internal helper methods
  // ──────────────────────────────────────────────────────────────

  /**
   * Validate that the Matrix is not null and has columns.
   *
   * Note: This only checks for columns, not rows. Writing a matrix with columns but zero rows
   * is a valid use case (e.g., writing just headers).
   */
  private static void validateMatrix(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix table cannot be null")
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Cannot export matrix with no columns")
    }
  }

  /**
   * Ensure the output File exists and is a regular file (not a directory).
   * If out is a directory, creates a file within it using the Matrix name.
   * Creates parent directories if they don't exist.
   */
  private static File ensureFileOutput(Matrix matrix, File out) {
    if (out.parentFile != null && !out.parentFile.exists()) {
      if (!out.parentFile.mkdirs()) {
        throw new IOException("Failed to create parent directory: ${out.parentFile.absolutePath}")
      }
    }
    if (out.isDirectory()) {
      String fileName = (matrix.matrixName ?: 'matrix') + '.csv'
      return new File(out, fileName)
    }
    out
  }

  // ──────────────────────────────────────────────────────────────
  // WriteBuilder
  // ──────────────────────────────────────────────────────────────

  /**
   * Fluent builder for writing Matrix data to CSV format.
   *
   * <p>Obtained via {@link CsvWriter#write(Matrix)}. Chain format configuration methods
   * and finish with a terminal {@code to*()} or {@code asString()} method to trigger I/O.</p>
   *
   * <pre>
   * CsvWriter.write(matrix)
   *     .delimiter(';' as char)
   *     .to(file)
   * </pre>
   */
  @CompileStatic
  static class WriteBuilder {
    private final Matrix _matrix
    private char _delimiter = ',' as char
    private Character _quoteCharacter = '"' as Character
    private Character _escapeCharacter = null
    private Character _commentMarker = null
    private boolean _trim = true
    private boolean _ignoreEmptyLines = true
    private boolean _ignoreSurroundingSpaces = true
    private String _nullString = null
    private String _recordSeparator = '\n'
    private boolean _withHeader = true

    private WriteBuilder(Matrix matrix) {
      _matrix = matrix
    }

    // ── Format configuration methods ──────────────────────────

    /** Sets the field delimiter character. */
    WriteBuilder delimiter(char c) { _delimiter = c; this }

    /** Sets the quote character for enclosing fields. */
    WriteBuilder quoteCharacter(Character c) { _quoteCharacter = c; this }

    /** Sets the escape character. */
    WriteBuilder escapeCharacter(Character c) { _escapeCharacter = c; this }

    /** Sets the comment marker character. */
    WriteBuilder commentMarker(Character c) { _commentMarker = c; this }

    /** Sets whether to trim whitespace from values. */
    WriteBuilder trim(boolean b) { _trim = b; this }

    /** Sets whether to ignore empty lines. */
    WriteBuilder ignoreEmptyLines(boolean b) { _ignoreEmptyLines = b; this }

    /** Sets whether to ignore spaces around quoted values. */
    WriteBuilder ignoreSurroundingSpaces(boolean b) { _ignoreSurroundingSpaces = b; this }

    /** Sets the string to interpret as null. */
    WriteBuilder nullString(String s) { _nullString = s; this }

    /** Sets the record separator string. */
    WriteBuilder recordSeparator(String s) { _recordSeparator = s; this }

    // ── Writer-specific methods ───────────────────────────────

    /** Sets whether to include column names in the first row (default: true). */
    WriteBuilder withHeader(boolean b) { _withHeader = b; this }

    // ── Preset methods ────────────────────────────────────────

    /** Configures Excel-compatible CSV format with CRLF record separators. */
    WriteBuilder excel() { _recordSeparator = '\r\n'; this }

    /** Configures tab-delimited format (TSV). */
    WriteBuilder tsv() { _delimiter = '\t' as char; this }

    /** Configures RFC 4180 compliant format with CRLF record separators. */
    WriteBuilder rfc4180() { _recordSeparator = '\r\n'; this }

    // ── Terminal operations ───────────────────────────────────

    /**
     * Writes CSV data to a File.
     *
     * @param out the file to write to (or a directory)
     */
    void to(File out) {
      validateMatrix(_matrix)
      out = ensureFileOutput(_matrix, out)
      to(new PrintWriter(out))
    }

    /**
     * Writes CSV data to a Path.
     *
     * @param path the path to write to
     */
    void to(Path path) {
      to(path.toFile())
    }

    /**
     * Writes CSV data to a Writer.
     *
     * @param writer the Writer to write to
     */
    void to(Writer writer) {
      to(new PrintWriter(writer))
    }

    /**
     * Writes CSV data to a PrintWriter.
     *
     * @param printWriter the PrintWriter to write to
     */
    void to(PrintWriter printWriter) {
      validateMatrix(_matrix)
      CsvFormat format = buildFormat()
      CSVFormat apacheFormat = format.toCSVFormat()
      try (CSVPrinter printer = new CSVPrinter(printWriter, apacheFormat)) {
        if (_withHeader) {
          if (_matrix.columnNames() != null) {
            printer.printRecord(_matrix.columnNames())
          } else {
            printer.printRecord((1.._matrix.columnCount()).collect { 'c' + it })
          }
        }
        printer.printRecords(_matrix.rows())
      }
    }

    /**
     * Writes CSV data to a file specified by path string.
     *
     * @param filePath the file path to write to
     */
    void toFile(String filePath) {
      to(new File(filePath))
    }

    /**
     * Writes CSV data to a String.
     *
     * @return String containing the CSV data
     */
    String asString() {
      StringWriter stringWriter = new StringWriter()
      to(stringWriter)
      stringWriter.toString()
    }

    private CsvFormat buildFormat() {
      CsvFormat.builder()
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
    }
  }
}
