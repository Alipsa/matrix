package se.alipsa.matrix.gsheets

import groovy.transform.CompileStatic

/**
 * Exception thrown when an operation on a Google Sheets spreadsheet fails.
 * This exception provides context about the operation that failed and the
 * spreadsheet ID involved.
 */
@CompileStatic
class SheetOperationException extends RuntimeException {

  /** The spreadsheet ID where the operation failed */
  final String spreadsheetId

  /** The operation that was being performed when the failure occurred */
  final String operation

  /**
   * Creates a new SheetOperationException.
   *
   * @param operation The operation that failed (e.g., "export", "import", "delete")
   * @param spreadsheetId The spreadsheet ID where the operation failed (may be null for create operations)
   * @param cause The underlying exception that caused the failure
   */
  SheetOperationException(String operation, String spreadsheetId, Throwable cause) {
    super(buildMessage(operation, spreadsheetId, cause), cause)
    this.operation = operation
    this.spreadsheetId = spreadsheetId
  }

  /**
   * Creates a new SheetOperationException without a spreadsheet ID.
   *
   * @param operation The operation that failed
   * @param message The error message
   */
  SheetOperationException(String operation, String message) {
    super("Failed to ${operation}: ${message}")
    this.operation = operation
    this.spreadsheetId = null
  }

  /**
   * Creates a new SheetOperationException without a cause.
   *
   * @param operation The operation that failed
   * @param spreadsheetId The spreadsheet ID where the operation failed
   * @param message The error message
   */
  SheetOperationException(String operation, String spreadsheetId, String message) {
    super("Failed to ${operation} spreadsheet ${spreadsheetId}: ${message}")
    this.operation = operation
    this.spreadsheetId = spreadsheetId
  }

  private static String buildMessage(String operation, String spreadsheetId, Throwable cause) {
    String baseMessage = "Failed to ${operation}"
    if (spreadsheetId != null) {
      baseMessage += " spreadsheet ${spreadsheetId}"
    }
    if (cause != null) {
      baseMessage += ": ${cause.message}"
    }
    return baseMessage
  }
}
