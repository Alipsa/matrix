package se.alipsa.matrix.spreadsheet.fastods

/**
 * Runtime exception thrown when an error occurs during ODS file processing.
 */
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
