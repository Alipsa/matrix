package se.alipsa.matrix.stats.linear

import groovy.transform.CompileStatic

import se.alipsa.matrix.stats.linalg.LinalgSingularMatrixException

/**
 * Internal specialization of {@link LinalgSingularMatrixException} used by the legacy
 * {@code stats.linear} implementation package.
 */
@CompileStatic
class SingularMatrixException extends LinalgSingularMatrixException {

  SingularMatrixException(String message) {
    super(message)
  }
}
