package se.alipsa.matrix.stats.linear

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.util.Logger

/**
 * Native matrix operations used by the statistics implementations to avoid runtime
 * dependence on commons-math3 linear algebra classes.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
final class MatrixAlgebra {

  private static final double SINGULARITY_THRESHOLD = 1e-14
  private static final double SYMMETRY_THRESHOLD = 1e-10
  private static final int MAX_JACOBI_SWEEPS = 100
  private static final Logger log = Logger.getLogger(MatrixAlgebra)

  private MatrixAlgebra() {
  }

  /**
   * Returns the transpose of the supplied rectangular matrix.
   *
   * @param matrix the source matrix
   * @return the transposed matrix
   * @throws IllegalArgumentException if the matrix is null, empty, or ragged
   */
  static double[][] transpose(double[][] matrix) {
    int[] shape = validateRectangular(matrix, 'matrix')
    int rows = shape[0]
    int columns = shape[1]
    double[][] result = new double[columns][rows]
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        result[j][i] = matrix[i][j]
      }
    }
    result
  }

  /**
   * Multiplies two matrices.
   *
   * @param left the left matrix
   * @param right the right matrix
   * @return the matrix product
   * @throws IllegalArgumentException if either matrix is invalid or the inner dimensions do not match
   */
  static double[][] multiply(double[][] left, double[][] right) {
    int[] leftShape = validateRectangular(left, 'left matrix')
    int[] rightShape = validateRectangular(right, 'right matrix')
    if (leftShape[1] != rightShape[0]) {
      throw new IllegalArgumentException(
          "Matrix dimension mismatch: ${leftShape[0]}x${leftShape[1]} cannot be multiplied by ${rightShape[0]}x${rightShape[1]}"
      )
    }

    double[][] result = new double[leftShape[0]][rightShape[1]]
    for (int i = 0; i < leftShape[0]; i++) {
      for (int k = 0; k < leftShape[1]; k++) {
        double value = left[i][k]
        if (value == 0.0d) {
          continue
        }
        for (int j = 0; j < rightShape[1]; j++) {
          result[i][j] += value * right[k][j]
        }
      }
    }
    validateFiniteMatrix(result, 'matrix multiplication result')
    result
  }

  /**
   * Subtracts two matrices element-wise.
   *
   * @param left the minuend matrix
   * @param right the subtrahend matrix
   * @return {@code left - right}
   * @throws IllegalArgumentException if the matrices are invalid or shapes differ
   */
  static double[][] subtract(double[][] left, double[][] right) {
    int[] leftShape = validateRectangular(left, 'left matrix')
    int[] rightShape = validateRectangular(right, 'right matrix')
    if (leftShape[0] != rightShape[0] || leftShape[1] != rightShape[1]) {
      throw new IllegalArgumentException(
          "Matrix dimension mismatch: ${leftShape[0]}x${leftShape[1]} does not match ${rightShape[0]}x${rightShape[1]}"
      )
    }

    double[][] result = new double[leftShape[0]][leftShape[1]]
    for (int i = 0; i < leftShape[0]; i++) {
      for (int j = 0; j < leftShape[1]; j++) {
        result[i][j] = left[i][j] - right[i][j]
      }
    }
    validateFiniteMatrix(result, 'matrix subtraction result')
    result
  }

  /**
   * Multiplies a matrix by a scalar.
   *
   * @param matrix the source matrix
   * @param scalar the scalar multiplier
   * @return the scaled matrix
   * @throws IllegalArgumentException if the matrix is invalid or the result contains non-finite values
   */
  static double[][] scale(double[][] matrix, double scalar) {
    int[] shape = validateRectangular(matrix, 'matrix')
    double[][] result = new double[shape[0]][shape[1]]
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        result[i][j] = matrix[i][j] * scalar
      }
    }
    validateFiniteMatrix(result, 'scaled matrix')
    result
  }

  /**
   * Inverts a square matrix using Gauss-Jordan elimination with partial pivoting.
   *
   * @param matrix the matrix to invert
   * @return the inverse matrix
   * @throws IllegalArgumentException if the matrix is invalid or singular
   */
  static double[][] inverse(double[][] matrix) {
    int[] shape = validateRectangular(matrix, 'matrix')
    int n = shape[0]
    if (n != shape[1]) {
      throw new IllegalArgumentException("Only square matrices can be inverted")
    }

    double[][] augmented = new double[n][2 * n]
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        augmented[i][j] = matrix[i][j]
        augmented[i][j + n] = i == j ? 1.0d : 0.0d
      }
    }

    for (int k = 0; k < n; k++) {
      int pivotRow = findPivotRow(augmented, k, n)
      if (pivotRow != k) {
        swapRows(augmented, k, pivotRow)
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
        if (i == k) {
          continue
        }
        double factor = augmented[i][k]
        for (int j = 0; j < 2 * n; j++) {
          augmented[i][j] -= factor * augmented[k][j]
        }
      }
    }

    double[][] result = new double[n][n]
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        result[i][j] = augmented[i][j + n]
      }
    }
    validateFiniteMatrix(result, 'matrix inverse')
    result
  }

  /**
   * Computes the lower-triangular Cholesky factor of a symmetric positive-definite matrix.
   *
   * @param matrix the symmetric positive-definite matrix
   * @return lower-triangular matrix {@code L} such that {@code LL' = matrix}
   * @throws IllegalArgumentException if the matrix is invalid, non-symmetric, or not positive definite
   */
  static double[][] cholesky(double[][] matrix) {
    int[] shape = validateRectangular(matrix, 'matrix')
    int n = shape[0]
    if (n != shape[1]) {
      throw new IllegalArgumentException("Cholesky decomposition requires a square matrix")
    }
    validateSymmetric(matrix)

    double[][] lower = new double[n][n]
    for (int i = 0; i < n; i++) {
      for (int j = 0; j <= i; j++) {
        double sum = matrix[i][j]
        for (int k = 0; k < j; k++) {
          sum -= lower[i][k] * lower[j][k]
        }

        if (i == j) {
          if (sum <= SINGULARITY_THRESHOLD) {
            log.warn("Non-positive diagonal at row $i during Cholesky decomposition")
            throw new IllegalArgumentException("Matrix is not positive definite")
          }
          lower[i][j] = Math.sqrt(sum)
        } else {
          lower[i][j] = sum / lower[j][j]
        }
      }
    }
    validateFiniteMatrix(lower, 'Cholesky factor')
    lower
  }

  /**
   * Computes the eigenvalues of a symmetric matrix using Jacobi sweeps.
   *
   * @param matrix the symmetric matrix
   * @return the eigenvalues in arbitrary order
   * @throws IllegalArgumentException if the matrix is invalid or non-symmetric
   */
  static double[] symmetricEigenvalues(double[][] matrix) {
    int[] shape = validateRectangular(matrix, 'matrix')
    int n = shape[0]
    if (n != shape[1]) {
      throw new IllegalArgumentException("Eigenvalue decomposition requires a square matrix")
    }
    validateSymmetric(matrix)

    double[][] a = copy(matrix)
    if (n == 1) {
      return [a[0][0]] as double[]
    }

    for (int sweep = 0; sweep < MAX_JACOBI_SWEEPS; sweep++) {
      int p = 0
      int q = 1
      double maxOffDiagonal = 0.0d

      for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
          double value = Math.abs(a[i][j])
          if (value > maxOffDiagonal) {
            maxOffDiagonal = value
            p = i
            q = j
          }
        }
      }

      if (maxOffDiagonal <= SYMMETRY_THRESHOLD) {
        return diagonal(a)
      }

      double app = a[p][p]
      double aqq = a[q][q]
      double apq = a[p][q]
      double tau = (aqq - app) / (2.0d * apq)
      double t = tau == 0.0d ?
          1.0d :
          Math.copySign(1.0d, tau) / (Math.abs(tau) + Math.sqrt(1.0d + tau * tau))
      double c = 1.0d / Math.sqrt(1.0d + t * t)
      double s = t * c

      for (int k = 0; k < n; k++) {
        if (k == p || k == q) {
          continue
        }
        double akp = a[k][p]
        double akq = a[k][q]
        a[k][p] = c * akp - s * akq
        a[p][k] = a[k][p]
        a[k][q] = s * akp + c * akq
        a[q][k] = a[k][q]
      }

      a[p][p] = app - t * apq
      a[q][q] = aqq + t * apq
      a[p][q] = 0.0d
      a[q][p] = 0.0d
    }

    log.warn("Jacobi eigenvalue iteration did not converge after $MAX_JACOBI_SWEEPS sweeps")
    throw new IllegalArgumentException("Jacobi eigenvalue iteration failed to converge")
  }

  private static int[] validateRectangular(double[][] matrix, String label) {
    if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one row and one column")
    }
    int columns = matrix[0].length
    for (int i = 1; i < matrix.length; i++) {
      if (matrix[i].length != columns) {
        throw new IllegalArgumentException("${label.capitalize()} rows must all have the same length")
      }
    }
    [matrix.length, columns] as int[]
  }

  private static void validateSymmetric(double[][] matrix) {
    for (int i = 0; i < matrix.length; i++) {
      for (int j = i + 1; j < matrix.length; j++) {
        double difference = Math.abs(matrix[i][j] - matrix[j][i])
        double scale = Math.max(1.0d, Math.max(Math.abs(matrix[i][j]), Math.abs(matrix[j][i])))
        if (difference > SYMMETRY_THRESHOLD * scale) {
          throw new IllegalArgumentException("Matrix must be symmetric")
        }
      }
    }
  }

  private static double[][] copy(double[][] matrix) {
    double[][] copy = new double[matrix.length][matrix[0].length]
    for (int i = 0; i < matrix.length; i++) {
      System.arraycopy(matrix[i], 0, copy[i], 0, matrix[i].length)
    }
    copy
  }

  private static double[] diagonal(double[][] matrix) {
    double[] values = new double[matrix.length]
    for (int i = 0; i < matrix.length; i++) {
      values[i] = matrix[i][i]
    }
    validateFiniteVector(values, 'eigenvalues')
    values
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

  private static void swapRows(double[][] matrix, int firstRow, int secondRow) {
    double[] temp = matrix[firstRow]
    matrix[firstRow] = matrix[secondRow]
    matrix[secondRow] = temp
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

  private static void validateFiniteVector(double[] values, String label) {
    for (int i = 0; i < values.length; i++) {
      if (!Double.isFinite(values[i])) {
        log.error("Non-finite value in $label at index $i: ${values[i]}")
        throw new IllegalArgumentException("Non-finite value in ${label} at index ${i}")
      }
    }
  }
}
