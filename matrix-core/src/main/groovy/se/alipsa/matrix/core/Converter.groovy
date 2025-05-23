package se.alipsa.matrix.core

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
