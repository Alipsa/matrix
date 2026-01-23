package se.alipsa.matrix.avro.exceptions

import groovy.transform.CompileStatic

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
 *     System.err.println("Validation error: " + e.getMessage())
 *     if (e.getSuggestion() != null) {
 *         System.err.println("  Suggestion: " + e.getSuggestion())
 *     }
 * }
 * }</pre>
 */
@CompileStatic
class AvroValidationException extends IllegalArgumentException {

  /** The parameter name that failed validation, if applicable */
  private final String parameterName

  /** A helpful suggestion for fixing the error */
  private final String suggestion

  /**
   * Creates a new AvroValidationException with a message.
   *
   * @param message the error message
   */
  AvroValidationException(String message) {
    super(message)
    this.parameterName = null
    this.suggestion = null
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
  }

  /**
   * Creates a new AvroValidationException with contextual information.
   *
   * @param message the error message
   * @param parameterName the parameter name that failed validation
   * @param suggestion a helpful suggestion for fixing the error
   */
  AvroValidationException(String message, String parameterName, String suggestion) {
    super(buildMessage(message, parameterName, suggestion))
    this.parameterName = parameterName
    this.suggestion = suggestion
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
    super(buildMessage(message, parameterName, suggestion), cause)
    this.parameterName = parameterName
    this.suggestion = suggestion
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

  private static String buildMessage(String message, String parameterName, String suggestion) {
    StringBuilder sb = new StringBuilder(message)
    if (parameterName != null) {
      sb.append(" [parameter: ").append(parameterName).append("]")
    }
    if (suggestion != null) {
      sb.append(". Suggestion: ").append(suggestion)
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
        "Matrix must have at least one column",
        "matrix",
        "Add at least one column to the Matrix before writing"
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
        "file",
        "Check that the file path is correct and the file exists"
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
        "file",
        "Provide a path to a file, not a directory"
    )
  }
}
