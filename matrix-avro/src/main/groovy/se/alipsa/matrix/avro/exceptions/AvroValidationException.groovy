package se.alipsa.matrix.avro.exceptions

/**
 * Exception thrown when input validation fails for Avro operations.
 *
 * <p>This exception is thrown in scenarios such as:
 * <ul>
 *   <li>Null parameter validation failures</li>
 *   <li>Empty or invalid Matrix</li>
 *   <li>File access issues (not exists, is directory)</li>
 *   <li>Invalid configuration options</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * try {
 *     MatrixAvroWriter.write(matrix, file)
 * } catch (AvroValidationException e) {
 *     System.err.println('Validation error: ' + e.getMessage())
 *     if (e.getSuggestion() != null) {
 *         System.err.println('  Suggestion: ' + e.getSuggestion())
 *     }
 * }
 * }</pre>
 */
class AvroValidationException extends IllegalArgumentException {

  private static final int NO_ROW = -1
  private static final String MATRIX_PARAMETER = 'matrix'
  private static final String FILE_PARAMETER = 'file'
  /** The parameter name that failed validation, if applicable */
  private final String parameterName
  /** A helpful suggestion for fixing the error */
  private final String suggestion
  /** The row number associated with this error, or -1 if not applicable */
  private final int rowNumber
  /**
   * Creates a new AvroValidationException with a message.
   *
   * @param message the error message
   */
  AvroValidationException(String message) {
    super(message)
    this.parameterName = null
    this.suggestion = null
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroValidationException with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  AvroValidationException(String message, Throwable cause) {
    super(message, cause)
    this.parameterName = null
    this.suggestion = null
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroValidationException with contextual information.
   *
   * @param message the error message
   * @param parameterName the parameter name that failed validation
   * @param suggestion a helpful suggestion for fixing the error
   */
  AvroValidationException(String message, String parameterName, String suggestion) {
    super(buildMessage(message, parameterName, NO_ROW, suggestion))
    this.parameterName = parameterName
    this.suggestion = suggestion
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroValidationException with contextual information and a cause.
   *
   * @param message the error message
   * @param parameterName the parameter name that failed validation
   * @param suggestion a helpful suggestion for fixing the error
   * @param cause the underlying cause
   */
  AvroValidationException(String message, String parameterName, String suggestion, Throwable cause) {
    super(buildMessage(message, parameterName, NO_ROW, suggestion), cause)
    this.parameterName = parameterName
    this.suggestion = suggestion
    this.rowNumber = NO_ROW
  }
  /**
   * Creates a new AvroValidationException with contextual information and row number.
   *
   * @param message the error message
   * @param parameterName the parameter name that failed validation
   * @param rowNumber the row number where validation failed (0-based)
   * @param suggestion a helpful suggestion for fixing the error
   */
  AvroValidationException(String message, String parameterName, int rowNumber, String suggestion) {
    super(buildMessage(message, parameterName, rowNumber, suggestion))
    this.parameterName = parameterName
    this.suggestion = suggestion
    this.rowNumber = rowNumber
  }
  /**
   * @return the parameter name that failed validation, or null if not applicable
   */
  String getParameterName() {
    return parameterName
  }
  /**
   * @return a helpful suggestion for fixing the error, or null if none available
   */
  String getSuggestion() {
    return suggestion
  }
  /**
   * @return the row number where validation failed, or -1 if not applicable
   */
  int getRowNumber() {
    return rowNumber
  }
  private static String buildMessage(String message, String parameterName, int rowNumber, String suggestion) {
    StringBuilder sb = new StringBuilder(message)
    List<String> context = []
    if (parameterName != null) {
      context.add('parameter: ' + parameterName)
    }
    if (rowNumber >= 0) {
      context.add('row: ' + rowNumber)
    }
    if (!context.isEmpty()) {
      sb.append(' [').append(String.join(', ', context)).append(']')
    }
    if (suggestion != null) {
      sb.append('. Suggestion: ').append(suggestion)
    }
    return sb.toString()
  }
  /**
   * Creates an exception for a null parameter.
   *
   * @param parameterName the parameter that was null
   * @return a new AvroValidationException
   */
  static AvroValidationException nullParameter(String parameterName) {
    return new AvroValidationException(
        "${parameterName} cannot be null",
        parameterName,
        "Provide a non-null value for ${parameterName}"
    )
  }
  /**
   * Creates an exception for an empty Matrix.
   *
   * @return a new AvroValidationException
   */
  static AvroValidationException emptyMatrix() {
    return new AvroValidationException(
        'Matrix must have at least one column',
        MATRIX_PARAMETER,
        'Add at least one column to the Matrix before writing'
    )
  }
  /**
   * Creates an exception for uneven column lengths.
   *
   * @param columnName the column with mismatched length
   * @param rowNumber the row index where mismatch begins (0-based)
   * @param columnSize the size of the offending column
   * @param rowCount the expected row count
   * @return a new AvroValidationException
   */
  static AvroValidationException columnSizeMismatch(String columnName, int rowNumber, int columnSize, int rowCount) {
    return new AvroValidationException(
        "Column '${columnName}' size (${columnSize}) does not match matrix row count (${rowCount})",
        MATRIX_PARAMETER,
        rowNumber,
        'Ensure all columns have the same number of rows before writing'
    )
  }
  /**
   * Creates an exception for a file that doesn't exist.
   *
   * @param path the file path that doesn't exist
   * @return a new AvroValidationException
   */
  static AvroValidationException fileNotFound(String path) {
    return new AvroValidationException(
        "File does not exist: ${path}",
        FILE_PARAMETER,
        'Check that the file path is correct and the file exists'
    )
  }
  /**
   * Creates an exception for a path that is a directory instead of a file.
   *
   * @param path the directory path
   * @return a new AvroValidationException
   */
  static AvroValidationException isDirectory(String path) {
    return new AvroValidationException(
        "Expected a file but got a directory: ${path}",
        FILE_PARAMETER,
        'Provide a path to a file, not a directory'
    )
  }

}
