package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.RealMatrix

/**
 * Internal helpers for regression diagnostics and standard error calculations.
 */
@CompileStatic
class RegressionUtils {

  /**
   * Compute (X'X)^{-1} for a polynomial design matrix [1, x, x^2, ...].
   * Returns null when the matrix is singular.
   */
  static double[][] polynomialXtxInverse(List<? extends Number> xValues, int degree) {
    int n = xValues.size()
    if (n == 0 || degree < 1) {
      return null
    }
    double[][] design = new double[n][degree + 1]
    for (int i = 0; i < n; i++) {
      double x = xValues[i].doubleValue()
      double term = 1.0d
      for (int j = 0; j <= degree; j++) {
        design[i][j] = term
        term *= x
      }
    }
    RealMatrix xMatrix = new Array2DRowRealMatrix(design, false)
    RealMatrix xtx = xMatrix.transpose().multiply(xMatrix)
    def solver = new LUDecomposition(xtx).solver
    if (!solver.isNonSingular()) {
      return null
    }
    return solver.inverse.data
  }

  /**
   * Compute leverage v'(X'X)^{-1}v for a polynomial basis vector.
   */
  static double polynomialLeverage(double[][] xtxInv, double x, int degree) {
    if (xtxInv == null) {
      return Double.NaN
    }
    int size = degree + 1
    double[] v = new double[size]
    double term = 1.0d
    for (int i = 0; i < size; i++) {
      v[i] = term
      term *= x
    }
    double[] tmp = new double[size]
    for (int i = 0; i < size; i++) {
      double sum = 0.0d
      double[] row = xtxInv[i]
      for (int j = 0; j < size; j++) {
        sum += row[j] * v[j]
      }
      tmp[i] = sum
    }
    double leverage = 0.0d
    for (int i = 0; i < size; i++) {
      leverage += v[i] * tmp[i]
    }
    return leverage
  }
}
