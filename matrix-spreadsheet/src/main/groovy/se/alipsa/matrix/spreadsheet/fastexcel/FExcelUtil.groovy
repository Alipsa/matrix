package se.alipsa.matrix.spreadsheet.fastexcel

import org.dhatim.fastexcel.reader.*
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.util.concurrent.atomic.AtomicInteger

class FExcelUtil {

  private FExcelUtil() {
    // only static methods
  }

  static Map<String, ?> getFormat(File file, String sheetName, String columnName, int rowNumber) {
    try(InputStream is = new FileInputStream(file)) {
      return getFormat(is, sheetName, columnName, rowNumber)
    }
  }

  static Map<String, ?> getFormat(URL url, String sheetName, String columnName, int rowNumber) {
    try(InputStream is = url.openStream()) {
      return getFormat(is, sheetName, columnName, rowNumber)
    }
  }

  static Map<String, ?> getFormat(InputStream is, String sheetName, int columnNumber, int rowNumber) {
    getFormat(is, sheetName, SpreadsheetUtil.asColumnName(columnNumber), rowNumber)
  }

  static Map<String, ?> getFormat(InputStream is, String sheetName, String columnName, int rowNumber) {
    ReadableWorkbook wb = new ReadableWorkbook(is, FExcelImporter.OPTIONS)
    Sheet sheet = wb.findSheet(sheetName).orElseThrow()
    List<Row> rowList = sheet.read()
    Row row = sheet.openStream().find { row ->
      if (row.rowNum == rowNumber) {
        return true
      }
      return false
    } as Row
    Cell cell = row.getCell(new CellAddress("$columnName$rowNumber"))
    if (cell == null) return null
    [cellAddress: cell.address, formatId: cell.dataFormatId, formatString: cell.dataFormatString, rawValue: cell.rawValue]
  }

  static Row getRow(Sheet sheet, int rowIdx) {
    AtomicInteger index = new AtomicInteger()
    sheet.openStream().find(n -> rowIdx == index.getAndIncrement()) as Row
  }
}
