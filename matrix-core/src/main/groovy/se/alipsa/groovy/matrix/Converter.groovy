package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic

@CompileStatic
class Converter {
  String columnName
  Class<?> type
  Closure converter

  Converter(String columnName, Class<?> type, Closure converter) {
    this.columnName = columnName
    this.type = type
    this.converter = converter
  }
}
