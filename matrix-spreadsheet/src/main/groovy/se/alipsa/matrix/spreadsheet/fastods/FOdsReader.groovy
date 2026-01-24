package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.FileUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsDataReader
import se.alipsa.matrix.spreadsheet.fastods.reader.Uncompressor

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.tableUrn

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Extract various information from a Calc (ods) file using fastods streaming.
 */
@CompileStatic
class FOdsReader implements SpreadsheetReader {

  private final File odsFile
  private final OdsDataReader reader
  private List<String> sheetNamesCache

  FOdsReader(File odsFile) {
    this(odsFile, OdsDataReader.create())
  }

  FOdsReader(File odsFile, OdsDataReader reader) {
    this.odsFile = odsFile
    this.reader = reader
  }

  FOdsReader(String filePath) {
    this(FileUtil.checkFilePath(filePath))
  }

  @Override
  int findRowNum(int sheetNumber, int colNumber, String content) throws Exception {
    Sheet sheet = readSheet(sheetNumber, 1, Integer.MAX_VALUE, colNumber, colNumber)
    return findRowNum(sheet, content)
  }

  @Override
  int findRowNum(int sheetNumber, String colName, String content) throws Exception {
    return findRowNum(sheetNumber, SpreadsheetUtil.asColumnNumber(colName), content)
  }

  @Override
  int findRowNum(String sheetName, String colName, String content) throws Exception {
    return findRowNum(sheetName, SpreadsheetUtil.asColumnNumber(colName), content)
  }

  @Override
  int findRowNum(String sheetName, int colNumber, String content) throws Exception {
    Sheet sheet = readSheet(sheetName, 1, Integer.MAX_VALUE, colNumber, colNumber)
    return findRowNum(sheet, content)
  }

  private static int findRowNum(Sheet sheet, String content) {
    if (sheet == null) {
      return -1
    }
    for (int rowIdx = 0; rowIdx < sheet.size(); rowIdx++) {
      List<?> row = sheet.get(rowIdx)
      Object value = row != null && !row.isEmpty() ? row.get(0) : null
      if (content == asString(value)) {
        return rowIdx + 1
      }
    }
    return -1
  }

  @Override
  int findColNum(int sheetNumber, int rowNumber, String content) throws Exception {
    Sheet sheet = readSheet(sheetNumber, rowNumber, rowNumber, 1, Integer.MAX_VALUE)
    return findColNum(sheet, content)
  }

  @Override
  int findColNum(String sheetName, int rowNumber, String content) throws Exception {
    Sheet sheet = readSheet(sheetName, rowNumber, rowNumber, 1, Integer.MAX_VALUE)
    return findColNum(sheet, content)
  }

  private static int findColNum(Sheet sheet, String content) {
    if (sheet == null || sheet.isEmpty()) {
      return -1
    }
    List<?> row = sheet.get(0)
    if (row == null) {
      return -1
    }
    for (int colIdx = 0; colIdx < row.size(); colIdx++) {
      if (content == asString(row.get(colIdx))) {
        return colIdx + 1
      }
    }
    return -1
  }

  @Override
  int findLastRow(int sheetNum) {
    Sheet sheet = readSheet(sheetNum, 1, Integer.MAX_VALUE, 1, 1)
    return sheet?.size() ?: 0
  }

  @Override
  int findLastRow(String sheetName) {
    Sheet sheet = readSheet(sheetName, 1, Integer.MAX_VALUE, 1, 1)
    return sheet?.size() ?: 0
  }

  @Override
  int findLastCol(int sheetNum) {
    Sheet sheet = readSheet(sheetNum, 1, 10, 1, Integer.MAX_VALUE)
    return findLastCol(sheet)
  }

  @Override
  int findLastCol(String sheetName) {
    Sheet sheet = readSheet(sheetName, 1, 10, 1, Integer.MAX_VALUE)
    return findLastCol(sheet)
  }

  private static int findLastCol(Sheet sheet) {
    if (sheet == null || sheet.isEmpty()) {
      return 0
    }
    int rowsToScan = Math.min(10, sheet.size())
    int maxCol = 0
    for (int rowIdx = 0; rowIdx < rowsToScan; rowIdx++) {
      List<?> row = sheet.get(rowIdx)
      if (row == null || row.isEmpty()) {
        continue
      }
      for (int colIdx = row.size() - 1; colIdx >= 0; colIdx--) {
        if (row.get(colIdx) != null) {
          maxCol = Math.max(maxCol, colIdx + 1)
          break
        }
      }
    }
    return maxCol
  }

  @Override
  List<String> getSheetNames() throws Exception {
    if (sheetNamesCache == null) {
      sheetNamesCache = readSheetNames()
    }
    return new ArrayList<>(sheetNamesCache)
  }

  private List<String> readSheetNames() {
    List<String> names = []
    XMLStreamReader xmlReader = null
    try (InputStream is = new FileInputStream(odsFile); Uncompressor unc = new Uncompressor(is)) {
      String entry = unc.nextFile()
      while (entry != null) {
        if (entry == 'content.xml') {
          xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(unc.getInputStream())
          while (xmlReader.hasNext()) {
            xmlReader.next()
            if (xmlReader.isStartElement() && xmlReader.localName == 'table') {
              String name = xmlReader.getAttributeValue(tableUrn, 'name')
              if (name != null) {
                names.add(name.trim())
              }
            }
          }
          break
        }
        entry = unc.nextFile()
      }
    } finally {
      if (xmlReader != null) {
        xmlReader.close()
      }
    }
    return names
  }

  private Sheet readSheet(Object sheetId, int startRow, int endRow, int startCol, int endCol) {
    try (InputStream is = new FileInputStream(odsFile)) {
      return reader.readOds(is, sheetId, startRow, endRow, startCol, endCol)
    }
  }

  private static String asString(Object value) {
    value == null ? null : String.valueOf(value)
  }

  @Override
  void close() throws IOException {
    sheetNamesCache = null
  }
}
