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
  // TODO implement our own Array2DRowRealMatrix and LUDecomposition that works with BigDecimal for cleaner groovy code,
  //  better precision and to avoid dependency on commons-math3
  static BigDecimal[][] polynomialXtxInverse(List<? extends Number> xValues, int degree) {
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
    double[][] inverse = solver.inverse.data
    BigDecimal[][] result = new BigDecimal[inverse.length][inverse[0].length]
    for (int i = 0; i < inverse.length; i++) {
      for (int j = 0; j < inverse[i].length; j++) {
        result[i][j] = inverse[i][j] as BigDecimal
      }
    }
    result
  }

  /**
   * Compute leverage v'(X'X)^{-1}v for a polynomial basis vector.
   * @deprecated Use {@link #polynomialLeverage(BigDecimal[][], BigDecimal, int)} for idiomatic groovy
   */
  @Deprecated
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

  static BigDecimal polynomialLeverage(BigDecimal[][] xtxInv, BigDecimal x, int degree) {
    if (xtxInv == null) {
      return null
    }
    int size = degree + 1
    BigDecimal[] v = new BigDecimal[size]
    BigDecimal term = 1.0
    for (int i = 0; i < size; i++) {
      v[i] = term
      term *= x
    }
    BigDecimal[] tmp = new BigDecimal[size]
    for (int i = 0; i < size; i++) {
      BigDecimal sum = 0.0
      BigDecimal[] row = xtxInv[i]
      for (int j = 0; j < size; j++) {
        sum += row[j] * v[j]
      }
      tmp[i] = sum
    }
    BigDecimal leverage = 0.0
    for (int i = 0; i < size; i++) {
      leverage += v[i] * tmp[i]
    }
    return leverage
  }
}
