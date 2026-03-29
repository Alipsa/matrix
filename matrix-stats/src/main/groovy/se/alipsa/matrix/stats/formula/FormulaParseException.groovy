package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Exception raised when a formula cannot be tokenized or parsed.
 */
@CompileStatic
class FormulaParseException extends IllegalArgumentException {

  final int position

  /**
   * Creates a new parse exception.
   *
   * @param message the human-readable parse error
   * @param position the zero-based character position where parsing failed
   */
  FormulaParseException(String message, int position) {
    super(message)
    this.position = position
  }
}
