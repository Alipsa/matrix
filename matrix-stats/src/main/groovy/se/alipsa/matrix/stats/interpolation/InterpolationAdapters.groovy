package se.alipsa.matrix.stats.interpolation

import groovy.transform.PackageScope

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Internal conversion and validation helpers for interpolation utilities.
 */
@PackageScope
final class InterpolationAdapters {

  private InterpolationAdapters() {
  }

  static double[] toDoubleArray(Matrix matrix, String columnName) {
    if (matrix == null) {
      throw new IllegalArgumentException('Matrix cannot be null')
    }
    if (columnName == null) {
      throw new IllegalArgumentException('Column name cannot be null')
    }
    if (matrix.rowCount() == 0 || matrix.columnCount() == 0) {
      throw new IllegalArgumentException('Matrix must contain at least one row and one column')
    }

    Column column
    try {
      column = matrix.column(columnName)
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Matrix does not contain column '${columnName}'", e)
    }

    double[] values = new double[matrix.rowCount()]
    for (int row = 0; row < matrix.rowCount(); row++) {
      values[row] = NumericConversion.toFiniteDouble(column[row], "matrix value at row ${row}, column '${columnName}'")
    }
    values
  }

  static double[] toDoubleArray(Grid<?> grid, int columnIndex) {
    if (grid == null) {
      throw new IllegalArgumentException('Grid cannot be null')
    }

    Map<String, Integer> dimensions = grid.dimensions()
    int rows = dimensions.observations
    int columns = dimensions.variables
    if (rows == 0 || columns == 0) {
      throw new IllegalArgumentException('Grid must contain at least one row and one column')
    }
    if (columnIndex < 0 || columnIndex >= columns) {
      throw new IllegalArgumentException("Grid column index ${columnIndex} is out of bounds for ${columns} columns")
    }

    double[] values = new double[rows]
    List<List<?>> rowData = grid.data
    for (int row = 0; row < rows; row++) {
      List<?> currentRow = rowData[row]
      if (currentRow == null || currentRow.size() != columns) {
        throw new IllegalArgumentException("Grid rows must all have the same length; row ${row} has ${currentRow == null ? 'null' : currentRow.size()} values, expected ${columns}")
      }
      values[row] = NumericConversion.toFiniteDouble(currentRow[columnIndex], "grid value at row ${row}, column ${columnIndex}")
    }
    values
  }

  static double[] validateCoordinates(double[] values, String label) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    double[] copy = new double[values.length]
    for (int i = 0; i < values.length; i++) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException("${label.capitalize()} must contain only finite values")
      }
      copy[i] = values[i]
    }
    copy
  }

  static double[] validateDomain(double[] x) {
    double[] domain = validateCoordinates(x, 'x')
    for (int i = 1; i < domain.length; i++) {
      if (domain[i] <= domain[i - 1]) {
        throw new IllegalArgumentException('X values must be strictly increasing')
      }
    }
    domain
  }

  static double[] validateRange(double[] y, int expectedLength) {
    double[] range = validateCoordinates(y, 'y')
    if (range.length != expectedLength) {
      throw new IllegalArgumentException("X length (${expectedLength}) must match y length (${range.length})")
    }
    range
  }

  static double validateTarget(double target, String label) {
    if (!Double.isFinite(target)) {
      throw new IllegalArgumentException("${label.capitalize()} must be finite")
    }
    target
  }
}
