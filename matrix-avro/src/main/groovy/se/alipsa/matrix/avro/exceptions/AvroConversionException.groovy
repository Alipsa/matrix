package se.alipsa.matrix.avro.exceptions

import groovy.transform.CompileStatic

/**
 * Exception thrown when there is an error converting between Avro and Java types.
 *
 * <p>This exception is thrown in scenarios such as:
 * <ul>
 *   <li>Incompatible type conversions</li>
 *   <li>Overflow or precision loss during numeric conversion</li>
 *   <li>Invalid date/time format</li>
 *   <li>Unsupported logical type conversion</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * try {
 *     Matrix m = MatrixAvroReader.read(file)
 * } catch (AvroConversionException e) {
 *     System.err.println("Conversion error at row " + e.getRowNumber() + ": " + e.getMessage())
 *     if (e.getColumnName() != null) {
 *         System.err.println("  Column: " + e.getColumnName())
 *     }
 * }
 * }</pre>
 */
@CompileStatic
class AvroConversionException extends RuntimeException {

  /** The column name associated with this error, if applicable */
  private final String columnName

  /** The row number (0-based) where the error occurred, or -1 if not applicable */
  private final int rowNumber

  /** The source type that could not be converted */
  private final String sourceType

  /** The target type for the conversion */
  private final String targetType

  /** The value that could not be converted */
  private final Object value

  /**
   * Creates a new AvroConversionException with a message.
   *
   * @param message the error message
   */
  AvroConversionException(String message) {
    super(message)
    this.columnName = null
    this.rowNumber = -1
    this.sourceType = null
    this.targetType = null
    this.value = null
  }

  /**
   * Creates a new AvroConversionException with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  AvroConversionException(String message, Throwable cause) {
    super(message, cause)
    this.columnName = null
    this.rowNumber = -1
    this.sourceType = null
    this.targetType = null
    this.value = null
  }

  /**
   * Creates a new AvroConversionException with full contextual information.
   *
   * @param message the error message
   * @param columnName the column name where the error occurred
   * @param rowNumber the row number (0-based) where the error occurred, or -1 if not applicable
   * @param sourceType the source type that could not be converted
   * @param targetType the target type for the conversion
   * @param value the value that could not be converted
   */
  AvroConversionException(String message, String columnName, int rowNumber,
                          String sourceType, String targetType, Object value) {
    super(buildMessage(message, columnName, rowNumber, sourceType, targetType, value))
    this.columnName = columnName
    this.rowNumber = rowNumber
    this.sourceType = sourceType
    this.targetType = targetType
    this.value = value
  }

  /**
   * Creates a new AvroConversionException with full contextual information and a cause.
   *
   * @param message the error message
   * @param columnName the column name where the error occurred
   * @param rowNumber the row number (0-based) where the error occurred, or -1 if not applicable
   * @param sourceType the source type that could not be converted
   * @param targetType the target type for the conversion
   * @param value the value that could not be converted
   * @param cause the underlying cause
   */
  AvroConversionException(String message, String columnName, int rowNumber,
                          String sourceType, String targetType, Object value, Throwable cause) {
    super(buildMessage(message, columnName, rowNumber, sourceType, targetType, value), cause)
    this.columnName = columnName
    this.rowNumber = rowNumber
    this.sourceType = sourceType
    this.targetType = targetType
    this.value = value
  }

  /**
   * @return the column name where the error occurred, or null if not applicable
   */
  String getColumnName() {
    return columnName
  }

  /**
   * @return the row number (0-based) where the error occurred, or -1 if not applicable
   */
  int getRowNumber() {
    return rowNumber
  }

  /**
   * @return the source type that could not be converted, or null if not applicable
   */
  String getSourceType() {
    return sourceType
  }

  /**
   * @return the target type for the conversion, or null if not applicable
   */
  String getTargetType() {
    return targetType
  }

  /**
   * @return the value that could not be converted, or null if not applicable
   */
  Object getValue() {
    return value
  }

  private static String buildMessage(String message, String columnName, int rowNumber,
                                     String sourceType, String targetType, Object value) {
    StringBuilder sb = new StringBuilder(message)
    List<String> context = new ArrayList<>()

    if (columnName != null) {
      context.add("column: " + columnName)
    }
    if (rowNumber >= 0) {
      context.add("row: " + rowNumber)
    }
    if (sourceType != null && targetType != null) {
      context.add(sourceType + " -> " + targetType)
    }
    if (value != null) {
      String valueStr = value.toString()
      if (valueStr.length() > 50) {
        valueStr = valueStr.substring(0, 47) + "..."
      }
      context.add("value: " + valueStr)
    }

    if (!context.isEmpty()) {
      sb.append(" [").append(String.join(", ", context)).append("]")
    }
    return sb.toString()
  }
}
