package se.alipsa.matrix.stats.linalg

import org.ejml.data.Complex_F64
import org.ejml.simple.SimpleEVD
import org.ejml.simple.SimpleMatrix
import org.ejml.simple.SimpleSVD

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Public linear algebra facade for `matrix-stats`.
 * <p>
 * This API accepts idiomatic Groovy-facing numeric inputs such as {@link Matrix},
 * {@link Grid}, and numeric lists. Matrix-heavy computation still runs in floating-point
 * {@code double} precision internally through EJML, but public scalar and vector results
 * are exposed as {@code BigDecimal} and {@code List<BigDecimal>}. Decomposition results
 * are exposed as {@link Matrix} components plus {@code List<BigDecimal>} singular values.
 * <p>
 * Matrix-shaped results returned as {@link Matrix} use synthetic column names (`c0`,
 * `c1`, ...) because decompositions and inverse matrices do not preserve the semantic
 * meaning of the original input column labels. Grid-shaped results are exposed as
 * {@code Grid<BigDecimal>}.
 */
final class Linalg {

  private static final double SINGULARITY_TOLERANCE = 1e-12
  private static final double IMAGINARY_TOLERANCE = 1e-10

  private Linalg() {
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
    LinalgAdapters.toMatrix(inverseValues(LinalgAdapters.toDoubleArray(matrix)))
  }

  /**
   * Invert a square Grid.
   *
   * @param grid the grid to invert
   * @return the inverse matrix as a {@code Grid<BigDecimal>}
   * @throws IllegalArgumentException if the grid is null, empty, ragged, contains non-numeric values, or is not square
   * @throws LinalgSingularMatrixException if the grid is singular
   */
  static Grid<BigDecimal> inverse(Grid<?> grid) {
    LinalgAdapters.toGrid(inverseValues(LinalgAdapters.toDoubleArray(grid)))
  }

  /**
   * Compute the determinant of a square Matrix.
   *
   * @param matrix the matrix whose determinant should be computed
   * @return the determinant
   * @throws IllegalArgumentException if the matrix is null, empty, contains non-numeric values, or is not square
   */
  static BigDecimal det(Matrix matrix) {
    BigDecimal.valueOf(detValue(LinalgAdapters.toDoubleArray(matrix)))
  }

  /**
   * Compute the determinant of a square Grid.
   *
   * @param grid the grid whose determinant should be computed
   * @return the determinant
   * @throws IllegalArgumentException if the grid is null, empty, ragged, contains non-numeric values, or is not square
   */
  static BigDecimal det(Grid<?> grid) {
    BigDecimal.valueOf(detValue(LinalgAdapters.toDoubleArray(grid)))
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
  static List<BigDecimal> solve(Matrix matrix, List<? extends Number> vector) {
    LinalgAdapters.toBigDecimalVector(solveValues(LinalgAdapters.toDoubleArray(matrix), vector))
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
  static List<BigDecimal> solve(Grid<?> grid, List<? extends Number> vector) {
    LinalgAdapters.toBigDecimalVector(solveValues(LinalgAdapters.toDoubleArray(grid), vector))
  }

  /**
   * Compute the real eigenvalues of a square Matrix.
   * <p>
   * Symmetric matrices are always supported. General real matrices are supported when
   * their eigenvalues are real within the configured imaginary-part tolerance. Matrices
   * with genuinely complex eigenvalues are rejected because this facade does not expose
   * complex-number results.
   *
   * @param matrix the matrix whose eigenvalues should be computed
   * @return the eigenvalues in descending order
   * @throws IllegalArgumentException if the matrix is invalid, not square, or has complex eigenvalues
   */
  static List<BigDecimal> eigenvalues(Matrix matrix) {
    LinalgAdapters.toBigDecimalVector(eigenvaluesValues(LinalgAdapters.toDoubleArray(matrix)))
  }

  /**
   * Compute the real eigenvalues of a square Grid.
   *
   * @param grid the grid whose eigenvalues should be computed
   * @return the eigenvalues in descending order
   * @throws IllegalArgumentException if the grid is invalid, not square, or has complex eigenvalues
   */
  static List<BigDecimal> eigenvalues(Grid<?> grid) {
    LinalgAdapters.toBigDecimalVector(eigenvaluesValues(LinalgAdapters.toDoubleArray(grid)))
  }

  /**
   * Compute the singular value decomposition of a dense matrix.
   *
   * @param matrix the matrix to decompose
   * @return the singular value decomposition result
   * @throws IllegalArgumentException if the matrix is null, empty, or ragged
   */
  private static SvdResult svdValues(double[][] matrix) {
    LinalgAdapters.validateRectangular(matrix, 'matrix')
    SimpleSVD<SimpleMatrix> decomposition = dense(matrix).svd()
    SimpleMatrix u = decomposition.getU()
    SimpleMatrix w = decomposition.getW()
    SimpleMatrix vt = decomposition.getV().transpose()

    double[] singularValues = new double[Math.min(w.numRows(), w.numCols())]
    for (int i = 0; i < singularValues.length; i++) {
      singularValues[i] = w.get(i, i)
    }
    new SvdResult(
      LinalgAdapters.toMatrix(LinalgAdapters.toDoubleArray(u)),
      LinalgAdapters.toMatrix(LinalgAdapters.toDoubleArray(vt)),
      LinalgAdapters.toBigDecimalVector(singularValues)
    )
  }

  /**
   * Compute the singular value decomposition of a Matrix.
   *
   * @param matrix the matrix to decompose
   * @return the singular value decomposition result
   * @throws IllegalArgumentException if the matrix is null, empty, or contains non-numeric values
   */
  static SvdResult svd(Matrix matrix) {
    svdValues(LinalgAdapters.toDoubleArray(matrix))
  }

  /**
   * Compute the singular value decomposition of a Grid.
   *
   * @param grid the grid to decompose
   * @return the singular value decomposition result
   * @throws IllegalArgumentException if the grid is null, empty, ragged, or contains non-numeric values
   */
  static SvdResult svd(Grid<?> grid) {
    svdValues(LinalgAdapters.toDoubleArray(grid))
  }

  private static double[][] inverseValues(double[][] matrix) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')

    SimpleMatrix dense = dense(matrix)
    requireNonSingular(dense, 'Matrix')
    LinalgAdapters.toDoubleArray(dense.invert())
  }

  private static double detValue(double[][] matrix) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')
    dense(matrix).determinant()
  }

  private static double[] solveValues(double[][] matrix, List<? extends Number> vector) {
    solveValues(matrix, NumericConversion.toDoubleArray(vector, 'vector'))
  }

  private static double[] solveValues(double[][] matrix, double[] vector) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, 'matrix')
    requireSquare(shape, 'Matrix')
    double[] rhs = validateVector(vector, shape[0], 'vector')

    SimpleMatrix dense = dense(matrix)
    requireNonSingular(dense, 'Matrix')
    SimpleMatrix solution = dense.solve(new SimpleMatrix(rhs.length, 1, true, rhs))
    toVector(solution)
  }

  private static double[] eigenvaluesValues(double[][] matrix) {
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
    List<Double> sorted = [*values]
    sorted.sort { Double a, Double b -> b <=> a }
    double[] eigenvalues = new double[sorted.size()]
    for (int i = 0; i < sorted.size(); i++) {
      eigenvalues[i] = sorted[i]
    }
    eigenvalues
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
