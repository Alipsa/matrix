package se.alipsa.matrix.stats.linalg

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix

/**
 * Internal conversion helpers for the public linear algebra facade.
 * <p>
 * The public API lives in {@code se.alipsa.matrix.stats.linalg}, while the older
 * {@code se.alipsa.matrix.stats.linear} package remains an internal implementation
 * package used by existing statistics code.
 */
@CompileStatic
@PackageScope
final class LinalgAdapters {

  private static final String SYNTHETIC_COLUMN_PREFIX = 'c'

  private LinalgAdapters() {
  }

  /**
   * Convert a rectangular numeric matrix into a {@code double[][]}.
   *
   * @param matrix the source matrix
   * @return a dense numeric array with the same shape and column order
   */
  static double[][] toDoubleArray(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException('Matrix cannot be null')
    }
    if (matrix.rowCount() == 0 || matrix.columnCount() == 0) {
      throw new IllegalArgumentException('Matrix must contain at least one row and one column')
    }

    List<Column> columns = matrix.columns()
    double[][] values = new double[matrix.rowCount()][matrix.columnCount()]
    for (int col = 0; col < columns.size(); col++) {
      Column column = columns[col]
      for (int row = 0; row < matrix.rowCount(); row++) {
        values[row][col] = toFiniteDouble(column[row], "matrix value at row ${row}, column '${column.name}'")
      }
    }
    values
  }

  /**
   * Convert a rectangular numeric grid into a {@code double[][]}.
   *
   * @param grid the source grid
   * @return a dense numeric array with the same shape
   */
  static double[][] toDoubleArray(Grid<?> grid) {
    if (grid == null) {
      throw new IllegalArgumentException('Grid cannot be null')
    }
    Map<String, Integer> dimensions = grid.dimensions()
    int rows = dimensions.observations
    int columns = dimensions.variables
    if (rows == 0 || columns == 0) {
      throw new IllegalArgumentException('Grid must contain at least one row and one column')
    }

    double[][] values = new double[rows][columns]
    List<List<?>> rowData = grid.data
    for (int row = 0; row < rows; row++) {
      List<?> currentRow = rowData[row]
      if (currentRow == null || currentRow.size() != columns) {
        throw new IllegalArgumentException("Grid rows must all have the same length; row ${row} has ${currentRow == null ? 'null' : currentRow.size()} values, expected ${columns}")
      }
      for (int col = 0; col < columns; col++) {
        values[row][col] = toFiniteDouble(currentRow[col], "grid value at row ${row}, column ${col}")
      }
    }
    values
  }

  /**
   * Convert a numeric vector into a dense {@code double[]} array.
   *
   * @param vector the numeric vector
   * @param label the label used in validation messages
   * @return the dense numeric vector
   */
  static double[] toDoubleArray(List<? extends Number> vector, String label = 'vector') {
    if (vector == null) {
      throw new IllegalArgumentException("${label.capitalize()} cannot be null")
    }
    if (vector.isEmpty()) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    double[] values = new double[vector.size()]
    for (int i = 0; i < vector.size(); i++) {
      values[i] = toFiniteDouble(vector[i], "${label} value at index ${i}")
    }
    values
  }

  /**
   * Convert a dense numeric array into a Matrix with synthetic column names.
   *
   * @param values the dense numeric array
   * @return a matrix containing the supplied values
   */
  static Matrix toMatrix(double[][] values) {
    int[] shape = validateRectangular(values, 'values')
    List<String> columnNames = syntheticColumnNames(shape[1])
    List<List> rows = []
    for (int row = 0; row < shape[0]; row++) {
      List<Double> currentRow = []
      for (int col = 0; col < shape[1]; col++) {
        currentRow << values[row][col]
      }
      rows << currentRow
    }
    Matrix.builder()
      .columnNames(columnNames)
      .rows(rows)
      .types(([Double] * shape[1]) as List<Class>)
      .build()
  }

  /**
   * Validate that a dense array is rectangular and non-empty.
   *
   * @param values the source array
   * @param label the array label used in validation messages
   * @return `[rowCount, columnCount]`
   */
  static int[] validateRectangular(double[][] values, String label) {
    if (values == null || values.length == 0 || values[0] == null || values[0].length == 0) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one row and one column")
    }
    int columns = values[0].length
    for (int row = 0; row < values.length; row++) {
      double[] currentRow = values[row]
      if (currentRow == null || currentRow.length != columns) {
        throw new IllegalArgumentException("${label.capitalize()} rows must all have the same length")
      }
      for (int col = 0; col < columns; col++) {
        if (!Double.isFinite(currentRow[col])) {
          throw new IllegalArgumentException("${label.capitalize()} contains a non-finite value at row ${row}, column ${col}")
        }
      }
    }
    [values.length, columns] as int[]
  }

  /**
   * Create deterministic synthetic column names for computed matrix results.
   *
   * @param columnCount the number of columns to name
   * @return the generated column names
   */
  static List<String> syntheticColumnNames(int columnCount) {
    (0..<columnCount).collect { int index -> SYNTHETIC_COLUMN_PREFIX + index }
  }

  private static double toFiniteDouble(Object value, String label) {
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("${label.capitalize()} must be numeric")
    }
    double numericValue = (value as Number).doubleValue()
    if (!Double.isFinite(numericValue)) {
      throw new IllegalArgumentException("${label.capitalize()} must be finite")
    }
    numericValue
  }
}
