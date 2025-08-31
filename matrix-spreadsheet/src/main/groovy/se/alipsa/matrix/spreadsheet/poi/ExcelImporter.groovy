package se.alipsa.matrix.spreadsheet.poi

import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.util.IOUtils
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.Importer
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter

import java.text.NumberFormat
import java.time.LocalDateTime

@CompileStatic
class ExcelImporter implements Importer {

  private static Logger log = LogManager.getLogger()

  static ExcelImporter create(int byteArrayMaxOverride = 600_000_000) {
    IOUtils.setByteArrayMaxOverride(byteArrayMaxOverride)
    new ExcelImporter()
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNum,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    try (InputStream is = url.openStream()) {
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      importSpreadsheet(is, sheetNum, startRow, endRow, startColNum, endColNum, firstRowAsColNames)
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
  Matrix importSpreadsheet(String file, int sheetNumber,
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                           boolean firstRowAsColNames = true) {
    List<String> header = []
    File excelFile = FileUtil.checkFilePath(file)
    try (Workbook workbook = WorkbookFactory.create(excelFile)) {
      Sheet sheet = workbook.getSheetAt(sheetNumber)
      // TODO refactor build header to either use first row or create column names
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  @Override
  Matrix importSpreadsheet(String file, int sheet,
                           int startRow = 1, int endRow,
                           String startColumn = 'A', String endColumn,
                           boolean firstRowAsColNames = true) {
    return importSpreadsheet(
        file,
        sheet,
        startRow,
        endRow,
        SpreadsheetUtil.asColumnNumber(startColumn),
        SpreadsheetUtil.asColumnNumber(endColumn),
        firstRowAsColNames
    )
  }

  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                           boolean firstRowAsColNames = true) {
    List<String> header = []
    File excelFile = FileUtil.checkFilePath(file)
    try (Workbook workbook = WorkbookFactory.create(excelFile)) {
      Sheet sheet = workbook.getSheet(sheetName)
      // TODO refactor build header to either use first row or create column names
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  /**
   *
   * @param is
   * @param sheetNum 1 indexed
   * @param startRow 1 indexed
   * @param endRow 1 indexed
   * @param startCol 1 indexed
   * @param endCol 1 indexed
   * @param firstRowAsColNames
   * @return
   */
  @Override
  Matrix importSpreadsheet(InputStream is, int sheetNum,
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                           boolean firstRowAsColNames = true) {
    List<String> header = []
    try (Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheetAt(sheetNum-1)
      // TODO refactor build header to either use first row or create column names
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        int ncol = endCol - startCol + 1
        (1..ncol).each {i-> header.add("c$i".toString())}
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  @Override
  Matrix importSpreadsheet(InputStream is, String sheetName = 'Sheet1',
                           int startRow = 1, int endRow,
                           int startCol = 1, int endCol,
                           boolean firstRowAsColNames = true) {
    List<String> header = []
    try (Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheet(sheetName)
      if (firstRowAsColNames) {
        buildHeaderRow(startRow, startCol, endCol, header, sheet)
        startRow = startRow + 1
      } else {
        for (int i = 1; i <= endCol - startCol; i++) {
          header.add(String.valueOf(i))
        }
      }
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNumber, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetNumber, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(URL url, List<Map> sheetParams, NumberFormat... formatOpt) {
    try(InputStream is = url.openStream()) {
      importSpreadsheets(is, sheetParams, formatOpt)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
    try (Workbook workbook = WorkbookFactory.create(is)) {
      Map<Object, Matrix> result = [:]
      sheetParams.each {
        List<String> header = []
        String sheetName = it.sheetName
        Sheet sheet = workbook.getSheet(sheetName)
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
        Matrix matrix = importExcelSheet(sheet, startRow, it.endRow as int, startCol, endCol, header)
        String key = it["key"] ?: sheetName
        matrix.setMatrixName(key)
        result.put(key, matrix)
      }
      return result
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(String fileName, List<Map> sheetParams, NumberFormat... formatOpt) {
    File file = FileUtil.checkFilePath(fileName)
    try (FileInputStream fis = new FileInputStream(file)) {
      importSpreadsheets(fis, sheetParams, formatOpt)
    }
  }


  private static void buildHeaderRow(int startRowNum, int startColNum, int endColNum, List<String> header, Sheet sheet) {
    startRowNum--
    startColNum--
    endColNum--
    ExcelValueExtractor ext = new ExcelValueExtractor(sheet)
    Row row = sheet.getRow(startRowNum)
    for (int i = 0; i <= endColNum - startColNum; i++) {
      String colName = ext.getString(row, startColNum + i)
      header.add(colName ?: "c$i".toString())
    }
  }

  private static Matrix importExcelSheet(Sheet sheet, int startRowNum, int endRowNum, int startColNum, int endColNum, List<String> colNames) {
    startRowNum--
    endRowNum--
    startColNum--
    endColNum--

    ExcelValueExtractor ext = new ExcelValueExtractor(sheet)
    List<List> matrix = []
    List rowList
    for (int rowIdx = startRowNum; rowIdx <= endRowNum; rowIdx++) {
      Row row = sheet.getRow(rowIdx)
      if (row == null) {
        log.warn("Row ${rowIdx + 1} is null")
        continue
      }
      rowList = []
      for (int colIdx = startColNum; colIdx <= endColNum; colIdx++) {
        def cell = row.getCell(colIdx)
        if (cell == null) {
          rowList.add(null)
        } else {
          //println("cell[$rowIdx, $colIdx]: ${cell.getCellType()}: val: ${ext.getObject(cell)}")
          switch (cell.getCellType()) {
            case CellType.BLANK -> rowList.add(null)
            case CellType.BOOLEAN -> rowList.add(ext.getBoolean(row, colIdx))
            case CellType.NUMERIC -> {
              if (DateUtil.isCellDateFormatted(cell)) {
                LocalDateTime val = ext.getLocalDateTime(cell)
                if (val != null && val.getHour() == 0 && val.getMinute() == 0 && val.getSecond() == 0 && val.getNano() == 0) {
                  rowList.add(val.toLocalDate())
                } else {
                  rowList.add(val)
                }
              } else {
                rowList.add(ext.getDouble(row, colIdx))
              }
            }
            case CellType.STRING -> rowList.add(ext.getString(row, colIdx))
            case CellType.FORMULA -> rowList.add(ext.getObject(cell))
            default -> rowList.add(ext.getString(row, colIdx))
          }
        }
      }
      matrix.add(rowList)
    }
    return Matrix.builder()
        .matrixName(sheet.getSheetName())
        .columnNames(colNames)
        .rows(matrix)
        .types([Object] * colNames.size())
        .build()
  }
}
