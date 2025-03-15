package se.alipsa.matrix.spreadsheet.excel

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter

import java.text.NumberFormat
import java.time.LocalDateTime

class ExcelImporter {

  private static Logger log = LogManager.getLogger()


  static Matrix importExcel(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      importExcel(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  static Matrix importExcel(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      importExcel(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  static Matrix importExcel(URL url, int sheetNum,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      importExcel(is, sheetNum, startRow, endRow, startColNum, endColNum, firstRowAsColNames)
    }
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
  static Matrix importExcel(InputStream is, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {

    return importExcel(
        is,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  static Matrix importExcel(String file, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {

    return importExcel(
        file,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  static Matrix importExcel(String file, int sheetNumber,
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    def header = []
    File excelFile = FileUtil.checkFilePath(file)
    try (Workbook workbook = WorkbookFactory.create(excelFile)) {
      Sheet sheet = workbook.getSheetAt(sheetNumber)
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  static Matrix importExcel(String file, int sheet,
                            int startRow = 1, int endRow,
                            String startColumn = 'A', String endColumn,
                            boolean firstRowAsColNames = true) {
    return importExcel(
        file,
        sheet,
        startRow,
        endRow,
        SpreadsheetUtil.asColumnNumber(startColumn),
        SpreadsheetUtil.asColumnNumber(endColumn),
        firstRowAsColNames
    )
  }

  static Matrix importExcel(String file, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    def header = []
    File excelFile = FileUtil.checkFilePath(file)
    try (Workbook workbook = WorkbookFactory.create(excelFile)) {
      Sheet sheet = workbook.getSheet(sheetName)
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  /**
   *
   * @param is
   * @param sheetNum 1 indexed
   * @param startRow 1 indexed
   * @param endRow 1 indexed
   * @param startCol 1 indexed
   * @param endCol 1 indexed
   * @param firstRowAsColNames
   * @return
   */
  static Matrix importExcel(InputStream is, int sheetNum,
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    def header = []
    try (Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheetAt(sheetNum-1)
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  static Matrix importExcel(InputStream is, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    def header = []
    try (Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheet(sheetName)
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  /**
   * Imports multiple sheets in one go. This is much more efficient compared to importing them one by one.
   * Example:
   * <code><pre>
   * Map<String, Matrix> sheets = ExcelImporter.importExcelSheets(is, [
   *   [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 5, firstRowAsColNames: true],
   *   [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true]
   * ])
   * </pre></code>
   *
   * @param is the InputStream pointing to the excel spreadsheet to import
   * @param sheetParams a Map of parameters containing the keys:
   *  sheetName, startRow, endRow, startCol (number or name), endCol (number or name), firstRowAsColNames
   *  key (optional, defaults to sheetName)
   * @return a map of sheet names and the corresponding Matrix
   */
  static Map<String, Matrix> importExcelSheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
    try (Workbook workbook = WorkbookFactory.create(is)) {
      Map<String, Matrix> result = [:]
      sheetParams.each {
        def header = []
        String sheetName = it.sheetName
        Sheet sheet = workbook.getSheet(sheetName)
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
          header.addAll(SpreadsheetUtil.createColumnNames(startCol, endCol))
        }
        Matrix matrix = importExcelSheet(sheet, startRow, it.endRow as int, startCol, endCol, header)
        String key = it.getOrDefault("key", sheetName)
        matrix.setMatrixName(key)
        result.put(key, matrix)
      }
      return result
    }
  }

  static Map<String, Matrix> importExcelSheets(String fileName, List<Map> sheetParams, NumberFormat... formatOpt) {
    File file = FileUtil.checkFilePath(fileName)
    try (FileInputStream fis = new FileInputStream(file)) {
      importExcelSheets(fis, sheetParams, formatOpt)
    }
  }


  private static void buildHeaderRow(int startRowNum, int startColNum, int endColNum, List<String> header, Sheet sheet) {
    startRowNum--
    startColNum--
    endColNum--
    ExcelValueExtractor ext = new ExcelValueExtractor(sheet)
    Row row = sheet.getRow(startRowNum)
    for (int i = 0; i <= endColNum - startColNum; i++) {
      header.add(ext.getString(row, startColNum + i))
    }
  }

  private static Matrix importExcelSheet(Sheet sheet, int startRowNum, int endRowNum, int startColNum, int endColNum, List<String> colNames) {
    startRowNum--
    endRowNum--
    startColNum--
    endColNum--

    ExcelValueExtractor ext = new ExcelValueExtractor(sheet)
    List<List<?>> matrix = []
    List<?> rowList
    for (int rowIdx = startRowNum; rowIdx <= endRowNum; rowIdx++) {
      Row row = sheet.getRow(rowIdx)
      if (row == null) {
        log.warn("Row ${rowIdx + 1} is null")
        continue
      }
      rowList = []
      for (int colIdx = startColNum; colIdx <= endColNum; colIdx++) {
        def cell = row.getCell(colIdx)
        if (cell == null) {
          rowList.add(null)
        } else {
          //println("cell[$rowIdx, $colIdx]: ${cell.getCellType()}: val: ${ext.getObject(cell)}")
          switch (cell.getCellType()) {
            case CellType.BLANK -> rowList.add(null)
            case CellType.BOOLEAN -> rowList.add(ext.getBoolean(row, colIdx))
            case CellType.NUMERIC -> {
              if (DateUtil.isCellDateFormatted(cell)) {
                LocalDateTime val = ext.getLocalDateTime(cell)
                if (val != null && val.getHour() == 0 && val.getMinute() == 0 && val.getSecond() == 0 && val.getNano() == 0) {
                  rowList.add(val.toLocalDate())
                } else {
                  rowList.add(val)
                }
              } else {
                rowList.add(ext.getDouble(row, colIdx))
              }
            }
            case CellType.STRING -> rowList.add(ext.getString(row, colIdx))
            case CellType.FORMULA -> rowList.add(ext.getObject(cell))
            default -> rowList.add(ext.getString(row, colIdx))
          }
        }
      }
      matrix.add(rowList)
    }
    return Matrix.builder()
        .matrixName(sheet.getSheetName())
        .columnNames(colNames)
        .rows(matrix)
        .types([Object] * colNames.size())
        .build()
  }
}
