package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

/**
 * Typed options for GAM penalized least squares.
 */
@CompileStatic
final class GamOptions implements FitOptions {
  final double lambda

  GamOptions(double lambda = 1.0d) {
    if (lambda < 0.0d) {
      throw new IllegalArgumentException("lambda must be >= 0, got ${lambda}")
    }
    this.lambda = lambda
  }
}
