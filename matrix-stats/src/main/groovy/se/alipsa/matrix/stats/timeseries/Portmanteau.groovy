package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/**
 * A portmanteau test is a type of statistical hypothesis test in which the null hypothesis is well specified,
 * but the alternative hypothesis is more loosely specified.
 * Tests constructed in this context can have the property of being at least moderately powerful against a
 * wide range of departures from the null hypothesis. Thus, in applied statistics, a portmanteau test
 * provides a reasonable way of proceeding as a general check of a model's match to a dataset where there
 * are many different ways in which the model may depart from the underlying data generating process.
 * Use of such tests avoids having to be very specific about the particular type of departure being tested.
 *
 * This class implements the Ljung-Box test, which is the most commonly used portmanteau test.
 *
 * Reference: Ljung, G. M. and Box, G. E. P. (1978). "On a Measure of Lack of Fit in Time Series Models"
 */
@CompileStatic
class Portmanteau {

  /**
   * The Ljung–Box test (named for Greta M. Ljung and George E. P. Box) is a type of statistical test of whether
   * any of a group of autocorrelations of a time series are different from zero.
   * Instead of testing randomness at each distinct lag, it tests the "overall" randomness based on a number of
   * lags, and is therefore a portmanteau test.
   *
   * The test statistic is:
   * Q = n(n+2) * Σ[ρ²ₖ/(n-k)] for k=1 to h
   *
   * where:
   * - n is the sample size
   * - h is the number of lags being tested
   * - ρₖ is the sample autocorrelation at lag k
   *
   * Under the null hypothesis of independence, Q follows a chi-squared distribution with h degrees of freedom.
   *
   * @param data The time series data
   * @param lags The number of lags to test (default: min(10, n/5))
   * @param fitdf The degrees of freedom used in fitting the model (e.g., p+q for ARMA(p,q)). Default is 0 for raw data.
   * @return LjungBoxResult containing test statistic, p-value, and conclusion
   */
  static LjungBoxResult ljungBox(List<? extends Number> data, Integer lags = null, int fitdf = 0) {
    validateInput(data, lags, fitdf)

    int n = data.size()
    double[] y = data.collect { it.doubleValue() } as double[]

    // Auto-select lags if not specified
    // Common rule: min(10, n/5) or sqrt(n)
    int h = lags ?: Math.min(10, (int)(n / 5))
    h = Math.max(1, h)

    if (h >= n) {
      throw new IllegalArgumentException("Number of lags (${h}) must be less than sample size (${n})")
    }

    // Calculate mean
    double mean = 0.0
    for (double val : y) {
      mean += val
    }
    mean /= n

    // Calculate variance
    double variance = 0.0
    for (double val : y) {
      double dev = val - mean
      variance += dev * dev
    }
    variance /= n

    if (variance < 1e-10) {
      throw new IllegalArgumentException("Data has no variation (constant or near-constant series)")
    }

    // Calculate autocorrelations for lags 1 to h
    double[] autocorrelations = new double[h]
    for (int k = 1; k <= h; k++) {
      double autocovariance = 0.0
      for (int t = k; t < n; t++) {
        autocovariance += (y[t] - mean) * (y[t - k] - mean)
      }
      autocovariance /= n
      autocorrelations[k - 1] = autocovariance / variance
    }

    // Calculate Ljung-Box statistic
    // Q = n(n+2) * Σ[ρ²ₖ/(n-k)] for k=1 to h
    double qStatistic = 0.0
    for (int k = 1; k <= h; k++) {
      double rho = autocorrelations[k - 1]
      qStatistic += (rho * rho) / (n - k)
    }
    qStatistic *= n * (n + 2)

    // Degrees of freedom for chi-squared test
    // If the data is from a fitted model with fitdf parameters, subtract those
    int degreesOfFreedom = h - fitdf
    if (degreesOfFreedom <= 0) {
      throw new IllegalArgumentException(
        "Degrees of freedom must be positive. " +
        "Number of lags (${h}) must be greater than fit df (${fitdf})"
      )
    }

    // Calculate p-value using chi-squared distribution
    ChiSquaredDistribution chiSquared = new ChiSquaredDistribution(degreesOfFreedom)
    double pValue = 1.0 - chiSquared.cumulativeProbability(qStatistic)

    return new LjungBoxResult(
      statistic: qStatistic,
      lags: h,
      fitdf: fitdf,
      degreesOfFreedom: degreesOfFreedom,
      sampleSize: n,
      pValue: pValue,
      autocorrelations: autocorrelations.toList()
    )
  }

  private static void validateInput(List<? extends Number> data, Integer lags, int fitdf) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be empty")
    }
    if (data.size() < 10) {
      throw new IllegalArgumentException("Ljung-Box test requires at least 10 observations, got ${data.size()}")
    }
    if (lags != null && lags < 1) {
      throw new IllegalArgumentException("Number of lags must be positive, got ${lags}")
    }
    if (fitdf < 0) {
      throw new IllegalArgumentException("Fit degrees of freedom must be non-negative, got ${fitdf}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Result class for the Ljung-Box test.
   */
  @CompileStatic
  static class LjungBoxResult {
    /** The Ljung-Box Q test statistic */
    double statistic

    /** The number of lags tested */
    int lags

    /** The degrees of freedom used in fitting the model (0 for raw data) */
    int fitdf

    /** The degrees of freedom for the chi-squared test */
    int degreesOfFreedom

    /** The sample size */
    int sampleSize

    /** The p-value from the chi-squared distribution */
    double pValue

    /** The sample autocorrelations at each lag */
    List<Double> autocorrelations

    /**
     * Interprets the Ljung-Box test result at the 5% significance level.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether the series appears to be independent
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: Series shows significant autocorrelation (p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No significant autocorrelation detected (p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A description of the test result with interpretation
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = pValue < alpha ?
        "significant autocorrelation present" :
        "no significant autocorrelation"

      StringBuilder sb = new StringBuilder()
      sb.append(String.format("Ljung-Box Q statistic: %.4f (df: %d, p-value: %.4f)\n", statistic, degreesOfFreedom, pValue))
      sb.append(String.format("Lags tested: %d\n", lags))
      if (fitdf > 0) {
        sb.append(String.format("Model fit df: %d\n", fitdf))
      }
      sb.append(String.format("Conclusion: %s at %.0f%% significance level", conclusion, alpha * 100))

      return sb.toString()
    }

    /**
     * Gets the autocorrelation at a specific lag.
     *
     * @param lag The lag (1-indexed)
     * @return The autocorrelation coefficient
     */
    double getAutocorrelation(int lag) {
      if (lag < 1 || lag > lags) {
        throw new IllegalArgumentException("Lag must be between 1 and ${lags}, got ${lag}")
      }
      return autocorrelations[lag - 1]
    }

    @Override
    String toString() {
      StringBuilder sb = new StringBuilder()
      sb.append("Ljung-Box Portmanteau Test\n")
      sb.append("Sample size: ${sampleSize}\n")
      sb.append("Lags tested: ${lags}\n")
      if (fitdf > 0) {
        sb.append("Model fit df: ${fitdf}\n")
      }
      sb.append("Q statistic: ${String.format('%.4f', statistic)}\n")
      sb.append("Degrees of freedom: ${degreesOfFreedom}\n")
      sb.append("p-value: ${String.format('%.4f', pValue)}\n")
      sb.append("\n${interpret()}\n")
      sb.append("\nAutocorrelations:\n")
      for (int i = 0; i < Math.min(lags, 10); i++) {
        sb.append(String.format("  Lag %2d: %7.4f\n", i + 1, autocorrelations[i]))
      }
      if (lags > 10) {
        sb.append("  ... (${lags - 10} more lags)\n")
      }

      return sb.toString()
    }
  }
}
