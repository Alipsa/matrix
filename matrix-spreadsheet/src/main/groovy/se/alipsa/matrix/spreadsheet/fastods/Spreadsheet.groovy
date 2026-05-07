package se.alipsa.matrix.spreadsheet.fastods

/**
 * Represents an ODS workbook as an ordered map of sheet identifiers to {@link Sheet} instances.
 */
class Spreadsheet extends LinkedHashMap<Object, Sheet> {
  void add(String sheetName, Sheet sheet) {
    put(sheetName, sheet)
  }
  void add(Integer sheetIndex, Sheet sheet) {
    put(sheetIndex, sheet)
  }
}
