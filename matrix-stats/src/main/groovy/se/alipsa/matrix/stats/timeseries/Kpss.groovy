package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic

/**
 * Kwiatkowski–Phillips–Schmidt–Shin (KPSS) tests are used for testing a null hypothesis that an observable
 * time series is stationary around a deterministic trend (i.e. trend-stationary) against the alternative
 * of a unit root.
 *
 * Contrary to most unit root tests (like ADF), the presence of a unit root is not the null hypothesis but the alternative.
 * Additionally, in the KPSS test, the absence of a unit root is not a proof of stationarity but, by design,
 * of trend-stationarity.
 *
 * The KPSS test statistic is:
 * KPSS = (1/T²) * Σ(S_t)² / s²(l)
 *
 * where S_t is the partial sum of residuals and s²(l) is the long-run variance.
 *
 * Reference: R's tseries::kpss.test()
 */
@CompileStatic
class Kpss {

  /**
   * Performs the KPSS test for stationarity.
   *
   * @param data The time series data
   * @param type The type of test: "level" (tests for level stationarity) or "trend" (tests for trend stationarity)
   * @param lags The number of lags for long-run variance estimation (default: auto-selected based on sample size)
   * @return KpssResult containing test statistic and conclusion
   */
  static KpssResult test(List<? extends Number> data, String type = "level", Integer lags = null) {
    validateInput(data, type)

    int n = data.size()
    double[] y = data.collect { it.doubleValue() } as double[]

    // Auto-select lags if not specified
    // Rule of thumb: l = floor(4 * (T/100)^(1/4))
    int l = lags ?: Math.floor(4.0 * Math.pow(n / 100.0, 0.25)) as int
    l = Math.max(1, Math.min(l, n / 3 as int))  // Ensure reasonable bounds

    // Detrend the data
    double[] residuals = detrend(y, type, n)

    // Calculate partial sums of residuals
    double[] partialSums = new double[n]
    partialSums[0] = residuals[0]
    for (int i = 1; i < n; i++) {
      partialSums[i] = partialSums[i - 1] + residuals[i]
    }

    // Calculate sum of squared partial sums
    double sumOfSquaredPartialSums = 0.0
    for (int i = 0; i < n; i++) {
      sumOfSquaredPartialSums += partialSums[i] * partialSums[i]
    }

    // Estimate long-run variance using Newey-West estimator
    double longRunVariance = estimateLongRunVariance(residuals, l, n)

    // Calculate KPSS statistic
    double kpssStatistic = sumOfSquaredPartialSums / (n * n * longRunVariance)

    // Get critical value
    double criticalValue = getCriticalValue(type)

    return new KpssResult(
      statistic: kpssStatistic,
      type: type,
      lags: l,
      sampleSize: n,
      criticalValue: criticalValue
    )
  }

  /**
   * Detrends the data by removing mean (level) or linear trend.
   */
  private static double[] detrend(double[] y, String type, int n) {
    double[] residuals = new double[n]

    if (type == "level") {
      // Remove mean
      double mean = 0.0
      for (double val : y) {
        mean += val
      }
      mean /= n

      for (int i = 0; i < n; i++) {
        residuals[i] = y[i] - mean
      }
    } else { // trend
      // Remove linear trend using OLS
      // y = a + b*t + residual
      double sumT = 0.0
      double sumY = 0.0
      double sumTY = 0.0
      double sumT2 = 0.0

      for (int i = 0; i < n; i++) {
        double t = i + 1.0
        sumT += t
        sumY += y[i]
        sumTY += t * y[i]
        sumT2 += t * t
      }

      double slope = (n * sumTY - sumT * sumY) / (n * sumT2 - sumT * sumT)
      double intercept = (sumY - slope * sumT) / n

      for (int i = 0; i < n; i++) {
        double t = i + 1.0
        residuals[i] = y[i] - (intercept + slope * t)
      }
    }

    return residuals
  }

  /**
   * Estimates long-run variance using the Newey-West estimator.
   * s²(l) = (1/T) * Σ e_t² + 2 * Σ[w(s,l) * (1/T) * Σ e_t * e_{t-s}]
   */
  private static double estimateLongRunVariance(double[] residuals, int lags, int n) {
    // Calculate variance (s = 0 term)
    double variance = 0.0
    for (double r : residuals) {
      variance += r * r
    }
    variance /= n

    // Add weighted autocovariances
    for (int s = 1; s <= lags; s++) {
      double autocovariance = 0.0
      for (int t = s; t < n; t++) {
        autocovariance += residuals[t] * residuals[t - s]
      }
      autocovariance /= n

      // Bartlett kernel weight: w(s,l) = 1 - s/(l+1)
      double weight = 1.0 - s / (lags + 1.0)

      variance += 2.0 * weight * autocovariance
    }

    return variance
  }

  /**
   * Gets approximate critical values for the KPSS test at the 5% significance level.
   * Values from Kwiatkowski et al. (1992).
   */
  private static double getCriticalValue(String type) {
    if (type == "level") {
      return 0.463  // 5% critical value for level stationarity
    } else { // trend
      return 0.146  // 5% critical value for trend stationarity
    }
  }

  private static void validateInput(List<? extends Number> data, String type) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be empty")
    }
    if (data.size() < 10) {
      throw new IllegalArgumentException("KPSS test requires at least 10 observations, got ${data.size()}")
    }
    if (!(type in ['level', 'trend'])) {
      throw new IllegalArgumentException("Type must be 'level' or 'trend', got: ${type}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Result class for the KPSS test.
   */
  @CompileStatic
  static class KpssResult {
    /** The KPSS test statistic */
    double statistic

    /** The type of test performed */
    String type

    /** The number of lags used for long-run variance estimation */
    int lags

    /** The sample size */
    int sampleSize

    /** The approximate 5% critical value */
    double criticalValue

    /**
     * Interprets the KPSS test result.
     *
     * @return A string describing whether the series appears stationary
     */
    String interpret() {
      if (statistic > criticalValue) {
        return "Reject H0: Series appears to have a unit root (non-stationary)"
      } else {
        return "Fail to reject H0: Series appears to be ${type}-stationary"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A description of the test result with interpretation
     */
    String evaluate() {
      String conclusion = statistic > criticalValue ?
        "non-stationary (unit root present)" :
        "${type}-stationary"

      return String.format("KPSS statistic: %.4f (critical value: %.3f at 5%% level)\nLags used: %d\nConclusion: Series appears %s",
                           statistic, criticalValue, lags, conclusion)
    }

    @Override
    String toString() {
      return """KPSS Stationarity Test
  Type: ${type}
  Sample size: ${sampleSize}
  Lags: ${lags}
  KPSS statistic: ${String.format("%.4f", statistic)}
  Critical value (5%%): ${String.format("%.3f", criticalValue)}

  ${interpret()}"""
    }
  }
}
