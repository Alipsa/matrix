package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Base exception type for Charm domain, mapping, and rendering errors.
 */
@CompileStatic
class CharmException extends RuntimeException {

  /**
   * Creates a new CharmException with a message.
   *
   * @param message the detail message
   */
  CharmException(String message) {
    super(message)
  }

  /**
   * Creates a new CharmException with a message and cause.
   *
   * @param message the detail message
   * @param cause the originating cause
   */
  CharmException(String message, Throwable cause) {
    super(message, cause)
  }
}

/**
 * Indicates invalid chart specifications and validation failures.
 */
@CompileStatic
class CharmValidationException extends CharmException {

  /**
   * Creates a new CharmValidationException with a message.
   *
   * @param message the detail message
   */
  CharmValidationException(String message) {
    super(message)
  }

  /**
   * Creates a new CharmValidationException with a message and cause.
   *
   * @param message the detail message
   * @param cause the originating cause
   */
  CharmValidationException(String message, Throwable cause) {
    super(message, cause)
  }
}

/**
 * Indicates invalid aesthetic and mapping input.
 */
@CompileStatic
class CharmMappingException extends CharmException {

  /**
   * Creates a new CharmMappingException with a message.
   *
   * @param message the detail message
   */
  CharmMappingException(String message) {
    super(message)
  }

  /**
   * Creates a new CharmMappingException with a message and cause.
   *
   * @param message the detail message
   * @param cause the originating cause
   */
  CharmMappingException(String message, Throwable cause) {
    super(message, cause)
  }
}

/**
 * Indicates failures during mutable-spec to immutable-chart compilation.
 */
@CompileStatic
class CharmCompilationException extends CharmException {

  /**
   * Creates a new CharmCompilationException with a message.
   *
   * @param message the detail message
   */
  CharmCompilationException(String message) {
    super(message)
  }

  /**
   * Creates a new CharmCompilationException with a message and cause.
   *
   * @param message the detail message
   * @param cause the originating cause
   */
  CharmCompilationException(String message, Throwable cause) {
    super(message, cause)
  }
}

/**
 * Indicates chart rendering failures.
 */
@CompileStatic
class CharmRenderException extends CharmException {

  /**
   * Creates a new CharmRenderException with a message.
   *
   * @param message the detail message
   */
  CharmRenderException(String message) {
    super(message)
  }

  /**
   * Creates a new CharmRenderException with a message and cause.
   *
   * @param message the detail message
   * @param cause the originating cause
   */
  CharmRenderException(String message, Throwable cause) {
    super(message, cause)
  }
}
