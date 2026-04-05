package se.alipsa.matrix.stats.linalg

import org.ejml.data.Complex_F64
import org.ejml.simple.SimpleEVD
import org.ejml.simple.SimpleMatrix
import org.ejml.simple.SimpleSVD

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix

/**
 * Public linear algebra facade for `matrix-stats`.
 * <p>
 * This API accepts dense numeric inputs as {@code double[][]}, {@link Matrix}, or
 * {@link Grid} and performs calculations in floating-point {@code double} precision.
 * It is intended for common dense linear algebra tasks, not arbitrary-precision algebra.
 * <p>
 * Matrix-shaped results returned as {@link Matrix} use synthetic column names (`c0`,
 * `c1`, ...) because decompositions and inverse matrices do not preserve the semantic
 * meaning of the original input column labels.
 */
final class Linalg {

  private static final double SINGULARITY_TOLERANCE = 1e-12
  private static final double IMAGINARY_TOLERANCE = 1e-10

  private Linalg() {
  }

  /**
   * Invert a square dense matrix.
   *
   * @param matrix the matrix to invert
   * @return the inverse matrix
   * @throws IllegalArgumentException if the matrix is null, ragged, empty, or not square
   * @throws LinalgSingularMatrixException if the matrix is singular
   */
  static double[][] inverse(double[][] matrix) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')

    SimpleMatrix dense = dense(matrix)
    requireNonSingular(dense, 'Matrix')
    LinalgAdapters.toDoubleArray(dense.invert())
  }

  /**
   * Invert a square Matrix.
   *
   * @param matrix the matrix to invert
   * @return the inverse matrix as a Matrix with synthetic column names
   * @throws IllegalArgumentException if the matrix is null, empty, contains non-numeric values, or is not square
   * @throws LinalgSingularMatrixException if the matrix is singular
   */
  static Matrix inverse(Matrix matrix) {
    LinalgAdapters.toMatrix(inverse(LinalgAdapters.toDoubleArray(matrix)))
  }

  /**
   * Invert a square Grid.
   *
   * @param grid the grid to invert
   * @return the inverse matrix as a Matrix with synthetic column names
   * @throws IllegalArgumentException if the grid is null, empty, ragged, contains non-numeric values, or is not square
   * @throws LinalgSingularMatrixException if the grid is singular
   */
  static Matrix inverse(Grid<?> grid) {
    LinalgAdapters.toMatrix(inverse(LinalgAdapters.toDoubleArray(grid)))
  }

  /**
   * Compute the determinant of a square dense matrix.
   *
   * @param matrix the matrix whose determinant should be computed
   * @return the determinant
   * @throws IllegalArgumentException if the matrix is null, ragged, empty, or not square
   */
  static double det(double[][] matrix) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')
    dense(matrix).determinant()
  }

  /**
   * Compute the determinant of a square Matrix.
   *
   * @param matrix the matrix whose determinant should be computed
   * @return the determinant
   * @throws IllegalArgumentException if the matrix is null, empty, contains non-numeric values, or is not square
   */
  static double det(Matrix matrix) {
    det(LinalgAdapters.toDoubleArray(matrix))
  }

  /**
   * Compute the determinant of a square Grid.
   *
   * @param grid the grid whose determinant should be computed
   * @return the determinant
   * @throws IllegalArgumentException if the grid is null, empty, ragged, contains non-numeric values, or is not square
   */
  static double det(Grid<?> grid) {
    det(LinalgAdapters.toDoubleArray(grid))
  }

  /**
   * Solve the square dense linear system {@code A * x = b}.
   *
   * @param matrix the coefficient matrix
   * @param vector the right-hand-side vector
   * @return the solution vector
   * @throws IllegalArgumentException if dimensions do not match or the inputs are invalid
   * @throws LinalgSingularMatrixException if the coefficient matrix is singular
   */
  static double[] solve(double[][] matrix, double[] vector) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')
    double[] rhs = validateVector(vector, shape[0], 'vector')

    SimpleMatrix dense = dense(matrix)
    requireNonSingular(dense, 'Matrix')
    SimpleMatrix solution = dense.solve(new SimpleMatrix(rhs.length, 1, true, rhs))
    toVector(solution)
  }

  /**
   * Solve the square dense linear system {@code A * x = b}.
   *
   * @param matrix the coefficient matrix
   * @param vector the right-hand-side vector
   * @return the solution vector
   * @throws IllegalArgumentException if dimensions do not match or the inputs are invalid
   * @throws LinalgSingularMatrixException if the coefficient matrix is singular
   */
  static double[] solve(double[][] matrix, List<? extends Number> vector) {
    solve(matrix, LinalgAdapters.toDoubleArray(vector))
  }

  /**
   * Solve the square linear system {@code A * x = b} for a Matrix coefficient matrix.
   *
   * @param matrix the coefficient matrix
   * @param vector the right-hand-side vector
   * @return the solution vector
   * @throws IllegalArgumentException if dimensions do not match or the inputs are invalid
   * @throws LinalgSingularMatrixException if the coefficient matrix is singular
   */
  static double[] solve(Matrix matrix, double[] vector) {
    solve(LinalgAdapters.toDoubleArray(matrix), vector)
  }

  /**
   * Solve the square linear system {@code A * x = b} for a Matrix coefficient matrix.
   *
   * @param matrix the coefficient matrix
   * @param vector the right-hand-side vector
   * @return the solution vector
   * @throws IllegalArgumentException if dimensions do not match or the inputs are invalid
   * @throws LinalgSingularMatrixException if the coefficient matrix is singular
   */
  static double[] solve(Matrix matrix, List<? extends Number> vector) {
    solve(LinalgAdapters.toDoubleArray(matrix), vector)
  }

  /**
   * Solve the square linear system {@code A * x = b} for a Grid coefficient matrix.
   *
   * @param grid the coefficient matrix
   * @param vector the right-hand-side vector
   * @return the solution vector
   * @throws IllegalArgumentException if dimensions do not match or the inputs are invalid
   * @throws LinalgSingularMatrixException if the coefficient matrix is singular
   */
  static double[] solve(Grid<?> grid, double[] vector) {
    solve(LinalgAdapters.toDoubleArray(grid), vector)
  }

  /**
   * Solve the square linear system {@code A * x = b} for a Grid coefficient matrix.
   *
   * @param grid the coefficient matrix
   * @param vector the right-hand-side vector
   * @return the solution vector
   * @throws IllegalArgumentException if dimensions do not match or the inputs are invalid
   * @throws LinalgSingularMatrixException if the coefficient matrix is singular
   */
  static double[] solve(Grid<?> grid, List<? extends Number> vector) {
    solve(LinalgAdapters.toDoubleArray(grid), vector)
  }

  /**
   * Compute the real eigenvalues of a square dense matrix.
   * <p>
   * Symmetric matrices are always supported. General real matrices are supported when
   * their eigenvalues are real within the configured imaginary-part tolerance. Matrices
   * with genuinely complex eigenvalues are rejected because this facade returns
   * {@code double[]} rather than complex numbers.
   *
   * @param matrix the matrix whose eigenvalues should be computed
   * @return the eigenvalues in descending order
   * @throws IllegalArgumentException if the matrix is invalid, not square, or has complex eigenvalues
   */
  static double[] eigenvalues(double[][] matrix) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')

    SimpleEVD<SimpleMatrix> decomposition = dense(matrix).eig()
    List<Double> values = []
    for (int i = 0; i < decomposition.numberOfEigenvalues; i++) {
      Complex_F64 eigenvalue = decomposition.getEigenvalue(i)
      if (Math.abs(eigenvalue.getImaginary()) > IMAGINARY_TOLERANCE) {
        throw new IllegalArgumentException('Complex eigenvalues are not supported by Linalg.eigenvalues()')
      }
      values << eigenvalue.getReal()
    }
    List<Double> sorted = [*values] as List<Double>
    sorted.sort { Double a, Double b -> b <=> a }
    double[] eigenvalues = new double[sorted.size()]
    for (int i = 0; i < sorted.size(); i++) {
      eigenvalues[i] = sorted[i]
    }
    eigenvalues
  }

  /**
   * Compute the real eigenvalues of a square Matrix.
   *
   * @param matrix the matrix whose eigenvalues should be computed
   * @return the eigenvalues in descending order
   * @throws IllegalArgumentException if the matrix is invalid, not square, or has complex eigenvalues
   */
  static double[] eigenvalues(Matrix matrix) {
    eigenvalues(LinalgAdapters.toDoubleArray(matrix))
  }

  /**
   * Compute the real eigenvalues of a square Grid.
   *
   * @param grid the grid whose eigenvalues should be computed
   * @return the eigenvalues in descending order
   * @throws IllegalArgumentException if the grid is invalid, not square, or has complex eigenvalues
   */
  static double[] eigenvalues(Grid<?> grid) {
    eigenvalues(LinalgAdapters.toDoubleArray(grid))
  }

  /**
   * Compute the singular value decomposition of a dense matrix.
   *
   * @param matrix the matrix to decompose
   * @return the singular value decomposition result
   * @throws IllegalArgumentException if the matrix is null, empty, or ragged
   */
  static SvdResult svd(double[][] matrix) {
    LinalgAdapters.validateRectangular(matrix, 'matrix')
    SimpleSVD<SimpleMatrix> decomposition = dense(matrix).svd()
    SimpleMatrix u = decomposition.getU()
    SimpleMatrix w = decomposition.getW()
    SimpleMatrix vt = decomposition.getV().transpose()

    double[] singularValues = new double[Math.min(w.numRows(), w.numCols())]
    for (int i = 0; i < singularValues.length; i++) {
      singularValues[i] = w.get(i, i)
    }
    new SvdResult(LinalgAdapters.toDoubleArray(u), LinalgAdapters.toDoubleArray(vt), singularValues)
  }

  /**
   * Compute the singular value decomposition of a Matrix.
   *
   * @param matrix the matrix to decompose
   * @return the singular value decomposition result
   * @throws IllegalArgumentException if the matrix is null, empty, or contains non-numeric values
   */
  static SvdResult svd(Matrix matrix) {
    svd(LinalgAdapters.toDoubleArray(matrix))
  }

  /**
   * Compute the singular value decomposition of a Grid.
   *
   * @param grid the grid to decompose
   * @return the singular value decomposition result
   * @throws IllegalArgumentException if the grid is null, empty, ragged, or contains non-numeric values
   */
  static SvdResult svd(Grid<?> grid) {
    svd(LinalgAdapters.toDoubleArray(grid))
  }

  private static SimpleMatrix dense(double[][] matrix) {
    new SimpleMatrix(matrix)
  }

  private static void requireSquare(int[] shape, String label) {
    if (shape[0] != shape[1]) {
      throw new IllegalArgumentException("${label} must be square")
    }
  }

  private static void requireNonSingular(SimpleMatrix matrix, String label) {
    if (Math.abs(matrix.determinant()) <= SINGULARITY_TOLERANCE) {
      throw new LinalgSingularMatrixException("${label} is singular and cannot be inverted or solved")
    }
  }

  private static double[] validateVector(double[] vector, int expectedLength, String label) {
    if (vector == null || vector.length == 0) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    if (vector.length != expectedLength) {
      throw new IllegalArgumentException("Matrix row count (${expectedLength}) must match ${label} length (${vector.length})")
    }
    double[] copy = new double[vector.length]
    for (int i = 0; i < vector.length; i++) {
      if (!Double.isFinite(vector[i])) {
        throw new IllegalArgumentException("${label.capitalize()} must contain only finite values")
      }
      copy[i] = vector[i]
    }
    copy
  }

  private static double[] toVector(SimpleMatrix matrix) {
    double[] values = new double[matrix.numRows()]
    for (int row = 0; row < matrix.numRows(); row++) {
      values[row] = matrix.get(row, 0)
    }
    values
  }
}
