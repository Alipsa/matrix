package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.fastods.FOdsImporter
import se.alipsa.matrix.spreadsheet.poi.ExcelImporter
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.sods.SOdsImporter

import java.text.NumberFormat

import static se.alipsa.matrix.spreadsheet.ExcelImplementation.*
import static se.alipsa.matrix.spreadsheet.OdsImplementation.*

@CompileStatic
class SpreadsheetImporter {

  /**
   *
   * @param file the ods or excel file to import
   * @param sheet the sheet index (index starting with 1)
   * @param startRow the first row to include (the first row is row 1)
   * @param endRow the last row to include
   * @param startColumn the first column to include (index starting with 1)
   * @param endColumn the last column to include
   * @param firstRowAsColNames whether to treat the first row as a header row for the column names or not,
   *        defaults to true
   * @return A Matrix corresponding to the spreadsheet data.
   */
  static Matrix importSpreadsheet(String file, int sheet,
                                  int startRow = 1, int endRow,
                                  int startColumn = 1, int endColumn,
                                  boolean firstRowAsColNames = true,
                                  ExcelImplementation excelImplementation = FastExcel,
                                  OdsImplementation odsImplementation = FastOdsStream) {
    if (file.toLowerCase().endsWith(".ods")) {
      return importOds(odsImplementation, file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
    sheet = sheet -1
    return switch (excelImplementation) {
      case POI -> ExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
      case FastExcel -> FExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
  }

  private static Matrix importOds(OdsImplementation odsImplementation, String file, int sheet,
                                  int startRow = 1, int endRow,
                                  int startColumn = 1, int endColumn,
                                  boolean firstRowAsColNames = true) {
    return switch(odsImplementation) {
      case SODS -> SOdsImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
      case FastOdsStream -> FOdsImporter.create(FastOdsStream).importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
      case FastOdsEvent -> FOdsImporter.create(FastOdsEvent).importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
  }

  private static Matrix importOds(OdsImplementation odsImplementation, String file, String sheet,
                                  int startRow = 1, int endRow,
                                  int startColumn = 1, int endColumn,
                                  boolean firstRowAsColNames = true) {
    return switch(odsImplementation) {
      case SODS -> SOdsImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
      case FastOdsStream -> FOdsImporter.create(FastOdsStream).importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
      case FastOdsEvent -> FOdsImporter.create(FastOdsEvent).importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
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
                                  boolean firstRowAsColNames = true,
                                  ExcelImplementation excelImplementation = FastExcel,
                                  OdsImplementation odsImplementation = FastOdsStream) {
    int startCol = SpreadsheetUtil.asColumnNumber(startColumn)
    int endCol = SpreadsheetUtil.asColumnNumber(endColumn)
    if (file.toLowerCase().endsWith(".ods")) {
      return importOds(odsImplementation, file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    sheet = sheet -1
    return switch (excelImplementation) {
      case POI -> ExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
      case FastExcel -> FExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
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
                                  boolean firstRowAsColNames = true,
                                  ExcelImplementation excelImplementation = ExcelImplementation.FastExcel,
                                  OdsImplementation odsImplementation = FastOdsStream) {
    if (file.toLowerCase().endsWith(".ods")) {
      return importOds(odsImplementation, file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
    return switch (excelImplementation) {
      case POI -> ExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
      case FastExcel -> FExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
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
                                  boolean firstRowAsColNames = true,
                                  ExcelImplementation excelImplementation = ExcelImplementation.FastExcel,
                                  OdsImplementation odsImplementation = FastOdsStream) {
    if (file.toLowerCase().endsWith(".ods")) {
      int startColumn = SpreadsheetUtil.asColumnNumber(startCol)
      int endColumn = SpreadsheetUtil.asColumnNumber(endCol)
      return importOds(odsImplementation, file, sheet, startRow, endRow, startColumn, endColumn, firstRowAsColNames)
    }
    return switch (excelImplementation) {
      case POI -> ExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
      case FastExcel -> FExcelImporter.create().importSpreadsheet(file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
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
    //println "SpreadsheetImporter.importSpreadsheet: parse = $params"
    Object sheet = params.get("sheet")
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

    ExcelImplementation excelImplementation = params.getOrDefault('excelImplementation', FastExcel) as ExcelImplementation
    OdsImplementation odsImplementation = params.getOrDefault('odsImplementation', FastOdsStream) as OdsImplementation
    //println "SpreadsheetImporter.importSpreadsheet: file=$file, sheet=$sheet, startRow= $startRow, endRow= $endRow, startCol= $startCol, endCol= $endCol, firstRowAsColNames= $firstRowAsColNames"
    if (sheet instanceof Number) {
      //println "importing sheet number $sheet"
      return importSpreadsheet(file,
          sheet as int,
          startRow as int,
          endRow as int,
          startCol as int,
          endCol as int,
          firstRowAsColNames as boolean,
          excelImplementation,
          odsImplementation)
    }
    //println "importing sheet name $sheet"
    return importSpreadsheet(
        file,
        sheet as String,
        startRow as int,
        endRow as int,
        startCol as int,
        endCol as int,
        firstRowAsColNames as boolean,
        excelImplementation,
        odsImplementation
    )
  }

  static Map<Object, Matrix> importSpreadsheets(String fileName,
                                                List<Map> sheetParams,
                                                ExcelImplementation excelImplementation = FastExcel,
                                                OdsImplementation odsImplementation = FastOdsStream,
                                                NumberFormat... formatOpt) {
    if (fileName.toLowerCase().endsWith(".ods")) {
      //return SOdsImporter.importOdsSheets(fileName, sheetParams, formatOpt)
      NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
      return FOdsImporter.create(odsImplementation).importSpreadsheets(fileName, sheetParams, format)
    }
    return switch (excelImplementation) {
      case POI -> ExcelImporter.create().importSpreadsheets(fileName, sheetParams, formatOpt)
      case FastExcel -> FExcelImporter.create().importSpreadsheets(fileName, sheetParams, formatOpt)
    }
  }

  static void validateNotNull(Object paramVal, String paramName) {
    if (paramVal == null) {
      throw new IllegalArgumentException("$paramName cannot be null")
    }
  }
}
