package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Typed options for GAM penalized least squares.
 */
final class GamOptions implements FitOptions {
  final double lambda

  GamOptions(Number lambda = 1.0) {
    double normalizedLambda = NumericConversion.toFiniteDouble(lambda, 'lambda')
    if (normalizedLambda < 0.0d) {
      throw new IllegalArgumentException("lambda must be >= 0, got ${lambda}")
    }
    this.lambda = normalizedLambda
  }
}
