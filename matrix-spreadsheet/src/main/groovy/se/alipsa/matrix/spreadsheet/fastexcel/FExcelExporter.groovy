package se.alipsa.matrix.spreadsheet.fastexcel

import groovy.transform.CompileStatic

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Collections

// @CompileStatic is intentionally omitted.
// When enabled, Groovy generates direct bytecode for sheet.style(row, col).format(...).set()
// which triggers an IllegalAccessError at runtime because GenericStyleSetter is
// package-private inside org.dhatim.fastexcel. Dynamic dispatch uses reflection/
// metaclass invocation which bypasses this restriction. Re-evaluate if fastexcel
// ever makes GenericStyleSetter public.
class FExcelExporter {

  static final Logger logger = Logger.getLogger(FExcelExporter)
  static final String APP_NAME = 'matrix-spreadsheet'
  static final String VERSION = '02.1000' // must be in the format XX.YYYY

  /**
   * Export a Matrix to a new Excel file.
   *
   * <p>If the file already exists and is not empty, an {@link IllegalArgumentException}
   * is thrown. Use {@link FExcelAppender} or {@link SpreadsheetWriter} for append/replace
   * operations.</p>
   *
   * @param filePath the path to the file to export
   * @param data the Matrix data to export
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(String filePath, Matrix data) {
    File file = new File(filePath)
    exportExcel(file, data)
  }

  /**
   * Export a Matrix to a new Excel file.
   *
   * <p>If the file already exists and is not empty, an {@link IllegalArgumentException}
   * is thrown. Use {@link FExcelAppender} or {@link SpreadsheetWriter} for append/replace
   * operations.</p>
   *
   * @param file the file to export
   * @param data the Matrix data to export
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(File file, Matrix data) {
    SpreadsheetUtil.rejectLegacyXls(file)
    String sheetName = SpreadsheetUtil.createValidSheetName(data.matrixName)
    exportExcel(file, data, sheetName, "A1")
    sheetName
  }

  /**
   * Export a Matrix to a new Excel file with a specific sheet name.
   *
   * <p>If the file already exists and is not empty, an {@link IllegalArgumentException}
   * is thrown. Use {@link FExcelAppender} or {@link SpreadsheetWriter} for append/replace
   * operations.</p>
   *
   * @param file the file to export
   * @param data the Matrix data to export
   * @param sheetName the name of the sheet to export to
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(File file, Matrix data, String sheetName) {
    exportExcel(file, data, sheetName, "A1")
  }

  /**
   * Export a Matrix to a new Excel file with a specific start position.
   *
   * <p>If the file already exists and is not empty, an {@link IllegalArgumentException}
   * is thrown. Use {@link FExcelAppender} or {@link SpreadsheetWriter} for append/replace
   * operations.</p>
   *
   * @param file the file to export
   * @param data the Matrix data to export
   * @param sheetName the name of the sheet to export to
   * @param startPosition the top-left cell for the header row (e.g. "B3")
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(File file, Matrix data, String sheetName, String startPosition) {
    SpreadsheetUtil.rejectLegacyXls(file)
    String validSheetName = SpreadsheetUtil.createValidSheetName(sheetName)
    SpreadsheetUtil.CellPosition position = SpreadsheetUtil.parseCellPosition(startPosition)
    if (file.exists() && file.length() > 0) {
      throw new IllegalArgumentException("Appending to an existing Excel file is not supported by FExcelExporter. Use FExcelAppender or SpreadsheetWriter for append/replace operations.")
    }
    try (FileOutputStream fos = new FileOutputStream(file); Workbook workbook = new Workbook(fos, APP_NAME, VERSION)) {
      Worksheet sheet = workbook.newWorksheet(validSheetName)
      buildSheet(data, sheet, position)
      workbook.finish()
    }
    validSheetName
  }

  static List<String> exportExcelSheets(String filePath, List<Matrix> data, List<String> sheetNames) {
    return exportExcelSheets(new File(filePath), data, sheetNames)
  }

  static List<String> exportExcelSheets(File file, List<Matrix> data) {
    exportExcelSheets(file, data, data.collect{it.matrixName})
  }

  static List<String> exportExcelSheets(File file, List<Matrix> data, List<String> sheetNames, boolean overwrite = false) throws IOException {
    return exportExcelSheets(file, data, sheetNames, null, overwrite)
  }

  static List<String> exportExcelSheets(File file, List<Matrix> data, List<String> sheetNames, List<String> startPositions, boolean overwrite = false) throws IOException {
    SpreadsheetUtil.rejectLegacyXls(file)
    if (file.exists() && !overwrite && file.length() > 0) {
      throw new IllegalArgumentException("Appending to an existing Excel file is not supported by FExcelExporter. Use FExcelAppender or SpreadsheetWriter for append/replace operations.")
    }
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    List<String> positions = startPositions ?: Collections.nCopies(data.size(), "A1")
    if (data.size() != positions.size()) {
      throw new IllegalArgumentException("Matrices and start positions lists must have the same size")
    }
    List<String> uniqueSheetNames = SpreadsheetUtil.createUniqueSheetNames(sheetNames)

    try (FileOutputStream fos = new FileOutputStream(file); Workbook workbook = new Workbook(fos, APP_NAME, VERSION)) {
      for (int i = 0; i < data.size(); i++) {
        Matrix dataFrame = data.get(i)
        String sheetName = uniqueSheetNames.get(i)
        SpreadsheetUtil.CellPosition position = SpreadsheetUtil.parseCellPosition(positions.get(i))
        buildSheet(dataFrame, workbook.newWorksheet(sheetName), position)
      }
      uniqueSheetNames
    } catch (IOException e) {
      logger.error("Failed to create excel file ${file.absolutePath}", e)
      throw new IOException("Failed to create excel file ${file}", e)
    }
  }

  private static void buildSheet(Matrix data, Worksheet sheet, SpreadsheetUtil.CellPosition startPosition) {
    int rowOffset = startPosition.row - 1
    int colOffset = startPosition.column - 1
    def names = data.columnNames()
    for (int i = 0; i < names.size(); i++) {
      sheet.value(rowOffset, colOffset + i, names[i])
    }

    data.columns().eachWithIndex { Column column, int c ->
      Class type = column.type
      int col = colOffset + c
      column.eachWithIndex { Object entry, int r ->
        int row = rowOffset + r + 1
        if (entry == null) {
          // do nothing
        } else if (Number.isAssignableFrom(type)) {
          sheet.value(row, col, ValueConverter.asNumber(entry))
        } else if (type in [byte, Byte]) {
          sheet.value(row, col, ValueConverter.asByte(entry))
        } else if (boolean == type || Boolean == type) {
          sheet.value(row, col, ValueConverter.asBoolean(entry))
        } else if (LocalDate == type) {
          sheet.value(row, col, ValueConverter.asLocalDate(entry))
          sheet.style(row, col).format("yyyy-MM-dd").set()
        } else if (LocalDateTime == type) {
          sheet.value(row, col, ValueConverter.asLocalDateTime(entry))
          sheet.style(row, col).format("yyyy-MM-dd HH:mm:ss.SSS").set()
        } else if (ZonedDateTime == type) {
          sheet.value(row, col, entry as ZonedDateTime)
          sheet.style(row, col).format("yyyy-MM-dd HH:mm:ss.SSS Z").set()
        } else if (Date == type) {
          sheet.value(row, col, ValueConverter.asDate(entry))
          sheet.style(row, col).format("yyyy-MM-dd HH:mm:ss.SSS").set()
        } else {
          sheet.value(row, col, String.valueOf(entry))
        }
      }
    }
  }

}
