package se.alipsa.matrix.avro.exceptions

/**
 * Exception thrown when there is a problem with Avro schema generation or processing.
 *
 * <p>This exception is thrown in scenarios such as:
 * <ul>
 *   <li>Incompatible schema types</li>
 *   <li>Invalid schema configuration</li>
 *   <li>Schema evolution conflicts</li>
 *   <li>Unsupported logical types</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * try {
 *     Matrix m = MatrixAvroReader.read(file)
 * } catch (AvroSchemaException e) {
 *     System.err.println('Schema error: ' + e.getMessage())
 *     if (e.getColumnName() != null) {
 *         System.err.println('  Column: ' + e.getColumnName())
 *     }
 *     if (e.getRowNumber() >= 0) {
 *         System.err.println('  Row: ' + e.getRowNumber())
 *     }
 * }
 * }</pre>
 */
class AvroSchemaException extends RuntimeException {

  private static final int NO_ROW = -1
  private static final String JOIN_DELIMITER = ', '
  /** The raw error message without context decorations */
  private final String rawMessage
  /** The column name associated with this error, if applicable */
  private final String columnName
  /** The expected type, if applicable */
  private final String expectedType
  /** The actual type encountered, if applicable */
  private final String actualType
  /** The row number (0-based) where the error occurred, or -1 if not applicable */
  private final int rowNumber
  /**
   * Creates a new AvroSchemaException with a message.
   *
   * @param message the error message
   */
  AvroSchemaException(String message) {
    super(buildMessage(message, null, null, null, NO_ROW))
    this.rawMessage = message
    this.columnName = null
    this.expectedType = null
    this.actualType = null
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroSchemaException with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  AvroSchemaException(String message, Throwable cause) {
    super(buildMessage(message, null, null, null, NO_ROW), cause)
    this.rawMessage = message
    this.columnName = null
    this.expectedType = null
    this.actualType = null
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroSchemaException with contextual information.
   *
   * @param message the error message
   * @param columnName the column name where the error occurred
   * @param expectedType the expected type (may be null)
   * @param actualType the actual type encountered (may be null)
   */
  AvroSchemaException(String message, String columnName, String expectedType, String actualType) {
    super(buildMessage(message, columnName, expectedType, actualType, NO_ROW))
    this.rawMessage = message
    this.columnName = columnName
    this.expectedType = expectedType
    this.actualType = actualType
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroSchemaException with contextual information and a cause.
   *
   * @param message the error message
   * @param columnName the column name where the error occurred
   * @param expectedType the expected type (may be null)
   * @param actualType the actual type encountered (may be null)
   * @param cause the underlying cause
   */
  AvroSchemaException(String message, String columnName, String expectedType, String actualType, Throwable cause) {
    super(buildMessage(message, columnName, expectedType, actualType, NO_ROW), cause)
    this.rawMessage = message
    this.columnName = columnName
    this.expectedType = expectedType
    this.actualType = actualType
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroSchemaException with contextual information, a row number, and a cause.
   *
   * @param message the error message
   * @param columnName the column name where the error occurred
   * @param expectedType the expected type (may be null)
   * @param actualType the actual type encountered (may be null)
   * @param rowNumber the row number (0-based) where the error occurred, or -1 if not applicable
   * @param cause the underlying cause (may be null)
   */
  AvroSchemaException(String message, String columnName, String expectedType, String actualType,
                      int rowNumber, Throwable cause) {
    super(buildMessage(message, columnName, expectedType, actualType, rowNumber), cause)
    this.rawMessage = message
    this.columnName = columnName
    this.expectedType = expectedType
    this.actualType = actualType
    this.rowNumber = rowNumber
  }
  /**
   * @return the column name where the error occurred, or null if not applicable
   */
  String getColumnName() {
    return columnName
  }
  /**
   * @return the expected type, or null if not applicable
   */
  String getExpectedType() {
    return expectedType
  }
  /**
   * @return the actual type encountered, or null if not applicable
   */
  String getActualType() {
    return actualType
  }
  /**
   * @return the row number (0-based) where the error occurred, or -1 if not applicable
   */
  int getRowNumber() {
    return rowNumber
  }
  /**
   * Returns an equivalent exception with the row number attached, preserving the
   * column, expected/actual types, and cause. Exceptions that already carry a row
   * number are returned unchanged.
   *
   * @param rowNumber the row number (0-based) where the error occurred
   * @return this exception if it already has a row number, otherwise a copy with the row number set
   */
  AvroSchemaException withRowNumber(int rowNumber) {
    if (this.rowNumber >= 0) {
      return this
    }
    new AvroSchemaException(rawMessage, columnName, expectedType, actualType, rowNumber, cause)
  }
  private static String buildMessage(String message, String columnName, String expectedType, String actualType,
                                     int rowNumber) {
    StringBuilder sb = new StringBuilder(message)
    List<String> context = []
    if (columnName != null) {
      context << 'column: ' + columnName
    }
    if (rowNumber >= 0) {
      context << 'row: ' + rowNumber
    }
    if (!context.isEmpty()) {
      sb.append(' [').append(context.join(JOIN_DELIMITER)).append(']')
    }
    List<String> details = []
    if (expectedType != null) {
      details << 'expected: ' + expectedType
    }
    if (actualType != null) {
      details << 'actual: ' + actualType
    }
    if (!details.isEmpty()) {
      sb.append(' (').append(details.join(JOIN_DELIMITER)).append(')')
    }
    return sb.toString()
  }

}
