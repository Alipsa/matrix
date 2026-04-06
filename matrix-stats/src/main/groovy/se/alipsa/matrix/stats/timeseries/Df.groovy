package se.alipsa.matrix.stats.timeseries

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * The Dickey-Fuller (DF) test is the foundational statistical test for detecting unit roots in time series.
 * A unit root indicates non-stationarity, meaning the series has no tendency to return to a long-run mean
 * and shocks have permanent effects. This is the basic version without augmentation terms.
 *
 * <p>The presence of a unit root has critical implications for econometric modeling and forecasting.
 * Non-stationary series can lead to spurious regressions, and many time series models require
 * stationary input data. The DF test provides a formal statistical framework for testing this property.</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 * <li>As a preliminary check before time series modeling (ARIMA, VAR)</li>
 * <li>When testing if a series is integrated of order one, I(1)</li>
 * <li>To determine if differencing is needed to achieve stationarity</li>
 * <li>For simple AR(1) processes without complex lag structures</li>
 * <li>When you need a baseline unit root test (use ADF for more complex cases)</li>
 * </ul>
 *
 * <h3>Hypotheses</h3>
 * <ul>
 * <li><strong>H0 (null):</strong> The series has a unit root; it is non-stationary (γ = 0)</li>
 * <li><strong>H1 (alternative):</strong> The series is stationary or trend-stationary (γ < 0)</li>
 * </ul>
 *
 * <h3>Test Regression</h3>
 * <p>Δy_t = α + βt + γy_{t-1} + ε_t</p>
 * <p>where the null hypothesis H0: γ = 0 is tested against H1: γ < 0</p>
 *
 * <p><strong>Note:</strong> For series with autocorrelated errors, use the Augmented Dickey-Fuller (ADF) test
 * which includes lagged differences to account for serial correlation.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Basic test with drift (default)
 * def timeSeries = [100, 102, 101, 105, 107, 106, 110, 112] as double[]
 * def result = Df.test(timeSeries)
 * println result.interpret()
 *
 * // Test with trend component
 * def result2 = Df.test(timeSeries, "trend")
 * println result2.evaluate()
 *
 * // Test without intercept or trend
 * def result3 = Df.test(timeSeries, "none")
 * </pre>
 *
 * <h3>References</h3>
 * <ul>
 * <li>Dickey, D. A., & Fuller, W. A. (1979). "Distribution of the Estimators for Autoregressive Time Series
 * with a Unit Root", Journal of the American Statistical Association, 74(366), 427-431.</li>
 * <li>Hamilton, J. D. (1994). Time Series Analysis, Chapter 17.</li>
 * <li>R's tseries package: adf.test() and urca package: ur.df()</li>
 * </ul>
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral', 'ParameterName', 'VariableName'])
class Df {

  /**
   * Performs the Dickey-Fuller test for a unit root.
   *
   * @param data The time series data
   * @param type The type of test: "none" (no intercept/trend), "drift" (intercept only), or "trend" (intercept and trend)
   * @return DfResult containing test statistic and conclusion
   */
  @SuppressWarnings('MethodSize')
  static DfResult test(double[] data, String type = "drift") {
    validateInput(data, type)
    int n = data.length
    ensureVariation(data)
    double[] response = firstDifferences(data)
    int nObs = response.length
    int nPredictors = predictorCount(type)
    double[][] X = buildDesignMatrix(data, type, nObs, nPredictors)
    double[][] XtX = crossProduct(X, nPredictors, nObs)
    double[] Xty = crossProduct(X, response, nPredictors, nObs)

    // Solve for coefficients using Gaussian elimination
    double[] beta = TimeSeriesUtils.solveLinearSystem(XtX, Xty)

    // The coefficient we care about is γ (coefficient for y_{t-1})
    int gammaIndex = nPredictors - 1  // Last coefficient is always y_{t-1}
    double gamma = beta[gammaIndex]

    double rss = residualSumOfSquares(X, beta, response, nPredictors, nObs)
    int df = nObs - nPredictors
    double sigma2 = rss / df

    // Calculate (X'X)^(-1) for standard errors
    double[][] XtXinv = TimeSeriesUtils.invertMatrix(XtX)

    // Standard error of gamma
    double gammaSE = Math.sqrt(sigma2 * XtXinv[gammaIndex][gammaIndex])

    // Calculate t-statistic (Dickey-Fuller statistic)
    double dfStatistic = gamma / gammaSE

    // Determine critical values based on type and sample size
    double cv1pct = getCriticalValue(type, n, 0.01)
    double cv5pct = getCriticalValue(type, n, 0.05)
    double cv10pct = getCriticalValue(type, n, 0.10)

    new DfResult(
      statistic: toBigDecimalOrNull(dfStatistic),
      gamma: toBigDecimalOrNull(gamma),
      standardError: toBigDecimalOrNull(gammaSE),
      sampleSize: n,
      testType: type,
      criticalValue1pct: BigDecimal.valueOf(cv1pct),
      criticalValue5pct: BigDecimal.valueOf(cv5pct),
      criticalValue10pct: BigDecimal.valueOf(cv10pct)
    )
  }

  private static BigDecimal toBigDecimalOrNull(double value) {
    Double.isFinite(value) ? BigDecimal.valueOf(value) : null
  }

  private static void validateInput(double[] data, String type) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.length < 10) {
      throw new IllegalArgumentException("Data must have at least 10 observations (got ${data.length})")
    }
    if (!(type in ["none", "drift", "trend"])) {
      throw new IllegalArgumentException("Type must be 'none', 'drift', or 'trend' (got '${type}')")
    }
  }

  private static void ensureVariation(double[] data) {
    double yMin = Double.POSITIVE_INFINITY
    double yMax = Double.NEGATIVE_INFINITY
    for (double value : data) {
      if (value < yMin) {
        yMin = value
      }
      if (value > yMax) {
        yMax = value
      }
    }
    if (Math.abs(yMax - yMin) < 1e-10) {
      throw new IllegalArgumentException("Data has no variation (constant series)")
    }
  }

  private static double[] firstDifferences(double[] data) {
    double[] differences = new double[data.length - 1]
    for (int i = 1; i < data.length; i++) {
      differences[i - 1] = data[i] - data[i - 1]
    }
    differences
  }

  private static int predictorCount(String type) {
    int count = 1
    if (type == "drift" || type == "trend") {
      count++
    }
    if (type == "trend") {
      count++
    }
    count
  }

  private static double[][] buildDesignMatrix(double[] data, String type, int nObs, int nPredictors) {
    double[][] designMatrix = new double[nObs][nPredictors]
    for (int i = 0; i < nObs; i++) {
      int column = 0
      if (type == "drift" || type == "trend") {
        designMatrix[i][column++] = 1.0
      }
      if (type == "trend") {
        designMatrix[i][column++] = i + 1.0
      }
      designMatrix[i][column] = data[i]
    }
    designMatrix
  }

  private static double[][] crossProduct(double[][] X, int nPredictors, int nObs) {
    double[][] XtX = new double[nPredictors][nPredictors]
    for (int i = 0; i < nPredictors; i++) {
      for (int j = 0; j < nPredictors; j++) {
        double sum = 0.0
        for (int k = 0; k < nObs; k++) {
          sum += X[k][i] * X[k][j]
        }
        XtX[i][j] = sum
      }
    }
    XtX
  }

  private static double[] crossProduct(double[][] X, double[] response, int nPredictors, int nObs) {
    double[] Xty = new double[nPredictors]
    for (int i = 0; i < nPredictors; i++) {
      double sum = 0.0
      for (int k = 0; k < nObs; k++) {
        sum += X[k][i] * response[k]
      }
      Xty[i] = sum
    }
    Xty
  }

  private static double residualSumOfSquares(double[][] X, double[] beta, double[] response, int nPredictors, int nObs) {
    double rss = 0.0
    for (int i = 0; i < nObs; i++) {
      double fitted = 0.0
      for (int j = 0; j < nPredictors; j++) {
        fitted += X[i][j] * beta[j]
      }
      double residual = response[i] - fitted
      rss += residual * residual
    }
    rss
  }

  /**
   * Performs the Dickey-Fuller test on a List of numbers.
   */
  static DfResult test(List<? extends Number> data, String type = "drift") {
    double[] array = data.collect { it.doubleValue() } as double[]
    test(array, type)
  }

  /**
   * Get critical values for Dickey-Fuller test.
   * These are MacKinnon (1996) approximate critical values.
   */
  private static double getCriticalValue(String type, int n, double significance) {
    // Critical values depend on test type and sample size
    // Using MacKinnon (1996) approximation
    double[] params

    if (type == "none") {
      if (significance == 0.01) {
        params = [-2.66, -2.62, -1.26, 0.00] as double[]
      } else if (significance == 0.05) {
        params = [-1.95, -1.95, -0.99, 0.00] as double[]
      } else {
        params = [-1.60, -1.62, -0.88, 0.00] as double[]  // 10%
      }
    } else if (type == "drift") {
      if (significance == 0.01) {
        params = [-3.75, -3.52, -1.79, -0.38] as double[]
      } else if (significance == 0.05) {
        params = [-3.00, -2.89, -1.47, -0.27] as double[]
      } else {
        params = [-2.63, -2.58, -1.29, -0.22] as double[]  // 10%
      }
    } else {  // trend
      if (significance == 0.01) {
        params = [-4.38, -3.95, -1.95, -0.40] as double[]
      } else if (significance == 0.05) {
        params = [-3.60, -3.43, -1.68, -0.31] as double[]
      } else {
        params = [-3.24, -3.13, -1.53, -0.26] as double[]  // 10%
      }
    }

    // MacKinnon approximation: CV(T) = β₀ + β₁/T + β₂/T² + β₃/T³
    double T = n as double
    params[0] + params[1] / T + params[2] / (T * T) + params[3] / (T * T * T)
  }

  /**
   * Result class for Dickey-Fuller test.
   */
  static class DfResult {
    /** The Dickey-Fuller test statistic (t-statistic for γ) */
    BigDecimal statistic

    /** The estimated γ coefficient */
    BigDecimal gamma

    /** Standard error of γ */
    BigDecimal standardError

    /** Sample size */
    int sampleSize

    /** Test type (none/drift/trend) */
    String testType

    /** Critical value at 1% significance */
    BigDecimal criticalValue1pct

    /** Critical value at 5% significance */
    BigDecimal criticalValue5pct

    /** Critical value at 10% significance */
    BigDecimal criticalValue10pct

    /**
     * Interprets the test result.
     *
     * @param alpha Significance level (default 0.05)
     * @return Interpretation string
     */
    String interpret(Number alpha = 0.05) {
      BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
      BigDecimal cv = alphaValue == 0.01 ? criticalValue1pct :
        alphaValue == 0.10 ? criticalValue10pct : criticalValue5pct

      if (statistic != null && statistic < cv) {
        return "Reject H0: Series appears stationary (DF = ${format(statistic, '%.4f')}, CV = ${format(cv, '%.4f')})"
      } else {
        return "Fail to reject H0: Unit root likely present, series appears non-stationary (DF = ${format(statistic, '%.4f')}, CV = ${format(cv, '%.4f')})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(Number alpha = 0.05) {
      BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
      String conclusion = statistic != null && statistic < criticalValue5pct ? "stationary" : "non-stationary (unit root present)"

      String.format(
        "Dickey-Fuller test:\\n" +
        "Test type: %s\\n" +
        "DF statistic: %.4f\\n" +
        "Critical values: 1%% = %.4f, 5%% = %.4f, 10%% = %.4f\\n" +
        "Sample size: %d\\n" +
        "Conclusion: Series appears %s at %.0f%% significance level",
        testType, statistic ?: Double.NaN, criticalValue1pct, criticalValue5pct, criticalValue10pct,
        sampleSize, conclusion, (alphaValue * 100) as double
      )
    }

    @Override
    String toString() {
      """Dickey-Fuller Test
  Type: ${testType}
  Sample size: ${sampleSize}
  DF statistic: ${format(statistic, '%.4f')}
  γ coefficient: ${format(gamma, '%.6f')}
  Standard error: ${format(standardError, '%.6f')}
  Critical values:
    1%: ${format(criticalValue1pct, '%.4f')}
    5%: ${format(criticalValue5pct, '%.4f')}
   10%: ${format(criticalValue10pct, '%.4f')}

  ${interpret()}"""
    }

    private static String format(Number value, String pattern) {
      value == null ? 'NaN' : String.format(pattern, value)
    }
  }
}
