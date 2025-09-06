package se.alipsa.matrix.core

import groovy.transform.CompileStatic

@CompileStatic
class Converter {
  String columnName
  Class<?> type
  Closure converter

  static Converter of(String columnName, Class<?> type, Closure converter) {
    return new Converter(columnName, type, converter)
  }

  Converter(String columnName, Class<?> type, Closure converter) {
    this.columnName = columnName
    this.type = type
    this.converter = converter
  }
}
