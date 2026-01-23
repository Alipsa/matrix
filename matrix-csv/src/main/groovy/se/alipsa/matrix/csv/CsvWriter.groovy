package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

/**
 * Writes Matrix data to CSV format using Apache Commons CSV.
 *
 * <p>Provides flexible CSV export with support for File, Path, Writer, PrintWriter,
 * CSVPrinter outputs, and String generation. Character-based format suitable for
 * human-readable data exchange.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * Matrix data = ...
 *
 * // Write to file
 * CsvWriter.write(data, new File("output.csv"))
 *
 * // Write to string
 * String csvContent = CsvWriter.writeString(data)
 *
 * // Write Excel CSV
 * CsvWriter.writeExcelCsv(data, new File("output.csv"))
 *
 * // Write TSV
 * CsvWriter.writeTsv(data, new File("output.tsv"))
 * </pre>
 *
 * @see CsvReader
 */
@CompileStatic
class CsvWriter {

  /**
   * Write a Matrix to a CSV file.
   * The out File is normally the destination file but out could also be a directory,
   * in which case the file will be composed of the directory + the matrix name + .csv
   *
   * @param matrix the matrix to write
   * @param out the file to write to or a directory to write to
   * @param format CSVFormat configuration (default: CSVFormat.DEFAULT)
   * @param withHeader whether to include the columns names in the first row (default: true)
   */
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
   */
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
   */
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
   */
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
   */
  static void write(Matrix matrix, CSVFormat format = CSVFormat.DEFAULT, PrintWriter printWriter, boolean withHeader = true) {
    try(CSVPrinter printer = new CSVPrinter(printWriter, format)) {
      if (format.header != null && format.header.length == matrix.columnCount()) {
        printer.printRecord(format.header)
      } else if (withHeader) {
        if (matrix.columnNames() != null) {
          printer.printRecord(matrix.columnNames())
        } else {
          printer.printRecord((1..matrix.columnCount()).collect{ 'c' + it})
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
   */
  static void write(Matrix matrix, CSVPrinter printer, boolean withHeader = true) {
    validateMatrix(matrix)
    if(withHeader) {
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
   */
  static String writeString(Matrix matrix, CSVFormat format = CSVFormat.DEFAULT, boolean withHeader = true) {
    StringWriter stringWriter = new StringWriter()
    write(matrix, stringWriter, format, withHeader)
    return stringWriter.toString()
  }

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
    validateMatrix(matrix)
    out = ensureFileOutput(matrix, out)
    write(matrix, CSVFormat.EXCEL, new PrintWriter(out), withHeader)
  }

  /**
   * Write a Matrix to a String in Excel-compatible CSV format.
   *
   * @param matrix the matrix to write
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @return String containing the Excel CSV data
   */
  static String writeExcelCsvString(Matrix matrix, boolean withHeader = true) {
    return writeString(matrix, CSVFormat.EXCEL, withHeader)
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
    validateMatrix(matrix)
    out = ensureFileOutput(matrix, out)
    write(matrix, CSVFormat.TDF, new PrintWriter(out), withHeader)
  }

  /**
   * Write a Matrix to a String in Tab-Separated Values (TSV) format.
   *
   * @param matrix the matrix to write
   * @param withHeader whether to include the columns names in the first row (default: true)
   * @return String containing the TSV data
   */
  static String writeTsvString(Matrix matrix, boolean withHeader = true) {
    return writeString(matrix, CSVFormat.TDF, withHeader)
  }

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
      out.parentFile.mkdirs()
    }
    if (out.isDirectory()) {
      String fileName = (matrix.matrixName ?: 'matrix') + '.csv'
      return new File(out, fileName)
    }
    return out
  }
}
