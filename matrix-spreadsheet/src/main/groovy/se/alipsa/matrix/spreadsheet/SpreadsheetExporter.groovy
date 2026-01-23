package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Exports Matrix data to spreadsheet files (Excel, ODS).
 *
 * @deprecated Use {@link SpreadsheetWriter} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>{@code SpreadsheetExporter.exportSpreadsheet(file, data)} → {@code SpreadsheetWriter.write(data, file)}</li>
 *   <li>{@code SpreadsheetExporter.exportSpreadsheet(file, data, sheetName)} → {@code SpreadsheetWriter.write(data, file, sheetName)}</li>
 *   <li>{@code SpreadsheetExporter.exportSpreadsheets(file, data, sheetNames)} → {@code SpreadsheetWriter.writeSheets(data, file, sheetNames)}</li>
 * </ul>
 *
 * <p><strong>Thread-safety limitation:</strong> This class uses a static {@code excelImplementation} field
 * that is not thread-safe. Concurrent calls with different implementation settings may cause race conditions.
 * Use {@link SpreadsheetWriter} which provides thread-safe alternatives.</p>
 *
 * @see SpreadsheetWriter
 */
@Deprecated
@CompileStatic
class SpreadsheetExporter {

  /**
   * @deprecated Use {@link SpreadsheetWriter#excelImplementation} instead
   */
  @Deprecated
  public static ExcelImplementation excelImplementation = ExcelImplementation.POI

  /**
   * Export a Matrix to a spreadsheet file.
   *
   * @param file the target file
   * @param data the Matrix to export
   * @return the absolute path of the created file
   * @deprecated Use {@link SpreadsheetWriter#write(Matrix, File)} instead
   */
  @Deprecated
  static String exportSpreadsheet(File file, Matrix data) {
    SpreadsheetWriter.excelImplementation = excelImplementation
    return SpreadsheetWriter.write(data, file)
  }

  /**
   * Export a Matrix to a spreadsheet file with a specific sheet name.
   *
   * @param file the target file
   * @param data the Matrix to export
   * @param sheetName the name for the sheet
   * @return the absolute path of the created file
   * @deprecated Use {@link SpreadsheetWriter#write(Matrix, File, String)} instead
   */
  @Deprecated
  static String exportSpreadsheet(File file, Matrix data, String sheetName) {
    SpreadsheetWriter.excelImplementation = excelImplementation
    return SpreadsheetWriter.write(data, file, sheetName)
  }

  /**
   * Export multiple Matrix objects to a single spreadsheet file as separate sheets.
   *
   * @param file the target file
   * @param data list of Matrix objects to export
   * @param sheetNames list of sheet names
   * @return list of absolute paths
   * @deprecated Use {@link SpreadsheetWriter#writeSheets(List, File, List)} instead
   */
  @Deprecated
  static List<String> exportSpreadsheets(File file, List<Matrix> data, List<String> sheetNames) {
    SpreadsheetWriter.excelImplementation = excelImplementation
    return SpreadsheetWriter.writeSheets(data, file, sheetNames)
  }

  /**
   * Export multiple Matrix objects using a parameter map.
   *
   * @param params map with keys: 'file', 'data', 'sheetNames'
   * @return list of absolute paths
   * @deprecated Use {@link SpreadsheetWriter#writeSheets(Map)} instead
   */
  @Deprecated
  static List<String> exportSpreadsheets(Map params) {
    SpreadsheetWriter.excelImplementation = excelImplementation
    return SpreadsheetWriter.writeSheets(params)
  }
}
