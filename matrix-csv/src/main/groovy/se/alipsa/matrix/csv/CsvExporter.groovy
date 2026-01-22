package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import se.alipsa.matrix.core.Matrix

/**
 * Exports a Matrix to a CSV using apache commons csv.
 */
@CompileStatic
class CsvExporter {

  /**
   * Export a Matrix to a CSV file.
   * the out File is normally the destination file but out could also be a directory,
   * in which case the file will the composed of the directory + the matrix name + .csv
   * @param table the matrix to export
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   */
  static void exportToCsv(Matrix table, File out, boolean withHeader = true) {
    validateMatrix(table)
    out = ensureFileOutput(table, out)
    exportToCsv(table, CSVFormat.DEFAULT, new PrintWriter(out), withHeader)
  }

  /**
   * Export a Matrix to a CSV file.
   * the out File is normally the destination file but out could also be a directory,
   * in which case the file will the composed of the directory + the matrix name + .csv
   * @param table the matrix to export
   * @param format the CSVFormat to use
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   */
  static void exportToCsv(Matrix table, CSVFormat format, File out, boolean withHeader = true) {
    validateMatrix(table)
    out = ensureFileOutput(table, out)
    exportToCsv(table, format, new PrintWriter(out), withHeader)
  }

  /**
   * Export a Matrix as a CSV to the writer specified.
   *
   * @param table the matrix to export
   * @param format the CSVFormat to use
   * @param out the writer to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   */
  static void exportToCsv(Matrix table, CSVFormat format = CSVFormat.DEFAULT, Writer out, boolean withHeader = true) {
    exportToCsv(table, format, new PrintWriter(out), withHeader)
  }

  /**
   * Export a Matrix as a CSV to the print-writer specified.
   *
   * @param table the matrix to export
   * @param format the CSVFormat to use
   * @param out the print writer to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   */
  static void exportToCsv(Matrix table, CSVFormat format = CSVFormat.DEFAULT, PrintWriter out, boolean withHeader = true) {
    try(CSVPrinter printer = new CSVPrinter(out, format)) {
      if (format.header != null && format.header.length == table.columnCount()) {
        printer.printRecord(format.header)
      } else if (withHeader) {
        if (table.columnNames() != null) {
          printer.printRecord(table.columnNames())
        } else {
          printer.printRecord((1..table.columnCount()).collect{ 'c' + it})
        }
      }
      printer.printRecords(table.rows())
    }
  }

  /**
   * Export a Matrix as a CSV to the CSVPrinter specified.
   *
   * @param table the matrix to export
   * @param printer the CSVPrinter to use
   * @param withHeader whether to include the columns names in the first row, default is true
   */
  static void exportToCsv(Matrix table, CSVPrinter printer, boolean withHeader = true) {
    validateMatrix(table)
    if(withHeader) {
      printer.printRecord(table.columnNames())
    }
    printer.printRecords(table.rows())
  }

  /**
   * Validate that the Matrix is not null and has columns.
   */
  private static void validateMatrix(Matrix table) {
    if (table == null) {
      throw new IllegalArgumentException("Matrix table cannot be null")
    }
    if (table.rowCount() == 0 && table.columnCount() == 0) {
      throw new IllegalArgumentException("Cannot export empty matrix with no columns")
    }
  }

  /**
   * Ensure the output File exists and is a regular file (not a directory).
   * If out is a directory, creates a file within it using the Matrix name.
   * Creates parent directories if they don't exist.
   */
  private static File ensureFileOutput(Matrix table, File out) {
    if (out.parentFile != null && !out.parentFile.exists()) {
      out.parentFile.mkdirs()
    }
    if (out.isDirectory()) {
      String fileName = (table.matrixName ?: 'matrix') + '.csv'
      return new File(out, fileName)
    }
    return out
  }
}
