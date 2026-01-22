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
 * Logistic Regression is a statistical model for binary classification that predicts the probability
 * of an outcome belonging to one of two classes (0 or 1) using the logistic (sigmoid) function.
 * Unlike linear regression, which predicts continuous values, logistic regression models probabilities
 * and produces S-shaped decision boundaries.
 *
 * <p><b>What is Logistic Regression?</b></p>
 * Logistic regression models the relationship between a predictor variable (X) and a binary response
 * variable (Y) using the logistic function. The model outputs probabilities P(Y=1|X) that are constrained
 * between 0 and 1, making it ideal for classification problems. The coefficients are estimated using
 * maximum likelihood estimation (MLE), which finds the parameters that maximize the likelihood of
 * observing the training data.
 *
 * <p><b>When to use Logistic Regression:</b></p>
 * <ul>
 *   <li>When the outcome variable is binary (two classes: 0/1, yes/no, pass/fail)</li>
 *   <li>When you need probability estimates in addition to class predictions</li>
 *   <li>When the relationship between log-odds and predictor is approximately linear</li>
 *   <li>For medical diagnosis (disease present/absent based on test values)</li>
 *   <li>For credit scoring (default/no-default based on credit score)</li>
 *   <li>For marketing (customer conversion based on engagement metrics)</li>
 *   <li>When interpretability is important (coefficients have clear probabilistic meaning)</li>
 *   <li>As a baseline model for binary classification tasks</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Outputs well-calibrated probability estimates (not just class predictions)</li>
 *   <li>Coefficients are directly interpretable in terms of log-odds and odds ratios</li>
 *   <li>Computationally efficient for moderate-sized datasets</li>
 *   <li>Works well when the relationship between X and log-odds is linear</li>
 *   <li>Less prone to overfitting than more complex models (with few predictors)</li>
 *   <li>Naturally handles binary outcomes without requiring thresholding</li>
 *   <li>Well-established statistical theory and inference methods</li>
 *   <li>Can be extended to multiple predictors (multivariate logistic regression)</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Assumes linear relationship between predictor and log-odds (may not fit non-linear patterns)</li>
 *   <li>Single predictor limitation (this implementation) - real problems often need multiple features</li>
 *   <li>Sensitive to outliers in the predictor variable</li>
 *   <li>Can suffer from complete or quasi-complete separation (perfect or near-perfect classification)</li>
 *   <li>Requires sufficient data in both classes (imbalanced data can be problematic)</li>
 *   <li>MLE optimization may fail to converge with problematic data</li>
 *   <li>Cannot extrapolate well beyond the range of training data</li>
 *   <li>Limited to binary classification (this implementation)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Predict customer conversion based on engagement score
 * def engagementScore = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
 * def converted = [0, 0, 0, 0, 1, 1, 1, 1]  // Binary outcome
 * def lr = new LogisticRegression(engagementScore, converted)
 *
 * // View the model equation
 * println lr  // P(Y=1) = 1/(1 + exp(-(β0 + β1*X)))
 *
 * // Predict probability of conversion for score=4.5
 * def prob = lr.predictProbability(4.5)
 * println "Probability of conversion: ${prob.setScale(3, RoundingMode.HALF_EVEN)}"  // ≈ 0.5
 *
 * // Predict class (0 or 1) with default threshold of 0.5
 * def classification = lr.predict(4.5)
 * println "Predicted class: ${classification}"  // 0 or 1
 *
 * // Use custom classification threshold
 * lr.setThreshold(0.7)  // Require 70% probability to classify as 1
 * def strictClass = lr.predict(4.5)
 *
 * // Get model coefficients
 * println "Intercept (β0): ${lr.getIntercept(3)}"
 * println "Slope (β1): ${lr.getSlope(3)}"
 *
 * // Predict for multiple values
 * def scores = [3.0, 5.0, 7.0]
 * def probabilities = lr.predictProbability(scores)
 * def classes = lr.predict(scores)
 *
 * // Use with Matrix
 * def data = Matrix.builder()
 *   .data(score: [1, 2, 3, 4, 5, 6, 7, 8], outcome: [0, 0, 0, 0, 1, 1, 1, 1])
 *   .build()
 * def lr2 = new LogisticRegression(data, 'score', 'outcome')
 * </pre>
 *
 * <p><b>Mathematical formulation:</b></p>
 * The logistic regression model is defined as:
 * <pre>
 * P(Y=1|X) = 1 / (1 + exp(-(β0 + β1*X)))
 * P(Y=0|X) = 1 - P(Y=1|X)
 *
 * Log-odds (logit): log(P(Y=1|X) / P(Y=0|X)) = β0 + β1*X
 * Odds ratio: OR = exp(β1)
 * </pre>
 * where:
 * <ul>
 *   <li>β0 is the intercept (log-odds when X=0)</li>
 *   <li>β1 is the slope coefficient (change in log-odds per unit increase in X)</li>
 *   <li>exp(β1) is the odds ratio - how much the odds of Y=1 change per unit increase in X</li>
 *   <li>The sigmoid function σ(z) = 1/(1+exp(-z)) maps linear combinations to probabilities [0,1]</li>
 * </ul>
 *
 * <p>The coefficients are estimated by maximizing the log-likelihood function:</p>
 * <pre>
 * LL(β0, β1) = Σ[yi * log(pi) + (1-yi) * log(1-pi)]
 * </pre>
 * where pi = P(Y=1|Xi) is the predicted probability for observation i. This implementation uses
 * the Nelder-Mead simplex optimization algorithm to find the coefficients that maximize LL.
 *
 * <p><b>Interpreting coefficients:</b></p>
 * <ul>
 *   <li><b>β1 > 0</b>: As X increases, probability of Y=1 increases</li>
 *   <li><b>β1 < 0</b>: As X increases, probability of Y=1 decreases</li>
 *   <li><b>β1 = 0</b>: X has no effect on Y (independence)</li>
 *   <li><b>exp(β1)</b>: Multiplicative change in odds of Y=1 per unit increase in X</li>
 *   <li><b>β0</b>: Log-odds of Y=1 when X=0 (may not be meaningful if X=0 is outside data range)</li>
 * </ul>
 *
 * <p><b>Classification threshold:</b></p>
 * The default threshold is 0.5 (classify as 1 if P(Y=1|X) ≥ 0.5). This can be adjusted using
 * setThreshold() to:
 * <ul>
 *   <li>Reduce false positives: increase threshold (e.g., 0.7) - more conservative</li>
 *   <li>Reduce false negatives: decrease threshold (e.g., 0.3) - more aggressive</li>
 *   <li>Balance precision and recall based on business requirements</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Hosmer, D. W., Lemeshow, S., & Sturdivant, R. X. (2013). "Applied Logistic Regression" (3rd ed.). Wiley.</li>
 *   <li>James, G., Witten, D., Hastie, T., & Tibshirani, R. (2013). "An Introduction to Statistical Learning". Springer, Chapter 4.</li>
 *   <li>Agresti, A. (2018). "An Introduction to Categorical Data Analysis" (3rd ed.). Wiley.</li>
 *   <li>Cox, D. R. (1958). "The Regression Analysis of Binary Sequences". Journal of the Royal Statistical Society, Series B, 20(2), 215-242.</li>
 * </ul>
 *
 * <p><b>Note:</b> This implementation supports single-predictor (univariate) logistic regression only.
 * For multivariate logistic regression with multiple predictors, consider using dedicated statistical
 * or machine learning libraries. The dependent variable must be binary (coded as 0 or 1).</p>
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
