package se.alipsa.matrix.stats.linalg

import groovy.transform.PackageScope

import org.ejml.simple.SimpleMatrix

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Internal conversion helpers for the public linear algebra facade.
 * <p>
 * The public API lives in {@code se.alipsa.matrix.stats.linalg}, while the older
 * {@code se.alipsa.matrix.stats.linear} package remains an internal implementation
 * package used by existing statistics code.
 */
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
    NumericConversion.toDoubleArray(matrix)
  }

  /**
   * Convert a rectangular numeric grid into a {@code double[][]}.
   *
   * @param grid the source grid
   * @return a dense numeric array with the same shape
   */
  static double[][] toDoubleArray(Grid<?> grid) {
    NumericConversion.toDoubleArray(grid)
  }

  /**
   * Convert an EJML matrix into a dense {@code double[][]} array.
   *
   * @param matrix the source EJML matrix
   * @return a dense numeric array with the same shape
   */
  static double[][] toDoubleArray(SimpleMatrix matrix) {
    double[][] values = new double[matrix.numRows()][matrix.numCols()]
    for (int row = 0; row < matrix.numRows(); row++) {
      for (int col = 0; col < matrix.numCols(); col++) {
        values[row][col] = matrix.get(row, col)
      }
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
      List<BigDecimal> currentRow = []
      for (int col = 0; col < shape[1]; col++) {
        currentRow << BigDecimal.valueOf(values[row][col])
      }
      rows << currentRow
    }
    Matrix.builder()
      .columnNames(columnNames)
      .rows(rows)
      .types(([BigDecimal] * shape[1]) as List<Class>)
      .build()
  }

  static Grid<BigDecimal> toGrid(double[][] values) {
    int[] shape = validateRectangular(values, 'values')
    List<List<BigDecimal>> rows = []
    for (int row = 0; row < shape[0]; row++) {
      List<BigDecimal> currentRow = []
      for (int col = 0; col < shape[1]; col++) {
        currentRow << BigDecimal.valueOf(values[row][col])
      }
      rows << currentRow
    }
    new Grid<BigDecimal>(rows, BigDecimal)
  }

  static List<BigDecimal> toBigDecimalVector(double[] values) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException('Values must contain at least one value')
    }
    List<BigDecimal> converted = []
    for (double value : values) {
      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException('Values must contain only finite values')
      }
      converted << BigDecimal.valueOf(value)
    }
    converted
  }

  /**
   * Validate that a dense array is rectangular and non-empty.
   *
   * @param values the source array
   * @param label the array label used in validation messages
   * @return `[rowCount, columnCount]`
   */
  static int[] validateRectangular(double[][] values, String label) {
    NumericConversion.validateRectangular(values, label)
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
}
