package se.alipsa.matrix.spreadsheet.fastexcel

import org.dhatim.fastexcel.reader.*

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.Importer
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.text.NumberFormat
import java.util.stream.Stream

/**
 * Imports Excel (.xlsx) files into Matrix instances using the FastExcel reader library.
 */
@SuppressWarnings('UnusedObject')
class FExcelImporter implements Importer {

  private static final String SHEET_NUM_ERROR = 'Sheet number must be 1 or greater'
  static final ReadingOptions OPTIONS = new ReadingOptions(true, true)

  static FExcelImporter create() {
    new FExcelImporter()
  }

  @Override
  Matrix importSpreadsheet(InputStream is, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {

    importSpreadsheet(
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
    if (url == null) {
      throw new IllegalArgumentException('url cannot be null')
    }
    if (sheetNumber < 1) {
      throw new IllegalArgumentException(SHEET_NUM_ERROR)
    }
    SpreadsheetUtil.rejectLegacyXls(url.path)
    try(InputStream is = url.openStream(); ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      int sheetIndex = sheetNumber - 1
      Sheet sheet = workbook.getSheet(sheetIndex)
          .orElseThrow(() -> new IllegalArgumentException("Sheet number $sheetNumber does not exist"))
      int startColNum = SpreadsheetUtil.asColumnNumber(startCol)
      int endColNum = SpreadsheetUtil.asColumnNumber(endCol)
      boolean isDate1904 = workbook.isDate1904()
      importExcelSheet(sheet, startRow, endRow, startColNum, endColNum, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                            int startRow, int endRow,
                            int startCol, int endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.rejectLegacyXls(url?.path)
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.rejectLegacyXls(url?.path)
    try(InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetName, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {

    SpreadsheetUtil.rejectLegacyXls(file)
    importSpreadsheet(
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
    SpreadsheetUtil.rejectLegacyXls(file)
    File excelFile = FileUtil.checkFilePath(file)
    try (ReadableWorkbook workbook = new ReadableWorkbook(excelFile, OPTIONS)) {
      if (sheetNumber < 1) {
        throw new IllegalArgumentException(SHEET_NUM_ERROR)
      }
      int sheetIndex = sheetNumber - 1
      Sheet sheet = workbook.getSheet(sheetIndex)
          .orElseThrow(() -> new IllegalArgumentException("Sheet number $sheetNumber does not exist"))
      boolean isDate1904 = workbook.isDate1904()
      importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(String file, int sheet,
                            int startRow = 1, int endRow,
                            String startCol = 'A', String endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.rejectLegacyXls(file)
    importSpreadsheet(
        file,
        sheet,
        startRow,
        endRow,
        SpreadsheetUtil.asColumnNumber(startCol),
        SpreadsheetUtil.asColumnNumber(endCol),
        firstRowAsColNames
    )
  }

  @Override
  Matrix importSpreadsheet(String file, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    SpreadsheetUtil.rejectLegacyXls(file)
    File excelFile = FileUtil.checkFilePath(file)
    try (ReadableWorkbook workbook = new ReadableWorkbook(excelFile, OPTIONS)) {
      Sheet sheet = workbook.findSheet(sheetName).orElseThrow {
        new NoSuchElementException("Sheet '${sheetName}' does not exist in the workbook")
      }
      boolean isDate1904 = workbook.isDate1904()
      importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(InputStream is, String sheetName = 'Sheet1',
                            int startRow = 1, int endRow,
                            int startCol = 1, int endCol,
                            boolean firstRowAsColNames = true) {
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Sheet sheet = workbook.findSheet(sheetName).orElseThrow {
        new NoSuchElementException("Sheet '${sheetName}' does not exist in the workbook")
      }
      boolean isDate1904 = workbook.isDate1904()
      importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
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
        throw new IllegalArgumentException(SHEET_NUM_ERROR)
      }
      int sheetIndex = sheetNum - 1
      Sheet sheet = workbook.getSheet(sheetIndex)
          .orElseThrow(() -> new IllegalArgumentException("Sheet number $sheetNum does not exist"))
      boolean isDate1904 = workbook.isDate1904()
      importExcelSheet(sheet, startRow, endRow, startCol, endCol, firstRowAsColNames, isDate1904)
    }
  }

  @Override
  Matrix importSpreadsheet(URL url, int sheetNumber, int startRow, int endRow, int startCol, int endCol, boolean firstRowAsColNames) {
    SpreadsheetUtil.rejectLegacyXls(url?.path)
    try (InputStream is = url.openStream()) {
      importSpreadsheet(is, sheetNumber, startRow, endRow, startCol, endCol, firstRowAsColNames)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(InputStream is, List<Map> sheetParams, NumberFormat... formatOpt) {
    NumberFormat format = formatOpt.length > 0 ? formatOpt[0] : NumberFormat.getInstance()
    try (ReadableWorkbook workbook = new ReadableWorkbook(is, OPTIONS)) {
      Map<Object, Matrix> result = [:]
      sheetParams.each {
        String sheetName = it.sheetName
        Sheet sheet = workbook.findSheet(sheetName).orElseThrow {
          new NoSuchElementException("Sheet '${sheetName}' does not exist in the workbook")
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
        boolean isDate1904 = workbook.isDate1904()
        boolean firstRowAsColNames = it.containsKey('firstRowAsColNames') ? it.firstRowAsColNames as boolean : true
        Matrix matrix = importExcelSheet(sheet, startRow, it.endRow as int, startCol, endCol, firstRowAsColNames, isDate1904)
        String key = it['key'] ?: sheetName
        matrix.setMatrixName(key)
        result.put(key, matrix)
      }
      result
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(URL url, List<Map> sheetParams, NumberFormat... formatOpt) {
    SpreadsheetUtil.rejectLegacyXls(url?.path)
    try(InputStream is = url.openStream()) {
      importSpreadsheets(is, sheetParams, formatOpt)
    }
  }

  @Override
  Map<Object, Matrix> importSpreadsheets(String fileName, List<Map> sheetParams, NumberFormat... formatOpt) {
    SpreadsheetUtil.rejectLegacyXls(fileName)
    File file = FileUtil.checkFilePath(fileName)
    try (FileInputStream fis = new FileInputStream(file)) {
      importSpreadsheets(fis, sheetParams, formatOpt)
    }
  }

  @SuppressWarnings('NestedBlockDepth')
  private static Matrix importExcelSheet(Sheet sheet, int startRowNum, int endRowNum, int startColNum, int endColNum, boolean firstRowAsColNames, boolean isDate1904) {
    FExcelValueExtractor ext = new FExcelValueExtractor(sheet, isDate1904)
    int startColNumZI = startColNum - 1
    int endColNumZI = endColNum - 1
    int ncol = endColNum - startColNum + 1
    List<String> colNames = []
    List<List> matrix = []
    int nextExpectedRow = startRowNum
    boolean headerConsumed = !firstRowAsColNames
    if (!firstRowAsColNames) {
      colNames.addAll(SpreadsheetUtil.createColumnNames(startColNum, endColNum))
    }
    try (Stream<Row> rows = sheet.openStream()) {
      rows.each { Row row ->
        if (row.rowNum < startRowNum || row.rowNum > endRowNum) {
          return
        }
        while (nextExpectedRow < row.rowNum) {
          if (headerConsumed) {
            matrix.add(nullRow(ncol))
          } else {
            colNames.addAll(SpreadsheetUtil.createColumnNames(startColNum, endColNum))
            headerConsumed = true
          }
          nextExpectedRow++
        }
        nextExpectedRow = row.rowNum + 1
        if (!headerConsumed) {
          for (int c = startColNumZI; c <= endColNumZI; c++) {
            Cell cell = row.getOptionalCell(c).orElse(null)
            String raw = cell?.rawValue
            colNames.add(raw == null || raw.isBlank() ? "c${c + 1}".toString() : raw)
          }
          headerConsumed = true
          return
        }
        List rowList = new ArrayList(ncol)
        for (int c = startColNumZI; c <= endColNumZI; c++) {
          Cell cell = row.getOptionalCell(c).orElse(null)
          rowList.add(cell == null ? null : ext.getObject(cell))
        }
        matrix.add(rowList)
      }
    }
    if (!headerConsumed) {
      colNames.addAll(SpreadsheetUtil.createColumnNames(startColNum, endColNum))
    }
    colNames = SpreadsheetUtil.createUniqueColumnNames(colNames)
    Matrix m = Matrix.builder()
        .matrixName(sheet.name)
        .columnNames(colNames)
        .rows(matrix)
        .types([Object] * ncol)
        .build()
    m.metaData.isDate1904 = isDate1904
    m
  }

  private static List nullRow(int ncol) {
    List row = new ArrayList(ncol)
    for (int i = 0; i < ncol; i++) {
      row.add(null)
    }
    row
  }

}
