package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/**
 * Linear regression, also known as Simple Linear Regression is a regression algorithm that models the
 * relationship between a dependent variable and one independent variable.
 * A linear regression model shows a relationship that is linear i.e. a sloped straight line in a carthesian grid.
 *
 * In Linear Regression, the dependent variable must be a real or continuous value.
 * However, you can measure the independent variable on continuous or categorical values.
 *
 * Simple Linear Regression can be expressed using the following formula:
 * Y = a + bX
 * Where:
 * <ul>
 *   <li>Y is the dependent variable.</li>
 *   <li>a is the intercept of the Regression line.</li>
 *   <li>b is the slope of the line.</li>
 *   <li>X is the independent variable.</li>
 * </ul>
 */
class LinearRegression {

  BigDecimal slope
  BigDecimal intercept
  BigDecimal r2
  BigDecimal interceptStdErr
  BigDecimal slopeStdErr
  String x = 'X'
  String y = 'Y'

  LinearRegression(Matrix table, String x, String y) {
    this(table[x] as List<? extends Number>, table[y] as List<? extends Number>)
    this.x = x
    this.y = y
  }

  LinearRegression(List<? extends Number> x, List<? extends Number> y) {
    if (x.size() != y.size()) {
      throw new IllegalArgumentException("Must have equal number of X and Y data points for a linear regression")
    }

    Integer numberOfDataValues = x.size()

    List<? extends Number> xSquared = x.collect {it * it }

    List<BigDecimal> xMultipliedByY = (0 ..< numberOfDataValues).collect {
      i -> x.get(i) * y.get(i)
    }

    BigDecimal xSummed = Stat.sum(x)
    BigDecimal ySummed = Stat.sum(y)
    BigDecimal sumOfXSquared = Stat.sum(xSquared)
    BigDecimal sumOfXMultipliedByY = Stat.sum(xMultipliedByY)

    BigDecimal slopeNominator = numberOfDataValues * sumOfXMultipliedByY - ySummed * xSummed
    BigDecimal slopeDenominator = numberOfDataValues * sumOfXSquared - xSummed ** 2
    slope = slopeNominator / slopeDenominator

    BigDecimal interceptNominator = ySummed - slope * xSummed
    BigDecimal interceptDenominator = numberOfDataValues
    intercept = interceptNominator / interceptDenominator

    BigDecimal xBar = xSummed / numberOfDataValues
    BigDecimal yBar = ySummed / numberOfDataValues

    BigDecimal xxbar = 0.0, yybar = 0.0, xybar = 0.0
    BigDecimal rss = 0.0     // residual sum of squares
    BigDecimal ssr = 0.0     // regression sum of squares
    for (int i = 0; i < numberOfDataValues; i++) {
      xxbar += (x[i] - xBar) ** 2
      yybar += (y[i] - yBar) ** 2
      xybar += (x[i] - xBar) * (y[i] - yBar)
      BigDecimal fit = predict(x[i])
      rss += (fit - y[i]) ** 2
      ssr += (fit - yBar) ** 2
    }
    int degreesOfFreedom = numberOfDataValues - 2
    r2 = ssr / yybar
    BigDecimal svar  = rss / degreesOfFreedom
    def slopeVar = svar / xxbar
    slopeStdErr = Math.sqrt(slopeVar)
    interceptStdErr = Math.sqrt(svar/numberOfDataValues + xBar * xBar * slopeVar)
  }

  BigDecimal predict(Number dependentVariable) {
    return (slope * dependentVariable) + intercept
  }

  BigDecimal predict(Number dependentVariable, int numberOfDecimals) {
    return predict(dependentVariable).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  List<BigDecimal> predict(List<Number> dependentVariables) {
    List<BigDecimal> predictions = []
    dependentVariables.each {
      predictions << predict(it)
    }
    return predictions
  }

  List<BigDecimal> predict(List<Number> dependentVariables, int numberOfDecimals) {
    List<BigDecimal> predictions = []
    dependentVariables.each {
      predictions << predict(it, numberOfDecimals)
    }
    return predictions
  }

  BigDecimal getSlope() {
    return slope
  }

  BigDecimal getSlope(int numberOfDecimals) {
    return slope.setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getIntercept() {
    return intercept
  }

  BigDecimal getIntercept(int numberOfDecimals) {
    return intercept.setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * R squared (R2) is a regression error metric that justifies the performance of the model.
   * It represents the value of how much the independent variables are able to describe the value
   * for the response/target variable.
   * Thus, an R-squared model describes how well the target variable is explained by the combination
   * of the independent variables as a single unit. The R squared value ranges between 0 to 1
   *
   * This is equivalent to the following in R:
   * <code><pre>
   * x <- c(2, 3, 5, 7, 9, 11, 14)
   * y <- c(4.02, 5.44, 7.12, 10.88, 15.10, 20.91, 26.02)
   * model <- lm(y ~ x)
   * r2 <- summary(model)$r.squared
  */
  BigDecimal getRsquared() {
    return r2
  }

  BigDecimal getRsquared(int numberOfDecimals) {
    return getRsquared().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Returns the standard error of the estimate for the intercept.
   * This is equivalent to the following in R:
   * <code><pre>
   * x <- c(2, 3, 5, 7, 9, 11, 14)
   * y <- c(4.02, 5.44, 7.12, 10.88, 15.10, 20.91, 26.02)
   * model <- lm(y ~ x)
   * interceptStdErr <- sqrt(diag(vcov(model)))[1])
   * </pre></code>
   * @return the standard error of the estimate for the intercept
   */
  BigDecimal getInterceptStdErr() {
    return interceptStdErr
  }

  BigDecimal getInterceptStdErr(int numberOfDecimals) {
    return getInterceptStdErr().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Returns the standard error of the estimate for the slope.
   * This is equivalent to the following in R:
   * <code><pre>
   * x <- c(2, 3, 5, 7, 9, 11, 14)
   * y <- c(4.02, 5.44, 7.12, 10.88, 15.10, 20.91, 26.02)
   * model <- lm(y ~ x)
   * slopeStdErr <- sqrt(diag(vcov(model)))[2])
   * </pre></code>
   * @return the standard error of the estimate for the slope
   */
  BigDecimal getSlopeStdErr() {
    return slopeStdErr
  }

  BigDecimal getSlopeStdErr(int numberOfDecimals) {
    return getSlopeStdErr().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  @Override
  String toString() {
    return "Y = ${getSlope(2)}X ${intercept > 0 ? '+' : '-'} ${getIntercept(2).abs()}"
  }

  String summary() {
    String interceptLab = '(Intercept)'
    String xLab = x
    if (interceptLab.length() > xLab.length()) {
      xLab = xLab.padRight(interceptLab.length())
    }
    Matrix coefficients = Matrix.builder()
    .matrixName('Coefficients')
    .columnNames(['           ','Estimate','Std. Error' /*, 't value', 'Pr(>|t|)'*/])
    .rows([
            [interceptLab, xLab],
            [getIntercept(3), getSlope(3)],
            [getInterceptStdErr(3), getSlopeStdErr(3)]
        ])
    .build()
    """
Equation: ${toString()}
Residuals: (not yet implemented)   

Coefficients
${coefficients.content()}

Multiple R-squared: ${getRsquared(3)}
""".stripIndent()
  }
}
