package se.alipsa.matrix.spreadsheet

import se.alipsa.matrix.spreadsheet.fastexcel.FExcelReader
import se.alipsa.matrix.spreadsheet.fastods.FOdsReader

/**
 * Read-only access to spreadsheet workbook metadata such as sheet names, row counts, and column counts.
 * Use {@link Factory#create(File)} to obtain a format-appropriate implementation.
 */
interface SpreadsheetReader extends Closeable {

  // Groovy does not support static methods in interfaces so we create an inner class for those
  class Factory {

    private static final String ODS_EXT = '.ods'

    static SpreadsheetReader create(File file) {
      if (file == null) {
        throw new IllegalArgumentException('File is null, cannot create SpreadsheetReader')
      }
      SpreadsheetUtil.rejectLegacyXls(file.getName())
      if (file.getName().toLowerCase().endsWith(ODS_EXT)) {
        return new FOdsReader(file)
      }
      new FExcelReader(file)
    }

    static SpreadsheetReader create(String filePath) {
      if (filePath == null) {
        throw new IllegalArgumentException('filePath is null, cannot create SpreadsheetReader')
      }
      SpreadsheetUtil.rejectLegacyXls(filePath)
      if (filePath.toLowerCase().endsWith(ODS_EXT)) {
        return new FOdsReader(filePath)
      }
      new FExcelReader(filePath)
    }

  }

  int findRowNum(int sheetNumber, int colNumber, String content) throws IOException
  int findRowNum(int sheetNumber, String colName, String content) throws IOException
  int findRowNum(String sheetName, String colName, String content) throws IOException
  int findRowNum(String sheetName, int colNumber, String content) throws IOException
  int findColNum(int sheetNumber, int rowNumber, String content) throws IOException
  int findColNum(String sheetName, int rowNumber, String content) throws IOException
  int findLastRow(int sheetNum)
  int findLastRow(String sheetName)
  int findLastCol(int sheetNum)
  int findLastCol(String sheetName)
  List<String> getSheetNames() throws IOException

}
