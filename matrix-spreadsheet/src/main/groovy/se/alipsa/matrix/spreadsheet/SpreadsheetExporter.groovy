package se.alipsa.matrix.spreadsheet


import se.alipsa.matrix.spreadsheet.ods.OdsExporter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.excel.ExcelExporter

class SpreadsheetExporter {

  static String exportSpreadsheet(File file, Matrix data) {
    if (file.getName().toLowerCase().endsWith(".ods")) {
      return OdsExporter.exportOds(file, data)
    }
    return ExcelExporter.exportExcel(file, data)
  }

  static String exportSpreadsheet(File file, Matrix data, String sheetName) {
    if (file.getName().toLowerCase().endsWith(".ods")) {
      return OdsExporter.exportOds(file, data, sheetName)
    }
    return ExcelExporter.exportExcel(file, data, sheetName)
  }

  static List<String> exportSpreadsheets(File file, List<Matrix> data, List<String> sheetNames) {
    if (file.getName().toLowerCase().endsWith(".ods")) {
      return OdsExporter.exportOdsSheets(file, data, sheetNames)
    }
    return ExcelExporter.exportExcelSheets(file, data, sheetNames)
  }

  static List<String> exportSpreadsheets(Map params) {
    def file = params.get("file") as File
    def data = (List<Matrix>) params.get("data")
    def sheetNames = params.get("sheetNames") as List<String>
    return exportSpreadsheets(file, data, sheetNames)
  }
}
