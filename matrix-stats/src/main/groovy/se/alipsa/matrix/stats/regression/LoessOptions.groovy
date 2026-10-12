package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Typed options for loess local regression.
 */
final class LoessOptions implements FitOptions {
  final double span
  final int degree

  /**
   * Creates local polynomial regression options.
   * @param span neighborhood fraction in (0, 1], default 0.75
   * @param degree local polynomial degree (1 or 2), default 2
   */
  LoessOptions(Number span = 0.75, int degree = 2) {
    double normalizedSpan = NumericConversion.toFiniteDouble(span, 'span')
    if (normalizedSpan <= 0.0d || normalizedSpan > 1.0d) {
      throw new IllegalArgumentException("span must be in (0, 1], got ${span}")
    }
    if (degree < 1 || degree > 2) {
      throw new IllegalArgumentException("degree must be 1 or 2, got ${degree}")
    }
    this.span = normalizedSpan
    this.degree = degree
  }
}
