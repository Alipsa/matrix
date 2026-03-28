package se.alipsa.matrix.stats.linear

import groovy.transform.CompileStatic

/**
 * Signals that a matrix operation failed because the matrix is singular or numerically singular.
 */
@CompileStatic
class SingularMatrixException extends IllegalArgumentException {

  SingularMatrixException(String message) {
    super(message)
  }
}
