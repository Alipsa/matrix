package se.alipsa.matrix.spreadsheet

/**
 * Checked exception thrown when a spreadsheet import operation fails.
 */
class SpreadsheetImportException extends Exception {

  SpreadsheetImportException(String message) {
    super(message)
  }

  SpreadsheetImportException(String message, Throwable cause) {
    super(message, cause)
  }

}
