package se.alipsa.matrix.stats.interpolation

import groovy.transform.PackageScope

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Internal conversion and validation helpers for interpolation utilities.
 */
@PackageScope
final class InterpolationAdapters {

  private InterpolationAdapters() {
  }

  static double[] toDoubleArray(Matrix matrix, String columnName) {
    NumericConversion.toDoubleArray(matrix, columnName)
  }

  static double[] toDoubleArray(Grid<?> grid, int columnIndex) {
    NumericConversion.toDoubleArray(grid, columnIndex)
  }

  static double[] validateCoordinates(double[] values, String label) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    double[] copy = new double[values.length]
    for (int i = 0; i < values.length; i++) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException("${label.capitalize()} must contain only finite values")
      }
      copy[i] = values[i]
    }
    copy
  }

  static double[] validateDomain(double[] x) {
    double[] domain = validateCoordinates(x, 'x')
    for (int i = 1; i < domain.length; i++) {
      if (domain[i] <= domain[i - 1]) {
        throw new IllegalArgumentException('X values must be strictly increasing')
      }
    }
    domain
  }

  static double[] validateRange(double[] y, int expectedLength) {
    double[] range = validateCoordinates(y, 'y')
    if (range.length != expectedLength) {
      throw new IllegalArgumentException("X length (${expectedLength}) must match y length (${range.length})")
    }
    range
  }

  static double validateTarget(double target, String label) {
    if (!Double.isFinite(target)) {
      throw new IllegalArgumentException("${label.capitalize()} must be finite")
    }
    target
  }
}
