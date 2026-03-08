package se.alipsa.matrix.core

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.RollingWindowHelper
import se.alipsa.matrix.core.util.RollingWindowOptions

import java.util.AbstractList

/**
 * Rolling window operations for a {@link Matrix}.
 */
@CompileStatic
class RollingMatrix {

  private final Matrix source
  private final RollingWindowOptions options
  private final List<Integer> orderedRowIndices
  private final Map<Integer, Integer> orderedPositionsByRowIndex
  private final List<List<Integer>> windowIndicesByRowIndex

  /**
   * Create a rolling view over a matrix.
   *
   * Built-in numeric operations are applied to numeric columns only. Non-numeric
   * columns, and the {@code by} column when supplied, are preserved unchanged in
   * the result.
   *
   * @param source the source matrix
   * @param options the rolling options
   */
  RollingMatrix(Matrix source, RollingWindowOptions options) {
    if (source == null) {
      throw new IllegalArgumentException('source matrix cannot be null')
    }
    if (options == null) {
      throw new IllegalArgumentException('rolling options cannot be null')
    }
    this.source = source
    this.options = options
    this.orderedRowIndices = RollingWindowHelper.orderedRowIndices(source, options.by)
    Map<Integer, Integer> positions = [:]
    orderedRowIndices.eachWithIndex { Integer rowIndex, int orderedPosition ->
      positions[rowIndex] = orderedPosition
    }
    this.orderedPositionsByRowIndex = positions
    this.windowIndicesByRowIndex = new AbstractList<List<Integer>>() {
      @Override
      int size() {
        source.rowCount()
      }

      @Override
      List<Integer> get(int rowIndex) {
        computeWindowRowIndices(rowIndex)
      }
    }
  }

  /**
   * Compute the rolling mean for each numeric column.
   *
   * @return a new matrix containing rolling means for numeric columns
   */
  Matrix mean() {
    aggregateNumericColumns { List<Number> values ->
      Stat.mean(values)
    }
  }

  /**
   * Compute the rolling sum for each numeric column.
   *
   * @return a new matrix containing rolling sums for numeric columns
   */
  Matrix sum() {
    aggregateNumericColumns { List<Number> values ->
      Stat.sum(values)
    }
  }

  /**
   * Compute the rolling minimum for each numeric column.
   *
   * @return a new matrix containing rolling minima for numeric columns
   */
  Matrix min() {
    aggregateNumericColumns(false) { List<Number> values ->
      values.min()
    }
  }

  /**
   * Compute the rolling maximum for each numeric column.
   *
   * @return a new matrix containing rolling maxima for numeric columns
   */
  Matrix max() {
    aggregateNumericColumns(false) { List<Number> values ->
      values.max()
    }
  }

  /**
   * Compute the rolling sample standard deviation for each numeric column.
   *
   * @return a new matrix containing rolling standard deviations for numeric columns
   */
  Matrix sd() {
    aggregateNumericColumns { List<Number> values ->
      Stat.sd(values)
    }
  }

  /**
   * Apply a custom rolling function to each matrix window.
   *
   * The closure receives a new matrix containing the raw window rows in rolling
   * order. The closure is only invoked when the window contains at least
   * {@code minPeriods} rows.
   *
   * @param function the custom function to apply to each window
   * @return a new column containing the rolling results
   */
  Column apply(Closure<?> function) {
    if (function == null) {
      throw new IllegalArgumentException('rolling function cannot be null')
    }
    Column result = new Column('rolling', Object)
    for (int rowIndex = 0; rowIndex < source.rowCount(); rowIndex++) {
      List<Integer> windowIndices = windowIndicesByRowIndex[rowIndex]
      Matrix window = source.subset(windowIndices)
      Object value = window.rowCount() < options.minPeriods ? null : function.call(window)
      result.add(value)
    }
    result
  }

  private Matrix aggregateNumericColumns(boolean decimalResult = true, Closure<?> function) {
    Matrix result = source.clone()
    source.columnNames().eachWithIndex { String columnName, int index ->
      Column sourceColumn = source.column(index)
      if (!shouldAggregateColumn(sourceColumn)) {
        return
      }
      Class resultType = decimalResult ? BigDecimal : source.type(index)
      Column rolledColumn = new Column(columnName, resultType)
      List<Number> values = new ArrayList<Number>()
      for (int rowIndex = 0; rowIndex < source.rowCount(); rowIndex++) {
        values.clear()
        List<Integer> windowIndices = windowIndicesByRowIndex[rowIndex]
        for (int i = 0; i < windowIndices.size(); i++) {
          Object value = sourceColumn[windowIndices.get(i)]
          if (value instanceof Number) {
            values.add(value as Number)
          }
        }
        rolledColumn.add(values.size() < options.minPeriods ? null : function.call(values))
      }
      result.replace(columnName, resultType, rolledColumn)
    }
    result
  }

  private boolean shouldAggregateColumn(Column column) {
    if (column == null) {
      return false
    }
    if (options.by != null && column.name == options.by) {
      return false
    }
    RollingWindowHelper.isNumericColumn(column)
  }

  private List<Integer> computeWindowRowIndices(int rowIndex) {
    if (orderedRowIndices.isEmpty()) {
      return []
    }
    int orderedPosition = orderedPositionsByRowIndex[rowIndex]
    IntRange range = RollingWindowHelper.windowRange(orderedRowIndices.size(), orderedPosition, options)
    orderedRowIndices.subList(range.from, range.to + 1)
  }
}
