package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

/**
 * Typed options for loess local regression.
 */
@CompileStatic
final class LoessOptions implements FitOptions {
  final double span
  final int degree

  LoessOptions(double span = 0.75d, int degree = 1) {
    if (span <= 0.0d || span > 1.0d) {
      throw new IllegalArgumentException("span must be in (0, 1], got ${span}")
    }
    if (degree < 1 || degree > 2) {
      throw new IllegalArgumentException("degree must be 1 or 2, got ${degree}")
    }
    this.span = span
    this.degree = degree
  }
}
