package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/**
 * Polynomial regression fits a polynomial of degree n to the data.
 * Uses commons-math3's PolynomialCurveFitter.
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
@CompileStatic
class PolynomialRegression {

  /** Polynomial coefficients [a0, a1, a2, ...] where a0 is constant */
  double[] coefficients

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
      throw new IllegalArgumentException("Must have equal number of X and Y data points")
    }
    if (degree < 1) {
      throw new IllegalArgumentException("Polynomial degree must be at least 1")
    }
    if (x.size() <= degree) {
      throw new IllegalArgumentException("Need more data points than polynomial degree")
    }

    this.degree = degree

    // Build observations for fitting
    WeightedObservedPoints obs = new WeightedObservedPoints()
    int n = x.size()
    for (int i = 0; i < n; i++) {
      obs.add(x[i].doubleValue(), y[i].doubleValue())
    }

    // Fit polynomial
    PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree)
    coefficients = fitter.fit(obs.toList())

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
    double xd = xVal.doubleValue()
    double result = coefficients[degree]
    for (int i = degree - 1; i >= 0; i--) {
      result = result * xd + coefficients[i]
    }
    return BigDecimal.valueOf(result)
  }

  BigDecimal predict(Number xVal, int numberOfDecimals) {
    return predict(xVal).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  List<BigDecimal> predict(List<Number> xVals) {
    List<BigDecimal> predictions = []
    xVals.each { predictions << predict(it) }
    return predictions
  }

  List<BigDecimal> predict(List<Number> xVals, int numberOfDecimals) {
    List<BigDecimal> predictions = []
    xVals.each { predictions << predict(it, numberOfDecimals) }
    return predictions
  }

  /**
   * Get coefficient at given power of x.
   * getCoefficient(0) returns constant term
   * getCoefficient(1) returns linear coefficient
   * getCoefficient(2) returns quadratic coefficient, etc.
   */
  BigDecimal getCoefficient(int power) {
    if (power < 0 || power > degree) {
      throw new IllegalArgumentException("Power must be between 0 and $degree")
    }
    return BigDecimal.valueOf(coefficients[power])
  }

  BigDecimal getRsquared() {
    return r2
  }

  BigDecimal getRsquared(int numberOfDecimals) {
    return getRsquared().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  int getDegree() {
    return degree
  }

  @Override
  String toString() {
    StringBuilder sb = new StringBuilder("Y = ")
    for (int i = 0; i <= degree; i++) {
      BigDecimal coef = getCoefficient(i).setScale(3, RoundingMode.HALF_EVEN)
      if (i == 0) {
        sb.append(coef)
      } else {
        sb.append(coef >= 0 ? " + " : " - ")
        sb.append(coef.abs())
        if (i == 1) {
          sb.append("X")
        } else {
          sb.append("X^$i")
        }
      }
    }
    return sb.toString()
  }

  String summary() {
    StringBuilder sb = new StringBuilder()
    sb.append("Polynomial Regression (degree $degree)\n")
    sb.append("Equation: ${toString()}\n")
    sb.append("\nCoefficients:\n")
    for (int i = 0; i <= degree; i++) {
      String term = i == 0 ? "(Intercept)" : (i == 1 ? x : "${x}^$i")
      sb.append("  $term: ${getCoefficient(i).setScale(6, RoundingMode.HALF_EVEN)}\n")
    }
    sb.append("\nR-squared: ${getRsquared(4)}\n")
    return sb.toString()
  }
}
