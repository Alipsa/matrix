package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic
import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.optim.InitialGuess
import org.apache.commons.math3.optim.MaxEval
import org.apache.commons.math3.optim.PointValuePair
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode

/**
 * Logistic Regression implements binary classification using the logistic (sigmoid) function.
 *
 * The logistic function is defined as:
 * P(Y=1|X) = 1 / (1 + exp(-(β0 + β1*X)))
 *
 * where β0 is the intercept and β1 is the slope coefficient.
 *
 * This implementation uses maximum likelihood estimation (MLE) via the Nelder-Mead
 * simplex optimization algorithm to find the optimal coefficients.
 *
 * The dependent variable should be binary (0 or 1).
 *
 * Example:
 * <pre>
 * // Classify based on a single predictor
 * def x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
 * def y = [0, 0, 0, 1, 1, 1]  // Binary outcomes
 * def lr = new LogisticRegression(x, y)
 * println lr  // Y = 1/(1 + exp(-(β0 + β1*X)))
 *
 * // Predict probability at x=3.5
 * def prob = lr.predictProbability(3.5)  // ≈ 0.5
 *
 * // Predict class (0 or 1) at x=3.5
 * def classification = lr.predict(3.5)  // 0 or 1
 * </pre>
 */
@CompileStatic
class LogisticRegression {

  /** The slope coefficient (β1) */
  BigDecimal slope

  /** The intercept coefficient (β0) */
  BigDecimal intercept

  /** Name of the independent variable (for display purposes) */
  String x = 'X'

  /** Name of the dependent variable (for display purposes) */
  String y = 'Y'

  /** Classification threshold (default: 0.5) */
  BigDecimal threshold = 0.5

  /**
   * Construct logistic regression from a Matrix
   *
   * @param table The data matrix
   * @param x Column name for independent variable
   * @param y Column name for dependent variable (must be binary: 0 or 1)
   */
  LogisticRegression(Matrix table, String x, String y) {
    this(table[x] as List<? extends Number>, table[y] as List<? extends Number>)
    this.x = x
    this.y = y
  }

  /**
   * Construct logistic regression from lists of x and y values
   *
   * @param xValues Independent variable values
   * @param yValues Dependent variable values (must be binary: 0 or 1)
   */
  LogisticRegression(List<? extends Number> xValues, List<? extends Number> yValues) {
    // Validation
    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException(
        "Must have equal number of X and Y data points for logistic regression. " +
        "Got ${xValues.size()} X values and ${yValues.size()} Y values."
      )
    }

    if (xValues.size() < 3) {
      throw new IllegalArgumentException(
        "Need at least 3 data points for logistic regression. Got ${xValues.size()}."
      )
    }

    // Validate that y values are binary (0 or 1)
    for (Number yVal : yValues) {
      double yDouble = yVal.doubleValue()
      if (yDouble != 0.0 && yDouble != 1.0) {
        throw new IllegalArgumentException(
          "Dependent variable must be binary (0 or 1). Found value: ${yVal}"
        )
      }
    }

    // Fit the logistic regression model
    fitLogisticRegression(xValues, yValues)
  }

  /**
   * Fit the logistic regression model using maximum likelihood estimation.
   *
   * The log-likelihood function for logistic regression is:
   * LL(β0, β1) = Σ[yi * log(pi) + (1-yi) * log(1-pi)]
   *
   * where pi = 1 / (1 + exp(-(β0 + β1*xi)))
   *
   * We maximize this by minimizing the negative log-likelihood.
   */
  private void fitLogisticRegression(List<? extends Number> xValues, List<? extends Number> yValues) {
    int n = xValues.size()

    // Convert to double arrays
    double[] x = xValues.collect { it.doubleValue() } as double[]
    double[] y = yValues.collect { it.doubleValue() } as double[]

    // Define the negative log-likelihood function to minimize
    MultivariateFunction negativeLogLikelihood = new MultivariateFunction() {
      @Override
      double value(double[] coefficients) {
        double beta0 = coefficients[0]
        double beta1 = coefficients[1]

        double logLikelihood = 0.0
        for (int i = 0; i < n; i++) {
          double z = beta0 + beta1 * x[i]
          double p = sigmoid(z)

          // Avoid log(0) by clamping p to [epsilon, 1-epsilon]
          double epsilon = 1e-15
          p = Math.max(epsilon, Math.min(1.0 - epsilon, p))

          logLikelihood += y[i] * Math.log(p) + (1.0 - y[i]) * Math.log(1.0 - p)
        }

        return -logLikelihood  // Return negative for minimization
      }
    }

    // Initial guess: start with coefficients [0, 0]
    double[] initialGuess = [0.0, 0.0] as double[]

    // Use Nelder-Mead simplex optimizer
    SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30)

    try {
      PointValuePair optimum = optimizer.optimize(
        new MaxEval(10000),
        new ObjectiveFunction(negativeLogLikelihood),
        GoalType.MINIMIZE,
        new InitialGuess(initialGuess),
        new NelderMeadSimplex(2)  // 2 parameters
      )

      double[] optimalCoefficients = optimum.getPoint()

      this.intercept = optimalCoefficients[0] as BigDecimal
      this.slope = optimalCoefficients[1] as BigDecimal

    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to fit logistic regression model. " +
        "This may be due to numerical instability or perfect separation in the data. " +
        "Error: ${e.message}", e
      )
    }
  }

  /**
   * Sigmoid (logistic) function: 1 / (1 + exp(-z))
   */
  private static double sigmoid(double z) {
    // Use numerically stable sigmoid
    if (z >= 0) {
      return 1.0 / (1.0 + Math.exp(-z))
    } else {
      double expZ = Math.exp(z)
      return expZ / (1.0 + expZ)
    }
  }

  /**
   * Predict the probability P(Y=1) for a given value of the independent variable
   *
   * @param x Value of the independent variable
   * @return Predicted probability (between 0 and 1)
   */
  BigDecimal predictProbability(Number x) {
    double z = (slope * x + intercept).doubleValue()
    return sigmoid(z) as BigDecimal
  }

  /**
   * Predict the probability P(Y=1), rounded to specified decimals
   *
   * @param x Value of the independent variable
   * @param numberOfDecimals Number of decimal places to round to
   * @return Predicted probability rounded to specified decimals
   */
  BigDecimal predictProbability(Number x, int numberOfDecimals) {
    return predictProbability(x).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Predict probabilities for a list of independent variable values
   *
   * @param xValues List of independent variable values
   * @return List of predicted probabilities
   */
  List<BigDecimal> predictProbability(List<Number> xValues) {
    return xValues.collect { predictProbability(it) }
  }

  /**
   * Predict probabilities for a list of independent variable values, rounded
   *
   * @param xValues List of independent variable values
   * @param numberOfDecimals Number of decimal places to round to
   * @return List of predicted probabilities rounded to specified decimals
   */
  List<BigDecimal> predictProbability(List<Number> xValues, int numberOfDecimals) {
    return xValues.collect { predictProbability(it, numberOfDecimals) }
  }

  /**
   * Predict the class (0 or 1) for a given value of the independent variable
   *
   * @param x Value of the independent variable
   * @return Predicted class: 0 if P(Y=1) < threshold, 1 otherwise
   */
  int predict(Number x) {
    return predictProbability(x) >= threshold ? 1 : 0
  }

  /**
   * Predict classes for a list of independent variable values
   *
   * @param xValues List of independent variable values
   * @return List of predicted classes (0 or 1)
   */
  List<Integer> predict(List<Number> xValues) {
    return xValues.collect { predict(it) }
  }

  /**
   * Set the classification threshold
   *
   * @param threshold Probability threshold for classification (default: 0.5)
   */
  void setThreshold(Number threshold) {
    BigDecimal t = threshold as BigDecimal
    if (t <= 0 || t >= 1) {
      throw new IllegalArgumentException(
        "Threshold must be in the open interval (0, 1). Got threshold=${threshold}."
      )
    }
    this.threshold = t
  }

  /**
   * Get the slope coefficient
   *
   * @return The slope (β1)
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
   * @return The intercept (β0)
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
   * Calculate the log-odds (logit) for a given x value
   *
   * @param x Value of the independent variable
   * @return Log-odds: β0 + β1*x
   */
  BigDecimal logOdds(Number x) {
    return slope * x + intercept
  }

  /**
   * String representation of the logistic regression model
   *
   * @return String showing the logistic function
   */
  @Override
  String toString() {
    String sign = intercept >= 0 ? '+' : '-'
    return "P(${y}=1) = 1/(1 + exp(-(${getIntercept(2)} ${sign} ${getSlope(2).abs()}*${x})))"
  }
}
