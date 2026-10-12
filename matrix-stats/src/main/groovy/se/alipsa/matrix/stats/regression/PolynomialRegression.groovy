package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.util.LeastSquaresKernel

import java.math.RoundingMode

/**
 * Polynomial regression fits a polynomial of degree n to the data.
 *
 * The model is: Y = a0 + a1*X + a2*X^2 + ... + an*X^n
 *
 * Example usage:
 * <pre>
 * def x = [1, 2, 3, 4, 5]
 * def y = [1, 4, 9, 16, 25]  // y = x^2
 * def poly = new PolynomialRegression(x, y, 2)
 * println poly.predict(6)  // ~36
 * </pre>
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class PolynomialRegression {


  /** Polynomial coefficients [a0, a1, a2, ...] where a0 is constant */
  double[] coefficients

  /** Center used for the stable polynomial basis. */
  final double center
  /** Scale used for the stable polynomial basis. */
  double scale
  private double[] standardizedCoefficients

  /** The degree of the polynomial */
  int degree

  /** R-squared value (coefficient of determination) */
  BigDecimal r2

  String x = 'X'
  String y = 'Y'

  PolynomialRegression(Matrix table, String x, String y, int degree) {
    this(table[x] as List<? extends Number>, table[y] as List<? extends Number>, degree)
    this.x = x
    this.y = y
  }

  PolynomialRegression(List<? extends Number> x, List<? extends Number> y, int degree) {
    if (x.size() != y.size()) {
      throw new IllegalArgumentException('Must have equal number of X and Y data points')
    }
    if (degree < 1) {
      throw new IllegalArgumentException("Polynomial degree must be at least 1, got: $degree")
    }
    if (x.size() <= degree) {
      int minPoints = degree + 1
      throw new IllegalArgumentException(
        "Need at least ${minPoints} data points for polynomial of degree ${degree}, got: ${x.size()}"
      )
    }

    this.degree = degree

    int n = x.size()
    center = Stat.mean(x) as double
    scale = x.collect { Number value -> Math.abs((value as double) - center) }.max() as double
    if (scale == 0.0d) {
      throw new IllegalArgumentException('Polynomial predictor has no variation')
    }
    double[][] design = new double[n][degree + 1]
    double[] response = new double[n]
    for (int i = 0; i < n; i++) {
      double xValue = ((x[i] as double) - center) / scale
      double term = 1.0d
      for (int j = 0; j <= degree; j++) {
        design[i][j] = term
        term *= xValue
      }
      response[i] = y[i].doubleValue()
    }

    standardizedCoefficients = LeastSquaresKernel.fitRankRevealing(response, design).coefficients()
    for (int j = 0; j < standardizedCoefficients.length; j++) {
      if (Double.isNaN(standardizedCoefficients[j])) {
        standardizedCoefficients[j] = 0.0d
      }
    }
    coefficients = new double[degree + 1]
    for (int j = 0; j <= degree; j++) {
      double binomial = 1.0d
      for (int k = 0; k <= j; k++) {
        coefficients[k] += standardizedCoefficients[j] * binomial * Math.pow(-center, j - k) / Math.pow(scale, j)
        binomial *= (j - k) / (double) (k + 1)
      }
    }

    // Compute R-squared
    computeRSquared(x, y)
  }

  private void computeRSquared(List<? extends Number> x, List<? extends Number> y) {
    int n = x.size()
    BigDecimal ySum = Stat.sum(y)
    BigDecimal yMean = ySum / n

    BigDecimal ssTot = 0.0G  // Total sum of squares
    BigDecimal ssRes = 0.0G  // Residual sum of squares

    for (int i = 0; i < n; i++) {
      BigDecimal yi = y[i] as BigDecimal
      BigDecimal yHat = predict(x[i])
      ssTot += (yi - yMean) ** 2
      ssRes += (yi - yHat) ** 2
    }

    r2 = ssTot > 0 ? (1.0G - ssRes / ssTot) : 1.0G
  }

  /**
   * Predict Y value for given X.
   * Uses Horner's method for efficient polynomial evaluation.
   */
  BigDecimal predict(Number xVal) {
    double xd = ((xVal as double) - center) / scale
    double result = standardizedCoefficients[degree]
    for (int i = degree - 1; i >= 0; i--) {
      result = result * xd + standardizedCoefficients[i]
    }
    BigDecimal.valueOf(result)
  }

  BigDecimal predict(Number xVal, int numberOfDecimals) {
    predict(xVal).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  List<BigDecimal> predict(List<Number> xVals) {
    xVals.collect { predict(it) }
  }

  List<BigDecimal> predict(List<Number> xVals, int numberOfDecimals) {
    xVals.collect { predict(it, numberOfDecimals) }
  }

  /**
   * Get coefficient at given power of x.
   * getCoefficient(0) returns constant term
   * getCoefficient(1) returns linear coefficient
   * getCoefficient(2) returns quadratic coefficient, etc.
   */
  BigDecimal getCoefficient(int power) {
    if (power < 0 || power > degree) {
      throw new IllegalArgumentException("Power must be between 0 and $degree (inclusive)")
    }
    BigDecimal.valueOf(coefficients[power])
  }

  BigDecimal getRsquared() {
    r2
  }

  BigDecimal getRsquared(int numberOfDecimals) {
    getRsquared().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  int getDegree() {
    degree
  }

  @Override
  String toString() {
    StringBuilder sb = new StringBuilder('Y = ')
    for (int i = 0; i <= degree; i++) {
      BigDecimal coef = getCoefficient(i).setScale(3, RoundingMode.HALF_EVEN)
      if (i == 0) {
        sb.append(coef)
      } else {
        sb.append(coef >= 0 ? ' + ' : ' - ')
        sb.append(coef.abs())
        if (i == 1) {
          sb.append('X')
        } else {
          sb.append("X^$i")
        }
      }
    }
    sb.toString()
  }

  String summary() {
    StringBuilder sb = new StringBuilder()
    sb.append("Polynomial Regression (degree $degree)\n")
    sb.append("Equation: ${this}\n")
    sb.append('\nCoefficients:\n')
    for (int i = 0; i <= degree; i++) {
      String term = i == 0 ? '(Intercept)' : (i == 1 ? x : "${x}^$i")
      sb.append("  $term: ${getCoefficient(i).setScale(6, RoundingMode.HALF_EVEN)}\n")
    }
    sb.append("\nR-squared: ${getRsquared(4)}\n")
    sb.toString()
  }
}
