package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic

@CompileStatic
class SpreadsheetImportException extends Exception {
  @Override
  Object invokeMethod(String name, Object args) {
    return super.invokeMethod(name, args)
  }

  SpreadsheetImportException(String message) {
    super(message)
  }

  SpreadsheetImportException(String message, Throwable cause) {
    super(message, cause)
  }
}
