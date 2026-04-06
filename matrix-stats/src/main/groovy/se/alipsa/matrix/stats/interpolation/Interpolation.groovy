package se.alipsa.matrix.stats.interpolation

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Public interpolation utilities for `matrix-stats`.
 * <p>
 * The v2.4.0 public API focuses on idiomatic Groovy numeric inputs and
 * {@code BigDecimal} scalar results. The primitive operation interpolates
 * a target x-position from explicit {@code (x, y)} observations. Convenience
 * overloads also support evenly spaced numeric series and Matrix/Grid-backed
 * numeric columns.
 * <p>
 * Public spline interpolation is not part of this API in v2.4.0. The existing
 * spline math in the formula package remains internal to GAM smooth-term support.
 */
final class Interpolation {

  private static final String TARGET_X_LABEL = 'targetX'
  private static final String TARGET_INDEX_LABEL = 'targetIndex'
  private static final String TARGET_X_NULL_MESSAGE = 'TargetX cannot be null'
  private static final String TARGET_INDEX_NULL_MESSAGE = 'TargetIndex cannot be null'

  private Interpolation() {
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
  static BigDecimal linear(List<? extends Number> x, List<? extends Number> y, Number targetX) {
    if (targetX == null) {
      throw new IllegalArgumentException(TARGET_X_NULL_MESSAGE)
    }
    if (x == null) {
      throw new IllegalArgumentException('X cannot be null')
    }
    if (y == null) {
      throw new IllegalArgumentException('Y cannot be null')
    }
    List<BigDecimal> domain = InterpolationAdapters.validateDomain(ListConverter.toBigDecimals(x))
    List<BigDecimal> range = InterpolationAdapters.validateRange(ListConverter.toBigDecimals(y), domain.size())
    BigDecimal target = InterpolationAdapters.validateTarget(targetX, TARGET_X_LABEL)

    if (target < domain[0] || target > domain[-1]) {
      throw new IllegalArgumentException("TargetX ${target} is outside the interpolation domain [${domain[0]}, ${domain[-1]}]")
    }

    if (domain.size() == 1) {
      return target == domain[0] ? range[0] : outsideSinglePointDomain(target, domain[0])
    }

    if (target == domain[0]) {
      return range[0]
    }

    for (int i = 1; i < domain.size(); i++) {
      if (target == domain[i]) {
        return range[i]
      }
      if (target < domain[i]) {
        return interpolate(domain[i - 1], range[i - 1], domain[i], range[i], target)
      }
    }
    range[-1]
  }

  /**
   * Linearly interpolate within an evenly spaced numeric series using zero-based row positions.
   *
   * @param values the series values
   * @param targetIndex the zero-based position to interpolate
   * @return the interpolated value
   * @throws IllegalArgumentException if the series is invalid or extrapolation is required
   */
  static BigDecimal linear(List<? extends Number> values, Number targetIndex) {
    if (targetIndex == null) {
      throw new IllegalArgumentException(TARGET_INDEX_NULL_MESSAGE)
    }
    if (values == null) {
      throw new IllegalArgumentException('Values cannot be null')
    }
    List<BigDecimal> series = InterpolationAdapters.validateCoordinates(ListConverter.toBigDecimals(values), 'values')
    BigDecimal index = InterpolationAdapters.validateTarget(targetIndex, TARGET_INDEX_LABEL)
    BigDecimal lastIndex = (series.size() - 1) as BigDecimal

    if (index < 0 || index > lastIndex) {
      throw new IllegalArgumentException("TargetIndex ${index} is outside the interpolation domain [0, ${lastIndex}]")
    }

    if (series.size() == 1) {
      return index == 0 ? series[0] : outsideSinglePointDomain(index, 0 as BigDecimal, TARGET_INDEX_LABEL)
    }

    int lower = index.intValue()
    if (index == lower || lower == series.size() - 1) {
      return series[lower]
    }

    int upper = lower + 1
    interpolate(lower as BigDecimal, series[lower], upper as BigDecimal, series[upper], index)
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
  static BigDecimal linear(Matrix data, String xColumn, String yColumn, Number targetX) {
    if (targetX == null) {
      throw new IllegalArgumentException(TARGET_X_NULL_MESSAGE)
    }
    linear(
      NumericConversion.toBigDecimalColumn(data, xColumn),
      NumericConversion.toBigDecimalColumn(data, yColumn),
      targetX
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
  static BigDecimal linear(Grid<?> grid, int xColumn, int yColumn, Number targetX) {
    if (targetX == null) {
      throw new IllegalArgumentException(TARGET_X_NULL_MESSAGE)
    }
    linear(
      NumericConversion.toBigDecimalColumn(grid, xColumn),
      NumericConversion.toBigDecimalColumn(grid, yColumn),
      targetX
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
  static BigDecimal linear(Matrix data, String columnName, Number targetIndex) {
    if (targetIndex == null) {
      throw new IllegalArgumentException(TARGET_INDEX_NULL_MESSAGE)
    }
    linear(
      NumericConversion.toBigDecimalColumn(data, columnName),
      targetIndex
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
  static BigDecimal linear(Grid<?> grid, int columnIndex, Number targetIndex) {
    if (targetIndex == null) {
      throw new IllegalArgumentException(TARGET_INDEX_NULL_MESSAGE)
    }
    linear(
      NumericConversion.toBigDecimalColumn(grid, columnIndex),
      targetIndex
    )
  }

  private static BigDecimal interpolate(BigDecimal x0, BigDecimal y0, BigDecimal x1, BigDecimal y1, BigDecimal targetX) {
    y0 + (targetX - x0) * (y1 - y0) / (x1 - x0)
  }

  private static BigDecimal outsideSinglePointDomain(BigDecimal target, BigDecimal point, String label = 'targetX') {
    throw new IllegalArgumentException("${label.capitalize()} ${target} is outside the interpolation domain [${point}, ${point}]")
  }
}
