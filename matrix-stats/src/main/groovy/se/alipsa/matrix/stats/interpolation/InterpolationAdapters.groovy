package se.alipsa.matrix.stats.interpolation

import groovy.transform.PackageScope

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Internal conversion and validation helpers for interpolation utilities.
 */
@PackageScope
final class InterpolationAdapters {

  private InterpolationAdapters() {
  }

  static List<BigDecimal> validateCoordinates(List<BigDecimal> values, String label) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    for (int i = 0; i < values.size(); i++) {
      if (values[i] == null) {
        throw new IllegalArgumentException("${label.capitalize()} must contain only numeric values")
      }
    }
    [*values]
  }

  static List<BigDecimal> validateDomain(List<BigDecimal> x) {
    List<BigDecimal> domain = validateCoordinates(x, 'x')
    for (int i = 1; i < domain.size(); i++) {
      if (domain[i] <= domain[i - 1]) {
        throw new IllegalArgumentException('X values must be strictly increasing')
      }
    }
    domain
  }

  static List<BigDecimal> validateRange(List<BigDecimal> y, int expectedLength) {
    List<BigDecimal> range = validateCoordinates(y, 'y')
    if (range.size() != expectedLength) {
      throw new IllegalArgumentException("X length (${expectedLength}) must match y length (${range.size()})")
    }
    range
  }

  static BigDecimal validateTarget(Number target, String label) {
    NumericConversion.toBigDecimal(target, label)
  }

}
