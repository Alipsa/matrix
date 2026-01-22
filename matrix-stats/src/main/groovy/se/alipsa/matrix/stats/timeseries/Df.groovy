package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic

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
@CompileStatic
class Df {

  /**
   * Performs the Dickey-Fuller test for a unit root.
   *
   * @param data The time series data
   * @param type The type of test: "none" (no intercept/trend), "drift" (intercept only), or "trend" (intercept and trend)
   * @return DfResult containing test statistic and conclusion
   */
  static DfResult test(double[] data, String type = "drift") {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.length < 10) {
      throw new IllegalArgumentException("Data must have at least 10 observations (got ${data.length})")
    }

    if (type != "none" && type != "drift" && type != "trend") {
      throw new IllegalArgumentException("Type must be 'none', 'drift', or 'trend' (got '${type}')")
    }

    int n = data.length

    // Check for constant series
    double yMin = Double.POSITIVE_INFINITY
    double yMax = Double.NEGATIVE_INFINITY
    for (double val : data) {
      if (val < yMin) yMin = val
      if (val > yMax) yMax = val
    }

    if (Math.abs(yMax - yMin) < 1e-10) {
      throw new IllegalArgumentException("Data has no variation (constant series)")
    }

    // Calculate first differences: Δy_t = y_t - y_{t-1}
    double[] dy = new double[n - 1]
    for (int i = 1; i < n; i++) {
      dy[i - 1] = data[i] - data[i - 1]
    }

    // Build regression: Δy_t = α + βt + γy_{t-1} + ε_t
    // Response: Δy_t for t = 2, ..., n
    // Predictors: [1, t, y_{t-1}] depending on type

    int nObs = n - 1
    double[] response = dy

    // Count number of predictors based on type
    int nPredictors = 1  // Always have y_{t-1}
    if (type == "drift" || type == "trend") nPredictors++  // Add intercept
    if (type == "trend") nPredictors++  // Add time trend

    // Build design matrix
    double[][] X = new double[nObs][nPredictors]
    for (int i = 0; i < nObs; i++) {
      int col = 0

      if (type == "drift" || type == "trend") {
        X[i][col++] = 1.0  // Intercept
      }

      if (type == "trend") {
        X[i][col++] = i + 1.0  // Time trend (1, 2, 3, ...)
      }

      X[i][col++] = data[i]  // y_{t-1}
    }

    // Perform OLS regression using simple matrix calculations
    // β = (X'X)^(-1) X'y

    // Calculate X'X
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

    // Calculate X'y
    double[] Xty = new double[nPredictors]
    for (int i = 0; i < nPredictors; i++) {
      double sum = 0.0
      for (int k = 0; k < nObs; k++) {
        sum += X[k][i] * response[k]
      }
      Xty[i] = sum
    }

    // Solve for coefficients using Gaussian elimination
    double[] beta = solveLinearSystem(XtX, Xty)

    // The coefficient we care about is γ (coefficient for y_{t-1})
    int gammaIndex = nPredictors - 1  // Last coefficient is always y_{t-1}
    double gamma = beta[gammaIndex]

    // Calculate residuals and standard error
    double[] residuals = new double[nObs]
    for (int i = 0; i < nObs; i++) {
      double fitted = 0.0
      for (int j = 0; j < nPredictors; j++) {
        fitted += X[i][j] * beta[j]
      }
      residuals[i] = response[i] - fitted
    }

    double rss = 0.0  // Residual sum of squares
    for (double r : residuals) {
      rss += r * r
    }

    int df = nObs - nPredictors
    double sigma2 = rss / df

    // Calculate (X'X)^(-1) for standard errors
    double[][] XtXinv = invertMatrix(XtX)

    // Standard error of gamma
    double gammaSE = Math.sqrt(sigma2 * XtXinv[gammaIndex][gammaIndex])

    // Calculate t-statistic (Dickey-Fuller statistic)
    double dfStatistic = gamma / gammaSE

    // Determine critical values based on type and sample size
    double cv1pct = getCriticalValue(type, n, 0.01)
    double cv5pct = getCriticalValue(type, n, 0.05)
    double cv10pct = getCriticalValue(type, n, 0.10)

    return new DfResult(
      statistic: dfStatistic,
      gamma: gamma,
      standardError: gammaSE,
      sampleSize: n,
      testType: type,
      criticalValue1pct: cv1pct,
      criticalValue5pct: cv5pct,
      criticalValue10pct: cv10pct
    )
  }

  /**
   * Performs the Dickey-Fuller test on a List of numbers.
   */
  static DfResult test(List<? extends Number> data, String type = "drift") {
    double[] array = data.collect { it.doubleValue() } as double[]
    return test(array, type)
  }

  /**
   * Solve linear system Ax = b using Gaussian elimination with partial pivoting.
   */
  private static double[] solveLinearSystem(double[][] A, double[] b) {
    int n = b.length
    double[][] augmented = new double[n][n + 1]

    // Create augmented matrix
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        augmented[i][j] = A[i][j]
      }
      augmented[i][n] = b[i]
    }

    // Forward elimination with partial pivoting
    for (int k = 0; k < n; k++) {
      // Find pivot
      int maxRow = k
      double maxVal = Math.abs(augmented[k][k])
      for (int i = k + 1; i < n; i++) {
        if (Math.abs(augmented[i][k]) > maxVal) {
          maxVal = Math.abs(augmented[i][k])
          maxRow = i
        }
      }

      // Swap rows
      if (maxRow != k) {
        double[] temp = augmented[k]
        augmented[k] = augmented[maxRow]
        augmented[maxRow] = temp
      }

      // Eliminate column
      for (int i = k + 1; i < n; i++) {
        double factor = augmented[i][k] / augmented[k][k]
        for (int j = k; j < n + 1; j++) {
          augmented[i][j] -= factor * augmented[k][j]
        }
      }
    }

    // Back substitution
    double[] x = new double[n]
    for (int i = n - 1; i >= 0; i--) {
      double sum = augmented[i][n]
      for (int j = i + 1; j < n; j++) {
        sum -= augmented[i][j] * x[j]
      }
      x[i] = sum / augmented[i][i]
    }

    return x
  }

  /**
   * Invert a matrix using Gaussian elimination.
   */
  private static double[][] invertMatrix(double[][] A) {
    int n = A.length
    double[][] result = new double[n][n]
    double[][] augmented = new double[n][2 * n]

    // Create augmented matrix [A | I]
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        augmented[i][j] = A[i][j]
        augmented[i][j + n] = (i == j) ? 1.0 : 0.0
      }
    }

    // Gaussian elimination
    for (int k = 0; k < n; k++) {
      // Pivot
      double pivot = augmented[k][k]
      for (int j = 0; j < 2 * n; j++) {
        augmented[k][j] /= pivot
      }

      // Eliminate
      for (int i = 0; i < n; i++) {
        if (i != k) {
          double factor = augmented[i][k]
          for (int j = 0; j < 2 * n; j++) {
            augmented[i][j] -= factor * augmented[k][j]
          }
        }
      }
    }

    // Extract result
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        result[i][j] = augmented[i][j + n]
      }
    }

    return result
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
      if (significance == 0.01) params = [-2.66, -2.62, -1.26, 0.00] as double[]
      else if (significance == 0.05) params = [-1.95, -1.95, -0.99, 0.00] as double[]
      else params = [-1.60, -1.62, -0.88, 0.00] as double[]  // 10%
    } else if (type == "drift") {
      if (significance == 0.01) params = [-3.75, -3.52, -1.79, -0.38] as double[]
      else if (significance == 0.05) params = [-3.00, -2.89, -1.47, -0.27] as double[]
      else params = [-2.63, -2.58, -1.29, -0.22] as double[]  // 10%
    } else {  // trend
      if (significance == 0.01) params = [-4.38, -3.95, -1.95, -0.40] as double[]
      else if (significance == 0.05) params = [-3.60, -3.43, -1.68, -0.31] as double[]
      else params = [-3.24, -3.13, -1.53, -0.26] as double[]  // 10%
    }

    // MacKinnon approximation: CV(T) = β₀ + β₁/T + β₂/T² + β₃/T³
    double T = n as double
    return params[0] + params[1] / T + params[2] / (T * T) + params[3] / (T * T * T)
  }

  /**
   * Result class for Dickey-Fuller test.
   */
  @CompileStatic
  static class DfResult {
    /** The Dickey-Fuller test statistic (t-statistic for γ) */
    double statistic

    /** The estimated γ coefficient */
    double gamma

    /** Standard error of γ */
    double standardError

    /** Sample size */
    int sampleSize

    /** Test type (none/drift/trend) */
    String testType

    /** Critical value at 1% significance */
    double criticalValue1pct

    /** Critical value at 5% significance */
    double criticalValue5pct

    /** Critical value at 10% significance */
    double criticalValue10pct

    /**
     * Interprets the test result.
     *
     * @param alpha Significance level (default 0.05)
     * @return Interpretation string
     */
    String interpret(double alpha = 0.05) {
      double cv = alpha == 0.01 ? criticalValue1pct :
                  alpha == 0.10 ? criticalValue10pct : criticalValue5pct

      if (statistic < cv) {
        return "Reject H0: Series appears stationary (DF = ${String.format('%.4f', statistic)}, CV = ${String.format('%.4f', cv)})"
      } else {
        return "Fail to reject H0: Unit root likely present, series appears non-stationary (DF = ${String.format('%.4f', statistic)}, CV = ${String.format('%.4f', cv)})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = statistic < criticalValue5pct ? "stationary" : "non-stationary (unit root present)"

      return String.format(
        "Dickey-Fuller test:\\n" +
        "Test type: %s\\n" +
        "DF statistic: %.4f\\n" +
        "Critical values: 1%% = %.4f, 5%% = %.4f, 10%% = %.4f\\n" +
        "Sample size: %d\\n" +
        "Conclusion: Series appears %s at %.0f%% significance level",
        testType, statistic, criticalValue1pct, criticalValue5pct, criticalValue10pct,
        sampleSize, conclusion, alpha * 100
      )
    }

    @Override
    String toString() {
      return """Dickey-Fuller Test
  Type: ${testType}
  Sample size: ${sampleSize}
  DF statistic: ${String.format('%.4f', statistic)}
  γ coefficient: ${String.format('%.6f', gamma)}
  Standard error: ${String.format('%.6f', standardError)}
  Critical values:
    1%: ${String.format('%.4f', criticalValue1pct)}
    5%: ${String.format('%.4f', criticalValue5pct)}
   10%: ${String.format('%.4f', criticalValue10pct)}

  ${interpret()}"""
    }
  }
}
