package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelReader
import se.alipsa.matrix.spreadsheet.fastods.FOdsReader

@CompileStatic
interface SpreadsheetReader extends Closeable {

  // Groovy does not support static methods in interfaces so we create an inner class for those
  class Factory {
    static SpreadsheetReader create(File file) {
      if (file == null) {
        throw new IllegalArgumentException("File is null, cannot create SpreadsheetReader")
      }
      ensureSupportedExcelFormat(file.getName())
      if (file.getName().toLowerCase().endsWith(".ods")) {
        return new FOdsReader(file)
      }
      return new FExcelReader(file)
    }

    static SpreadsheetReader create(String filePath) throws Exception {
      if (filePath == null) {
        throw new IllegalArgumentException("filePath is null, cannot create SpreadsheetReader")
      }
      ensureSupportedExcelFormat(filePath)
      if (filePath.toLowerCase().endsWith(".ods")) {
        return new FOdsReader(filePath)
      }
      return new FExcelReader(filePath)
    }

    private static void ensureSupportedExcelFormat(String fileName) {
      if (fileName.toLowerCase().endsWith(".xls")) {
        throw new IllegalArgumentException("Unsupported Excel format .xls. Only .xlsx is supported.")
      }
    }
  }

  int findRowNum(int sheetNumber, int colNumber, String content) throws Exception
  int findRowNum(int sheetNumber, String colName, String content) throws Exception
  int findRowNum(String sheetName, String colName, String content) throws Exception
  int findRowNum(String sheetName, int colNumber, String content) throws Exception
  int findColNum(int sheetNumber, int rowNumber, String content) throws Exception
  int findColNum(String sheetName, int rowNumber, String content) throws Exception
  int findLastRow(int sheetNum)
  int findLastRow(String sheetName)
  int findLastCol(int sheetNum)
  int findLastCol(String sheetName)
  List<String> getSheetNames() throws Exception

}
