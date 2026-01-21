package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution

/**
 * The Anderson–Darling test is a statistical test of whether a given sample of data is drawn from a
 * given probability distribution. In its basic form, the test assumes that there are no parameters to
 * be estimated in the distribution being tested, in which case the test and its set of critical values
 * is distribution-free.
 *
 * This implementation focuses on testing normality and uses the modified Anderson-Darling test
 * which adjusts for estimated parameters (mean and standard deviation).
 *
 * The test is more sensitive to deviations in the tails of the distribution compared to the
 * Kolmogorov-Smirnov test.
 *
 * Reference: R's nortest::ad.test()
 */
@CompileStatic
class AndersonDarling {

  /**
   * Performs the Anderson-Darling test for normality.
   *
   * The null hypothesis is that the data follows a normal distribution.
   * The alternative hypothesis is that the data does not follow a normal distribution.
   *
   * @param data The sample data to test
   * @param alpha Significance level (default: 0.05)
   * @return AndersonDarlingResult containing test statistic, adjusted statistic, and p-value
   * @throws IllegalArgumentException if data is null, empty, or has fewer than 3 observations
   */
  static AndersonDarlingResult testNormality(List<? extends Number> data, double alpha = 0.05) {
    validateInput(data)

    int n = data.size()

    // Calculate sample mean and standard deviation
    double[] values = data.collect { it.doubleValue() } as double[]
    double mean = values.sum() / n
    double variance = 0.0
    for (double val : values) {
      variance += (val - mean) * (val - mean)
    }
    variance /= (n - 1)
    double stdDev = Math.sqrt(variance)

    // Standardize and sort the data
    double[] sorted = new double[n]
    for (int i = 0; i < n; i++) {
      sorted[i] = (values[i] - mean) / stdDev
    }
    Arrays.sort(sorted)

    // Calculate the Anderson-Darling test statistic
    double aSquared = calculateTestStatistic(sorted, n)

    // Adjust for estimated parameters (mean and variance)
    // Formula from Stephens (1986): A²* = A² × (1 + 0.75/n + 2.25/n²)
    double adjustedASquared = aSquared * (1.0 + 0.75/n + 2.25/(n*n))

    // Calculate p-value using the adjusted statistic
    double pValue = calculatePValue(adjustedASquared)

    return new AndersonDarlingResult(
      statistic: aSquared,
      adjustedStatistic: adjustedASquared,
      pValue: pValue,
      alpha: alpha,
      sampleSize: n,
      mean: mean,
      stdDev: stdDev
    )
  }

  /**
   * Calculates the Anderson-Darling test statistic.
   *
   * A² = -n - (1/n) * Σ[(2i-1) * (ln(Φ(z_i)) + ln(1 - Φ(z_{n+1-i})))]
   *
   * where z_i are the standardized sorted observations and Φ is the standard normal CDF.
   */
  private static double calculateTestStatistic(double[] sorted, int n) {
    NormalDistribution standardNormal = new NormalDistribution(0, 1)

    double sum = 0.0
    for (int i = 0; i < n; i++) {
      double phi_i = standardNormal.cumulativeProbability(sorted[i])
      double phi_n_minus_i = standardNormal.cumulativeProbability(sorted[n - 1 - i])

      // Avoid log(0) by using a small threshold
      phi_i = Math.max(phi_i, 1e-15)
      phi_n_minus_i = Math.min(phi_n_minus_i, 1.0 - 1e-15)

      double term = (2.0 * (i + 1) - 1.0) * (Math.log(phi_i) + Math.log(1.0 - phi_n_minus_i))
      sum += term
    }

    return -n - sum / n
  }

  /**
   * Calculates the p-value for the adjusted Anderson-Darling statistic.
   *
   * Uses the approximation from R's nortest package, which is based on
   * Marsaglia and Marsaglia (2004).
   */
  private static double calculatePValue(double adjustedASquared) {
    if (adjustedASquared < 0.2) {
      return 1.0 - Math.exp(-13.436 + 101.14 * adjustedASquared - 223.73 * adjustedASquared * adjustedASquared)
    } else if (adjustedASquared < 0.34) {
      return 1.0 - Math.exp(-8.318 + 42.796 * adjustedASquared - 59.938 * adjustedASquared * adjustedASquared)
    } else if (adjustedASquared < 0.6) {
      return Math.exp(0.9177 - 4.279 * adjustedASquared - 1.38 * adjustedASquared * adjustedASquared)
    } else {
      return Math.exp(1.2937 - 5.709 * adjustedASquared + 0.0186 * adjustedASquared * adjustedASquared)
    }
  }

  private static void validateInput(List<? extends Number> data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be empty")
    }
    if (data.size() < 3) {
      throw new IllegalArgumentException("Anderson-Darling test requires at least 3 observations, got ${data.size()}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Result class for the Anderson-Darling normality test.
   */
  @CompileStatic
  static class AndersonDarlingResult {
    /** The raw Anderson-Darling test statistic */
    double statistic

    /** The adjusted test statistic (accounting for estimated parameters) */
    double adjustedStatistic

    /** The p-value for the test */
    double pValue

    /** The significance level used for evaluation */
    double alpha

    /** The sample size */
    int sampleSize

    /** The sample mean */
    double mean

    /** The sample standard deviation */
    double stdDev

    /**
     * Evaluates the test result at the specified significance level.
     *
     * @return "Reject H0: Data does not appear to be normally distributed (p = ...)" if p-value < alpha,
     *         "Fail to reject H0: Data appears to be normally distributed (p = ...)" otherwise
     */
    String evaluate() {
      if (pValue < alpha) {
        return String.format("Reject H0: Data does not appear to be normally distributed (p = %.4f < α = %.2f)",
                             pValue, alpha)
      } else {
        return String.format("Fail to reject H0: Data appears to be normally distributed (p = %.4f >= α = %.2f)",
                             pValue, alpha)
      }
    }

    @Override
    String toString() {
      return """Anderson-Darling Normality Test
  Sample size: ${sampleSize}
  Sample mean: ${String.format("%.4f", mean)}
  Sample SD: ${String.format("%.4f", stdDev)}
  Test statistic (A²): ${String.format("%.4f", statistic)}
  Adjusted statistic (A²*): ${String.format("%.4f", adjustedStatistic)}
  P-value: ${String.format("%.4f", pValue)}

  ${evaluate()}"""
    }
  }
}
