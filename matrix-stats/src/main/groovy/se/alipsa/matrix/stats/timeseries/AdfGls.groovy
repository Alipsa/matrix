package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic

/**
 * The ADF-GLS test (or DF-GLS test) is a test for a unit root in a time series.
 * It was developed by Elliott, Rothenberg and Stock (ERS) in 1996 as a modification of the
 * augmented Dickey-Fuller test (ADF).
 *
 * The ADF-GLS test uses generalized least squares (GLS) detrending prior to running the ADF test.
 * This modification provides improved power when there is an uncertain trend under the alternative hypothesis.
 *
 * The procedure:
 * 1. GLS-detrend the data with parameter α (α = -7 for drift, α = -13.5 for trend)
 * 2. Run ADF regression on the detrended data
 * 3. Use ADF-GLS specific critical values
 *
 * Example:
 * <pre>
 * def timeSeries = [100, 102, 101, 105, 107, 106, 110, 112] as double[]
 * def result = AdfGls.test(timeSeries)
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - Elliott, G., Rothenberg, T.J., & Stock, J.H. (1996). "Efficient tests for an autoregressive unit root"
 * - R's urca package (ur.ers)
 */
@CompileStatic
class AdfGls {

  /**
   * Performs the ADF-GLS test for a unit root.
   *
   * @param data The time series data
   * @param lags The number of lags to include (default: auto-selected using modified AIC)
   * @param type The type of test: "drift" (constant only) or "trend" (constant and trend)
   * @return AdfGlsResult containing test statistic and conclusion
   */
  static AdfGlsResult test(double[] data, Integer lags = null, String type = "drift") {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.length < 10) {
      throw new IllegalArgumentException("Data must have at least 10 observations (got ${data.length})")
    }

    if (type != "drift" && type != "trend") {
      throw new IllegalArgumentException("Type must be 'drift' or 'trend' (got '${type}')")
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

    // Auto-select lags if not provided
    int p = lags != null ? lags : selectLags(data, type)

    // GLS detrending parameter: α = 1 + c/n
    // c = -7 for drift, c = -13.5 for trend
    double c = type == "drift" ? -7.0 : -13.5
    double alpha = 1.0 + c / n

    // GLS-detrend the data
    double[] detrended = glsDetrend(data, alpha, type)

    // Run ADF-type regression on detrended data
    // Δỹ_t = γỹ_{t-1} + δ₁Δỹ_{t-1} + ... + δₚΔỹ_{t-p} + ε_t

    // Calculate first differences of detrended data
    double[] dy = new double[n - 1]
    for (int i = 1; i < n; i++) {
      dy[i - 1] = detrended[i] - detrended[i - 1]
    }

    // Prepare regression
    int nObs = n - p - 1
    if (nObs < 10) {
      throw new IllegalArgumentException("Insufficient observations after accounting for lags")
    }

    // Build design matrix: [ỹ_{t-1}, Δỹ_{t-1}, ..., Δỹ_{t-p}]
    double[][] X = new double[nObs][p + 1]
    double[] response = new double[nObs]

    for (int i = 0; i < nObs; i++) {
      int t = p + i
      // Response: Δỹ_t
      response[i] = dy[t]

      // Predictor: ỹ_{t-1}
      X[i][0] = detrended[t]

      // Lagged differences: Δỹ_{t-1}, ..., Δỹ_{t-p}
      for (int j = 1; j <= p; j++) {
        X[i][j] = dy[t - j]
      }
    }

    // Perform OLS regression
    double[] beta = solveLinearSystem(X, response)
    double gamma = beta[0]

    // Calculate residuals and standard error
    double[] residuals = new double[nObs]
    for (int i = 0; i < nObs; i++) {
      double fitted = 0.0
      for (int j = 0; j < p + 1; j++) {
        fitted += X[i][j] * beta[j]
      }
      residuals[i] = response[i] - fitted
    }

    double rss = 0.0
    for (double r : residuals) {
      rss += r * r
    }

    int df = nObs - (p + 1)
    double sigma2 = rss / df

    // Calculate (X'X)^(-1) for standard errors
    double[][] XtX = new double[p + 1][p + 1]
    for (int i = 0; i < p + 1; i++) {
      for (int j = 0; j < p + 1; j++) {
        double sum = 0.0
        for (int k = 0; k < nObs; k++) {
          sum += X[k][i] * X[k][j]
        }
        XtX[i][j] = sum
      }
    }

    double[][] XtXinv = invertMatrix(XtX)
    double gammaSE = Math.sqrt(sigma2 * XtXinv[0][0])

    // Calculate t-statistic (ADF-GLS statistic)
    double tStatistic = gamma / gammaSE

    // Get critical values
    double cv1pct = getCriticalValue(type, n, p, 0.01)
    double cv5pct = getCriticalValue(type, n, p, 0.05)
    double cv10pct = getCriticalValue(type, n, p, 0.10)

    return new AdfGlsResult(
      statistic: tStatistic,
      gamma: gamma,
      standardError: gammaSE,
      lags: p,
      sampleSize: n,
      testType: type,
      criticalValue1pct: cv1pct,
      criticalValue5pct: cv5pct,
      criticalValue10pct: cv10pct
    )
  }

  /**
   * Performs the ADF-GLS test on a List of numbers.
   */
  static AdfGlsResult test(List<? extends Number> data, Integer lags = null, String type = "drift") {
    double[] array = data.collect { it.doubleValue() } as double[]
    return test(array, lags, type)
  }

  /**
   * GLS detrending of the data.
   * Transforms data using quasi-differencing: z_t = y_t - α*y_{t-1}
   * Then regresses on transformed deterministic terms and removes fitted values.
   */
  private static double[] glsDetrend(double[] data, double alpha, String type) {
    int n = data.length

    // Quasi-difference the data and deterministic terms
    double[] z = new double[n]
    z[0] = data[0]
    for (int i = 1; i < n; i++) {
      z[i] = data[i] - alpha * data[i - 1]
    }

    // Build quasi-differenced deterministic terms
    double[][] X = new double[n][type == "drift" ? 1 : 2]

    // Constant term
    X[0][0] = 1.0
    for (int i = 1; i < n; i++) {
      X[i][0] = 1.0 - alpha
    }

    // Trend term (if needed)
    if (type == "trend") {
      X[0][1] = 1.0
      for (int i = 1; i < n; i++) {
        X[i][1] = (i + 1.0) - alpha * i
      }
    }

    // Perform GLS regression: z = X*β + ε
    double[] beta = solveLinearSystem(X, z)

    // Compute fitted values on original deterministic terms
    double[] fitted = new double[n]
    for (int i = 0; i < n; i++) {
      fitted[i] = beta[0]  // Constant
      if (type == "trend") {
        fitted[i] += beta[1] * (i + 1.0)  // Trend
      }
    }

    // Detrended series
    double[] detrended = new double[n]
    for (int i = 0; i < n; i++) {
      detrended[i] = data[i] - fitted[i]
    }

    return detrended
  }

  /**
   * Select optimal number of lags using modified AIC (MAIC).
   * Following Ng and Perron (2001) recommendation.
   */
  private static int selectLags(double[] data, String type) {
    int n = data.length
    int maxLags = Math.min(12, (int) Math.floor(12.0 * Math.pow(n / 100.0, 0.25)))

    double bestMAIC = Double.POSITIVE_INFINITY
    int bestLag = 0

    for (int p = 0; p <= maxLags; p++) {
      try {
        AdfGlsResult result = test(data, p, type)

        // Calculate MAIC
        double sigma2 = result.standardError * result.standardError
        double maic = Math.log(sigma2) + 2.0 * (p + 1) / (n - p - 1)

        if (maic < bestMAIC) {
          bestMAIC = maic
          bestLag = p
        }
      } catch (Exception ignored) {
        // Skip this lag if calculation fails
      }
    }

    return bestLag
  }

  /**
   * Solve linear system Ax = b using Gaussian elimination with partial pivoting.
   */
  private static double[] solveLinearSystem(double[][] A, double[] b) {
    int n = b.length
    int m = A[0].length
    double[][] augmented = new double[n][m + 1]

    // Create augmented matrix
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++) {
        augmented[i][j] = A[i][j]
      }
      augmented[i][m] = b[i]
    }

    // Forward elimination with partial pivoting
    for (int k = 0; k < m; k++) {
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
      if (maxRow != k && maxRow < n) {
        double[] temp = augmented[k]
        augmented[k] = augmented[maxRow]
        augmented[maxRow] = temp
      }

      // Eliminate column
      if (Math.abs(augmented[k][k]) > 1e-14) {
        for (int i = k + 1; i < n; i++) {
          double factor = augmented[i][k] / augmented[k][k]
          for (int j = k; j < m + 1; j++) {
            augmented[i][j] -= factor * augmented[k][j]
          }
        }
      }
    }

    // Back substitution
    double[] x = new double[m]
    for (int i = m - 1; i >= 0; i--) {
      double sum = augmented[i][m]
      for (int j = i + 1; j < m; j++) {
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
   * Get critical values for ADF-GLS test.
   * Based on Elliott, Rothenberg, and Stock (1996) and updated tables.
   */
  private static double getCriticalValue(String type, int n, int p, double significance) {
    // Critical values are less negative than standard ADF
    // Using approximate values from ERS (1996) Table 1

    double[] params

    if (type == "drift") {
      if (significance == 0.01) params = [-2.64, -1.95, -0.80, 0.00] as double[]
      else if (significance == 0.05) params = [-1.95, -1.53, -0.60, 0.00] as double[]
      else params = [-1.62, -1.33, -0.49, 0.00] as double[]  // 10%
    } else {  // trend
      if (significance == 0.01) params = [-3.58, -2.58, -1.04, -0.10] as double[]
      else if (significance == 0.05) params = [-3.03, -2.22, -0.87, -0.08] as double[]
      else params = [-2.74, -2.03, -0.78, -0.07] as double[]  // 10%
    }

    // Asymptotic approximation: CV(T) = β₀ + β₁/T + β₂/T² + β₃/T³
    double T = n as double
    return params[0] + params[1] / T + params[2] / (T * T) + params[3] / (T * T * T)
  }

  /**
   * Result class for ADF-GLS test.
   */
  @CompileStatic
  static class AdfGlsResult {
    /** The ADF-GLS test statistic (t-statistic for γ) */
    double statistic

    /** The estimated γ coefficient */
    double gamma

    /** Standard error of γ */
    double standardError

    /** Number of lags used */
    int lags

    /** Sample size */
    int sampleSize

    /** Test type (drift/trend) */
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
        return "Reject H0: Series appears stationary (ADF-GLS = ${String.format('%.4f', statistic)}, CV = ${String.format('%.4f', cv)})"
      } else {
        return "Fail to reject H0: Unit root likely present, series appears non-stationary (ADF-GLS = ${String.format('%.4f', statistic)}, CV = ${String.format('%.4f', cv)})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = statistic < criticalValue5pct ? "stationary" : "non-stationary (unit root present)"

      return String.format(
        "ADF-GLS test:\\n" +
        "Test type: %s\\n" +
        "Lags: %d\\n" +
        "ADF-GLS statistic: %.4f\\n" +
        "Critical values: 1%% = %.4f, 5%% = %.4f, 10%% = %.4f\\n" +
        "Sample size: %d\\n" +
        "Conclusion: Series appears %s at %.0f%% significance level",
        testType, lags, statistic, criticalValue1pct, criticalValue5pct, criticalValue10pct,
        sampleSize, conclusion, alpha * 100
      )
    }

    @Override
    String toString() {
      return """ADF-GLS Test (Elliott-Rothenberg-Stock)
  Type: ${testType}
  Lags: ${lags}
  Sample size: ${sampleSize}
  ADF-GLS statistic: ${String.format('%.4f', statistic)}
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
