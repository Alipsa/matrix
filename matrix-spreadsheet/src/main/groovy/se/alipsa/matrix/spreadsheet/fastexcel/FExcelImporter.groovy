package se.alipsa.matrix.spreadsheet.fastexcel

import groovy.transform.CompileStatic
import org.dhatim.fastexcel.reader.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.text.NumberFormat
import java.util.stream.Stream

@CompileStatic
class FExcelImporter {

  static final ReadingOptions OPTIONS = new ReadingOptions(true, true)

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

  static Matrix importExcel(URL url, int sheetNumber,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    if (url == null) throw new IllegalArgumentException("url cannot be null")
    try(InputStream is = url.openStream(); ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Sheet sheet = workbook.getSheet(sheetNumber - 1).orElse(null)
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startColNum, endColNum, firstRowAsColNames, isDate1904)
    }
  }

  static Matrix importExcel(URL url, String sheetName = 'Sheet1',
                            int startRow, int endRow,
                            int startCol, int endCol,
                            boolean firstRowAsColNames = true) {
    try(InputStream is = url.openStream()) {
      importExcel(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  static Matrix importExcel(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    try(InputStream is = url.openStream()) {
      importExcel(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
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
    File excelFile = FileUtil.checkFilePath(file)
    try (ReadableWorkbook workbook = new ReadableWorkbook(excelFile, OPTIONS)) {
      Sheet sheet = workbook.getSheet(sheetNumber).orElse(null)
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
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
    File excelFile = FileUtil.checkFilePath(file)
    try (ReadableWorkbook workbook = new ReadableWorkbook(excelFile, OPTIONS)) {
      Sheet sheet = workbook.findSheet(sheetName).orElseThrow()
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  static Matrix importExcel(InputStream is, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Sheet sheet = workbook.findSheet(sheetName).orElseThrow()
      boolean isDate1904 = workbook.isDate1904()
      def m = importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
      return m
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
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Map<String, Matrix> result = [:]
      sheetParams.each {
        String sheetName = it.sheetName
        Sheet sheet = workbook.findSheet(sheetName).orElseThrow()
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
        boolean isDate1904 = workbook.isDate1904()
        Matrix matrix = importExcelSheet(sheet, startRow, it.endRow as int, startCol, endCol, it.firstRowAsColNames as Boolean, isDate1904)
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

  private static Matrix importExcelSheet(Sheet sheet, int startRowNum, int endRowNum, int startColNum, int endColNum, boolean firstRowAsColNames, boolean isDate1904) {
    //println "Importing sheet ${sheet.name}, startRowNum = $startRowNum, endRowNum = $endRowNum"
    //int startRowNumZI = startRowNum - 1
    //int endRowNumZI = endRowNum - 1
    FExcelValueExtractor ext = new FExcelValueExtractor(sheet, isDate1904)
    int startColNumZI = startColNum - 1
    int endColNumZI = endColNum - 1
    List<String> colNames = []
    List<List<?>> matrix = []
    List<?> rowList
    int ncol = endColNum - startColNum + 1 // both start and end are included
    try (Stream<Row> rows = sheet.openStream()) {
      rows.each { Row row ->
        //println "on row $row.rowNum, row.rowNum >= startRowNum = ${row.rowNum >= startRowNum} row.rowNum <= endRowNum = ${row.rowNum <= endRowNum}"
        if (row.rowNum >= startRowNum && row.rowNum <= endRowNum) {
          rowList = []
          List list = row.asList()
          if (colNames.size() == 0) {
            //println "Building header"
            // TODO refactor build header to either use first row or create column names
            if (firstRowAsColNames) {
              list.eachWithIndex { Cell cell, int i ->
                if (i >= startColNumZI && i <= endColNumZI) {
                  colNames.add(cell.rawValue)
                }
              }
              return
            } else {
              for (int i = 1; i <= endColNum - startColNum + 1; i++) {
                colNames.add("c$i".toString())
              }
            }
          }
          list.eachWithIndex { cell, cIdx ->
            //println "row $cIdx adding " + cell?.rawValue
            if (cIdx >= startColNumZI && cIdx <= endColNumZI) {
              if (cell == null) {
                rowList.add(null)
              } else {
                rowList.add(ext.getObject(cell))
              }
            }
          }
          if (rowList.size() < ncol) {
            //println("padding null to rowList rowList.size() = ${rowList.size()}, ncol=$ncol")
            def padSize = ncol - rowList.size()
            (1..padSize).each {
              //print "."
              rowList << null
            }
            //println()
          }
          //println row.rowNum + ": " + rowList
          matrix.add(rowList)
        }
      }
    }
    Matrix m = Matrix.builder()
        .matrixName(sheet.name)
        .columnNames(colNames)
        .rows(matrix)
        .types([Object] * colNames.size())
        .build()
    m.metaData.isDate1904 = isDate1904
    m
  }
}
