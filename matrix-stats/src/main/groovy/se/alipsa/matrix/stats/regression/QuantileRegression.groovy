package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic
import org.apache.commons.math3.optim.MaxIter
import org.apache.commons.math3.optim.PointValuePair
import org.apache.commons.math3.optim.linear.*
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode

/**
 * Quantile Regression implements linear regression that estimates conditional quantiles
 * rather than the conditional mean (as in OLS regression).
 *
 * For a given quantile tau (denoted mathematically as τ), the regression line minimizes
 * the asymmetric loss function:
 * ρ_τ(u) = u * (τ - I(u < 0))
 * where I(u < 0) is an indicator function that is 1 when u < 0, and 0 otherwise.
 *
 * This implementation solves the linear programming formulation of quantile regression
 * using a simplex-based method (via Apache Commons Math's SimplexSolver).
 *
 * Example:
 * <pre>
 * // Median regression (tau = 0.5)
 * def x = [1, 2, 3, 4, 5]
 * def y = [2.1, 3.9, 6.2, 7.8, 10.1]
 * def qr = new QuantileRegression(x, y, 0.5)
 * println qr  // Y = 2.02X + 0.04 (τ=0.5)
 *
 * // Predict at x=3
 * def yPred = qr.predict(3)  // ≈ 6.1
 * </pre>
 */
@CompileStatic
class QuantileRegression {

  /** The slope coefficient */
  BigDecimal slope

  /** The intercept coefficient */
  BigDecimal intercept

  /** The quantile (tau) - must be in (0, 1) */
  BigDecimal tau

  /** Name of the independent variable (for display purposes) */
  String x = 'X'

  /** Name of the dependent variable (for display purposes) */
  String y = 'Y'

  /**
   * Construct quantile regression from a Matrix
   *
   * @param table The data matrix
   * @param x Column name for independent variable
   * @param y Column name for dependent variable
   * @param tau The quantile to estimate (must be in (0, 1))
   */
  QuantileRegression(Matrix table, String x, String y, Number tau) {
    this(table[x] as List<? extends Number>, table[y] as List<? extends Number>, tau)
    this.x = x
    this.y = y
  }

  /**
   * Construct quantile regression from lists of x and y values
   *
   * @param xValues Independent variable values
   * @param yValues Dependent variable values
   * @param tau The quantile to estimate (must be in (0, 1))
   */
  QuantileRegression(List<? extends Number> xValues, List<? extends Number> yValues, Number tau) {
    // Validation
    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException(
        "Must have equal number of X and Y data points for quantile regression. " +
        "Got ${xValues.size()} X values and ${yValues.size()} Y values."
      )
    }

    if (xValues.size() < 2) {
      throw new IllegalArgumentException(
        "Need at least 2 data points for quantile regression. Got ${xValues.size()}."
      )
    }

    BigDecimal tauBD = tau as BigDecimal
    if (tauBD <= 0 || tauBD >= 1) {
      throw new IllegalArgumentException(
        "Quantile (tau) must be in the open interval (0, 1). Got tau=${tau}."
      )
    }

    this.tau = tauBD

    // Solve quantile regression using linear programming
    solveQuantileRegression(xValues, yValues, tauBD)
  }

  /**
   * Solve the quantile regression problem using the Barrodale-Roberts simplex method.
   *
   * The quantile regression problem is formulated as a linear program:
   *
   * minimize: τ * Σ(u+) + (1-τ) * Σ(u-)
   * subject to: y = β0 + β1*x + u+ - u-
   *             u+, u- >= 0
   *
   * where u+ are positive residuals and u- are negative residuals (in absolute value).
   *
   * We reformulate this as:
   * minimize: c^T * [β0, β1, u1+, u1-, u2+, u2-, ..., un+, un-]
   * subject to: A * variables = y
   *             variables >= 0 (handled by bounds)
   */
  private void solveQuantileRegression(List<? extends Number> xValues, List<? extends Number> yValues, BigDecimal tauValue) {
    int n = xValues.size()
    double tauDouble = tauValue as double

    // Convert to double arrays for Apache Commons Math
    double[] x = xValues.collect { it as double } as double[]
    double[] y = yValues.collect { it as double } as double[]

    // Apache Commons Math SimplexSolver requires all variables to be >= 0,
    // but the intercept and slope should be allowed to take negative values.
    // To handle this, we reformulate: let intercept = β0+ - β0- and slope = β1+ - β1-,
    // and keep residuals as non-negative variables u+ and u- for each observation.
    // Variables: [β0+, β0-, β1+, β1-, u1+, u1-, u2+, u2-, ..., un+, un-]
    // Total variables: 4 + 2*n
    int numVarsReformulated = 4 + 2 * n
    double[] objectiveReformulated = new double[numVarsReformulated]
    objectiveReformulated[0] = 0.0  // β0+
    objectiveReformulated[1] = 0.0  // β0-
    objectiveReformulated[2] = 0.0  // β1+
    objectiveReformulated[3] = 0.0  // β1-
    for (int i = 0; i < n; i++) {
      objectiveReformulated[4 + 2*i] = tauDouble           // u+
      objectiveReformulated[4 + 2*i + 1] = 1.0 - tauDouble  // u-
    }

    List<LinearConstraint> constraintsReformulated = []
    for (int i = 0; i < n; i++) {
      double[] coeffs = new double[numVarsReformulated]
      coeffs[0] = 1.0       // β0+
      coeffs[1] = -1.0      // -β0-
      coeffs[2] = x[i]      // β1+ * x[i]
      coeffs[3] = -x[i]     // -β1- * x[i]
      coeffs[4 + 2*i] = 1.0       // u+[i]
      coeffs[4 + 2*i + 1] = -1.0  // -u-[i]

      constraintsReformulated << new LinearConstraint(coeffs, Relationship.EQ, y[i])
    }

    LinearObjectiveFunction objFuncReformulated = new LinearObjectiveFunction(objectiveReformulated, 0.0)

    try {
      SimplexSolver solver = new SimplexSolver()
      PointValuePair solution = solver.optimize(
        new MaxIter(10000),
        objFuncReformulated,
        new LinearConstraintSet(constraintsReformulated),
        GoalType.MINIMIZE,
        new NonNegativeConstraint(true)
      )

      double[] point = solution.getPoint()

      // Extract coefficients: intercept = β0+ - β0-, slope = β1+ - β1-
      BigDecimal interceptValue = (point[0] - point[1]) as BigDecimal
      BigDecimal slopeValue = (point[2] - point[3]) as BigDecimal

      this.intercept = interceptValue
      this.slope = slopeValue

    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to solve quantile regression for tau=${tau}. " +
        "This may be due to numerical instability or degenerate data. " +
        "Error: ${e.message}", e
      )
    }
  }

  /**
   * Predict the response variable for a given value of the independent variable
   *
   * @param x Value of the independent variable
   * @return Predicted value of the dependent variable
   */
  BigDecimal predict(Number x) {
    return slope * x + intercept
  }

  /**
   * Predict the response variable for a given value, rounded to specified decimals
   *
   * @param x Value of the independent variable
   * @param numberOfDecimals Number of decimal places to round to
   * @return Predicted value rounded to specified decimals
   */
  BigDecimal predict(Number x, int numberOfDecimals) {
    return predict(x).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Predict response values for a list of independent variable values
   *
   * @param xValues List of independent variable values
   * @return List of predicted values
   */
  List<BigDecimal> predict(List<Number> xValues) {
    return xValues.collect { predict(it) }
  }

  /**
   * Predict response values for a list of independent variable values, rounded
   *
   * @param xValues List of independent variable values
   * @param numberOfDecimals Number of decimal places to round to
   * @return List of predicted values rounded to specified decimals
   */
  List<BigDecimal> predict(List<Number> xValues, int numberOfDecimals) {
    return xValues.collect { predict(it, numberOfDecimals) }
  }

  /**
   * Get the slope coefficient
   *
   * @return The slope
   */
  BigDecimal getSlope() {
    return slope
  }

  /**
   * Get the slope coefficient rounded to specified decimals
   *
   * @param numberOfDecimals Number of decimal places (-1 for no rounding)
   * @return The slope rounded to specified decimals
   */
  BigDecimal getSlope(int numberOfDecimals) {
    if (numberOfDecimals < 0) {
      return slope
    }
    return slope.setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Get the intercept coefficient
   *
   * @return The intercept
   */
  BigDecimal getIntercept() {
    return intercept
  }

  /**
   * Get the intercept coefficient rounded to specified decimals
   *
   * @param numberOfDecimals Number of decimal places (-1 for no rounding)
   * @return The intercept rounded to specified decimals
   */
  BigDecimal getIntercept(int numberOfDecimals) {
    if (numberOfDecimals < 0) {
      return intercept
    }
    return intercept.setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * String representation of the regression equation
   *
   * @return String in format "Y = {slope}X + {intercept} (τ={tau})"
   */
  @Override
  String toString() {
    String sign = intercept >= 0 ? '+' : '-'
    return "${y} = ${getSlope(2)}${x} ${sign} ${getIntercept(2).abs()} (τ=${tau.setScale(2, RoundingMode.HALF_EVEN)})"
  }
}
