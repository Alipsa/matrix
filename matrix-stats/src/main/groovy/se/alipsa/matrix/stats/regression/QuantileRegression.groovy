package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic
import org.apache.commons.math3.optim.MaxIter
import org.apache.commons.math3.optim.PointValuePair
import org.apache.commons.math3.optim.linear.*
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode

/**
 * Quantile Regression extends linear regression by modeling conditional quantiles rather than
 * the conditional mean. While ordinary least squares (OLS) regression finds the line through
 * the average of Y given X, quantile regression finds the line through any specified quantile
 * (e.g., median, 25th percentile, 90th percentile), providing a more complete picture of the
 * relationship between variables.
 *
 * <p><b>What is Quantile Regression?</b></p>
 * Quantile regression estimates the relationship between a predictor and a specific quantile of
 * the response distribution. For example, median regression (τ=0.5) finds the line that splits
 * the data so that 50% of observations fall below and 50% above. Unlike OLS which minimizes
 * squared errors symmetrically, quantile regression minimizes an asymmetric loss function that
 * gives different weights to positive and negative residuals based on the target quantile.
 *
 * <p><b>When to use Quantile Regression:</b></p>
 * <ul>
 *   <li>When the response distribution is skewed or has heavy tails (non-normal errors)</li>
 *   <li>When you want robust regression that is less sensitive to outliers (use median regression τ=0.5)</li>
 *   <li>When the relationship varies across the distribution (e.g., effect differs for low vs. high values)</li>
 *   <li>For modeling extreme values (use τ=0.10 or τ=0.90 for tails)</li>
 *   <li>When heteroscedasticity is present (variance changes with X)</li>
 *   <li>For growth charts and reference curves (pediatric height/weight percentiles)</li>
 *   <li>In economics to study income inequality across distribution quantiles</li>
 *   <li>In environmental science to model extreme events (floods, temperatures)</li>
 *   <li>When conditional mean is not the quantity of interest</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Robust to outliers in Y (especially median regression τ=0.5)</li>
 *   <li>No assumptions about error distribution (non-parametric regarding errors)</li>
 *   <li>Provides complete picture of relationship across entire distribution</li>
 *   <li>Naturally handles heteroscedasticity without transformations</li>
 *   <li>Can model asymmetric relationships (upper vs. lower tail behavior)</li>
 *   <li>Coefficients have direct interpretation at specific quantiles</li>
 *   <li>More informative than mean regression when distribution is skewed</li>
 *   <li>Enables modeling of conditional variability and dispersion</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Computationally more intensive than OLS (requires linear programming solver)</li>
 *   <li>Less efficient than OLS when errors are actually normal</li>
 *   <li>Standard errors and confidence intervals require special methods (bootstrap recommended)</li>
 *   <li>Not unique solution when data points lie exactly on quantile line</li>
 *   <li>May produce crossing quantile lines when multiple quantiles are fitted separately</li>
 *   <li>Interpretation is more nuanced than mean regression</li>
 *   <li>Single predictor limitation (this implementation)</li>
 *   <li>Requires more data to estimate extreme quantiles reliably</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Median regression (robust to outliers)
 * def x = [1, 2, 3, 4, 5, 6, 7, 8]
 * def y = [2.1, 3.9, 6.2, 7.8, 10.1, 11.9, 14.2, 50.0]  // Last value is outlier
 * def medianReg = new QuantileRegression(x, y, 0.5)  // τ=0.5 for median
 * println medianReg  // Y = 2.0X + 0.1 (τ=0.5)
 *
 * // Compare with different quantiles
 * def q10 = new QuantileRegression(x, y, 0.10)  // 10th percentile (lower tail)
 * def q90 = new QuantileRegression(x, y, 0.90)  // 90th percentile (upper tail)
 *
 * // Predict median value at x=5
 * println medianReg.predict(5)  // Predicted median
 *
 * // Model growth chart percentiles
 * def ages = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
 * def heights = [75, 85, 95, 100, 108, 115, 120, 128, 132, 138]
 *
 * // Fit 5th, 50th, and 95th percentile curves
 * def p5 = new QuantileRegression(ages, heights, 0.05)
 * def p50 = new QuantileRegression(ages, heights, 0.50)
 * def p95 = new QuantileRegression(ages, heights, 0.95)
 *
 * println "At age 6:"
 * println "  5th percentile: ${p5.predict(6, 1)} cm"
 * println "  Median: ${p50.predict(6, 1)} cm"
 * println "  95th percentile: ${p95.predict(6, 1)} cm"
 *
 * // Use with Matrix
 * def data = Matrix.builder()
 *   .data(experience: [1, 2, 3, 4, 5], salary: [35, 42, 48, 55, 70])
 *   .build()
 * def qr = new QuantileRegression(data, 'experience', 'salary', 0.75)
 * println "75th percentile salary prediction: ${qr.predict(3)}"
 * </pre>
 *
 * <p><b>Mathematical formulation:</b></p>
 * Quantile regression finds coefficients β0 and β1 that minimize the asymmetric loss function:
 * <pre>
 * minimize: Σ ρτ(yi - β0 - β1*xi)
 *
 * where ρτ(u) = u * (τ - I(u < 0))
 *             = {  τ * u      if u ≥ 0  (positive residuals)
 *               { (τ-1) * u   if u < 0  (negative residuals)
 * </pre>
 * where:
 * <ul>
 *   <li>τ (tau) is the target quantile, τ ∈ (0, 1)</li>
 *   <li>I(u < 0) is an indicator function: 1 if u < 0, otherwise 0</li>
 *   <li>For median regression (τ=0.5), this reduces to minimizing absolute deviations (LAD)</li>
 *   <li>For τ > 0.5, over-predictions are penalized more (pushes line upward)</li>
 *   <li>For τ < 0.5, under-predictions are penalized more (pushes line downward)</li>
 * </ul>
 *
 * <p>The optimization problem is reformulated as a linear program and solved using the simplex method:</p>
 * <pre>
 * minimize: τ * Σ(ui+) + (1-τ) * Σ(ui-)
 * subject to: yi = β0 + β1*xi + ui+ - ui-
 *             ui+, ui- ≥ 0
 * </pre>
 * where ui+ and ui- represent positive and negative residuals respectively.
 *
 * <p><b>Interpreting coefficients:</b></p>
 * <ul>
 *   <li><b>β1</b>: Change in the τ-th quantile of Y per unit increase in X</li>
 *   <li><b>β0</b>: The τ-th quantile of Y when X=0 (if X=0 is in range)</li>
 *   <li>Example: If β1=2.0 at τ=0.75, then a 1-unit increase in X raises the 75th percentile of Y by 2.0</li>
 * </ul>
 *
 * <p><b>Common quantile values:</b></p>
 * <ul>
 *   <li><b>τ = 0.10, 0.25</b>: Lower tail, useful for modeling worst-case or low-end scenarios</li>
 *   <li><b>τ = 0.50</b>: Median regression, robust alternative to OLS mean regression</li>
 *   <li><b>τ = 0.75, 0.90</b>: Upper tail, useful for modeling best-case or high-end scenarios</li>
 *   <li><b>Multiple quantiles</b>: Fit several to understand how the relationship varies across distribution</li>
 * </ul>
 *
 * <p><b>Comparing OLS vs. Quantile Regression:</b></p>
 * <ul>
 *   <li><b>OLS</b>: E[Y|X] - estimates conditional mean, optimal when errors are normal</li>
 *   <li><b>Quantile (τ)</b>: Qτ[Y|X] - estimates conditional quantile, robust to outliers and skewness</li>
 *   <li><b>When equal</b>: If Y|X is symmetric and errors are normal, median regression ≈ OLS</li>
 *   <li><b>When different</b>: Skewed distributions show different slopes across quantiles</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Koenker, R., & Bassett Jr, G. (1978). "Regression Quantiles". Econometrica, 46(1), 33-50.</li>
 *   <li>Koenker, R. (2005). "Quantile Regression". Cambridge University Press.</li>
 *   <li>Hao, L., & Naiman, D. Q. (2007). "Quantile Regression". SAGE Publications.</li>
 *   <li>Cade, B. S., & Noon, B. R. (2003). "A gentle introduction to quantile regression for ecologists". Frontiers in Ecology and the Environment, 1(8), 412-420.</li>
 * </ul>
 *
 * <p><b>Note:</b> This implementation supports single-predictor (univariate) quantile regression only.
 * For multivariate quantile regression, consider using R's quantreg package or Python's statsmodels.
 * The quantile parameter τ must be in the open interval (0, 1).</p>
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
