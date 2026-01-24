package se.alipsa.matrix.avro.exceptions

import groovy.transform.CompileStatic

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
 *     System.err.println("Schema error: " + e.getMessage())
 *     if (e.getColumnName() != null) {
 *         System.err.println("  Column: " + e.getColumnName())
 *     }
 * }
 * }</pre>
 */
@CompileStatic
class AvroSchemaException extends RuntimeException {

  /** The column name associated with this error, if applicable */
  private final String columnName

  /** The expected type, if applicable */
  private final String expectedType

  /** The actual type encountered, if applicable */
  private final String actualType

  /**
   * Creates a new AvroSchemaException with a message.
   *
   * @param message the error message
   */
  AvroSchemaException(String message) {
    super(message)
    this.columnName = null
    this.expectedType = null
    this.actualType = null
  }

  /**
   * Creates a new AvroSchemaException with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  AvroSchemaException(String message, Throwable cause) {
    super(message, cause)
    this.columnName = null
    this.expectedType = null
    this.actualType = null
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
    super(buildMessage(message, columnName, expectedType, actualType))
    this.columnName = columnName
    this.expectedType = expectedType
    this.actualType = actualType
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
    super(buildMessage(message, columnName, expectedType, actualType), cause)
    this.columnName = columnName
    this.expectedType = expectedType
    this.actualType = actualType
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

  private static String buildMessage(String message, String columnName, String expectedType, String actualType) {
    StringBuilder sb = new StringBuilder(message)
    if (columnName != null) {
      sb.append(" [column: ").append(columnName).append("]")
    }
    if (expectedType != null && actualType != null) {
      sb.append(" (expected: ").append(expectedType).append(", actual: ").append(actualType).append(")")
    } else if (expectedType != null) {
      sb.append(" (expected: ").append(expectedType).append(")")
    } else if (actualType != null) {
      sb.append(" (actual: ").append(actualType).append(")")
    }
    return sb.toString()
  }
}
