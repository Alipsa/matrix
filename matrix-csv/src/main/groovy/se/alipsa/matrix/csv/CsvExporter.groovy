package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import se.alipsa.matrix.core.Matrix

/**
 * Exports a Matrix to a CSV using apache commons csv.
 *
 * @deprecated Use {@link CsvWriter} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>{@code CsvExporter.exportToCsv(matrix, file)} → {@code CsvWriter.write(matrix, file)}</li>
 *   <li>{@code CsvExporter.exportToExcelCsv(matrix, file)} → {@code CsvWriter.writeExcelCsv(matrix, file)}</li>
 *   <li>{@code CsvExporter.exportToTsv(matrix, file)} → {@code CsvWriter.writeTsv(matrix, file)}</li>
 * </ul>
 *
 * @see CsvWriter
 */
@Deprecated
@CompileStatic
class CsvExporter {

  /**
   * Export a Matrix to a CSV file.
   * the out File is normally the destination file but out could also be a directory,
   * in which case the file will the composed of the directory + the matrix name + .csv
   * @param table the matrix to export
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#write(Matrix, File, CSVFormat, boolean)} instead
   */
  @Deprecated
  static void exportToCsv(Matrix table, File out, boolean withHeader = true) {
    CsvWriter.write(table, out, CSVFormat.DEFAULT, withHeader)
  }

  /**
   * Export a Matrix to a CSV file.
   * the out File is normally the destination file but out could also be a directory,
   * in which case the file will the composed of the directory + the matrix name + .csv
   * @param table the matrix to export
   * @param format the CSVFormat to use
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#write(Matrix, File, CSVFormat, boolean)} instead
   */
  @Deprecated
  static void exportToCsv(Matrix table, CSVFormat format, File out, boolean withHeader = true) {
    CsvWriter.write(table, out, format, withHeader)
  }

  /**
   * Export a Matrix as a CSV to the writer specified.
   *
   * @param table the matrix to export
   * @param format the CSVFormat to use
   * @param out the writer to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#write(Matrix, Writer, CSVFormat, boolean)} instead
   */
  @Deprecated
  static void exportToCsv(Matrix table, CSVFormat format = CSVFormat.DEFAULT, Writer out, boolean withHeader = true) {
    CsvWriter.write(table, out, format, withHeader)
  }

  /**
   * Export a Matrix as a CSV to the print-writer specified.
   *
   * @param table the matrix to export
   * @param format the CSVFormat to use
   * @param out the print writer to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#write(Matrix, CSVFormat, PrintWriter, boolean)} instead
   */
  @Deprecated
  static void exportToCsv(Matrix table, CSVFormat format = CSVFormat.DEFAULT, PrintWriter out, boolean withHeader = true) {
    CsvWriter.write(table, format, out, withHeader)
  }

  /**
   * Export a Matrix as a CSV to the CSVPrinter specified.
   *
   * @param table the matrix to export
   * @param printer the CSVPrinter to use
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#write(Matrix, CSVPrinter, boolean)} instead
   */
  @Deprecated
  static void exportToCsv(Matrix table, CSVPrinter printer, boolean withHeader = true) {
    CsvWriter.write(table, printer, withHeader)
  }

  /**
   * Export a Matrix to an Excel-compatible CSV file.
   *
   * <p>Uses the Excel CSV format which is compatible with Microsoft Excel.</p>
   *
   * @param table the matrix to export
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#writeExcelCsv(Matrix, File, boolean)} instead
   */
  @Deprecated
  static void exportToExcelCsv(Matrix table, File out, boolean withHeader = true) {
    CsvWriter.writeExcelCsv(table, out, withHeader)
  }

  /**
   * Export a Matrix to a Tab-Separated Values (TSV) file.
   *
   * <p>Uses tab characters as delimiters instead of commas.</p>
   *
   * @param table the matrix to export
   * @param out the file to write to or a directory to write to
   * @param withHeader whether to include the columns names in the first row, default is true
   * @deprecated Use {@link CsvWriter#writeTsv(Matrix, File, boolean)} instead
   */
  @Deprecated
  static void exportToTsv(Matrix table, File out, boolean withHeader = true) {
    CsvWriter.writeTsv(table, out, withHeader)
  }
}
