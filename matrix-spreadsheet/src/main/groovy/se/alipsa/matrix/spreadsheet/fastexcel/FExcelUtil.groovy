package se.alipsa.matrix.spreadsheet.fastexcel

import org.dhatim.fastexcel.reader.*

import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.util.stream.Stream

/**
 * Utility methods for inspecting Excel (.xlsx) cell formatting and metadata.
 */
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

  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  static Map<String, ?> getFormat(InputStream is, String sheetName, String columnName, int rowNumber) {
    try (ReadableWorkbook wb = new ReadableWorkbook(is, FExcelImporter.OPTIONS)) {
      Sheet sheet = wb.findSheet(sheetName).orElseThrow()
      Row row
      try (def rows = sheet.openStream()) {
        row = rows.find { Row r -> r.rowNum == rowNumber } as Row
      }
      if (row == null) {
        return null
      }
      Cell cell = row.getCell(new CellAddress("$columnName$rowNumber"))
      if (cell == null) {
        return null
      }
      [cellAddress: cell.address, formatId: cell.dataFormatId, formatString: cell.dataFormatString, rawValue: cell.rawValue]
    }
  }

  static Row getRow(Sheet sheet, int rowIdx) {
    int rowNum = rowIdx + 1
    try (Stream<Row> rows = sheet.openStream()) {
      rows.filter { Row row -> row.rowNum == rowNum }.findFirst().orElse(null)
    }
  }

  /**
   * Null-safe cell lookup.
   *
   * @return null when the row is null, the index is negative, or the row has no cell at the index
   */
  static Cell cellAt(Row row, int index) {
    row == null || index < 0 ? null : row.getOptionalCell(index).orElse(null)
  }

}
