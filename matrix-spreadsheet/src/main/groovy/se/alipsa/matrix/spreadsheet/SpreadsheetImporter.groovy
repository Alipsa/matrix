package se.alipsa.matrix.spreadsheet


import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.excel.ExcelImporter
import se.alipsa.matrix.spreadsheet.ods.OdsImporter

class SpreadsheetImporter {

  /**
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet index (index starting with 1)
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startColumn the first column to include (index starting with 1)
   * @param endColumn the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for thecolumn names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, int sheet,
                                  int startRow = 1, int endRow,
                                  int startColumn = 1, int endColumn,
                                  boolean firstRowAsColNames = true) {
    sheet = sheet -1
    if (file.toLowerCase().endsWith(".ods")) {
      return OdsImporter.importOds(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
    return ExcelImporter.importExcel(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
  }

  /**
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet index (index starting with 1)
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startColumn the first column name to include (A is the first column name etc.)
   * @param endColumn the last column name to include
   * @param firstRowAsColNames whether to treat the first row as a header row for thecolumn names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, int sheet,
                                       int startRow = 1, int endRow,
                                       String startColumn = 'A', String endColumn,
                                       boolean firstRowAsColNames = true) {
    sheet = sheet -1
    if (file.toLowerCase().endsWith(".ods")) {
      return OdsImporter.importOds(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
    return ExcelImporter.importExcel(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
  }

  /**
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet name, defaults to Sheet1
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startColumn the first column to include (index starting with 1)
   * @param endColumn the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for thecolumn names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, String sheet = 'Sheet1',
                                      int startRow = 1, int endRow,
                                      int startCol = 1, int endCol,
                                      boolean firstRowAsColNames = true) {
    if (file.toLowerCase().endsWith(".ods")) {
      return OdsImporter.importOds(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    return ExcelImporter.importExcel(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet name, defaults to Sheet1
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startColumn the first column name to include (A is the first column name etc.)
   * @param endColumn the last column name to include
   * @param firstRowAsColNames whether to treat the first row as a header row for thecolumn names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, String sheet = 'Sheet1',
                                      int startRow = 1, int endRow,
                                      String startCol = 'A', String endCol,
                                      boolean firstRowAsColNames = true) {
    if (file.toLowerCase().endsWith(".ods")) {
      return OdsImporter.importOds(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    return ExcelImporter.importExcel(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   *
   * @param params the named parameters i.e.
   * file (a File or a String path),
   * sheet (the sheet tab to import, an integer of String),
   * startRow (an Integer),
   * endRow (and integer),
   * startCol (an Integer or String),
   * endCol (an Integer or String),
   * firstRowAsColNames (a boolean)
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(Map params) {
    def fp = params.getOrDefault('file', null)
    validateNotNull(fp, 'file')
    String file
    if (fp instanceof File) {
      file = fp.getAbsolutePath()
    } else {
      file = String.valueOf(fp)
    }
    println "importSpreadsheet, parase = $params"
    def sheet = params.get("sheet")
    if (sheet == null) {
      sheet = params.get("sheetNumber")
      if (sheet == null) {
        sheet = params.getOrDefault('sheetName', 1)
      }
    }
    validateNotNull(sheet, 'sheet')
    Integer startRow = params.getOrDefault('startRow',1) as Integer
    validateNotNull(startRow, 'startRow')
    Integer endRow = params.getOrDefault('endRow', null) as Integer
    validateNotNull(endRow, 'endRow')
    def startCol = params.getOrDefault('startCol', 1)
    if (startCol instanceof String) {
      startCol = SpreadsheetUtil.asColumnNumber(startCol)
    }
    validateNotNull(startCol, 'startCol')
    def endCol = params.get('endCol')
    if (endCol instanceof String) {
      endCol = SpreadsheetUtil.asColumnNumber(endCol)
    }
    validateNotNull(endCol, 'endCol')
    Boolean firstRowAsColNames = params.getOrDefault('firstRowAsColNames', true) as Boolean
    validateNotNull(firstRowAsColNames, 'firstRowAsColNames')

    if (sheet instanceof Number) {
      return importSpreadsheet(file,
          sheet as int,
          startRow as int,
          endRow as int,
          startCol as int,
          endCol as int,
          firstRowAsColNames as boolean)
    }
    return importSpreadsheet(
        file,
        sheet as String,
        startRow as int,
        endRow as int,
        startCol as int,
        endCol as int,
        firstRowAsColNames as boolean
    )
  }

  static void validateNotNull(Object paramVal, String paramName) {
    if (paramVal == null) {
      throw new IllegalArgumentException("$paramName cannot be null")
    }
  }
}
