package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
final class TimeSeriesUtils {

  private static final double SINGULARITY_THRESHOLD = 1e-14

  private TimeSeriesUtils() {
  }

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
    if (rowCount != columnCount) {
      return fitOLS(b, A)
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
      if (Math.abs(augmented[k][k]) < SINGULARITY_THRESHOLD) {
        throw new IllegalArgumentException("Singular matrix - cannot solve linear system")
      }

      for (int i = k + 1; i < rowCount; i++) {
        double factor = augmented[i][k] / augmented[k][k]
        for (int j = k; j < columnCount + 1; j++) {
          augmented[i][j] -= factor * augmented[k][j]
        }
      }
    }

    double[] x = new double[columnCount]
    for (int i = columnCount - 1; i >= 0; i--) {
      double sum = augmented[i][columnCount]
      for (int j = i + 1; j < columnCount; j++) {
        sum -= augmented[i][j] * x[j]
      }
      x[i] = sum / augmented[i][i]
    }
    x
  }

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
      int maxRow = k
      double maxVal = Math.abs(augmented[k][k])
      for (int i = k + 1; i < n; i++) {
        double value = Math.abs(augmented[i][k])
        if (value > maxVal) {
          maxVal = value
          maxRow = i
        }
      }

      if (maxRow != k) {
        swapRows(augmented, k, maxRow)
      }
      double pivot = augmented[k][k]
      if (Math.abs(pivot) < SINGULARITY_THRESHOLD) {
        throw new IllegalArgumentException("Singular matrix - cannot invert")
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
    result
  }

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

  private static void swapRows(double[][] matrix, int firstRow, int secondRow) {
    double[] temp = matrix[firstRow]
    matrix[firstRow] = matrix[secondRow]
    matrix[secondRow] = temp
  }
}
