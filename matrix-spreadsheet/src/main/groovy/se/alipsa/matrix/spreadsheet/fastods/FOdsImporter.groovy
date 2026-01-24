package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.Importer
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsDataReader

import java.text.NumberFormat

/**
 * Import Calc (ods file)
 */
@CompileStatic
class FOdsImporter implements Importer {

  OdsDataReader odsDataReader

  static FOdsImporter create(OdsDataReader odsDataReader = OdsDataReader.create()) {
    new FOdsImporter(odsDataReader)
  }

  FOdsImporter(OdsDataReader reader) {
    odsDataReader = reader
  }

  @Override
  Matrix importSpreadsheet(String file, int sheetNumber,
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    return importSpreadsheet(
        file,
        sheetNumber as int,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  @Override
  Matrix importSpreadsheet(String file, int sheetNumber,
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    //println("FOdsImporter.importOds: file=$file, sheetNumber=$sheetNumber, startRow=$startRow, endRow=$endRow, startCol=$startCol, endCol=$endCol, firstRowAsColNames=$firstRowAsColNames")
    File odsFile = FileUtil.checkFilePath(file)
    Sheet sheet
    try (FileInputStream fis = new FileInputStream(odsFile)) {
      sheet = odsDataReader.readOds(fis,
          sheetNumber, startRow, endRow, startCol, endCol)
    }
    return buildMatrix(sheet, firstRowAsColNames)
  }


  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                          int startRow = 1, int endRow,
                          int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    File odsFile = FileUtil.checkFilePath(file)
    Sheet sheet
    try (FileInputStream fis = new FileInputStream(odsFile)) {
      sheet = odsDataReader.readOds(fis,
          sheetName, startRow, endRow, startCol, endCol)
    }
    return buildMatrix(sheet, firstRowAsColNames)
  }

  @Override
  Matrix importSpreadsheet(String file, String sheetName, int startRow, int endRow, String startCol, String endCol, boolean firstRowAsColNames) {
    importSpreadsheet(file,
        sheetName,
        startRow,
        endRow,
        SpreadsheetUtil.asColumnNumber(startCol),
        SpreadsheetUtil.asColumnNumber(endCol),
        firstRowAsColNames)
  }

  @Override
  Matrix importSpreadsheet(InputStream is, String sheetName = 'Sheet1',
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {

    return importSpreadsheet(
        is,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  @Override
  Matrix importSpreadsheet(InputStream is, String sheetName,
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    def sheet = odsDataReader.readOds(is, sheetName, startRow, endRow, startCol, endCol)
    return buildMatrix(sheet, firstRowAsColNames)
  }

  @Override
  Matrix importSpreadsheet(InputStream is, int sheetNum,
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    def sheet = odsDataReader.readOds(is, sheetNum, startRow, endRow, startCol, endCol)
    return buildMatrix(sheet, firstRowAsColNames)
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNum,
                           int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    try(InputStream is = url.openStream()) {
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      def sheet = odsDataReader.readOds(is, sheetNum, startRow, endRow, startColNum, endColNum)
      return buildMatrix(sheet, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName, int startRow = 1, int endRow, int startCol = 1, int endCol, boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  Matrix importSpreadsheet(InputStream is, Map params, NumberFormat... formatOpt) {
    def sheet = params.sheetName
    if (sheet == null) {
      sheet = params.sheetNumber
    }
    if (sheet == null) {
      throw new IllegalArgumentException("Sheet name or number must be provided but was null")
    }
    Integer startRow = params.startRow as Integer
    Integer endRow = params.endRow as Integer
    Integer startCol
    if (ValueConverter.isNumeric(params.startCol, formatOpt)) {
      startCol = ValueConverter.asInteger(params.startCol)
    } else {
      startCol = SpreadsheetUtil.asColumnNumber(params.startCol as String)
    }
    Integer endCol
    if (ValueConverter.isNumeric(params.endCol, formatOpt)) {
      endCol = ValueConverter.asInteger(params.endCol)
    } else {
      endCol = SpreadsheetUtil.asColumnNumber(params.endCol as String)
    }

    Boolean firstRowAsColNames = params.firstRowAsColNames
    Sheet ss = odsDataReader.readOds(is, sheet, startRow, endRow, startCol, endCol)

    Matrix matrix = buildMatrix(ss, firstRowAsColNames)
    def key = params["key"] ?: sheet
    matrix.setMatrixName(String.valueOf(key))
    matrix
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNumber, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetNumber, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    byte[] bytes = IOUtils.toByteArray(is)
    Map<Object, Matrix> result = [:]
    sheetParams.each {
      try (InputStream is2 = new ByteArrayInputStream(bytes)) {
        Matrix matrix = importSpreadsheet(is2, it, formatOpt)
        def sheet = it.sheetName ?: it.sheetNumber
        def key = it["key"] ?: sheet
        result.put(key, matrix)
      }
    }
    result
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(URL url, List<Map> sheetParams, NumberFormat... formatOpt) {
    try (InputStream is = url.openStream()) {
      importSpreadsheets(is, sheetParams, formatOpt)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(String fileName, List<Map> sheetParams, NumberFormat... formatOpt) {
    File file = FileUtil.checkFilePath(fileName)
    importSpreadsheets(file.toURI().toURL(), sheetParams, formatOpt)
  }

  static Matrix buildMatrix(Sheet sheet, boolean firstRowAsColNames) {
    if (sheet == null) {
      throw new IllegalArgumentException("sheet is null, impossible to build a Matrix from that")
    }
    if (sheet.size() == 0) {
      println("sheet ${sheet?.sheetName} is empty, nothing to do, return Matrix as null")
      return null
    }
    def header = buildHeaderRow(sheet, firstRowAsColNames)
    Matrix.builder()
    .matrixName(sheet.sheetName)
    .columnNames(header)
    .rows(sheet as List<List>)
    .build()
  }


  private static List<String> buildHeaderRow(Sheet sheet, boolean firstRowHasColumnNames) {
    if (firstRowHasColumnNames) {
      return ListConverter.toStrings(sheet.remove(0))
    } else {
      return SpreadsheetUtil.createColumnNames(sheet.first().size())
    }
  }
}
