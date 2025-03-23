package se.alipsa.matrix.spreadsheet


import se.alipsa.matrix.spreadsheet.fastexcel.FExcelReader
import se.alipsa.matrix.spreadsheet.sods.SOdsReader
import se.alipsa.matrix.spreadsheet.poi.ExcelReader

interface SpreadsheetReader extends Closeable {

  // Groovy does not support static methods in interfaces so we create an inner class for those
  class Factory {
    static SpreadsheetReader create(File file, excelImplementation = ExcelImplementation.FastExcel) {
      if (file == null) {
        throw new IllegalArgumentException("File is null, cannot create SpreadsheetReader")
      }
      if (file.getName().toLowerCase().endsWith(".ods")) {
        return new SOdsReader(file)
      }
      return switch (excelImplementation) {
        case ExcelImplementation.POI -> new ExcelReader(file)
        case ExcelImplementation.FastExcel -> new FExcelReader(file)
        default -> throw new IllegalArgumentException("Unknown excelImplementation: $excelImplementation")
      }
    }

    static SpreadsheetReader create(String filePath, excelImplementation = ExcelImplementation.FastExcel) throws Exception {
      if (filePath == null) {
        throw new IllegalArgumentException("filePath is null, cannot create SpreadsheetReader")
      }
      if (filePath.toLowerCase().endsWith(".ods")) {
        return new SOdsReader(filePath)
      }
      return switch (excelImplementation) {
        case ExcelImplementation.POI -> new ExcelReader(filePath)
        case ExcelImplementation.FastExcel -> new FExcelReader(filePath)
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
