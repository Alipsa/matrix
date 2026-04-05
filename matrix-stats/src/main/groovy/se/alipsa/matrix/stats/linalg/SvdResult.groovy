package se.alipsa.matrix.stats.linalg

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.linear.MatrixAlgebra

/**
 * Result carrier for singular value decomposition.
 * <p>
 * The decomposition uses the standard layout {@code A = U * Sigma * Vt}. Matrix-shaped
 * convenience accessors synthesize column names (`c0`, `c1`, ...) because the result
 * spaces no longer correspond directly to the original input column metadata.
 */
@CompileStatic
class SvdResult {

  final double[][] u
  final double[][] vt
  final double[] singularValues

  SvdResult(double[][] u, double[][] vt, double[] singularValues) {
    this.u = copyMatrix(u, 'u')
    this.vt = copyMatrix(vt, 'vt')
    this.singularValues = copyVector(singularValues, 'singularValues')
  }

  /**
   * Build the rectangular Sigma matrix for the decomposition.
   *
   * @return the Sigma matrix with singular values on the diagonal
   */
  double[][] sigma() {
    double[][] sigma = new double[u.length][vt[0].length]
    int diagonalSize = Math.min(Math.min(u.length, vt[0].length), singularValues.length)
    for (int i = 0; i < diagonalSize; i++) {
      sigma[i][i] = singularValues[i]
    }
    sigma
  }

  /**
   * Reconstruct the original matrix using {@code U * Sigma * Vt}.
   *
   * @return the reconstructed dense matrix
   */
  double[][] reconstruct() {
    MatrixAlgebra.multiply(MatrixAlgebra.multiply(u, sigma()), vt)
  }

  /**
   * @return {@code U} as a Matrix with synthetic column names
   */
  Matrix uMatrix() {
    LinalgAdapters.toMatrix(u)
  }

  /**
   * @return {@code Vt} as a Matrix with synthetic column names
   */
  Matrix vtMatrix() {
    LinalgAdapters.toMatrix(vt)
  }

  /**
   * @return {@code Sigma} as a Matrix with synthetic column names
   */
  Matrix sigmaMatrix() {
    LinalgAdapters.toMatrix(sigma())
  }

  private static double[][] copyMatrix(double[][] matrix, String label) {
    int[] shape = LinalgAdapters.validateRectangular(matrix, label)
    double[][] copy = new double[shape[0]][shape[1]]
    for (int row = 0; row < shape[0]; row++) {
      System.arraycopy(matrix[row], 0, copy[row], 0, shape[1])
    }
    copy
  }

  private static double[] copyVector(double[] values, String label) {
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
}
