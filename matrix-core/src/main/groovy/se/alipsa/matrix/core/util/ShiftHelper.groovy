package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Column

/**
 * Internal helpers for shift, lag, lead, and diff column operations.
 *
 * <p>Boundary positions are padded with null. Null elements in the source are
 * repositioned as-is for shift/lag/lead, and produce null in diff results.</p>
 */
@CompileStatic
class ShiftHelper {

  /**
   * Shift column values by the given number of positions.
   *
   * <p>Positive periods shift forward (nulls pad the start).
   * Negative periods shift backward (nulls pad the end).
   * Zero returns a copy.</p>
   *
   * @param source the source column
   * @param periods the number of positions to shift
   * @return a new column with shifted values
   */
  static Column shift(Column source, int periods) {
    requireNonNull(source, 'shift')
    int size = source.size()
    Class resultType = source.type != null ? source.type : Object
    Column result = new Column(source.name, resultType)
    int absPeriods = Math.abs(periods)
    if (size == 0) {
      return result
    }
    if (absPeriods >= size) {
      for (int i = 0; i < size; i++) {
        result.add(null)
      }
      return result
    }
    if (periods > 0) {
      for (int i = 0; i < periods; i++) {
        result.add(null)
      }
      for (int i = 0; i < size - periods; i++) {
        result.add(source.get(i))
      }
    } else if (periods < 0) {
      for (int i = absPeriods; i < size; i++) {
        result.add(source.get(i))
      }
      for (int i = 0; i < absPeriods; i++) {
        result.add(null)
      }
    } else {
      for (int i = 0; i < size; i++) {
        result.add(source.get(i))
      }
    }
    result
  }

  /**
   * Return the column lagged by {@code n} positions (look back).
   *
   * <p>Equivalent to {@code shift(source, n)}.</p>
   *
   * @param source the source column
   * @param n the number of positions to lag (must be non-negative)
   * @return a new column with lagged values
   * @throws IllegalArgumentException if n is negative
   */
  static Column lag(Column source, int n) {
    if (n < 0) {
      throw new IllegalArgumentException("lag requires a non-negative period but got ${n}")
    }
    shift(source, n)
  }

  /**
   * Return the column led by {@code n} positions (look ahead).
   *
   * <p>Equivalent to {@code shift(source, -n)}.</p>
   *
   * @param source the source column
   * @param n the number of positions to lead (must be non-negative)
   * @return a new column with led values
   * @throws IllegalArgumentException if n is negative
   */
  static Column lead(Column source, int n) {
    if (n < 0) {
      throw new IllegalArgumentException("lead requires a non-negative period but got ${n}")
    }
    shift(source, -n)
  }

  /**
   * Compute element-wise differences with a lagged version of the column.
   *
   * <p>For each position {@code i}, computes {@code column[i] - column[i - periods]}.
   * Boundary positions and positions referencing or containing null yield null.
   * Requires a numeric column.</p>
   *
   * @param source the source column (must be numeric)
   * @param periods the lag distance for differencing
   * @return a new column containing the differences (BigDecimal)
   * @throws IllegalArgumentException if the column is not numeric
   */
  static Column diff(Column source, int periods) {
    CumulativeHelper.requireNumeric(source, 'diff')
    int size = source.size()
    Column result = new Column(source.name, BigDecimal)
    for (int i = 0; i < size; i++) {
      int refIndex = i - periods
      if (refIndex < 0 || refIndex >= size) {
        result.add(null)
      } else {
        Object current = source.get(i)
        Object reference = source.get(refIndex)
        if (current == null || reference == null) {
          result.add(null)
        } else {
          BigDecimal currentVal = CumulativeHelper.numericValue(current, source, 'diff')
          BigDecimal referenceVal = CumulativeHelper.numericValue(reference, source, 'diff')
          result.add(currentVal - referenceVal)
        }
      }
    }
    result
  }

  private static void requireNonNull(Column source, String operationName) {
    if (source == null) {
      throw new IllegalArgumentException("${operationName} requires a non-null column")
    }
  }
}
