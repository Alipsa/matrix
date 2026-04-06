package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.linear.MatrixAlgebra
import se.alipsa.matrix.stats.linear.SingularMatrixException

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

  private static final double RANK_THRESHOLD = 1e-12d

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
    double[][] design = new double[n][degree + 1]
    double[] response = new double[n]
    for (int i = 0; i < n; i++) {
      double xValue = x[i].doubleValue()
      double term = 1.0d
      for (int j = 0; j <= degree; j++) {
        design[i][j] = term
        term *= xValue
      }
      response[i] = y[i].doubleValue()
    }

    coefficients = fitCoefficients(design, response)

    // Compute R-squared
    computeRSquared(x, y)
  }

  private static double[] fitCoefficients(double[][] design, double[] response) {
    try {
      return solveLeastSquares(design, response)
    } catch (SingularMatrixException ignored) {
      int[] selectedColumns = selectIndependentColumns(design)
      if (selectedColumns.length == 0) {
        throw new IllegalStateException("Polynomial design matrix has no independent columns")
      }
      double[][] reducedDesign = new double[design.length][selectedColumns.length]
      for (int row = 0; row < design.length; row++) {
        for (int column = 0; column < selectedColumns.length; column++) {
          reducedDesign[row][column] = design[row][selectedColumns[column]]
        }
      }

      double[] reducedCoefficients = solveLeastSquares(reducedDesign, response)
      double[] fullCoefficients = new double[design[0].length]
      for (int i = 0; i < selectedColumns.length; i++) {
        fullCoefficients[selectedColumns[i]] = reducedCoefficients[i]
      }
      return fullCoefficients
    }
  }

  private static double[] solveLeastSquares(double[][] design, double[] response) {
    double[][] xt = MatrixAlgebra.transpose(design)
    double[][] xtx = MatrixAlgebra.multiply(xt, design)
    double[][] xtxInv = MatrixAlgebra.inverse(xtx)
    double[] xty = new double[xt.length]
    for (int i = 0; i < xt.length; i++) {
      double sum = 0.0d
      for (int j = 0; j < response.length; j++) {
        sum += xt[i][j] * response[j]
      }
      xty[i] = sum
    }

    double[] fitted = new double[xt.length]
    for (int i = 0; i < xt.length; i++) {
      double sum = 0.0d
      for (int j = 0; j < xt.length; j++) {
        sum += xtxInv[i][j] * xty[j]
      }
      fitted[i] = sum
    }
    fitted
  }

  private static int[] selectIndependentColumns(double[][] design) {
    int rowCount = design.length
    int columnCount = design[0].length
    boolean[] used = new boolean[columnCount]
    List<double[]> orthonormalBasis = []
    List<Integer> selectedColumns = []

    for (int selection = 0; selection < columnCount; selection++) {
      int bestColumn = -1
      double bestNorm = 0.0d
      double[] bestResidual = null

      for (int column = 0; column < columnCount; column++) {
        if (used[column]) {
          continue
        }
        double[] residual = new double[rowCount]
        for (int row = 0; row < rowCount; row++) {
          residual[row] = design[row][column]
        }
        orthonormalBasis.each { double[] basisVector ->
          double projection = 0.0d
          for (int row = 0; row < rowCount; row++) {
            projection += residual[row] * basisVector[row]
          }
          for (int row = 0; row < rowCount; row++) {
            residual[row] -= projection * basisVector[row]
          }
        }

        double norm = 0.0d
        for (int row = 0; row < rowCount; row++) {
          norm += residual[row] * residual[row]
        }
        if (norm > bestNorm + RANK_THRESHOLD ||
            (Math.abs(norm - bestNorm) <= RANK_THRESHOLD && (bestColumn < 0 || column > bestColumn))) {
          bestColumn = column
          bestNorm = norm
          bestResidual = residual
        }
      }

      if (bestColumn < 0 || bestNorm <= RANK_THRESHOLD) {
        break
      }

      double scale = Math.sqrt(bestNorm)
      for (int row = 0; row < rowCount; row++) {
        bestResidual[row] /= scale
      }
      orthonormalBasis << bestResidual
      selectedColumns << bestColumn
      used[bestColumn] = true
    }

    selectedColumns as int[]
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
    return xVals.collect { predict(it) }
  }

  List<BigDecimal> predict(List<Number> xVals, int numberOfDecimals) {
    return xVals.collect { predict(it, numberOfDecimals) }
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
    sb.append("Equation: ${this}\n")
    sb.append("\nCoefficients:\n")
    for (int i = 0; i <= degree; i++) {
      String term = i == 0 ? "(Intercept)" : (i == 1 ? x : "${x}^$i")
      sb.append("  $term: ${getCoefficient(i).setScale(6, RoundingMode.HALF_EVEN)}\n")
    }
    sb.append("\nR-squared: ${getRsquared(4)}\n")
    return sb.toString()
  }
}
