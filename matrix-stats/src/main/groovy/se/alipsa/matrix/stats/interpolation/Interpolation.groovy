package se.alipsa.matrix.stats.interpolation

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Public interpolation utilities for `matrix-stats`.
 * <p>
 * The v2.4.0 public API intentionally focuses on linear interpolation in
 * floating-point {@code double} precision. The primitive operation interpolates
 * a target x-position from explicit {@code (x, y)} observations. Convenience
 * overloads also support evenly spaced numeric series and Matrix/Grid-backed
 * numeric columns.
 * <p>
 * Public spline interpolation is not part of this API in v2.4.0. The existing
 * spline math in the formula package remains internal to GAM smooth-term support.
 */
final class Interpolation {

  private Interpolation() {
  }

  /**
   * Linearly interpolate a y-value from explicit {@code (x, y)} observations.
   *
   * @param x strictly increasing x-values
   * @param y y-values aligned with {@code x}
   * @param targetX the x-position to interpolate
   * @return the interpolated y-value
   * @throws IllegalArgumentException if the inputs are empty, contain non-finite values,
   * are not strictly increasing, have mismatched lengths, or require extrapolation
   */
  static double linear(double[] x, double[] y, double targetX) {
    double[] domain = InterpolationAdapters.validateDomain(x)
    double[] range = InterpolationAdapters.validateRange(y, domain.length)
    double target = InterpolationAdapters.validateTarget(targetX, 'targetX')

    if (target < domain[0] || target > domain[domain.length - 1]) {
      throw new IllegalArgumentException("TargetX ${target} is outside the interpolation domain [${domain[0]}, ${domain[domain.length - 1]}]")
    }

    if (domain.length == 1) {
      return target == domain[0] ? range[0] : outsideSinglePointDomain(target, domain[0])
    }

    if (target == domain[0]) {
      return range[0]
    }

    for (int i = 1; i < domain.length; i++) {
      if (target == domain[i]) {
        return range[i]
      }
      if (target < domain[i]) {
        return interpolate(domain[i - 1], range[i - 1], domain[i], range[i], target)
      }
    }
    range[range.length - 1]
  }

  /**
   * Linearly interpolate a y-value from explicit {@code (x, y)} observations.
   *
   * @param x strictly increasing x-values
   * @param y y-values aligned with {@code x}
   * @param targetX the x-position to interpolate
   * @return the interpolated y-value
   * @throws IllegalArgumentException if the inputs are invalid or extrapolation is required
   */
  static double linear(List<? extends Number> x, List<? extends Number> y, Number targetX) {
    if (targetX == null) {
      throw new IllegalArgumentException('TargetX cannot be null')
    }
    linear(
      NumericConversion.toDoubleArray(x, 'x'),
      NumericConversion.toDoubleArray(y, 'y'),
      NumericConversion.toFiniteDouble(targetX, 'targetX')
    )
  }

  /**
   * Linearly interpolate within an evenly spaced numeric series using zero-based row positions.
   *
   * @param values the series values
   * @param targetIndex the zero-based position to interpolate
   * @return the interpolated value
   * @throws IllegalArgumentException if the series is invalid or extrapolation is required
   */
  static double linear(double[] values, double targetIndex) {
    double[] series = InterpolationAdapters.validateCoordinates(values, 'values')
    double index = InterpolationAdapters.validateTarget(targetIndex, 'targetIndex')

    if (index < 0.0d || index > series.length - 1) {
      throw new IllegalArgumentException("TargetIndex ${index} is outside the interpolation domain [0.0, ${series.length - 1}.0]")
    }

    if (series.length == 1) {
      return index == 0.0d ? series[0] : outsideSinglePointDomain(index, 0.0d, 'targetIndex')
    }

    int lower = Math.floor(index) as int
    if (index == lower || lower == series.length - 1) {
      return series[lower]
    }

    int upper = lower + 1
    interpolate(lower, series[lower], upper, series[upper], index)
  }

  /**
   * Linearly interpolate within an evenly spaced numeric series using zero-based row positions.
   *
   * @param values the series values
   * @param targetIndex the zero-based position to interpolate
   * @return the interpolated value
   * @throws IllegalArgumentException if the series is invalid or extrapolation is required
   */
  static double linear(List<? extends Number> values, Number targetIndex) {
    if (targetIndex == null) {
      throw new IllegalArgumentException('TargetIndex cannot be null')
    }
    linear(
      NumericConversion.toDoubleArray(values, 'values'),
      NumericConversion.toFiniteDouble(targetIndex, 'targetIndex')
    )
  }

  /**
   * Linearly interpolate a value from two numeric columns in a Matrix.
   *
   * @param data the source matrix
   * @param xColumn the x column name
   * @param yColumn the y column name
   * @param targetX the x-position to interpolate
   * @return the interpolated y-value
   * @throws IllegalArgumentException if the matrix or columns are invalid
   */
  static double linear(Matrix data, String xColumn, String yColumn, Number targetX) {
    if (targetX == null) {
      throw new IllegalArgumentException('TargetX cannot be null')
    }
    linear(
      InterpolationAdapters.toDoubleArray(data, xColumn),
      InterpolationAdapters.toDoubleArray(data, yColumn),
      NumericConversion.toFiniteDouble(targetX, 'targetX')
    )
  }

  /**
   * Linearly interpolate a value from two numeric columns in a Grid.
   *
   * @param grid the source grid
   * @param xColumn the zero-based x column index
   * @param yColumn the zero-based y column index
   * @param targetX the x-position to interpolate
   * @return the interpolated y-value
   * @throws IllegalArgumentException if the grid or columns are invalid
   */
  static double linear(Grid<?> grid, int xColumn, int yColumn, Number targetX) {
    if (targetX == null) {
      throw new IllegalArgumentException('TargetX cannot be null')
    }
    linear(
      InterpolationAdapters.toDoubleArray(grid, xColumn),
      InterpolationAdapters.toDoubleArray(grid, yColumn),
      NumericConversion.toFiniteDouble(targetX, 'targetX')
    )
  }

  /**
   * Linearly interpolate within a numeric Matrix column using zero-based row positions.
   *
   * @param data the source matrix
   * @param columnName the series column name
   * @param targetIndex the zero-based position to interpolate
   * @return the interpolated value
   * @throws IllegalArgumentException if the matrix or column is invalid
   */
  static double linear(Matrix data, String columnName, Number targetIndex) {
    if (targetIndex == null) {
      throw new IllegalArgumentException('TargetIndex cannot be null')
    }
    linear(
      InterpolationAdapters.toDoubleArray(data, columnName),
      NumericConversion.toFiniteDouble(targetIndex, 'targetIndex')
    )
  }

  /**
   * Linearly interpolate within a numeric Grid column using zero-based row positions.
   *
   * @param grid the source grid
   * @param columnIndex the zero-based series column index
   * @param targetIndex the zero-based position to interpolate
   * @return the interpolated value
   * @throws IllegalArgumentException if the grid or column is invalid
   */
  static double linear(Grid<?> grid, int columnIndex, Number targetIndex) {
    if (targetIndex == null) {
      throw new IllegalArgumentException('TargetIndex cannot be null')
    }
    linear(
      InterpolationAdapters.toDoubleArray(grid, columnIndex),
      NumericConversion.toFiniteDouble(targetIndex, 'targetIndex')
    )
  }


  private static double interpolate(double x0, double y0, double x1, double y1, double targetX) {
    y0 + (targetX - x0) * (y1 - y0) / (x1 - x0)
  }

  private static double outsideSinglePointDomain(double target, double point, String label = 'targetX') {
    throw new IllegalArgumentException("${label.capitalize()} ${target} is outside the interpolation domain [${point}, ${point}]")
  }
}
