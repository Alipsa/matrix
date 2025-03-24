package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsDataReader

import java.text.NumberFormat

/**
 * Import Calc (ods file)
 */
@CompileStatic
class FOdsImporter {

  static final OdsDataReader odsDataReader = OdsDataReader.create(OdsDataReader.ReaderImpl.EVENT)

  static Matrix importOds(String file, int sheetNumber,
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    return importOds(
        file,
        sheetNumber as int,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  static Matrix importOds(String file, int sheetNumber,
                          Integer startRow = 1, Integer endRow,
                          Integer startCol = 1, Integer endCol,
                          boolean firstRowAsColNames = true) {

    File odsFile = FileUtil.checkFilePath(file)
    Spreadsheet spreadSheet
    try (FileInputStream fis = new FileInputStream(odsFile)) {
      spreadSheet = odsDataReader.readOds(fis, [
          (sheetNumber): [startRow, endRow, startCol, endCol]
      ] as Map<Object, List<Integer>>)
    }
    Sheet sheet = spreadSheet[sheetNumber]
    return buildMatrix(sheet, firstRowAsColNames)
  }

  static Matrix importOds(InputStream is, String sheetName = 'Sheet1',
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {

    return importOds(
        is,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  static Matrix importOds(InputStream is, String sheetName,
                          Integer startRow = 1, Integer endRow,
                          Integer startCol = 1, Integer endCol,
                          boolean firstRowAsColNames = true) {
    def spreadSheet = odsDataReader.readOds(is, [
        (sheetName): [startRow, endRow, startCol, endCol]
    ] as Map<Object, List<Integer>>)
    Sheet sheet = spreadSheet[sheetName]
    return buildMatrix(sheet, firstRowAsColNames)
  }

  static Matrix importOds(InputStream is, Integer sheetNum,
                          Integer startRow = 1, Integer endRow,
                          Integer startCol = 1, Integer endCol,
                          boolean firstRowAsColNames = true) {
    def spreadSheet = odsDataReader.readOds(is, [
        (sheetNum): [startRow, endRow, startCol, endCol]
    ] as Map<Object, List<Integer>>)
    Sheet sheet = spreadSheet[sheetNum]
    return buildMatrix(sheet, firstRowAsColNames)
  }

  static Matrix importOds(URL url, String sheetName = 'Sheet1',
                          int startRow = 1, int endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    try(InputStream is = url.openStream()) {
      importOds(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  static Matrix importOds(URL url, Integer sheetNum,
                          Integer startRow = 1, Integer endRow,
                          String startCol = 'A', String endCol,
                          boolean firstRowAsColNames = true) {
    try(InputStream is = url.openStream()) {
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      def spreadSheet = odsDataReader.readOds(is, [
          (sheetNum): [startRow, endRow, startColNum, endColNum]
      ] as Map<Object, List<Integer>>)
      Sheet sheet = spreadSheet[sheetNum]
      return buildMatrix(sheet, firstRowAsColNames)
    }
  }

  static Matrix importOds(URL url, String sheetName, int startRow = 1, int endRow, int startCol = 1, int endCol, boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      importOds(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
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

  /**
   * As it is possible to address different sections of a Sheet we cannot use the "single pass" approach but must
   * parse the entire thing for each sheet defined. There is room for optimization here.
   * @param is
   * @param sheetParams
   * @param formatOpt
   * @return
   */
  static Map<Object, Matrix> importOdsSheets(URL url, List<Map> sheetParams, NumberFormat format = NumberFormat.getInstance()) {
    Map<Object, Matrix> result = [:]
    byte[] content = IOUtils.toByteArray(url)

    sheetParams.each {
      def sheet = it.getOrDefault(it.sheetName, it.sheetNumber)
      Integer startRow = it.startRow as Integer
      Integer endRow = it.endRow as Integer
      Integer startCol
      if (ValueConverter.isNumeric(it.startCol, format)) {
        startCol = ValueConverter.asInteger(it.startCol)
      } else {
        startCol = SpreadsheetUtil.asColumnNumber(it.startCol as String)
      }
      Integer endCol
      if (ValueConverter.isNumeric(it.endCol, format)) {
        endCol = ValueConverter.asInteger(it.endCol)
      } else {
        endCol = SpreadsheetUtil.asColumnNumber(it.endCol as String)
      }

      Boolean firstRowAsColNames = it.firstRowAsColNames
      Spreadsheet ss
      try (InputStream is = new ByteArrayInputStream(content)) {
        println "Create Spreadsheet $sheet with params ${[startRow, endRow, startCol, endCol]}"
        ss = odsDataReader.readOds(is, [(sheet): [startRow, endRow, startCol, endCol]])
      }
      Matrix matrix = buildMatrix(ss[sheet], firstRowAsColNames)
      def key = it.getOrDefault("key", sheet)
      matrix.setMatrixName(String.valueOf(key))
      result.put(key, matrix)
    }
    result
  }

  static Map<Object, Matrix> importOdsSheets(String fileName, List<Map> sheetParams, NumberFormat format = NumberFormat.getInstance()) {
    File file = FileUtil.checkFilePath(fileName)
    importOdsSheets(file.toURI().toURL(),sheetParams, format)
  }

  private static List<String> buildHeaderRow(Sheet sheet, boolean firstRowHasColumnNames) {
    if (firstRowHasColumnNames) {
     return ListConverter.toStrings(sheet.remove(0))
    } else {
      SpreadsheetUtil.createColumnNames(sheet.first.size())
    }
  }

}
