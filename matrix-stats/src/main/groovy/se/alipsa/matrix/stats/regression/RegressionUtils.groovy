package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.stats.util.LeastSquaresKernel

/**
 * Internal helpers for regression diagnostics and standard error calculations.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class RegressionUtils {

  private static final BigDecimal[][] EMPTY_BIG_DECIMAL_MATRIX = new BigDecimal[0][0]

  /**
   * Compute (X'X)^{-1} for a polynomial design matrix [1, x, x^2, ...].
   * Returns an empty array when the input is invalid or the matrix is singular.
   * @param xValues predictor observations
   * @param degree polynomial degree
   * @param center basis center, default zero
   * @param scale basis scale, default one; use the fitted PolynomialRegression scale
   * @return unscaled covariance in the requested polynomial basis
   */
  static BigDecimal[][] polynomialXtxInverse(List<? extends Number> xValues, int degree, Number center = 0, Number scale = 1) {
    int n = xValues.size()
    if (n == 0 || degree < 1) {
      return EMPTY_BIG_DECIMAL_MATRIX
    }
    double[][] design = new double[n][degree + 1]
    for (int i = 0; i < n; i++) {
      double x = (xValues[i].doubleValue() - center.doubleValue()) / scale.doubleValue()
      double term = 1.0d
      for (int j = 0; j <= degree; j++) {
        design[i][j] = term
        term *= x
      }
    }
    double[][] inverse
    try {
      inverse = LeastSquaresKernel.fitMultipleLinearRegression(new double[n], design).unscaledCovariance()
    } catch (IllegalArgumentException ignored) {
      return EMPTY_BIG_DECIMAL_MATRIX
    }
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
    if (xtxInv == null || xtxInv.length == 0) {
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
    leverage
  }

  /**
   * Calculates leverage using the same basis as the fitted polynomial.
   * @param xtxInv unscaled covariance returned by polynomialXtxInverse
   * @param x predictor at which to calculate leverage
   * @param degree polynomial degree
   * @param center basis center, default zero
   * @param scale basis scale, default one
   * @return leverage, or null when covariance is unavailable
   */
  static BigDecimal polynomialLeverage(BigDecimal[][] xtxInv, BigDecimal x, int degree, Number center = 0, Number scale = 1) {
    x = (x - (center as BigDecimal)) / (scale as BigDecimal)
    if (xtxInv == null || xtxInv.length == 0) {
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
    leverage
  }
}
