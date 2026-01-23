package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelExporter
import se.alipsa.matrix.spreadsheet.sods.SOdsExporter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.poi.ExcelExporter

/**
 * Writes Matrix data to spreadsheet files (Excel, ODS).
 *
 * <p>Supports multiple spreadsheet formats:</p>
 * <ul>
 *   <li>Excel (.xlsx, .xls) - via Apache POI or FastExcel</li>
 *   <li>OpenDocument Spreadsheet (.ods) - via SODS</li>
 * </ul>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * Matrix data = Matrix.builder()
 *   .data(id: [1, 2, 3], name: ['Alice', 'Bob', 'Charlie'])
 *   .build()
 *
 * // Write to file (format auto-detected by extension)
 * String path = SpreadsheetWriter.write(data, new File("output.xlsx"))
 *
 * // Write with specific sheet name
 * String path = SpreadsheetWriter.write(data, new File("output.xlsx"), "MySheet")
 *
 * // Write multiple sheets
 * List&lt;Matrix&gt; sheets = [data1, data2, data3]
 * List&lt;String&gt; names = ["Sheet1", "Sheet2", "Sheet3"]
 * List&lt;String&gt; paths = SpreadsheetWriter.writeSheets(sheets, new File("multi.xlsx"), names)
 * </pre>
 *
 * <h3>Format Selection</h3>
 * <pre>
 * // Excel with specific implementation
 * SpreadsheetWriter.excelImplementation = ExcelImplementation.POI  // or FastExcel
 * String path = SpreadsheetWriter.write(data, new File("output.xlsx"))
 * </pre>
 *
 * @see SpreadsheetExporter (deprecated)
 * @see ExcelExporter
 * @see FExcelExporter
 * @see SOdsExporter
 */
@CompileStatic
class SpreadsheetWriter {

  /** Default Excel implementation to use (POI or FastExcel) */
  public static ExcelImplementation excelImplementation = ExcelImplementation.POI

  /**
   * Write a Matrix to a spreadsheet file.
   *
   * <p>The format is auto-detected based on file extension (.ods for OpenDocument,
   * .xlsx/.xls for Excel).</p>
   *
   * @param matrix the Matrix to write
   * @param file the target file
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String write(Matrix matrix, File file) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    if (file.getName().toLowerCase().endsWith(".ods")) {
      return SOdsExporter.exportOds(file, matrix)
    }
    return switch (excelImplementation) {
      case ExcelImplementation.POI -> ExcelExporter.exportExcel(file, matrix)
      case ExcelImplementation.FastExcel -> FExcelExporter.exportExcel(file, matrix)
    }
  }

  /**
   * Write a Matrix to a spreadsheet file with a specific sheet name.
   *
   * @param matrix the Matrix to write
   * @param file the target file
   * @param sheetName the name for the sheet
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String write(Matrix matrix, File file, String sheetName) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    if (sheetName == null) {
      throw new IllegalArgumentException("Sheet name cannot be null")
    }
    if (file.getName().toLowerCase().endsWith(".ods")) {
      return SOdsExporter.exportOds(file, matrix, sheetName)
    }
    return switch (excelImplementation) {
      case ExcelImplementation.POI -> ExcelExporter.exportExcel(file, matrix, sheetName)
      case ExcelImplementation.FastExcel -> FExcelExporter.exportExcel(file, matrix, sheetName)
    }
  }

  /**
   * Write multiple Matrix objects to a single spreadsheet file as separate sheets.
   *
   * @param matrices list of Matrix objects to write
   * @param file the target file
   * @param sheetNames list of sheet names (must match length of matrices list)
   * @return list of actual sheet names created (illegal characters replaced by space)
   */
  static List<String> writeSheets(List<Matrix> matrices, File file, List<String> sheetNames) {
    if (matrices == null) {
      throw new IllegalArgumentException("Matrices list cannot be null")
    }
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    if (sheetNames == null) {
      throw new IllegalArgumentException("Sheet names list cannot be null")
    }
    if (matrices.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    if (file.getName().toLowerCase().endsWith(".ods")) {
      return SOdsExporter.exportOdsSheets(file, matrices, sheetNames)
    }
    return switch (excelImplementation) {
      case ExcelImplementation.POI -> ExcelExporter.exportExcelSheets(file, matrices, sheetNames)
      case ExcelImplementation.FastExcel -> FExcelExporter.exportExcelSheets(file, matrices, sheetNames)
    }
  }

  /**
   * Write multiple Matrix objects using a parameter map.
   *
   * @param params map with keys: 'file' (File), 'data' (List&lt;Matrix&gt;), 'sheetNames' (List&lt;String&gt;)
   * @return list of actual sheet names created
   */
  static List<String> writeSheets(Map params) {
    def file = params.get("file") as File
    def data = (List<Matrix>) params.get("data")
    def sheetNames = params.get("sheetNames") as List<String>
    return writeSheets(data, file, sheetNames)
  }
}
