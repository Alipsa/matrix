package se.alipsa.matrix.core

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.CumulativeHelper
import se.alipsa.matrix.core.util.RollingWindowOptions

/**
 * A column is a list with some arithmetic operations changed compared to how lists normally behaves in Groovy.
 * the multiply, div, plus, minus, and power applies to each element in the list instead of on the list itself. E.g.
 * new Column([1,2,3]) * 2 == [2,4,6] instead of [2,4,6,2,4,6] which the default result would on a list i Groovy.
 */
@CompileStatic
class Column extends ArrayList {

  String name
  Class type

  Column(int initialCapacity) {
    this(initialCapacity, Object)
  }

  Column() {
    this(Object)
  }

  Column(Collection<?> c) {
    this(c, Object)
  }

  Column(int initialCapacity, Class type) {
    super(initialCapacity)
    this.type = type
  }

  Column(Class type) {
    this.type = type
  }

  Column(Collection c, Class type) {
    super(c)
    this.type = type
  }

  Column(String name, Collection c) {
    super(c)
    this.name = name
  }

  Column(String name, Collection c, Class type) {
    super(c)
    this.type = type
    this.name = name
  }

  Column(String name, Class type) {
    this.name = name
    this.type = type
  }

  <T> T getAt(Number index, Class<T> type) {
    ValueConverter.convert(this.get(index.intValue()), type)
  }

  @CompileDynamic
  private List applyOperation(Object val, Closure operation) {
    List result = new Column()
    this.each {
      if (it == null) {
        result.add(null)
      } else {
        result.add(operation(it, val))
      }
    }
    result
  }

  @CompileDynamic
  List plus(Object val) {
    if (val == null) {
      throw new IllegalArgumentException("Cannot add null to a column, use removeNulls() to remove nulls from the column or replaceNulls() to replace nulls with a value before adding")
    }
    applyOperation(val, { a, b -> a + b })
  }

  @CompileDynamic
  List plus(List list) {
    if (list == null) {
      throw new IllegalArgumentException("Cannot add a null list to a column")
    }
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it + (val as Number))
      } else if (it instanceof Character) {
        result.add(it + (val as Character))
      } else if (it instanceof String) {
        result.add(it + (val as Character))
      } else {
        result.add(it + val)
      }

    }
    result
  }

  @CompileDynamic
  List minus(Object val) {
    if (val == null) {
      throw new IllegalArgumentException("Cannot subtract null from a column, use removeNulls() to remove nulls from the column or replaceNulls() to replace nulls with a value before subtracting")
    }
    applyOperation(val, { a, b -> a - b })
  }

  @CompileDynamic
  List minus(List list) {
    if (list == null) {
      throw new IllegalArgumentException("Cannot subtract a null list from a column")
    }
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it - (val as Number))
      } else if (it instanceof Character) {
        result.add(it - (val as Character))
      } else if (it instanceof String) {
        result.add(it - (val as Character))
      } else {
        result.add(it - val)
      }
    }
    result
  }

  @CompileDynamic
  List multiply(Number val) {
    if (val == null) {
      throw new IllegalArgumentException("Cannot multiply null with a column, use removeNulls() to remove nulls from the column or replaceNulls() to replace nulls with a value before multiplying")
    }
    applyOperation(val, { a, b -> a * b })
  }

  @CompileDynamic
  List multiply(List list) {
    if (list == null) {
      throw new IllegalArgumentException("Cannot multiply a column by a null list")
    }
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it * (val as Number))
      } else {
        result.add(it * val)
      }
    }
    result
  }

  @CompileDynamic
  List div(Number val) {
    if (val == null) {
      throw new IllegalArgumentException("Cannot divide a column by null, use removeNulls() to remove nulls from the column or replaceNulls() to replace nulls with a value before dividing")
    }
    applyOperation(val, { a, b -> a / b })
  }

  @CompileDynamic
  List div(List list) {
    if (list == null) {
      throw new IllegalArgumentException("Cannot divide a column by a null list")
    }
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it / (val as Number))
      } else {
        result.add(it / val)
      }
    }
    result
  }

  @CompileDynamic
  List power(Number val) {
    if (val == null) {
      throw new IllegalArgumentException("Cannot raise a column to the power of null, use removeNulls() to remove nulls from the column or replaceNulls() to replace nulls with a value before exponentiating")
    }
    applyOperation(val, { a, b -> a ** b })
  }

  @CompileDynamic
  List power(List list) {
    if (list == null) {
      throw new IllegalArgumentException("Cannot raise a column to the power of a null list")
    }
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it ** (val as Number))
      } else {
        result.add(it ** val)
      }
    }
    result
  }

  List removeNulls() {
    this.findAll { it != null } as Column
  }

  /**
   * Replaces all null values in this column with the specified value.
   * Mutates this column in place.
   *
   * @param val the value to replace nulls with
   * @return this column
   */
  Column replaceNulls(Object val) {
    replace(null, val)
  }

  /**
   * Replaces all occurrences of oldVal with val in this column.
   * Mutates this column in place.
   *
   * @param oldVal the value to find and replace
   * @param val the replacement value
   * @return this column
   */
  Column replace(Object oldVal, Object val) {
    for (int i = 0; i < size(); i++) {
      if (get(i) == oldVal) {
        set(i, val)
      }
    }
    this
  }

  private Column fill(List list) {
    def that = new Column(list)
    int listSize = list.size()
    int size = this.size()
    if (listSize < size) {
      that.addAll([null]*(size-listSize))
    }
    that
  }

  List subList(IntRange range) {
    this.subList(range.min(), range.max() +1)
  }

  /**
   * Change the default behavior of the unique method to not mutate
   * (otherwise the rest of the column values will be filled with null).
   * Returns a new Column with unique values from this column.
   *
   * @return a new Column with unique values
   */
  List unique() {
    unique(false)
  }

  @CompileDynamic
  Number mean() {
    Stat.mean(this)
  }

  @CompileDynamic
  Number sd() {
    Stat.sd(this)
  }

  @CompileDynamic
  Number median() {
    Stat.median(this)
  }

  @CompileDynamic
  Number variance(boolean isBiasedCorrected = true) {
    Stat.variance(this, isBiasedCorrected)
  }

  /**
   * Compute the cumulative sum of this column.
   *
   * <p>Requires a numeric column. Null values are skipped (the output position is null
   * and the running total is unchanged).</p>
   *
   * @return a new column containing the cumulative sums
   */
  Column cumsum() {
    CumulativeHelper.cumsum(this)
  }

  /**
   * Compute the cumulative product of this column.
   *
   * <p>Requires a numeric column. Null values are skipped (the output position is null
   * and the running product is unchanged).</p>
   *
   * @return a new column containing the cumulative products
   */
  Column cumprod() {
    CumulativeHelper.cumprod(this)
  }

  /**
   * Compute the cumulative minimum of this column.
   *
   * <p>Works on columns whose non-null values are individually Comparable and
   * mutually comparable (for example numbers, strings, or dates within the same
   * comparable family). Null values are skipped.</p>
   *
   * @return a new column containing the cumulative minima
   */
  Column cummin() {
    CumulativeHelper.cummin(this)
  }

  /**
   * Compute the cumulative maximum of this column.
   *
   * <p>Works on columns whose non-null values are individually Comparable and
   * mutually comparable (for example numbers, strings, or dates within the same
   * comparable family). Null values are skipped.</p>
   *
   * @return a new column containing the cumulative maxima
   */
  Column cummax() {
    CumulativeHelper.cummax(this)
  }

  /**
   * Create a trailing rolling view over this column.
   *
   * Equivalent to {@code rolling(window: window)}.
   *
   * @param window the rolling window size
   * @return a rolling view for chained aggregations
   */
  RollingColumn rolling(int window) {
    new RollingColumn(this, new RollingWindowOptions(window, window))
  }

  /**
   * Create a rolling view over this column.
   *
   * Supported options are:
   * {@code window} (required), {@code minPeriods} (defaults to {@code window}),
   * and {@code center} (defaults to {@code false}).
   *
   * Built-in numeric aggregations ignore null values and return null when the
   * window contains fewer than {@code minPeriods} non-null numeric values.
   * Custom {@code apply {}} receives the raw window values.
   *
   * @param options rolling options
   * @return a rolling view for chained aggregations
   */
  RollingColumn rolling(Map<String, ?> options) {
    new RollingColumn(this, RollingWindowOptions.column(options))
  }

  List getValues() {
    this.collect { it }
  }
}
