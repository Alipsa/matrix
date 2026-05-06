package se.alipsa.matrix.spreadsheet.fastexcel

import org.dhatim.fastexcel.reader.Cell
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import org.dhatim.fastexcel.reader.Sheet

import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.util.stream.Stream

class FExcelReader implements SpreadsheetReader {
  private ReadableWorkbook workbook

  FExcelReader(File excelFile) {
    SpreadsheetUtil.rejectLegacyXls(excelFile.name)
    workbook = new ReadableWorkbook(excelFile, FExcelImporter.OPTIONS)
  }

  FExcelReader(String filePath) {
    this(FileUtil.checkFilePath(filePath))
  }

  /**
   * @return a List of the names of the sheets in the excel
   */
  @Override
  List<String> getSheetNames() throws IOException {
    getSheetNames(workbook)
  }

  static List<String> getSheetNames(ReadableWorkbook workbook) throws IOException {
    workbook.sheets.collect { Sheet it -> it.name }
  }

  /**
   * Find the first row index matching the content.
   * @param sheetNumber the sheet index (1 indexed)
   * @param colNumber the column number (1 indexed)
   * @param content the string to search for
   * @return the Row as seen in Excel (1 is first row)
   * @throws IOException if reading the workbook fails
   */
  @Override
  int findRowNum(int sheetNumber, int colNumber, String content) {
    Sheet sheet = workbook.getSheet(sheetNumber - 1)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetNumber"))
    findRowNum(sheet, colNumber, content)
  }

  /**
   * @param sheetName the name of the sheet
   * @param colNumber the column number (1 indexed)
   * @param content the string to search for
   * @return the Row as seen in Excel (1 is first row)
   */
  @Override
  int findRowNum(String sheetName, int colNumber, String content) {
    Sheet sheet = workbook.findSheet(sheetName)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetName"))
    findRowNum(sheet, colNumber, content)
  }

  @Override
  int findRowNum(int sheetNumber, String colName, String content) throws IOException {
    Sheet sheet = workbook.getSheet(sheetNumber - 1)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetNumber"))
    findRowNum(sheet, SpreadsheetUtil.asColumnNumber(colName), content)
  }

  /**
   * Find the first row index matching the content.
   * @param sheetName the name of the sheet to search in
   * @param colName the column name (A for first column etc.)
   * @param content the string to search for
   * @return the Row as seen in Excel (1 is first row)
   */
  @Override
  int findRowNum(String sheetName, String colName, String content) {
    Sheet sheet = workbook.findSheet(sheetName)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetName"))
    findRowNum(sheet, SpreadsheetUtil.asColumnNumber(colName), content)
  }

  /**
   *
   * @param sheet the sheet to search in
   * @param colNumber the column number (1 indexed)
   * @param content the string to search for
   * @return the Row as seen in Excel (1 is first row) or -1 if not found or sheet is null
   */
  static int findRowNum(Sheet sheet, int colNumber, String content) {
    if (sheet == null) return -1
    int poiColNum = colNumber - 1
    int rowNum = -1
    try (Stream<Row> rows = sheet.openStream()) {
      rows.each { Row row ->
        Cell cell = row.getCell(poiColNum)
        if (cell != null && content == cell.getRawValue()) {
          rowNum = row.getRowNum()
          return
        }
      }
    }
    rowNum
  }

  /**
   * Find the first column index matching the content criteria
   * @param sheetNumber the sheet index (1 indexed)
   * @param rowNumber the row number (1 indexed)
   * @param content the string to search for
   * @return the row number that matched or -1 if not found
   */
  @Override
  int findColNum(int sheetNumber, int rowNumber, String content) {
    Sheet sheet = workbook.getSheet(sheetNumber - 1)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetNumber"))
    findColNum(sheet, rowNumber, content)
  }

  /** return the column as seen in excel (e.g. using column(), 1 is the first column etc
   * @param sheetName the name of the sheet
   * @param rowNumber the row number (1 indexed)
   * @param content the string to search for
   * @return the row number that matched or -1 if not found
   */
  @Override
  int findColNum(String sheetName, int rowNumber, String content) {
    Sheet sheet = workbook.findSheet(sheetName).orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetName"))
    findColNum(sheet, rowNumber, content)
  }

  /**
   * Find the first column index matching the content criteria
   * @param sheet the Sheet to search
   * @param rowNumber the row number (1 indexed)
   * @param content the string to search for
   * @return return the column as seen in excel (e.g. using column(), 1 is the first column etc
   */
  int findColNum(Sheet sheet, int rowNumber, String content) {
    if (content == null) return -1
    FExcelValueExtractor ext = new FExcelValueExtractor(sheet, workbook.isDate1904())
    Row row = FExcelUtil.getRow(sheet, rowNumber - 1)
    if (row == null) return -1
    for (int colNum = 0; colNum < row.cellCount; colNum++) {
      Cell cell = row.getCell(colNum)
      if (content == ext.getString(cell)) {
        return colNum + 1
      }
    }
    -1
  }

  @Override
  int findLastRow(int sheetNum) {
    findLastRow(workbook.getSheet(sheetNum - 1)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetNum")))
  }

  @Override
  int findLastRow(String sheetName) {
    def sheet = workbook.findSheet(sheetName)
        .orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetName"))
    findLastRow(sheet)
  }

  static int findLastRow(Sheet sheet) {
    try (Stream<Row> rows = sheet.openStream()) {
      rows.count() as int
    }
  }

  @Override
  int findLastCol(int sheetNum) {
    findLastCol(workbook.getSheet(sheetNum - 1).orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetNum")))
  }

  @Override
  int findLastCol(String sheetName) {
    findLastCol(workbook.findSheet(sheetName).orElseThrow(() -> new IllegalArgumentException("Failed to find sheet $sheetName")))
  }

  static int findLastCol(Sheet sheet, int numRowsToScan = 10) {
    int maxColumn = -1
    try (Stream<Row> rows = sheet.openStream()) {
      rows.limit(numRowsToScan + 1L).each { Row row ->
        int nCol = row.size()
        if (nCol > maxColumn) {
          maxColumn = nCol
        }
      }
    }
    maxColumn
  }

  @Override
  void close() throws IOException {
    if (workbook != null) {
      workbook.close()
      workbook = null
    }
  }
}
