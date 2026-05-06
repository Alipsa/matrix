package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastods.FOdsImporter

import java.text.NumberFormat

/**
 * Static facade for importing spreadsheet data into Matrix objects.
 *
 * <p>Auto-detects the file format (.ods vs .xlsx) and delegates to the
 * appropriate importer ({@link FOdsImporter} or {@link FExcelImporter}).</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * // Import by sheet index (1-based)
 * Matrix data = SpreadsheetImporter.importSpreadsheet("data.xlsx", 1, 1, 100, 1, 5)
 *
 * // Import by sheet name
 * Matrix data = SpreadsheetImporter.importSpreadsheet("data.xlsx", "Sales", 1, 100, 1, 5)
 *
 * // Import using named parameters
 * Matrix data = SpreadsheetImporter.importSpreadsheet(
 *   file: "data.xlsx", sheet: 1, endRow: 100, endCol: 5)
 * </pre>
 *
 * @see SpreadsheetWriter
 * @see FExcelImporter
 * @see FOdsImporter
 */
@CompileStatic
class SpreadsheetImporter {

  /**
   * Import a spreadsheet by file path and sheet index.
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet index (index starting with 1)
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startCol the first column to include (index starting with 1)
   * @param endCol the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, int sheet,
                                  int startRow = 1, int endRow,
                                  int startCol = 1, int endCol,
                                  boolean firstRowAsColNames = true) {
    if (isOdsFile(file)) {
      return odsImporter().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    SpreadsheetUtil.ensureXlsx(file)
    return excelImporter()
        .importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Import an ODS spreadsheet from a URL by sheet index.
   *
   * @param url the URL of the ods file to import
   * @param sheet the sheet index (index starting with 1)
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startCol the first column to include (index starting with 1)
   * @param endCol the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importOds(URL url, int sheet,
                          int startRow = 1, int endRow,
                          int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {

    odsImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  static Matrix importOds(URL url, int sheet,
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {

    odsImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Import an ODS spreadsheet from a URL by sheet name.
   *
   * @param url the URL of the ods file to import
   * @param sheet the sheet name
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startCol the first column to include (index starting with 1)
   * @param endCol the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importOds(URL url, String sheet,
                          int startRow = 1, int endRow,
                          int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    odsImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  static Matrix importOds(URL url, String sheet,
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    odsImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  static Matrix importExcel(URL url, int sheet,
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.ensureXlsx(url?.path)
    excelImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  static Matrix importExcel(URL url, String sheet,
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.ensureXlsx(url?.path)
    excelImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  static Matrix importExcel(URL url, int sheet,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.ensureXlsx(url?.path)
    excelImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  static Matrix importExcel(URL url, String sheet,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {

    SpreadsheetUtil.ensureXlsx(url?.path)
    excelImporter().importSpreadsheet(url, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Import a spreadsheet by file path and sheet index using column names.
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet index (index starting with 1)
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startCol the first column name to include (A is the first column name etc.)
   * @param endCol the last column name to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, int sheet,
                                  int startRow = 1, int endRow,
                                  String startCol = 'A', String endCol,
                                  boolean firstRowAsColNames = true) {
    if (isOdsFile(file)) {
      return odsImporter().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    SpreadsheetUtil.ensureXlsx(file)
    return excelImporter()
        .importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Import a spreadsheet by file path and sheet name.
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet name, defaults to 'Sheet1'
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startCol the first column to include (index starting with 1)
   * @param endCol the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, String sheet = 'Sheet1',
                                  int startRow = 1, int endRow,
                                  int startCol = 1, int endCol,
                                  boolean firstRowAsColNames = true) {
    if (isOdsFile(file)) {
      return odsImporter().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    SpreadsheetUtil.ensureXlsx(file)
    excelImporter()
        .importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Import a spreadsheet by file path and sheet name using column names.
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet name, defaults to 'Sheet1'
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startCol the first column name to include (A is the first column name etc.)
   * @param endCol the last column name to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, String sheet = 'Sheet1',
                                  int startRow = 1, int endRow,
                                  String startCol = 'A', String endCol,
                                  boolean firstRowAsColNames = true) {
    if (isOdsFile(file)) {
      int startColumn = SpreadsheetUtil.asColumnNumber(startCol)
      int endColumn = SpreadsheetUtil.asColumnNumber(endCol)
      return odsImporter()
          .importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
    SpreadsheetUtil.ensureXlsx(file)
    excelImporter()
        .importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  // --- File-based overloads ---

  /**
   * Delegates to {@link #importSpreadsheet(String, int, int, int, int, int, boolean)}
   * after resolving the absolute path.
   */
  static Matrix importSpreadsheet(File file, int sheet,
                                  int startRow = 1, int endRow,
                                  int startCol = 1, int endCol,
                                  boolean firstRowAsColNames = true) {
    importSpreadsheet(file.absolutePath, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Delegates to {@link #importSpreadsheet(String, String, int, int, int, int, boolean)}
   * after resolving the absolute path.
   */
  static Matrix importSpreadsheet(File file, String sheet,
                                  int startRow = 1, int endRow,
                                  int startCol = 1, int endCol,
                                  boolean firstRowAsColNames = true) {
    importSpreadsheet(file.absolutePath, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Delegates to {@link #importSpreadsheet(String, int, int, int, String, String, boolean)}
   * after resolving the absolute path.
   */
  static Matrix importSpreadsheet(File file, int sheet,
                                  int startRow = 1, int endRow,
                                  String startCol = 'A', String endCol,
                                  boolean firstRowAsColNames = true) {
    importSpreadsheet(file.absolutePath, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  /**
   * Delegates to {@link #importSpreadsheet(String, String, int, int, String, String, boolean)}
   * after resolving the absolute path.
   */
  static Matrix importSpreadsheet(File file, String sheet,
                                  int startRow = 1, int endRow,
                                  String startCol = 'A', String endCol,
                                  boolean firstRowAsColNames = true) {
    importSpreadsheet(file.absolutePath, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
  }

  // --- Whole-sheet convenience imports (auto-detect dimensions) ---

  /**
   * Import an entire spreadsheet sheet using auto-detected dimensions.
   *
   * @param file the spreadsheet file to import
   * @param sheetNumber the sheet index (1-based), defaults to 1
   * @param firstRowAsColNames whether to treat the first row as column names
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(File file, int sheetNumber = 1, boolean firstRowAsColNames = true) {
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      int endRow = reader.findLastRow(sheetNumber)
      int endCol = reader.findLastCol(sheetNumber)
      return importSpreadsheet(file.absolutePath, sheetNumber, 1, endRow, 1, endCol, firstRowAsColNames)
    }
  }

  /**
   * Import an entire spreadsheet sheet using auto-detected dimensions.
   *
   * @param file the spreadsheet file path to import
   * @param sheetNumber the sheet index (1-based), defaults to 1
   * @param firstRowAsColNames whether to treat the first row as column names
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, int sheetNumber = 1, boolean firstRowAsColNames = true) {
    File spreadsheetFile = FileUtil.checkFilePath(file)
    SpreadsheetReader.Factory.create(spreadsheetFile).withCloseable { SpreadsheetReader reader ->
      int endRow = reader.findLastRow(sheetNumber)
      int endCol = reader.findLastCol(sheetNumber)
      return importSpreadsheet(file, sheetNumber, 1, endRow, 1, endCol, firstRowAsColNames)
    }
  }

  /**
   * Import an entire spreadsheet sheet by name using auto-detected dimensions.
   *
   * @param file the spreadsheet file path to import
   * @param sheetName the sheet name
   * @param firstRowAsColNames whether to treat the first row as column names
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, String sheetName, boolean firstRowAsColNames = true) {
    File spreadsheetFile = FileUtil.checkFilePath(file)
    SpreadsheetReader.Factory.create(spreadsheetFile).withCloseable { SpreadsheetReader reader ->
      int endRow = reader.findLastRow(sheetName)
      int endCol = reader.findLastCol(sheetName)
      return importSpreadsheet(file, sheetName, 1, endRow, 1, endCol, firstRowAsColNames)
    }
  }

  /**
   * Import an entire spreadsheet sheet by name using auto-detected dimensions.
   *
   * @param file the spreadsheet file to import
   * @param sheetName the sheet name
   * @param firstRowAsColNames whether to treat the first row as column names
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(File file, String sheetName, boolean firstRowAsColNames = true) {
    importSpreadsheet(file.absolutePath, sheetName, firstRowAsColNames)
  }

  // --- Map-based import ---

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
    def fp = params['file']
    validateNotNull(fp, 'file')
    String file
    if (fp instanceof File) {
      file = fp.getAbsolutePath()
    } else {
      file = String.valueOf(fp)
    }
    Object sheet = params.get("sheet")
    if (sheet == null) {
      sheet = params.get("sheetNumber")
      if (sheet == null) {
        sheet = params['sheetName'] ?: 1
      }
    }
    validateNotNull(sheet, 'sheet')
    Integer startRow = params['startRow'] as Integer ?: 1
    validateNotNull(startRow, 'startRow')
    Integer endRow = params['endRow'] as Integer
    validateNotNull(endRow, 'endRow')
    def startCol = params['startCol'] ?: 1
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

  static Map<Object, Matrix> importSpreadsheets(File file,
                                                List<Map> sheetParams,
                                                NumberFormat... formatOpt) {
    importSpreadsheets(file.absolutePath, sheetParams, formatOpt)
  }

  static Map<Object, Matrix> importSpreadsheets(String fileName,
                                                List<Map> sheetParams,
                                                NumberFormat... formatOpt) {
    if (isOdsFile(fileName)) {
      NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
      return odsImporter().importSpreadsheets(fileName, sheetParams, format)
    }
    SpreadsheetUtil.ensureXlsx(fileName)
    return excelImporter().importSpreadsheets(fileName, sheetParams, formatOpt)
  }

  static Map<Object, Matrix> importOdsSheets(URL url,
                                                List<Map> sheetParams,
                                                NumberFormat... formatOpt) {
      NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
      return odsImporter().importSpreadsheets(url, sheetParams, format)
  }

  static Map<Object, Matrix> importExcelSheets(URL url,
                                                List<Map> sheetParams,
                                                NumberFormat... formatOpt) {
    excelImporter()
        .importSpreadsheets(url, sheetParams, formatOpt)
  }

  private static void validateNotNull(Object paramVal, String paramName) {
    if (paramVal == null) {
      throw new IllegalArgumentException("$paramName cannot be null")
    }
  }

  private static Importer odsImporter() {
    return FOdsImporter.create()
  }

  private static Importer excelImporter() {
    return FExcelImporter.create()
  }

  private static boolean isOdsFile(String fileName) {
    return fileName?.toLowerCase()?.endsWith(".ods") ?: false
  }

}
