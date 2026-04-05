package se.alipsa.matrix.stats.util

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix

/**
 * Shared numeric conversion helpers for `matrix-stats`.
 * <p>
 * This utility centralizes numeric conversion, Matrix/Grid extraction, and shared
 * validation so public facades such as linear algebra and interpolation do not
 * duplicate the same adapter logic.
 */
final class NumericConversion {

  private NumericConversion() {
  }

  /**
   * Convert a numeric vector into a dense {@code double[]} array.
   *
   * @param values the numeric vector
   * @param label the label used in validation messages
   * @return the dense numeric vector
   */
  static double[] toDoubleArray(List<? extends Number> values, String label = 'values') {
    if (values == null) {
      throw new IllegalArgumentException("${label.capitalize()} cannot be null")
    }
    if (values.isEmpty()) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    double[] numericValues = new double[values.size()]
    for (int i = 0; i < values.size(); i++) {
      numericValues[i] = toFiniteDouble(values[i], "${label} value at index ${i}")
    }
    numericValues
  }

  /**
   * Convert a numeric Matrix column into a dense {@code double[]} array.
   *
   * @param matrix the source matrix
   * @param columnName the column name to extract
   * @return the dense numeric vector
   * @since 2.4.0
   */
  static double[] toDoubleArray(Matrix matrix, String columnName) {
    List<BigDecimal> values = toBigDecimalColumn(matrix, columnName)
    double[] numericValues = new double[values.size()]
    for (int i = 0; i < values.size(); i++) {
      numericValues[i] = values[i] as double
    }
    numericValues
  }

  /**
   * Convert a numeric Grid column into a dense {@code double[]} array.
   *
   * @param grid the source grid
   * @param columnIndex the zero-based column index
   * @return the dense numeric vector
   * @since 2.4.0
   */
  static double[] toDoubleArray(Grid<?> grid, int columnIndex) {
    List<BigDecimal> values = toBigDecimalColumn(grid, columnIndex)
    double[] numericValues = new double[values.size()]
    for (int i = 0; i < values.size(); i++) {
      numericValues[i] = values[i] as double
    }
    numericValues
  }

  /**
   * Convert a rectangular numeric Matrix into a dense {@code double[][]} array.
   *
   * @param matrix the source matrix
   * @return the dense numeric array
   * @since 2.4.0
   */
  static double[][] toDoubleArray(Matrix matrix) {
    Grid<BigDecimal> grid = toBigDecimalGrid(matrix)
    toDoubleArray(grid)
  }

  /**
   * Convert a rectangular numeric Grid into a dense {@code double[][]} array.
   *
   * @param grid the source grid
   * @return the dense numeric array
   * @since 2.4.0
   */
  static double[][] toDoubleArray(Grid<?> grid) {
    int[] shape = validateRectangular(grid, 'grid')
    Grid<BigDecimal> numericGrid = toBigDecimalGrid(grid)
    double[][] values = new double[shape[0]][shape[1]]
    for (int row = 0; row < shape[0]; row++) {
      for (int col = 0; col < shape[1]; col++) {
        values[row][col] = numericGrid.getAt(row, col) as double
      }
    }
    values
  }

  /**
   * Convert a numeric value to {@code BigDecimal}.
   *
   * @param value the source value
   * @param label the label used in validation messages
   * @return the numeric value as {@code BigDecimal}
   * @since 2.4.0
   */
  static BigDecimal toBigDecimal(Object value, String label) {
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("${label.capitalize()} must be numeric")
    }
    if (value instanceof Double || value instanceof Float) {
      double numericValue = value as double
      if (!Double.isFinite(numericValue)) {
        throw new IllegalArgumentException("${label.capitalize()} must be finite")
      }
      return BigDecimal.valueOf(numericValue)
    }
    value as BigDecimal
  }

  /**
   * Convert a numeric vector into a {@code List<BigDecimal>}.
   *
   * @param values the numeric vector
   * @param label the label used in validation messages
   * @return the numeric vector
   * @since 2.4.0
   */
  static List<BigDecimal> toBigDecimalList(List<? extends Number> values, String label = 'values') {
    if (values == null) {
      throw new IllegalArgumentException("${label.capitalize()} cannot be null")
    }
    if (values.isEmpty()) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    List<BigDecimal> numericValues = []
    for (int i = 0; i < values.size(); i++) {
      numericValues << toBigDecimal(values[i], "${label} value at index ${i}")
    }
    numericValues
  }

  /**
   * Extract a numeric Matrix column as {@code BigDecimal} values.
   *
   * @param matrix the source matrix
   * @param columnName the column name to extract
   * @return the extracted numeric values
   * @since 2.4.0
   */
  static List<BigDecimal> toBigDecimalColumn(Matrix matrix, String columnName) {
    validateMatrix(matrix)
    if (columnName == null) {
      throw new IllegalArgumentException('Column name cannot be null')
    }

    Column column
    try {
      column = matrix.column(columnName)
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Matrix does not contain column '${columnName}'", e)
    }

    List<BigDecimal> values = []
    for (int row = 0; row < matrix.rowCount(); row++) {
      values << toBigDecimal(column[row], "matrix value at row ${row}, column '${columnName}'")
    }
    values
  }

  /**
   * Extract a numeric Grid column as {@code BigDecimal} values.
   *
   * @param grid the source grid
   * @param columnIndex the zero-based column index
   * @return the extracted numeric values
   * @since 2.4.0
   */
  static List<BigDecimal> toBigDecimalColumn(Grid<?> grid, int columnIndex) {
    int[] shape = validateRectangular(grid, 'grid')
    if (columnIndex < 0 || columnIndex >= shape[1]) {
      throw new IllegalArgumentException("Grid column index ${columnIndex} is out of bounds for ${shape[1]} columns")
    }

    List<BigDecimal> values = []
    List<List<?>> rowData = grid.data
    for (int row = 0; row < shape[0]; row++) {
      values << toBigDecimal(rowData[row][columnIndex], "grid value at row ${row}, column ${columnIndex}")
    }
    values
  }

  /**
   * Convert a rectangular numeric Matrix into a {@code Grid<BigDecimal>}.
   *
   * @param matrix the source matrix
   * @return the numeric grid
   * @since 2.4.0
   */
  static Grid<BigDecimal> toBigDecimalGrid(Matrix matrix) {
    validateMatrix(matrix)

    List<Column> columns = matrix.columns()
    List<List<BigDecimal>> rows = []
    for (int row = 0; row < matrix.rowCount(); row++) {
      List<BigDecimal> currentRow = []
      for (int col = 0; col < columns.size(); col++) {
        Column column = columns[col]
        currentRow << toBigDecimal(column[row], "matrix value at row ${row}, column '${column.name}'")
      }
      rows << currentRow
    }
    new Grid<BigDecimal>(rows, BigDecimal)
  }

  /**
   * Convert a rectangular numeric Grid into a {@code Grid<BigDecimal>}.
   *
   * @param grid the source grid
   * @return the numeric grid
   * @since 2.4.0
   */
  static Grid<BigDecimal> toBigDecimalGrid(Grid<?> grid) {
    int[] shape = validateRectangular(grid, 'grid')

    List<List<?>> rowData = grid.data
    List<List<BigDecimal>> rows = []
    for (int row = 0; row < shape[0]; row++) {
      List<BigDecimal> currentRow = []
      for (int col = 0; col < shape[1]; col++) {
        currentRow << toBigDecimal(rowData[row][col], "grid value at row ${row}, column ${col}")
      }
      rows << currentRow
    }
    new Grid<BigDecimal>(rows, BigDecimal)
  }

  /**
   * Convert a value to a finite {@code double}.
   *
   * @param value the source value
   * @param label the label used in validation messages
   * @return the finite numeric value
   */
  static double toFiniteDouble(Object value, String label) {
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("${label.capitalize()} must be numeric")
    }
    double numericValue = value as double
    if (!Double.isFinite(numericValue)) {
      throw new IllegalArgumentException("${label.capitalize()} must be finite")
    }
    numericValue
  }

  /**
   * Validate that a dense array is rectangular and non-empty.
   *
   * @param values the source array
   * @param label the array label used in validation messages
   * @return `[rowCount, columnCount]`
   * @since 2.4.0
   */
  static int[] validateRectangular(double[][] values, String label = 'values') {
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
   * Validate that a Grid is rectangular and non-empty.
   *
   * @param grid the source grid
   * @param label the grid label used in validation messages
   * @return `[rowCount, columnCount]`
   * @since 2.4.0
   */
  static int[] validateRectangular(Grid<?> grid, String label = 'grid') {
    if (grid == null) {
      throw new IllegalArgumentException("${label.capitalize()} cannot be null")
    }
    Map<String, Integer> dimensions = grid.dimensions()
    int rows = dimensions.observations
    int columns = dimensions.variables
    if (rows == 0 || columns == 0) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one row and one column")
    }

    List<List<?>> rowData = grid.data
    for (int row = 0; row < rows; row++) {
      List<?> currentRow = rowData[row]
      if (currentRow == null || currentRow.size() != columns) {
        throw new IllegalArgumentException("${label.capitalize()} rows must all have the same length; row ${row} has ${currentRow == null ? 'null' : currentRow.size()} values, expected ${columns}")
      }
    }
    [rows, columns] as int[]
  }

  private static void validateMatrix(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException('Matrix cannot be null')
    }
    if (matrix.rowCount() == 0 || matrix.columnCount() == 0) {
      throw new IllegalArgumentException('Matrix must contain at least one row and one column')
    }
  }
}
