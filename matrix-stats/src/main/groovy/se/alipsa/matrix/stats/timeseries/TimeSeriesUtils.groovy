package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import se.alipsa.matrix.core.util.Logger

/**
 * Shared linear algebra helpers for the time-series test implementations in this package.
 * The solving routines support the current time-series callers while adding
 * consistent validation, singularity checks, and diagnostic logging for numerical edge cases.
 */
@PackageScope
@SuppressWarnings(['ParameterName'])
final class TimeSeriesUtils {

  /** Threshold below which a pivot is treated as numerically singular. */
  private static final double SINGULARITY_THRESHOLD = 1e-14
  private static final Logger log = Logger.getLogger(TimeSeriesUtils)

  private TimeSeriesUtils() {
  }

  /**
   * Solve a linear system {@code Ax = b} using Gaussian elimination with partial pivoting.
   * {@link AdfGls} passes its GLS-detrending design matrix directly as a rectangular
   * overdetermined system, so this method reduces the pivoted column subset directly rather than
   * forming the normal equations; this is not a general least-squares solver.
   *
   * @param A the coefficient matrix
   * @param b the right-hand-side vector
   * @return the solved coefficient vector
   * @throws IllegalArgumentException if the inputs are invalid, the square system is singular,
   * or the computed solution contains non-finite values
   */
  static double[] solveLinearSystem(double[][] A, double[] b) {
    if (A == null || b == null) {
      throw new IllegalArgumentException("Matrix and vector cannot be null")
    }
    if (A.length == 0 || A[0].length == 0) {
      throw new IllegalArgumentException("Matrix must contain at least one row and one column")
    }
    if (A.length != b.length) {
      throw new IllegalArgumentException("Matrix row count (${A.length}) must match vector length (${b.length})")
    }

    int rowCount = A.length
    int columnCount = A[0].length
    if (A.any { double[] row -> row.length != columnCount }) {
      throw new IllegalArgumentException("Matrix rows must all have the same length")
    }
    if (rowCount < columnCount) {
      throw new IllegalArgumentException("Underdetermined system: ${rowCount} rows for ${columnCount} columns")
    }

    double[][] augmented = new double[rowCount][columnCount + 1]
    for (int i = 0; i < rowCount; i++) {
      for (int j = 0; j < columnCount; j++) {
        augmented[i][j] = A[i][j]
      }
      augmented[i][columnCount] = b[i]
    }

    for (int k = 0; k < columnCount; k++) {
      int maxRow = k
      double maxVal = Math.abs(augmented[k][k])
      for (int i = k + 1; i < rowCount; i++) {
        double value = Math.abs(augmented[i][k])
        if (value > maxVal) {
          maxVal = value
          maxRow = i
        }
      }

      if (maxRow != k) {
        swapRows(augmented, k, maxRow)
      }
      if (Math.abs(augmented[k][k]) > SINGULARITY_THRESHOLD) {
        for (int i = k + 1; i < rowCount; i++) {
          double factor = augmented[i][k] / augmented[k][k]
          for (int j = k; j < columnCount + 1; j++) {
            augmented[i][j] -= factor * augmented[k][j]
          }
        }
      } else if (rowCount == columnCount) {
        log.warn("Singular pivot at column $k while solving ${rowCount}x${columnCount} system")
        throw new IllegalArgumentException("Singular matrix at column ${k} - cannot solve linear system")
      } else {
        log.warn("Near-singular pivot at column $k while reducing ${rowCount}x${columnCount} overdetermined system; back-substitution will validate the diagonal before division")
      }
    }

    double[] x = new double[columnCount]
    for (int i = columnCount - 1; i >= 0; i--) {
      double sum = augmented[i][columnCount]
      for (int j = i + 1; j < columnCount; j++) {
        sum -= augmented[i][j] * x[j]
      }
      double diagonal = augmented[i][i]
      if (Math.abs(diagonal) <= SINGULARITY_THRESHOLD) {
        log.warn("Singular diagonal at column $i during back-substitution for ${rowCount}x${columnCount} system")
        throw new IllegalArgumentException("Singular matrix at column ${i} - cannot solve linear system")
      }
      x[i] = sum / diagonal
    }
    validateFiniteVector(x, 'linear system solution')
    x
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
      throw new IllegalArgumentException("Matrix must contain at least one row and one column")
    }
    if (A.length != A[0].length) {
      throw new IllegalArgumentException("Only square matrices can be inverted")
    }

    int n = A.length
    if (A.any { double[] row -> row.length != n }) {
      throw new IllegalArgumentException("Matrix rows must all have the same length")
    }

    double[][] result = new double[n][n]
    double[][] augmented = new double[n][2 * n]
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        augmented[i][j] = A[i][j]
        augmented[i][j + n] = (i == j) ? 1.0 : 0.0
      }
    }

    for (int k = 0; k < n; k++) {
      int maxRow = findPivotRow(augmented, k, n)
      if (maxRow != k) {
        swapRows(augmented, k, maxRow)
      }
      double pivot = augmented[k][k]
      if (Math.abs(pivot) <= SINGULARITY_THRESHOLD) {
        log.warn("Singular pivot at column $k while inverting ${n}x${n} matrix")
        throw new IllegalArgumentException("Singular matrix at column ${k} - cannot invert matrix")
      }
      for (int j = 0; j < 2 * n; j++) {
        augmented[k][j] /= pivot
      }

      for (int i = 0; i < n; i++) {
        if (i != k) {
          double factor = augmented[i][k]
          for (int j = 0; j < 2 * n; j++) {
            augmented[i][j] -= factor * augmented[k][j]
          }
        }
      }
    }

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        result[i][j] = augmented[i][j + n]
      }
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
    if (y == null || X == null) {
      throw new IllegalArgumentException("Response vector and design matrix cannot be null")
    }
    if (X.length == 0 || X[0].length == 0) {
      throw new IllegalArgumentException("Design matrix must contain at least one row and one column")
    }
    if (X.length != y.length) {
      throw new IllegalArgumentException("Design matrix row count (${X.length}) must match response length (${y.length})")
    }

    int n = y.length
    int k = X[0].length
    if (X.any { double[] row -> row.length != k }) {
      throw new IllegalArgumentException("Design matrix rows must all have the same length")
    }

    double[][] XtX = new double[k][k]
    for (int i = 0; i < k; i++) {
      for (int j = 0; j < k; j++) {
        double sum = 0.0
        for (int m = 0; m < n; m++) {
          sum += X[m][i] * X[m][j]
        }
        XtX[i][j] = sum
      }
    }

    double[] Xty = new double[k]
    for (int i = 0; i < k; i++) {
      double sum = 0.0
      for (int m = 0; m < n; m++) {
        sum += X[m][i] * y[m]
      }
      Xty[i] = sum
    }

    solveLinearSystem(XtX, Xty)
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
    if (y == null || X == null || beta == null) {
      throw new IllegalArgumentException("Response vector, design matrix, and coefficients cannot be null")
    }
    if (X.length == 0) {
      throw new IllegalArgumentException("Design matrix must contain at least one row")
    }
    if (X.length != y.length) {
      throw new IllegalArgumentException("Design matrix row count (${X.length}) must match response length (${y.length})")
    }
    if (X[0].length != beta.length) {
      throw new IllegalArgumentException("Coefficient count (${beta.length}) must match design matrix column count (${X[0].length})")
    }
    if (X.any { double[] row -> row.length != beta.length }) {
      throw new IllegalArgumentException("Design matrix rows must all have the same length")
    }

    int n = y.length
    double rss = 0.0
    for (int i = 0; i < n; i++) {
      double fitted = 0.0
      for (int j = 0; j < beta.length; j++) {
        fitted += X[i][j] * beta[j]
      }
      double residual = y[i] - fitted
      rss += residual * residual
    }
    rss
  }

  private static int findPivotRow(double[][] matrix, int pivotColumn, int rowCount) {
    int maxRow = pivotColumn
    double maxVal = Math.abs(matrix[pivotColumn][pivotColumn])
    for (int i = pivotColumn + 1; i < rowCount; i++) {
      double value = Math.abs(matrix[i][pivotColumn])
      if (value > maxVal) {
        maxVal = value
        maxRow = i
      }
    }
    maxRow
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

  private static void swapRows(double[][] matrix, int firstRow, int secondRow) {
    double[] temp = matrix[firstRow]
    matrix[firstRow] = matrix[secondRow]
    matrix[secondRow] = temp
  }
}
