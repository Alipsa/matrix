package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic

@CompileStatic
enum OdsImplementation {
  SODS, FastOdsStream, FastOdsEvent
}