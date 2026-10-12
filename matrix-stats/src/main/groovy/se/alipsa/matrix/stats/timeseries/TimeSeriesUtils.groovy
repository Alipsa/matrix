package se.alipsa.matrix.stats.timeseries

import groovy.transform.PackageScope

import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.util.LeastSquaresKernel
import se.alipsa.matrix.stats.util.NumericTolerance

/**
 * Shared linear algebra helpers for the time-series test implementations in this package.
 * The solving routines support the current time-series callers while adding
 * consistent validation, singularity checks, and diagnostic logging for numerical edge cases.
 * The section-4.6 hotspot decision keeps the Groovy-facing package helpers here while routing
 * dense OLS loops through a shared Java kernel.
 */
@PackageScope
@SuppressWarnings(['ParameterName'])
final class TimeSeriesUtils {

  /** Threshold below which a pivot is treated as numerically singular. */
  private static final String NON_EMPTY_MATRIX_MESSAGE = 'Matrix must contain at least one row and one column'
  private static final String RECTANGULAR_MATRIX_MESSAGE = 'Matrix rows must all have the same length'

  @PackageScope
  static final String NON_FINITE_DATA_MESSAGE = 'Data contains non-finite values'

  @PackageScope
  static final String CONSTANT_SERIES_MESSAGE = 'Data has no variation (constant series)'
  private static final Logger log = Logger.getLogger(TimeSeriesUtils)

  private TimeSeriesUtils() {
  }

  /**
   * Solve a square or overdetermined system using Householder QR least squares.
   * Independent columns retain their order, with column-relative rank detection.
   *
   * @param A the coefficient matrix
   * @param b the right-hand-side vector
   * @return the solved coefficient vector
   * @throws IllegalArgumentException if the inputs are invalid, the square system is singular,
   * or the computed solution contains non-finite values
   */
  static double[] solveLinearSystem(double[][] A, double[] b) {
    if (A == null || b == null) {
      throw new IllegalArgumentException('Matrix and vector cannot be null')
    }
    if (A.length == 0 || A[0].length == 0) {
      throw new IllegalArgumentException(NON_EMPTY_MATRIX_MESSAGE)
    }
    if (A.length != b.length) {
      throw new IllegalArgumentException("Matrix row count (${A.length}) must match vector length (${b.length})")
    }

    int rowCount = A.length
    int columnCount = A[0].length
    if (A.any { double[] row -> row.length != columnCount }) {
      throw new IllegalArgumentException(RECTANGULAR_MATRIX_MESSAGE)
    }
    if (rowCount < columnCount) {
      throw new IllegalArgumentException("Underdetermined system: ${rowCount} rows for ${columnCount} columns")
    }

    double[] solution = LeastSquaresKernel.fitOls(b, A)
    validateFiniteVector(solution, 'linear system solution')
    solution
  }

  /**
   * Invert a square matrix using Gauss-Jordan elimination with partial pivoting.
   *
   * @param A the matrix to invert
   * @return the inverted matrix
   * @throws IllegalArgumentException if the input is invalid, the matrix is singular,
   * or the computed inverse contains non-finite values
  */
  static double[][] invertMatrix(double[][] A) {
    if (A == null || A.length == 0 || A[0].length == 0) {
      throw new IllegalArgumentException(NON_EMPTY_MATRIX_MESSAGE)
    }
    if (A.length != A[0].length) {
      throw new IllegalArgumentException('Only square matrices can be inverted')
    }

    int n = A.length
    if (A.any { double[] row -> row.length != n }) {
      throw new IllegalArgumentException(RECTANGULAR_MATRIX_MESSAGE)
    }

    double[][] result = new double[n][n]
    (0..<n).each { int column ->
      double[] unit = new double[n]
      unit[column] = 1
      double[] solution = LeastSquaresKernel.fitOls(unit, A)
      (0..<n).each { int row -> result[row][column] = solution[row] }
    }
    validateFiniteMatrix(result, 'matrix inverse')
    result
  }

  /**
   * Fit an ordinary least squares regression and return the coefficient vector.
   *
   * @param y the response vector
   * @param X the design matrix
   * @return the fitted regression coefficients
   * @throws IllegalArgumentException if the inputs are invalid or the normal equations cannot be solved
   */
  static double[] fitOLS(double[] y, double[][] X) {
    LeastSquaresKernel.fitOls(y, X)
  }

  /**
   * Calculate the residual sum of squares for a fitted regression model.
   *
   * @param y the observed response vector
   * @param X the design matrix
   * @param beta the fitted coefficient vector
   * @return the residual sum of squares
   * @throws IllegalArgumentException if the inputs are invalid
   */
  static double calculateRSS(double[] y, double[][] X, double[] beta) {
    LeastSquaresKernel.calculateResidualSumOfSquares(y, X, beta)
  }

  /**
   * Determines whether a series has variation relative to its magnitude.
   *
   * @param values the values to inspect
   * @return true when the series has sufficient variation
   */
  @PackageScope
  static boolean hasVariation(double[] values) {
    double minimum = Double.POSITIVE_INFINITY
    double maximum = Double.NEGATIVE_INFINITY
    for (double value : values) {
      if (value < minimum) {
        minimum = value
      }
      if (value > maximum) {
        maximum = value
      }
    }
    !NumericTolerance.isNegligible(maximum - minimum, Math.max(Math.abs(maximum), Math.abs(minimum)))
  }

  /**
   * Determines whether every value in a series is finite.
   *
   * @param values the values to inspect
   * @return true when no value is NaN or positive or negative infinity
   */
  @PackageScope
  static boolean isFinite(double[] values) {
    for (double value : values) {
      if (!Double.isFinite(value)) {
        return false
      }
    }
    true
  }

  private static void validateFiniteVector(double[] values, String label) {
    for (int i = 0; i < values.length; i++) {
      if (!Double.isFinite(values[i])) {
        log.error("Non-finite value in $label at index $i: ${values[i]}")
        throw new IllegalArgumentException("Non-finite value in ${label} at index ${i}")
      }
    }
  }

  private static void validateFiniteMatrix(double[][] matrix, String label) {
    for (int i = 0; i < matrix.length; i++) {
      for (int j = 0; j < matrix[i].length; j++) {
        if (!Double.isFinite(matrix[i][j])) {
          log.error("Non-finite value in $label at row $i, column $j: ${matrix[i][j]}")
          throw new IllegalArgumentException("Non-finite value in ${label} at row ${i}, column ${j}")
        }
      }
    }
  }

}
