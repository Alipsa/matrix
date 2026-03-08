package se.alipsa.matrix.core

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.RollingWindowHelper
import se.alipsa.matrix.core.util.RollingWindowOptions

/**
 * Rolling window operations for a single {@link Column}.
 */
@CompileStatic
class RollingColumn {

  private final Column source
  private final RollingWindowOptions options

  /**
   * Create a rolling view over a column.
   *
   * @param source the source column
   * @param options the rolling options
   */
  RollingColumn(Column source, RollingWindowOptions options) {
    if (source == null) {
      throw new IllegalArgumentException('source column cannot be null')
    }
    if (options == null) {
      throw new IllegalArgumentException('rolling options cannot be null')
    }
    this.source = source
    this.options = options
  }

  /**
   * Compute the rolling mean for the column.
   *
   * Null values are ignored, and a row yields null when the window contains
   * fewer than {@code minPeriods} non-null numeric values.
   *
   * @return a new column containing the rolling means
   */
  Column mean() {
    aggregateNumeric('mean') { List<Number> values ->
      Stat.mean(values)
    }
  }

  /**
   * Compute the rolling sum for the column.
   *
   * Null values are ignored, and a row yields null when the window contains
   * fewer than {@code minPeriods} non-null numeric values.
   *
   * @return a new column containing the rolling sums
   */
  Column sum() {
    aggregateNumeric('sum') { List<Number> values ->
      Stat.sum(values)
    }
  }

  /**
   * Compute the rolling minimum for the column.
   *
   * Null values are ignored, and a row yields null when the window contains
   * fewer than {@code minPeriods} non-null values.
   *
   * @return a new column containing the rolling minima
   */
  Column min() {
    aggregateComparable { List<?> values ->
      values.min()
    }
  }

  /**
   * Compute the rolling maximum for the column.
   *
   * Null values are ignored, and a row yields null when the window contains
   * fewer than {@code minPeriods} non-null values.
   *
   * @return a new column containing the rolling maxima
   */
  Column max() {
    aggregateComparable { List<?> values ->
      values.max()
    }
  }

  /**
   * Compute the rolling sample standard deviation for the column.
   *
   * Null values are ignored, and a row yields null when the window contains
   * fewer than {@code minPeriods} non-null numeric values or when the sampled
   * window is too small for the existing {@link Stat#sd(List)} semantics.
   *
   * @return a new column containing the rolling standard deviations
   */
  Column sd() {
    aggregateNumeric('sd') { List<Number> values ->
      Stat.sd(values)
    }
  }

  /**
   * Apply a custom rolling function to each window.
   *
   * The closure receives a new {@link Column} containing the raw window values
   * in rolling order. The closure is only invoked when the window contains at
   * least {@code minPeriods} non-null values.
   *
   * @param function the custom function to apply to each window
   * @return a new column containing the rolling results
   */
  Column apply(Closure<?> function) {
    if (function == null) {
      throw new IllegalArgumentException('rolling function cannot be null')
    }
    Column result = new Column(source.name, Object)
    for (int rowIndex = 0; rowIndex < source.size(); rowIndex++) {
      Column window = slice(rowIndex)
      List<?> nonNullValues = RollingWindowHelper.nonNullValues(window)
      Object value = nonNullValues.size() < options.minPeriods ? null : function.call(window)
      result.add(value)
    }
    result
  }

  private Column aggregateNumeric(String operationName, Closure<?> function) {
    requireNumeric(operationName)
    Column result = new Column(source.name, BigDecimal)
    for (int rowIndex = 0; rowIndex < source.size(); rowIndex++) {
      List<Number> numbers = RollingWindowHelper.nonNullNumbers(slice(rowIndex))
      result.add(numbers.size() < options.minPeriods ? null : function.call(numbers))
    }
    result
  }

  private Column aggregateComparable(Closure<?> function) {
    Class resultType = source.type != null ? source.type : Object
    Column result = new Column(source.name, resultType)
    for (int rowIndex = 0; rowIndex < source.size(); rowIndex++) {
      List<?> values = RollingWindowHelper.nonNullValues(slice(rowIndex))
      result.add(values.size() < options.minPeriods ? null : function.call(values))
    }
    result
  }

  private Column slice(int rowIndex) {
    if (source.isEmpty()) {
      return new Column(source.name, [], source.type)
    }
    IntRange range = RollingWindowHelper.windowRange(source.size(), rowIndex, options)
    new Column(source.name, source.subList(range), source.type)
  }

  private void requireNumeric(String operationName) {
    if (!RollingWindowHelper.isNumericColumn(source)) {
      throw new IllegalArgumentException("rolling ${operationName} requires a numeric column")
    }
  }
}
