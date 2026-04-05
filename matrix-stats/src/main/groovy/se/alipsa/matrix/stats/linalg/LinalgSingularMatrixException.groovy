package se.alipsa.matrix.stats.linalg

import groovy.transform.CompileStatic

/**
 * Signals that a linear algebra operation requires a non-singular matrix but the
 * supplied coefficient matrix is singular or numerically rank-deficient.
 */
@CompileStatic
class LinalgSingularMatrixException extends IllegalArgumentException {

  LinalgSingularMatrixException(String message) {
    super(message)
  }

  LinalgSingularMatrixException(String message, Throwable cause) {
    super(message, cause)
  }
}
