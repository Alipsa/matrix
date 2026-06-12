package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.util.NumericConversion

import java.math.MathContext
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
@SuppressWarnings('DuplicateNumberLiteral')
class LinearRegression {

  BigDecimal slope
  BigDecimal intercept
  BigDecimal r2
  BigDecimal interceptStdErr
  BigDecimal slopeStdErr
  String x = 'X'
  String y = 'Y'

  /**
   * Fits a simple linear regression from two numeric matrix columns.
   *
   * @param table the source matrix
   * @param x the predictor column name
   * @param y the response column name
   * @throws IllegalArgumentException if the columns are missing, nonnumeric, or not estimable
   */
  LinearRegression(Matrix table, String x, String y) {
    this(matrixData(table, x, y))
    this.x = x
    this.y = y
  }

  /**
   * Fits a simple linear regression from predictor and response values.
   *
   * @param x the predictor values
   * @param y the response values
   * @throws IllegalArgumentException if the inputs are invalid or not estimable
   */
  LinearRegression(List<? extends Number> x, List<? extends Number> y) {
    this(listData(x, y))
  }

  private LinearRegression(RegressionData data) {
    validateShape(data.xValues, data.yValues)
    List<BigDecimal> xValues = data.xValues
    List<BigDecimal> yValues = data.yValues
    validateVariation(xValues)

    Integer numberOfDataValues = xValues.size()

    List<BigDecimal> xSquared = xValues.collect { BigDecimal value -> value * value }

    List<BigDecimal> xMultipliedByY = (0 ..< numberOfDataValues).collect {
      i -> xValues[i] * yValues[i]
    }

    BigDecimal xSummed = Stat.sum(xValues) as BigDecimal
    BigDecimal ySummed = Stat.sum(yValues) as BigDecimal
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
      xxbar += (xValues[i] - xBar) ** 2
      yybar += (yValues[i] - yBar) ** 2
      xybar += (xValues[i] - xBar) * (yValues[i] - yBar)
      BigDecimal fit = predict(xValues[i])
      rss += (fit - yValues[i]) ** 2
      ssr += (fit - yBar) ** 2
    }
    int degreesOfFreedom = numberOfDataValues - 2
    r2 = yybar == 0 ? 1.0G : ssr / yybar
    BigDecimal svar  = rss / degreesOfFreedom
    BigDecimal slopeVar = svar / xxbar
    slopeStdErr = slopeVar.sqrt(MathContext.DECIMAL128)
    interceptStdErr = (svar/numberOfDataValues + xBar * xBar * slopeVar).sqrt(MathContext.DECIMAL128)
  }

  BigDecimal predict(Number dependentVariable) {
    (slope * dependentVariable) + intercept
  }

  BigDecimal predict(Number dependentVariable, int numberOfDecimals) {
    predict(dependentVariable).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  List<BigDecimal> predict(List<Number> dependentVariables) {
    dependentVariables.collect { predict(it) }
  }

  List<BigDecimal> predict(List<Number> dependentVariables, int numberOfDecimals) {
    dependentVariables.collect { predict(it, numberOfDecimals) }
  }

  BigDecimal getSlope() {
    slope
  }

  BigDecimal getSlope(int numberOfDecimals) {
    slope.setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getIntercept() {
    intercept
  }

  BigDecimal getIntercept(int numberOfDecimals) {
    intercept.setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
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
    r2
  }

  BigDecimal getRsquared(int numberOfDecimals) {
    getRsquared().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
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
    interceptStdErr
  }

  BigDecimal getInterceptStdErr(int numberOfDecimals) {
    getInterceptStdErr().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
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
    slopeStdErr
  }

  BigDecimal getSlopeStdErr(int numberOfDecimals) {
    getSlopeStdErr().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  @Override
  String toString() {
    "Y = ${getSlope(2)}X ${intercept > 0 ? '+' : '-'} ${getIntercept(2).abs()}"
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
Equation: ${this}
Residuals: (not yet implemented)

Coefficients
${coefficients.content()}

Multiple R-squared: ${getRsquared(3)}
""".stripIndent()
  }

  private static RegressionData listData(List<? extends Number> x, List<? extends Number> y) {
    new RegressionData(
      x == null ? null : numericValues(x, 'x'),
      y == null ? null : numericValues(y, 'y')
    )
  }

  private static RegressionData matrixData(Matrix table, String xColumn, String yColumn) {
    validateMatrixColumns(table, xColumn, yColumn)
    new RegressionData(
      columnValues(table, xColumn),
      columnValues(table, yColumn)
    )
  }

  private static void validateMatrixColumns(Matrix table, String xColumn, String yColumn) {
    if (table == null) {
      throw new IllegalArgumentException('Matrix cannot be null')
    }
    validateMatrixColumn(table, xColumn)
    validateMatrixColumn(table, yColumn)
  }

  private static void validateMatrixColumn(Matrix table, String columnName) {
    if (columnName == null) {
      throw new IllegalArgumentException('Column name cannot be null')
    }
    if (!table.columnNames().contains(columnName)) {
      throw new IllegalArgumentException("Matrix does not contain column '${columnName}'")
    }
  }

  private static List<BigDecimal> columnValues(Matrix table, String columnName) {
    List column = table.column(columnName)
    (0 ..< table.rowCount()).collect { int row ->
      NumericConversion.toBigDecimal(column[row], "matrix value at row ${row}, column '${columnName}'")
    } as List<BigDecimal>
  }

  private static void validateShape(List<? extends Number> x, List<? extends Number> y) {
    if (x == null || y == null) {
      throw new IllegalArgumentException('X and Y data points cannot be null')
    }
    if (x.size() != y.size()) {
      throw new IllegalArgumentException('Must have equal number of X and Y data points for a linear regression')
    }
    if (x.size() < 3) {
      throw new IllegalArgumentException(
        "Linear regression requires at least three observations, got ${x.size()}"
      )
    }
  }

  private static List<BigDecimal> numericValues(List<?> values, String label) {
    values.withIndex().collect { Object value, int index ->
      NumericConversion.toBigDecimal(value, "${label} value at index ${index}")
    } as List<BigDecimal>
  }

  private static void validateVariation(List<BigDecimal> xValues) {
    BigDecimal first = xValues[0]
    if (xValues.every { BigDecimal value -> value == first }) {
      throw new IllegalArgumentException('Linear regression requires at least two distinct X values')
    }
  }

  private static class RegressionData {
    final List<BigDecimal> xValues
    final List<BigDecimal> yValues

    RegressionData(List<BigDecimal> xValues, List<BigDecimal> yValues) {
      this.xValues = xValues
      this.yValues = yValues
    }
  }
}
