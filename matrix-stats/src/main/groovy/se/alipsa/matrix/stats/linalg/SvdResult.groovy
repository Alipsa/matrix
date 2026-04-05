package se.alipsa.matrix.stats.linalg

import org.ejml.simple.SimpleMatrix

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Result carrier for singular value decomposition.
 * <p>
 * The decomposition uses the standard layout {@code A = U * Sigma * Vt}. Matrix-shaped
 * components use synthetic column names (`c0`, `c1`, ...) because the result spaces no
 * longer correspond directly to the original input column metadata.
 */
class SvdResult {

  final Matrix u
  final Matrix vt
  final List<BigDecimal> singularValues

  /**
   * Create a singular value decomposition result with idiomatic Groovy-facing components.
   *
   * @param u the left singular vectors
   * @param vt the transposed right singular vectors
   * @param singularValues the singular values on the Sigma diagonal
   */
  SvdResult(Matrix u, Matrix vt, List<? extends Number> singularValues) {
    this.u = copyMatrix(u, 'u')
    this.vt = copyMatrix(vt, 'vt')
    this.singularValues = copyVector(singularValues, 'singularValues')
  }

  /**
   * Build the rectangular Sigma matrix for the decomposition.
   *
   * @return the Sigma matrix with singular values on the diagonal
   */
  Matrix sigma() {
    int rows = u.rowCount()
    int columns = vt.columnCount()
    List<List<BigDecimal>> matrixRows = []
    for (int row = 0; row < rows; row++) {
      List<BigDecimal> currentRow = []
      for (int col = 0; col < columns; col++) {
        currentRow << (row == col && row < singularValues.size() ? singularValues[row] : 0.0)
      }
      matrixRows << currentRow
    }
    Matrix.builder()
      .columnNames(LinalgAdapters.syntheticColumnNames(columns))
      .rows(matrixRows)
      .types(([BigDecimal] * columns) as List<Class>)
      .build()
  }

  /**
   * Reconstruct the original matrix using {@code U * Sigma * Vt}.
   *
   * @return the reconstructed matrix with synthetic column names
   */
  Matrix reconstruct() {
    SimpleMatrix denseU = new SimpleMatrix(LinalgAdapters.toDoubleArray(u))
    SimpleMatrix denseSigma = new SimpleMatrix(LinalgAdapters.toDoubleArray(sigma()))
    SimpleMatrix denseVt = new SimpleMatrix(LinalgAdapters.toDoubleArray(vt))
    LinalgAdapters.toMatrix(LinalgAdapters.toDoubleArray(denseU.mult(denseSigma).mult(denseVt)))
  }

  /**
   * @return {@code U} as a Matrix with synthetic column names
   */
  Matrix uMatrix() {
    u
  }

  /**
   * @return {@code Vt} as a Matrix with synthetic column names
   */
  Matrix vtMatrix() {
    vt
  }

  /**
   * @return {@code Sigma} as a Matrix with synthetic column names
   */
  Matrix sigmaMatrix() {
    sigma()
  }

  private static Matrix copyMatrix(Matrix matrix, String label) {
    if (matrix == null) {
      throw new IllegalArgumentException("${label.capitalize()} cannot be null")
    }
    LinalgAdapters.toMatrix(LinalgAdapters.toDoubleArray(matrix))
  }

  private static List<BigDecimal> copyVector(List<? extends Number> values, String label) {
    LinalgAdapters.toBigDecimalVector(NumericConversion.toDoubleArray(values, label)).asImmutable() as List<BigDecimal>
  }
}
