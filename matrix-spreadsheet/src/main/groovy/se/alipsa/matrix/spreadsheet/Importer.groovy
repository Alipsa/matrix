package se.alipsa.matrix.spreadsheet

import se.alipsa.matrix.core.Matrix

import java.text.NumberFormat

interface Importer {

  Matrix importSpreadsheet(URL url, String sheetName,
                           int startRow, int endRow,
                           int startCol, int endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(URL url, String sheetName,
                           int startRow, int endRow,
                           String startCol, String endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(URL url, int sheetNum,
                           int startRow, int endRow,
                           String startCol, String endCol,
                           boolean firstRowAsColNames)

  /**
   * Imports multiple sheets in one go.
   * Example:
   * <code><pre>
   * Map<String, Matrix> sheets = ExcelImporter.create().importSpreadsheets(is, [
   *   [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 5, firstRowAsColNames: true],
   *   [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true]
   * ])
   * </pre></code>
   *
   * @param is the InputStream pointing to the spreadsheet to import
   * @param sheetParams a Map of parameters containing the keys:
   *  sheetName, startRow, endRow, startCol (number or name), endCol (number or name), firstRowAsColNames
   *  key (optional, defaults to sheetName)
   * @return a map of sheet names and the corresponding Matrix
   */
  Map<Object, Matrix> importSpreadsheets(InputStream is,
                                         List<Map> sheetParams,
                                         NumberFormat... formatOpt)

  /**
   * Import a spreadsheet
   * @param file the filePath or the file object pointing to the file
   * @param sheetName the name of the sheet to import, default is 'Sheet1'
   * @param startRow the starting row for the import (as you would see the row number in excel), defaults to 1
   * @param endRow the last row to import
   * @param startCol the starting column name (A, B etc) or column number (1, 2 etc.)
   * @param endCol the end column name (K, L etc) or column number (11, 12 etc.)
   * @param firstRowAsColNames whether the first row should be used for the names of each column, if false
   * it column names will be c1, c2 etc. Defaults to true
   * @return A Matrix with the spreadsheet data.
   */
  Matrix importSpreadsheet(InputStream is, String sheetName,
                           int startRow, int endRow,
                           String startCol, String endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(String file, String sheetName,
                           int startRow, int endRow,
                           String startCol, String endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(String file, int sheetNumber,
                           int startRow, int endRow,
                           int startCol, int endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(String file, int sheetNumber,
                           int startRow, int endRow,
                           String startColumn, String endColumn,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(String file, String sheetName,
                           int startRow, int endRow,
                           int startCol, int endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(InputStream is, int sheetNum,
                           int startRow, int endRow,
                           int startCol, int endCol,
                           boolean firstRowAsColNames)

  Matrix importSpreadsheet(InputStream is, String sheetName,
                           int startRow, int endRow,
                           int startCol, int endCol,
                           boolean firstRowAsColNames)

  Map<Object, Matrix> importSpreadsheets(String fileName,
                                         List<Map> sheetParams,
                                         NumberFormat... formatOpt)
}