package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Column

/**
 * Internal helpers for cumulative column operations.
 *
 * <p>Null values are skipped: the output at a null position is null and the running
 * accumulator is unchanged. An all-null prefix yields null results until the first
 * non-null element.</p>
 */
@CompileStatic
class CumulativeHelper {

  /**
   * Compute the cumulative sum of a numeric column.
   *
   * @param source the source column (must be numeric)
   * @return a new column containing cumulative sums (BigDecimal)
   * @throws IllegalArgumentException if the column is not numeric
   */
  static Column cumsum(Column source) {
    requireNumeric(source, 'cumsum')
    Column result = new Column(source.name, BigDecimal)
    BigDecimal accumulator = null
    for (int i = 0; i < source.size(); i++) {
      Object element = source.get(i)
      if (element == null) {
        result.add(null)
      } else {
        BigDecimal value = (element as Number) as BigDecimal
        accumulator = accumulator == null ? value : accumulator + value
        result.add(accumulator)
      }
    }
    result
  }

  /**
   * Compute the cumulative product of a numeric column.
   *
   * @param source the source column (must be numeric)
   * @return a new column containing cumulative products (BigDecimal)
   * @throws IllegalArgumentException if the column is not numeric
   */
  static Column cumprod(Column source) {
    requireNumeric(source, 'cumprod')
    Column result = new Column(source.name, BigDecimal)
    BigDecimal accumulator = null
    for (int i = 0; i < source.size(); i++) {
      Object element = source.get(i)
      if (element == null) {
        result.add(null)
      } else {
        BigDecimal value = (element as Number) as BigDecimal
        accumulator = accumulator == null ? value : accumulator * value
        result.add(accumulator)
      }
    }
    result
  }

  /**
   * Compute the cumulative minimum of a Comparable column.
   *
   * @param source the source column (values must be Comparable)
   * @return a new column containing cumulative minima (preserves source type)
   * @throws IllegalArgumentException if non-null values are not Comparable
   */
  static Column cummin(Column source) {
    requireComparable(source, 'cummin')
    Class resultType = source.type != null ? source.type : Object
    Column result = new Column(source.name, resultType)
    Comparable accumulator = null
    for (int i = 0; i < source.size(); i++) {
      Object element = source.get(i)
      if (element == null) {
        result.add(null)
      } else {
        Comparable value = element as Comparable
        accumulator = accumulator == null ? value : (value.compareTo(accumulator) < 0 ? value : accumulator)
        result.add(accumulator)
      }
    }
    result
  }

  /**
   * Compute the cumulative maximum of a Comparable column.
   *
   * @param source the source column (values must be Comparable)
   * @return a new column containing cumulative maxima (preserves source type)
   * @throws IllegalArgumentException if non-null values are not Comparable
   */
  static Column cummax(Column source) {
    requireComparable(source, 'cummax')
    Class resultType = source.type != null ? source.type : Object
    Column result = new Column(source.name, resultType)
    Comparable accumulator = null
    for (int i = 0; i < source.size(); i++) {
      Object element = source.get(i)
      if (element == null) {
        result.add(null)
      } else {
        Comparable value = element as Comparable
        accumulator = accumulator == null ? value : (value.compareTo(accumulator) > 0 ? value : accumulator)
        result.add(accumulator)
      }
    }
    result
  }

  private static void requireNumeric(Column source, String operationName) {
    if (source == null) {
      throw new IllegalArgumentException("${operationName} requires a non-null column")
    }
    if (!source.isEmpty() && !RollingWindowHelper.isNumericColumn(source)) {
      throw new IllegalArgumentException("${operationName} requires a numeric column")
    }
  }

  private static void requireComparable(Column source, String operationName) {
    if (source == null) {
      throw new IllegalArgumentException("${operationName} requires a non-null column")
    }
    if (!source.isEmpty()) {
      List<?> presentValues = RollingWindowHelper.nonNullValues(source)
      if (!presentValues.isEmpty() && !presentValues.every { it instanceof Comparable }) {
        throw new IllegalArgumentException("${operationName} requires a column with Comparable values")
      }
    }
  }
}
