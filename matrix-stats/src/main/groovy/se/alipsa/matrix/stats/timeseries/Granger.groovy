package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.FDistribution

/**
 * The Granger causality test determines whether one time series is useful in forecasting another.
 * First proposed by Clive Granger in 1969, it operationalizes the concept of causality in economics
 * through predictive ability rather than mere correlation.
 *
 * <p>Granger causality does not imply true causation but rather precedence and information content.
 * If X "Granger-causes" Y, it means that past values of X provide statistically significant information
 * about future values of Y beyond what is contained in past values of Y alone.</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 * <li>To test whether one economic variable helps predict another (e.g., money supply → inflation)</li>
 * <li>When analyzing lead-lag relationships in financial markets</li>
 * <li>To establish temporal precedence in causal inference</li>
 * <li>For identifying information flow between time series</li>
 * <li>As a preliminary test before building vector autoregression (VAR) models</li>
 * </ul>
 *
 * <h3>Hypotheses</h3>
 * <ul>
 * <li><strong>H0 (null):</strong> X does not Granger-cause Y (γ₁ = γ₂ = ... = γₚ = 0)</li>
 * <li><strong>H1 (alternative):</strong> X Granger-causes Y (at least one γᵢ ≠ 0)</li>
 * </ul>
 *
 * <h3>Test Procedure</h3>
 * <p>The test compares two models:</p>
 * <ul>
 * <li><strong>Restricted:</strong> Y_t = α + β₁Y_{t-1} + ... + βₚY_{t-p} + ε_t</li>
 * <li><strong>Unrestricted:</strong> Y_t = α + β₁Y_{t-1} + ... + βₚY_{t-p} + γ₁X_{t-1} + ... + γₚX_{t-p} + ε_t</li>
 * </ul>
 * <p>An F-test determines if adding lagged X values significantly improves the model.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test if X Granger-causes Y
 * def x = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] as double[]
 * def y = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11] as double[]
 *
 * // Test with 2 lags
 * def result = Granger.test(x, y, 2)
 * println result.interpret()
 *
 * // Auto-select optimal lag using AIC
 * def result2 = Granger.test(x, y)
 * println "Using ${result2.lags} lags (AIC-selected)"
 * </pre>
 *
 * <h3>Important Notes</h3>
 * <ul>
 * <li>Both series should be stationary; test for unit roots first (ADF, KPSS)</li>
 * <li>Granger causality is symmetric; test both directions (X→Y and Y→X)</li>
 * <li>Results are sensitive to lag length selection</li>
 * <li>Does not detect instantaneous causality (use VAR for contemporaneous effects)</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 * <li>Granger, C. W. J. (1969). "Investigating Causal Relations by Econometric Models and Cross-spectral Methods",
 * Econometrica, 37(3), 424-438.</li>
 * <li>Granger, C. W. J. (1980). "Testing for Causality: A Personal Viewpoint",
 * Journal of Economic Dynamics and Control, 2, 329-352.</li>
 * <li>R's lmtest package: grangertest() function</li>
 * <li>Stata's vargranger command</li>
 * </ul>
 */
@CompileStatic
class Granger {

  /**
   * Performs the Granger causality test.
   *
   * @param x The potential cause time series
   * @param y The response time series
   * @param maxLag The maximum number of lags to include (default: auto-selected)
   * @return GrangerResult containing test statistic, p-value, and conclusion
   */
  static GrangerResult test(double[] x, double[] y, Integer maxLag = null) {
    if (x == null || y == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }

    if (x.length != y.length) {
      throw new IllegalArgumentException("X and Y must have the same length (got ${x.length} and ${y.length})")
    }

    int n = x.length

    if (n < 10) {
      throw new IllegalArgumentException("Need at least 10 observations (got ${n})")
    }

    // Auto-select lag if not provided
    int p = maxLag != null ? maxLag : selectLag(x, y, n)

    if (p < 1) {
      throw new IllegalArgumentException("Lag must be at least 1 (got ${p})")
    }

    if (n <= 2 * p + 1) {
      throw new IllegalArgumentException("Insufficient observations for ${p} lags (need > ${2 * p + 1}, got ${n})")
    }

    // Build data for regression (dropping first p observations)
    int nObs = n - p
    double[] yResponse = new double[nObs]
    double[][] XRestricted = new double[nObs][p + 1]  // Intercept + p lags of Y
    double[][] XUnrestricted = new double[nObs][2 * p + 1]  // Intercept + p lags of Y + p lags of X

    for (int i = 0; i < nObs; i++) {
      int t = p + i
      yResponse[i] = y[t]

      // Intercept
      XRestricted[i][0] = 1.0
      XUnrestricted[i][0] = 1.0

      // Lags of Y
      for (int lag = 1; lag <= p; lag++) {
        XRestricted[i][lag] = y[t - lag]
        XUnrestricted[i][lag] = y[t - lag]
      }

      // Lags of X (only in unrestricted model)
      for (int lag = 1; lag <= p; lag++) {
        XUnrestricted[i][p + lag] = x[t - lag]
      }
    }

    // Fit restricted model (Y ~ lags of Y)
    double[] betaRestricted = fitOLS(yResponse, XRestricted)
    double rssRestricted = calculateRSS(yResponse, XRestricted, betaRestricted)

    // Fit unrestricted model (Y ~ lags of Y + lags of X)
    double[] betaUnrestricted = fitOLS(yResponse, XUnrestricted)
    double rssUnrestricted = calculateRSS(yResponse, XUnrestricted, betaUnrestricted)

    // Calculate F-statistic
    // F = [(RSS_R - RSS_U) / p] / [RSS_U / (n - 2p - 1)]
    double numerator = (rssRestricted - rssUnrestricted) / p
    double denominator = rssUnrestricted / (nObs - 2 * p - 1)

    if (denominator < 1e-10) {
      throw new IllegalArgumentException("Unrestricted model has perfect fit (RSS ≈ 0)")
    }

    double fStatistic = numerator / denominator

    // Calculate p-value
    int df1 = p
    int df2 = nObs - 2 * p - 1
    FDistribution fDist = new FDistribution(df1, df2)
    double pValue = 1.0 - fDist.cumulativeProbability(fStatistic)

    return new GrangerResult(
      statistic: fStatistic,
      pValue: pValue,
      lags: p,
      df1: df1,
      df2: df2,
      rssRestricted: rssRestricted,
      rssUnrestricted: rssUnrestricted,
      sampleSize: n,
      effectiveSampleSize: nObs
    )
  }

  /**
   * Performs the Granger causality test with List inputs.
   */
  static GrangerResult test(List<? extends Number> x, List<? extends Number> y, Integer maxLag = null) {
    double[] xArray = x.collect { it.doubleValue() } as double[]
    double[] yArray = y.collect { it.doubleValue() } as double[]
    return test(xArray, yArray, maxLag)
  }

  /**
   * Select optimal lag using AIC.
   */
  private static int selectLag(double[] x, double[] y, int n) {
    int maxPossibleLag = Math.min(10, (int) Math.floor((n - 1) / 3.0))
    double bestAIC = Double.POSITIVE_INFINITY
    int bestLag = 1

    for (int p = 1; p <= maxPossibleLag; p++) {
      try {
        GrangerResult result = test(x, y, p)

        // Calculate AIC: AIC = n * log(RSS/n) + 2k
        int k = 2 * p + 1  // Number of parameters in unrestricted model
        double aic = result.effectiveSampleSize * Math.log(result.rssUnrestricted / result.effectiveSampleSize) + 2 * k

        if (aic < bestAIC) {
          bestAIC = aic
          bestLag = p
        }
      } catch (Exception ignored) {
        // Skip this lag if calculation fails
      }
    }

    return bestLag
  }

  /**
   * Fit OLS regression and return coefficients.
   */
  private static double[] fitOLS(double[] y, double[][] X) {
    int n = y.length
    int k = X[0].length

    // Calculate X'X
    double[][] XtX = new double[k][k]
    for (int i = 0; i < k; i++) {
      for (int j = 0; j < k; j++) {
        double sum = 0.0
        for (int m = 0; m < n; m++) {
          sum += X[m][i] * X[m][j]
        }
        XtX[i][j] = sum
      }
    }

    // Calculate X'y
    double[] Xty = new double[k]
    for (int i = 0; i < k; i++) {
      double sum = 0.0
      for (int m = 0; m < n; m++) {
        sum += X[m][i] * y[m]
      }
      Xty[i] = sum
    }

    // Solve for beta
    return solveLinearSystem(XtX, Xty)
  }

  /**
   * Calculate residual sum of squares.
   */
  private static double calculateRSS(double[] y, double[][] X, double[] beta) {
    int n = y.length
    double rss = 0.0

    for (int i = 0; i < n; i++) {
      double fitted = 0.0
      for (int j = 0; j < beta.length; j++) {
        fitted += X[i][j] * beta[j]
      }
      double residual = y[i] - fitted
      rss += residual * residual
    }

    return rss
  }

  /**
   * Solve linear system Ax = b using Gaussian elimination.
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

      // Check for singular matrix
      if (Math.abs(augmented[k][k]) < 1e-14) {
        throw new IllegalArgumentException("Singular matrix - cannot solve linear system")
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
   * Result class for Granger causality test.
   */
  @CompileStatic
  static class GrangerResult {
    /** The F-statistic */
    double statistic

    /** The p-value */
    double pValue

    /** Number of lags used */
    int lags

    /** Degrees of freedom (numerator) */
    int df1

    /** Degrees of freedom (denominator) */
    int df2

    /** RSS for restricted model */
    double rssRestricted

    /** RSS for unrestricted model */
    double rssUnrestricted

    /** Original sample size */
    int sampleSize

    /** Effective sample size (after losing observations to lags) */
    int effectiveSampleSize

    /**
     * Interprets the test result.
     *
     * @param alpha Significance level (default 0.05)
     * @return Interpretation string
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: X Granger-causes Y (F = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: X does not Granger-cause Y (F = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = pValue < alpha ? "X Granger-causes Y" : "X does not Granger-cause Y"

      return String.format(
        "Granger causality test:\\n" +
        "Lags: %d\\n" +
        "F-statistic: %.4f\\n" +
        "p-value: %.4f\\n" +
        "Degrees of freedom: (%d, %d)\\n" +
        "RSS restricted: %.4f\\n" +
        "RSS unrestricted: %.4f\\n" +
        "Sample size: %d (effective: %d)\\n" +
        "Conclusion: %s at %.0f%% significance level",
        lags, statistic, pValue, df1, df2,
        rssRestricted, rssUnrestricted,
        sampleSize, effectiveSampleSize,
        conclusion, alpha * 100
      )
    }

    @Override
    String toString() {
      return """Granger Causality Test
  Lags: ${lags}
  Sample size: ${sampleSize} (effective: ${effectiveSampleSize})
  F-statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}
  Degrees of freedom: (${df1}, ${df2})
  RSS restricted: ${String.format('%.4f', rssRestricted)}
  RSS unrestricted: ${String.format('%.4f', rssUnrestricted)}

  ${interpret()}"""
    }
  }
}
