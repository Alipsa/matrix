package se.alipsa.groovy.spreadsheet.ods

import com.github.miachm.sods.Sheet
import com.github.miachm.sods.SpreadSheet
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.ValueConverter
import se.alipsa.groovy.spreadsheet.FileUtil
import se.alipsa.groovy.spreadsheet.SpreadsheetUtil

import java.text.NumberFormat

/**
 * Import Calc (ods file) into Renjin R in the form of a data.frame (ListVector).
 */
class OdsImporter {

  /**
   * Import an excel spreadsheet
   * @param file the filePath or the file object pointing to the excel file
   * @param sheetName the name of the sheet to import, default is 'Sheet1'
   * @param startRow the starting row for the import (as you would see the row number in excel), defaults to 1
   * @param endRow the last row to import
   * @param startCol the starting column name (A, B etc) or column number (1, 2 etc.)
   * @param endCol the end column name (K, L etc) or column number (11, 12 etc.)
   * @param firstRowAsColNames whether the first row should be used for the names of each column, if false
   * it column names will be v1, v2 etc. Defaults to true
   * @return A Matrix with the excel data.
   */
  static Matrix importOds(String file, int sheetNumber,
                               int startRow = 1, int endRow,
                               String startCol = 'A', String endCol,
                               boolean firstRowAsColNames = true) {

    return importOds(
        file,
        sheetNumber as int,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  static Matrix importOds(InputStream is, String sheetName = 'Sheet1',
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {

    return importOds(
        is,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  /**
   * Import an excel spreadsheet
   * @param file the filePath or the file object pointing to the excel file
   * @param sheetName the name of the sheet to import, default is 'Sheet1'
   * @param startRow the starting row for the import (as you would see the row number in excel), defaults to 1
   * @param endRow the last row to import
   * @param startCol the starting column name (A, B etc) or column number (1, 2 etc.)
   * @param endCol the end column name (K, L etc) or column number (11, 12 etc.)
   * @param firstRowAsColNames whether the first row should be used for the names of each column, if false
   * it column names will be v1, v2 etc. Defaults to true
   * @return A Matrix with the excel data.
   */
  static Matrix importOds(String file, String sheetName = 'Sheet1',
                                      int startRow = 1, int endRow,
                                      String startCol = 'A', String endCol,
                                      boolean firstRowAsColNames = true) {

    return importOds(
        file,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  static Matrix importOds(String file, String sheetName = 'Sheet1',
                                      int startRow = 1, int endRow,
                                      int startCol = 1, int endCol,
                                      boolean firstRowAsColNames = true) {
    def header = []
    File excelFile = FileUtil.checkFilePath(file)
    SpreadSheet spreadSheet = new SpreadSheet(excelFile)
    Sheet sheet = spreadSheet.getSheet(sheetName)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      for (int i = 1; i <= endCol - startCol; i++) {
        header.add(String.valueOf(i))
      }
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }

  static Matrix importOds(String file, int sheetNumber,
                               int startRow = 1, int endRow,
                               int startCol = 1, int endCol,
                               boolean firstRowAsColNames = true) {
    def header = []
    File excelFile = FileUtil.checkFilePath(file)
    SpreadSheet spreadSheet = new SpreadSheet(excelFile)
    Sheet sheet = spreadSheet.getSheet(sheetNumber)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      for (int i = 1; i <= endCol - startCol; i++) {
        header.add(String.valueOf(i))
      }
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }

  static Matrix importOds(InputStream is, String sheetName,
                          int startRow = 1, int endRow,
                          int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    def header = []

    SpreadSheet spreadSheet = new SpreadSheet(is)
    Sheet sheet = spreadSheet.getSheet(sheetName)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      for (int i = 1; i <= endCol - startCol; i++) {
        header.add(String.valueOf(i))
      }
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }

  /**
   * Imports multiple sheets in one go. This is much more efficient compared to importing them one by one.
   * Example:
   * <code><pre>
   * Map<String, Matrix> sheets = OdsImporter.importOdsSheets(is, [
   *   [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 5, firstRowAsColNames: true],
   *   [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true]
   * ])
   * </pre></code>
   *
   * @param is the InputStream pointing to the ods spreadsheet to import
   * @param sheetParams a Map of parameters containing the keys:
   *  sheetName, startRow, endRow, startCol (number or name), endCol (number or name), firstRowAsColNames
   * @return a map of sheet names and the corresponding Matrix
   */
  static Map<String, Matrix> importOdsSheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
    SpreadSheet spreadSheet = new SpreadSheet(is)
    Map<String, Matrix> result = [:]
    sheetParams.each {
      def header = []
      String sheetName = it.sheetName
      Sheet sheet = spreadSheet.getSheet(sheetName)
      int startRow = it.startRow as int
      int startCol
      if (ValueConverter.isNumeric(it.startCol, format)) {
        startCol = ValueConverter.asInteger(it.startCol)
      } else {
        startCol = SpreadsheetUtil.asColumnNumber(it.startCol as String)
      }
      int endCol
      if (ValueConverter.isNumeric(it.endCol, format)) {
        endCol = ValueConverter.asInteger(it.endCol)
      } else {
        endCol = SpreadsheetUtil.asColumnNumber(it.endCol as String)
      }
      if (it.firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      Matrix matrix = importOds(
          sheet,
          startRow,
          it.endRow as int,
          startCol,
          endCol,
          header)
      result.put(sheetName, matrix)
    }
    result
  }

  static Matrix importOds(Sheet sheet, int startRow, int endRow, int startCol, int endCol, List<String> colNames) {
    startRow--
    endRow--
    startCol--
    endCol--

    OdsValueExtractor ext = new OdsValueExtractor(sheet)
    List<List<?>> matrix = []
    List<?> rowList
    for (int rowIdx = startRow; rowIdx <= endRow; rowIdx++) {
      rowList = []
      for (int colIdx = startCol; colIdx <= endCol; colIdx++) {
        String val = ext.getString(rowIdx, colIdx)
        if (val == null) {
          rowList.add(null)
        } else {
          if (val.endsWith("%")) {
            // In excel there is no % in the end of percentage cell, so we make it the same
            // This has the unfortunate consequence that intentional columns ending with % will be changed
            try {
              double dblVal = Double.parseDouble(val.replace("%", "").replace(",", ".")) / 100
              val = String.valueOf(dblVal)
            } catch (NumberFormatException ignored) {
              // it is not a percentage number, leave the value as it was
            }
          }
          rowList.add(val)
        }
      }
      matrix.add(rowList)
    }
    return Matrix.create(sheet.name, colNames, matrix, [String]*colNames.size())
  }

  private static void buildHeaderRow(int startRowNum, int startColNum, int endColNum, List<String> header, Sheet sheet) {
    startRowNum--
    startColNum--
    endColNum--
    OdsValueExtractor ext = new OdsValueExtractor(sheet)
    for (int i = 0; i <= endColNum - startColNum; i++) {
      header.add(ext.getString(startRowNum, startColNum + i))
    }
  }
}
