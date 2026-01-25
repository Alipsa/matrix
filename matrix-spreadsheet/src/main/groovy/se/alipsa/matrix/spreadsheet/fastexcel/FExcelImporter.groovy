package se.alipsa.matrix.spreadsheet.fastexcel

import groovy.transform.CompileStatic
import org.dhatim.fastexcel.reader.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.Importer
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.text.NumberFormat
import java.util.stream.Stream

@CompileStatic
class FExcelImporter implements Importer {

  static final ReadingOptions OPTIONS = new ReadingOptions(true, true)

  static FExcelImporter create() {
    new FExcelImporter()
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
  Matrix importSpreadsheet(URL url, int sheetNumber,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    if (url == null) throw new IllegalArgumentException("url cannot be null")
    ensureXlsx(url.path)
    try(InputStream is = url.openStream(); ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Sheet sheet = workbook.getSheet(sheetNumber - 1).orElse(null)
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startColNum, endColNum, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                            int startRow, int endRow,
                            int startCol, int endCol,
                            boolean firstRowAsColNames = true) {
    ensureXlsx(url?.path)
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    ensureXlsx(url?.path)
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {

    ensureXlsx(file)
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

  /**
   * Import a sheet from a file using a 1-indexed sheet number.
   *
   * @param file the file path or resource name
   * @param sheetNumber the 1-indexed sheet number to import
   * @param startRow the first row to include (1-indexed)
   * @param endRow the last row to include (1-indexed)
   * @param startCol the first column to include (1-indexed)
   * @param endCol the last column to include (1-indexed)
   * @param firstRowAsColNames whether the first row should be treated as column headers
   * @return the imported sheet as a {@link Matrix}
   * @throws IllegalArgumentException if {@code sheetNumber} is less than 1 or greater than the number of sheets
   */
  @Override
  Matrix importSpreadsheet(String file, int sheetNumber,
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    ensureXlsx(file)
    File excelFile = FileUtil.checkFilePath(file)
    try (ReadableWorkbook workbook = new ReadableWorkbook(excelFile, OPTIONS)) {
      if (sheetNumber < 1) {
        throw new IllegalArgumentException("Sheet number must be 1 or greater")
      }
      int sheetIndex = sheetNumber - 1
      Sheet sheet = workbook.getSheet(sheetIndex)
          .orElseThrow(() -> new IllegalArgumentException("Sheet number $sheetNumber does not exist"))
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(String file, int sheet,
                            int startRow = 1, int endRow,
                            String startColumn = 'A', String endColumn,
                            boolean firstRowAsColNames = true) {
    ensureXlsx(file)
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
    ensureXlsx(file)
    File excelFile = FileUtil.checkFilePath(file)
    try (ReadableWorkbook workbook = new ReadableWorkbook(excelFile, OPTIONS)) {
      Sheet sheet = workbook.findSheet(sheetName).orElseThrow()
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(InputStream is, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Sheet sheet = workbook.findSheet(sheetName).orElseThrow()
      boolean isDate1904 = workbook.isDate1904()
      def m = importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
      return m
    }
  }

  /**
   * Import a sheet from an {@link InputStream} using a 1-indexed sheet number.
   *
   * @param is the stream containing the workbook data
   * @param sheetNum the 1-indexed sheet number to import
   * @param startRow the first row to include (1-indexed)
   * @param endRow the last row to include (1-indexed)
   * @param startCol the first column to include (1-indexed)
   * @param endCol the last column to include (1-indexed)
   * @param firstRowAsColNames whether the first row should be treated as column headers
   * @return the imported sheet as a {@link Matrix}
   * @throws IllegalArgumentException if {@code sheetNum} is less than 1 or greater than the number of sheets
   */
  @Override
  Matrix importSpreadsheet(InputStream is, int sheetNum, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      if (sheetNum < 1) {
        throw new IllegalArgumentException("Sheet number must be 1 or greater")
      }
      int sheetIndex = sheetNum - 1
      Sheet sheet = workbook.getSheet(sheetIndex)
          .orElseThrow(() -> new IllegalArgumentException("Sheet number $sheetNum does not exist"))
      boolean isDate1904 = workbook.isDate1904()
      return importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNumber, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    ensureXlsx(url?.path)
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetNumber, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  private static void ensureXlsx(String fileName) {
    if (fileName != null && fileName.toLowerCase().endsWith(".xls")) {
      throw new IllegalArgumentException("Unsupported Excel format .xls. Only .xlsx is supported.")
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Map<Object, Matrix> result = [:]
      sheetParams.each {
        String sheetName = it.sheetName
        Sheet sheet = workbook.findSheet(sheetName).orElseThrow()
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
        boolean isDate1904 = workbook.isDate1904()
        Matrix matrix = importExcelSheet(sheet, startRow, it.endRow as int, startCol, endCol, it.firstRowAsColNames as Boolean, isDate1904)
        String key = it["key"] ?: sheetName
        matrix.setMatrixName(key)
        result.put(key, matrix)
      }
      return result
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(URL url, List<Map> sheetParams, NumberFormat... formatOpt) {
    ensureXlsx(url?.path)
    try(InputStream is = url.openStream()) {
      importSpreadsheets(is, sheetParams, formatOpt)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(String fileName, List<Map> sheetParams, NumberFormat... formatOpt) {
    ensureXlsx(fileName)
    File file = FileUtil.checkFilePath(fileName)
    try (FileInputStream fis = new FileInputStream(file)) {
      importSpreadsheets(fis, sheetParams, formatOpt)
    }
  }

  private static Matrix importExcelSheet(Sheet sheet, int startRowNum, int endRowNum, int startColNum, int endColNum, boolean firstRowAsColNames, boolean isDate1904) {
    //println "Importing sheet ${sheet.name}, startRowNum = $startRowNum, endRowNum = $endRowNum"
    //int startRowNumZI = startRowNum - 1
    //int endRowNumZI = endRowNum - 1
    FExcelValueExtractor ext = new FExcelValueExtractor(sheet, isDate1904)
    int startColNumZI = startColNum - 1
    int endColNumZI = endColNum - 1
    List<String> colNames = []
    List<List> matrix = []
    List rowList
    int ncol = endColNum - startColNum + 1 // both start and end are included
    try (Stream<Row> rows = sheet.openStream()) {
      rows.each { Row row ->
        //println "on row $row.rowNum, row.rowNum >= startRowNum = ${row.rowNum >= startRowNum} row.rowNum <= endRowNum = ${row.rowNum <= endRowNum}"
        if (row.rowNum >= startRowNum && row.rowNum <= endRowNum) {
          rowList = []
          List list = row.asList()
          if (colNames.size() == 0) {
            if (firstRowAsColNames) {
              list.eachWithIndex { Cell cell, int i ->
                if (i >= startColNumZI && i <= endColNumZI) {
                  colNames.add(cell.rawValue)
                }
              }
              return
            } else {
              colNames.addAll(SpreadsheetUtil.createColumnNames(startColNum, endColNum))
            }
          }
          list.eachWithIndex { cell, cIdx ->
            //println "row $cIdx adding " + cell?.rawValue
            if (cIdx >= startColNumZI && cIdx <= endColNumZI) {
              if (cell == null) {
                rowList.add(null)
              } else {
                rowList.add(ext.getObject(cell))
              }
            }
          }
          if (rowList.size() < ncol) {
            //println("padding null to rowList rowList.size() = ${rowList.size()}, ncol=$ncol")
            def padSize = ncol - rowList.size()
            (1..padSize).each {
              //print "."
              rowList << null
            }
            //println()
          }
          //println row.rowNum + ": " + rowList
          matrix.add(rowList)
        }
      }
    }
    Matrix m = Matrix.builder()
        .matrixName(sheet.name)
        .columnNames(colNames)
        .rows(matrix)
        .types([Object] * colNames.size())
        .build()
    m.metaData.isDate1904 = isDate1904
    m
  }
}
