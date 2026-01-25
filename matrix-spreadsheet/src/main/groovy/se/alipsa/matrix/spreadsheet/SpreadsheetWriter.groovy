package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelAppender
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelExporter
import se.alipsa.matrix.spreadsheet.fastods.FOdsAppender
import se.alipsa.matrix.spreadsheet.fastods.FOdsExporter

import java.util.Collections

/**
 * Writes Matrix data to spreadsheet files (Excel, ODS).
 *
 * <p>Supports spreadsheet formats:</p>
 * <ul>
 *   <li>Excel (.xlsx) - via FastExcel</li>
 *   <li>OpenDocument Spreadsheet (.ods) - via FastOds</li>
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
 * @see SpreadsheetExporter (deprecated)
 * @see FExcelExporter
 * @see FOdsExporter
 */
@CompileStatic
class SpreadsheetWriter {

  private static final String DEFAULT_START_POSITION = "A1"

  /**
   * Write a Matrix to a spreadsheet file.
   *
   * <p>The format is auto-detected based on file extension (.ods for OpenDocument,
   * .xlsx for Excel).</p>
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
    ensureSupportedExcelFormat(file)
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Matrix must have at least one column")
    }
    if (file.getName().toLowerCase().endsWith(".ods")) {
      if (file.exists() && file.length() > 0) {
        return FOdsAppender.appendOrReplaceSheets(file, [matrix], [matrix.matrixName], [DEFAULT_START_POSITION])[0]
      }
      return FOdsExporter.exportOds(file, matrix)
    }
    if (file.exists() && file.length() > 0) {
      return FExcelAppender.appendOrReplaceSheets(file, [matrix], [matrix.matrixName], [DEFAULT_START_POSITION])[0]
    }
    return FExcelExporter.exportExcel(file, matrix)
  }

  /**
   * Write a Matrix to a spreadsheet file with a specific sheet name and start position.
   *
   * @param matrix the Matrix to write
   * @param file the target file
   * @param sheetName the name for the sheet
   * @param startPosition the top-left cell for the header row (e.g. "B3")
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String write(Matrix matrix, File file, String sheetName, String startPosition = DEFAULT_START_POSITION) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    ensureSupportedExcelFormat(file)
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Matrix must have at least one column")
    }
    if (sheetName == null) {
      throw new IllegalArgumentException("Sheet name cannot be null")
    }
    SpreadsheetUtil.parseCellPosition(startPosition)
    if (file.getName().toLowerCase().endsWith(".ods")) {
      if (file.exists() && file.length() > 0) {
        return FOdsAppender.appendOrReplaceSheets(file, [matrix], [sheetName], [startPosition])[0]
      }
      return FOdsExporter.exportOdsSheets(file, [matrix], [sheetName], [startPosition])[0]
    }
    if (file.exists() && file.length() > 0) {
      return FExcelAppender.appendOrReplaceSheets(file, [matrix], [sheetName], [startPosition])[0]
    }
    return FExcelExporter.exportExcel(file, matrix, sheetName, startPosition)
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
    return writeSheetsInternal(matrices, file, sheetNames, null)
  }

  /**
   * Write multiple Matrix objects to a single spreadsheet file with per-sheet start positions.
   *
   * @param matrices list of Matrix objects to write
   * @param file the target file
   * @param sheetNamesAndPositions map of sheet name -> start position (LinkedHashMap order is preserved)
   * @return list of actual sheet names created (illegal characters replaced by space)
   */
  static List<String> writeSheets(List<Matrix> matrices, File file, LinkedHashMap<String, String> sheetNamesAndPositions = null) {
    if (sheetNamesAndPositions == null) {
      if (matrices == null) {
        throw new IllegalArgumentException("Matrices list cannot be null")
      }
      List<String> sheetNames = matrices.collect { it.matrixName }
      return writeSheetsInternal(matrices, file, sheetNames, null)
    }
    if (matrices == null) {
      throw new IllegalArgumentException("Matrices list cannot be null")
    }
    if (sheetNamesAndPositions.size() != matrices.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    List<String> sheetNames = []
    List<String> startPositions = []
    sheetNamesAndPositions.each { String name, String position ->
      sheetNames.add(name)
      startPositions.add(position ?: DEFAULT_START_POSITION)
    }
    return writeSheetsInternal(matrices, file, sheetNames, startPositions)
  }

  private static List<String> writeSheetsInternal(List<Matrix> matrices, File file, List<String> sheetNames, List<String> startPositions) {
    if (matrices == null) {
      throw new IllegalArgumentException("Matrices list cannot be null")
    }
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    ensureSupportedExcelFormat(file)
    if (sheetNames == null) {
      throw new IllegalArgumentException("Sheet names list cannot be null")
    }
    if (matrices.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    List<String> positions = startPositions ?: Collections.nCopies(matrices.size(), DEFAULT_START_POSITION)
    if (positions.size() != matrices.size()) {
      throw new IllegalArgumentException("Matrices and start positions lists must have the same size")
    }
    positions.each { String position ->
      SpreadsheetUtil.parseCellPosition(position)
    }
    // Validate each matrix has columns
    matrices.eachWithIndex { matrix, idx ->
      if (matrix == null) {
        throw new IllegalArgumentException("Matrix at index ${idx} is null")
      }
      if (matrix.columnCount() == 0) {
        throw new IllegalArgumentException("Matrix at index ${idx} must have at least one column")
      }
    }
    if (file.getName().toLowerCase().endsWith(".ods")) {
      if (file.exists() && file.length() > 0) {
        return FOdsAppender.appendOrReplaceSheets(file, matrices, sheetNames, positions)
      }
      return FOdsExporter.exportOdsSheets(file, matrices, sheetNames, positions)
    }
    if (file.exists() && file.length() > 0) {
      return FExcelAppender.appendOrReplaceSheets(file, matrices, sheetNames, positions)
    }
    return FExcelExporter.exportExcelSheets(file, matrices, sheetNames, positions)
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

  private static void ensureSupportedExcelFormat(File file) {
    if (file.getName().toLowerCase().endsWith(".xls")) {
      throw new IllegalArgumentException("Unsupported Excel format .xls. Only .xlsx is supported.")
    }
  }
}
