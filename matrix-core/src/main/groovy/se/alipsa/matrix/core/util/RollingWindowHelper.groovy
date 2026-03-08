package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix

/**
 * Internal helpers shared by rolling window wrappers.
 */
@CompileStatic
class RollingWindowHelper {

  private static final Set<String> NUMERIC_PRIMITIVES = ['byte', 'short', 'int', 'long', 'float', 'double'] as Set<String>

  /**
   * Compute the inclusive row range for a rolling window.
   *
   * @param rowCount the total number of rows in the source
   * @param position the row position in rolling order
   * @param options rolling options
   * @return the inclusive range for the current window
   */
  static IntRange windowRange(int rowCount, int position, RollingWindowOptions options) {
    if (rowCount <= 0) {
      throw new IllegalArgumentException('rowCount must be greater than 0')
    }
    if (position < 0 || position >= rowCount) {
      throw new IndexOutOfBoundsException("position ${position} is outside available rows 0..${rowCount - 1}")
    }
    int start
    int end
    if (options.center) {
      int leftOffset = (options.window - 1).intdiv(2)
      int rightOffset = options.window - leftOffset - 1
      start = Math.max(0, position - leftOffset)
      end = Math.min(rowCount - 1, position + rightOffset)
    } else {
      start = Math.max(0, position - options.window + 1)
      end = position
    }
    start..end
  }

  /**
   * Resolve the row order used for rolling evaluation.
   *
   * @param matrix the source matrix
   * @param by optional column name to sort by
   * @return row indices in rolling order
   */
  static List<Integer> orderedRowIndices(Matrix matrix, String by) {
    if (matrix == null) {
      throw new IllegalArgumentException('matrix cannot be null')
    }
    if (by == null) {
      List<Integer> indexes = []
      for (int i = 0; i < matrix.rowCount(); i++) {
        indexes << i
      }
      return indexes
    }
    if (!(by in matrix.columnNames())) {
      throw new IllegalArgumentException("The column name ${by} does not exist is this table (${matrix.matrixName})")
    }
    List<List<?>> sortableRows = []
    Column column = matrix.column(by)
    for (int i = 0; i < matrix.rowCount(); i++) {
      sortableRows << [column[i], i]
    }
    Collections.sort(sortableRows as List<List>, new RowComparator(0))
    sortableRows.collect { List<?> row -> (row[1] as Number).intValue() } as List<Integer>
  }

  /**
   * Return only non-null values from a window.
   *
   * @param values the raw window values
   * @return the non-null values
   */
  static List<?> nonNullValues(List<?> values) {
    values.findAll { it != null } as List<?>
  }

  /**
   * Return only non-null numeric values from a window.
   *
   * @param values the raw window values
   * @return the numeric values
   */
  static List<Number> nonNullNumbers(List<?> values) {
    values.findAll { it instanceof Number } as List<Number>
  }

  /**
   * Determine whether a column should be treated as numeric for rolling aggregations.
   *
   * @param column the column to inspect
   * @return true when the column is numeric
   */
  static boolean isNumericColumn(Column column) {
    if (column == null) {
      return false
    }
    if (isNumericType(column.type)) {
      return true
    }
    List<?> nonNullValues = nonNullValues(column)
    !nonNullValues.isEmpty() && nonNullValues.every { it instanceof Number }
  }

  /**
   * Determine whether a type is numeric.
   *
   * @param type the type to inspect
   * @return true when the type is numeric
   */
  static boolean isNumericType(Class type) {
    if (type == null) {
      return false
    }
    Number.isAssignableFrom(type) || (type.isPrimitive() && type.name in NUMERIC_PRIMITIVES)
  }
}
