package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic

@CompileStatic
class FastOdsException extends RuntimeException {

  FastOdsException(String message) {
    super(message)
  }

  FastOdsException(String message, Throwable cause) {
    super(message, cause)
  }

  FastOdsException(Throwable cause) {
    super(cause)
  }

  FastOdsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace)
  }
}
