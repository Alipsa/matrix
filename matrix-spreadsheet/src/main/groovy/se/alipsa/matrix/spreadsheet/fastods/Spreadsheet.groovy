package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic

@CompileStatic
class Spreadsheet extends LinkedHashMap<Object, Sheet> {
  void add(String sheetName, Sheet sheet) {
    put(sheetName, sheet)
  }
  void add(Integer sheetIndex, Sheet sheet) {
    put(sheetIndex, sheet)
  }
}
