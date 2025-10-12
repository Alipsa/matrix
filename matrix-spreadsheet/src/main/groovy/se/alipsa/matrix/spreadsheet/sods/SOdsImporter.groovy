package se.alipsa.matrix.spreadsheet.sods


import com.github.miachm.sods.Sheet
import com.github.miachm.sods.SpreadSheet
import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.Importer
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter

import java.text.NumberFormat

/**
 * Import Calc (ods file) into a Matrix.
 */
@CompileStatic
class SOdsImporter implements Importer {

  static SOdsImporter create() {
    new SOdsImporter()
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
      importSpreadsheet(is, sheetNum, startRow, endRow, startColNum, endColNum, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNumber, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetNumber, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
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
    List<String> header = []

    SpreadSheet spreadSheet = new SpreadSheet(is)
    Sheet sheet = spreadSheet.getSheet(sheetName)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      for (int i = 1; i <= endCol - startCol; i++) {
        header.add(String.valueOf(i))
      }
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }

  @Override
  Matrix importSpreadsheet(InputStream is, int sheetNum,
                          int startRow = 1, int endRow,
                          int startCol = 1, int endCol,
                          boolean firstRowAsColNames = true) {
    List<String> header = []

    SpreadSheet spreadSheet = new SpreadSheet(is)
    Sheet sheet = spreadSheet.getSheet(sheetNum -1)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      int ncol = endCol - startCol + 1
      (1..ncol).each {i -> header.add("c$i".toString())}
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }


  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                                      int startRow = 1, int endRow,
                                      String startCol = 'A', String endCol,
                                      boolean firstRowAsColNames = true) {

    return importSpreadsheet(
        file,
        sheetName,
        startRow as int,
        endRow as int,
        SpreadsheetUtil.asColumnNumber(startCol) as int,
        SpreadsheetUtil.asColumnNumber(endCol) as int,
        firstRowAsColNames as boolean
    )
  }

  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                                      int startRow = 1, int endRow,
                                      int startCol = 1, int endCol,
                                      boolean firstRowAsColNames = true) {
    List<String> header = []
    File excelFile = FileUtil.checkFilePath(file)
    SpreadSheet spreadSheet = new SpreadSheet(excelFile)
    Sheet sheet = spreadSheet.getSheet(sheetName)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      for (int i = 1; i <= endCol - startCol; i++) {
        header.add(String.valueOf(i))
      }
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }

  @Override
  Matrix importSpreadsheet(String file, int sheetNumber,
                               int startRow = 1, int endRow,
                               int startCol = 1, int endCol,
                               boolean firstRowAsColNames = true) {
    List<String> header = []
    File excelFile = FileUtil.checkFilePath(file)
    SpreadSheet spreadSheet = new SpreadSheet(excelFile)
    Sheet sheet = spreadSheet.getSheet(sheetNumber -1)
    if (firstRowAsColNames) {
      buildHeaderRow(startRow, startCol, endCol, header, sheet)
      startRow = startRow + 1
    } else {
      for (int i = 1; i <= endCol - startCol; i++) {
        header.add(String.valueOf(i))
      }
    }
    return importOds(sheet, startRow, endRow, startCol, endCol, header)
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
    SpreadSheet spreadSheet = new SpreadSheet(is)
    Map<Object, Matrix> result = [:]
    sheetParams.each {
      List<String> header = []
      Sheet sheet
      if (it.sheetName != null) {
        sheet = spreadSheet.getSheet(it.sheetName as String)
      } else if (it.sheetNumber != null) {
        sheet = spreadSheet.getSheet(it.sheetNumber as int)
      } else {
        throw new IllegalArgumentException("Either sheetName or sheetNumber must be specified")
      }
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
      if (it.firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        header.addAll(SpreadsheetUtil.createColumnNames(startCol, endCol))
      }
      //println ("importOdsSheets: startCol ${it.startCol} = $startCol, endCol ${it.endCol} = $endCol")
      Matrix matrix = importOds(
          sheet,
          startRow,
          it.endRow as int,
          startCol,
          endCol,
          header)
      String key = it["key"] ?: sheet.name
      matrix.setMatrixName(key)
      result.put(key, matrix)
    }
    result
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(URL url, List<Map> sheetParams, NumberFormat... formatOpt) {
    try(InputStream is = url.openStream()) {
      importSpreadsheets(is, sheetParams, formatOpt)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(String fileName, List<Map> sheetParams, NumberFormat... formatOpt) {
    File file = FileUtil.checkFilePath(fileName)
    try (FileInputStream fis = new FileInputStream(file)) {
      importSpreadsheets(fis, sheetParams, formatOpt)
    }
  }

  static Matrix importOds(Sheet sheet, int startRow, int endRow, int startCol, int endCol, List<String> colNames) {
    startRow--
    endRow--
    startCol--
    endCol--

    //println "importOds: startCol = $startCol, endCol = $endCol"
    SOdsValueExtractor ext = new SOdsValueExtractor(sheet)
    List<List> matrix = []
    List rowList
    for (int rowIdx = startRow; rowIdx <= endRow; rowIdx++) {
      rowList = []
      for (int colIdx = startCol; colIdx <= endCol; colIdx++) {
        def val = ext.getObject(rowIdx, colIdx)
        rowList.add(val)
      }
      matrix.add(rowList)
    }
    return Matrix.builder()
    .matrixName(sheet.name)
    .columnNames(colNames)
    .rows(matrix)
    .types([Object]*colNames.size())
    .build()
  }

  private static void buildHeaderRow(int startRowNum, int startColNum, int endColNum, List<String> header, Sheet sheet) {
    startRowNum--
    startColNum--
    endColNum--
    SOdsValueExtractor ext = new SOdsValueExtractor(sheet)
    for (int i = 0; i <= endColNum - startColNum; i++) {
      header.add(ext.getString(startRowNum, startColNum + i))
    }
  }
}
