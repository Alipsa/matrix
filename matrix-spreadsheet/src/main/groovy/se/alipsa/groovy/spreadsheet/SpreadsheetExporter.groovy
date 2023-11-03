package se.alipsa.groovy.spreadsheet


import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.spreadsheet.excel.ExcelExporter
import se.alipsa.groovy.spreadsheet.ods.OdsExporter

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
