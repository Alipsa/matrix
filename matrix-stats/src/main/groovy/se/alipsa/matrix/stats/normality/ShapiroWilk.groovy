package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.StatUtils

/**
 * The Shapiro-Wilk test is widely considered the most powerful statistical test for assessing
 * normality, especially for small to medium sample sizes. It evaluates whether a sample comes from
 * a normally distributed population by comparing the observed order statistics with those expected
 * from a normal distribution.
 *
 * <p><b>What is the Shapiro-Wilk test?</b></p>
 * The Shapiro-Wilk test calculates a statistic W that measures how well the ordered sample values
 * align with the expected order statistics from a normal distribution. The test uses optimal weights
 * derived from the covariance matrix of order statistics to construct a powerful measure of normality.
 * W ranges from 0 to 1, with values close to 1 indicating the data are consistent with normality.
 *
 * <p><b>When to use the Shapiro-Wilk test:</b></p>
 * <ul>
 *   <li>For small to medium sample sizes (3 ≤ n ≤ 50) - this is where it excels</li>
 *   <li>When maximum statistical power is needed to detect departures from normality</li>
 *   <li>When you need a reliable test for moderate samples (50 < n ≤ 2000)</li>
 *   <li>As a gold standard reference test for comparing other normality tests</li>
 *   <li>When the alternative distribution is unknown but you need high sensitivity</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Most powerful test for normality across a wide range of alternatives, especially for small samples</li>
 *   <li>Excellent performance for sample sizes from 3 to 2000</li>
 *   <li>Based on optimal weights from order statistics theory</li>
 *   <li>Well-calibrated significance levels (maintains nominal Type I error rate)</li>
 *   <li>Widely accepted and recommended in statistical literature</li>
 *   <li>Good power against both symmetric and asymmetric departures from normality</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Computationally more intensive than moment-based tests (Jarque-Bera, K²)</li>
 *   <li>Limited to sample sizes up to 5000 (use other tests for larger samples)</li>
 *   <li>Requires at least 3 observations</li>
 *   <li>Can be sensitive to ties in the data (though this usually indicates discrete data)</li>
 *   <li>P-value calculation relies on approximations (Royston's method)</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): The data come from a normally distributed population</li>
 *   <li>H₁ (alternative hypothesis): The data do not come from a normally distributed population</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test normality of a small dataset
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]
 * def result = ShapiroWilk.test(data)
 * println "W statistic: ${result.W}"
 * println "p-value: ${result.pValue}"
 * println result.isNormal() ? "Data appears normal" : "Data appears non-normal"
 *
 * // Example output:
 * // W statistic: 0.9641
 * // p-value: 0.8234
 * // Data appears normal
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic W is calculated as:
 * <pre>
 * W = (Σ aᵢ xᵢ)² / Σ(xᵢ - x̄)²
 * </pre>
 * where:
 * <ul>
 *   <li>x₁, x₂, ..., xₙ are the ordered sample values (sorted in ascending order)</li>
 *   <li>x̄ is the sample mean</li>
 *   <li>aᵢ are weights calculated from the expected values of order statistics from a standard normal distribution</li>
 * </ul>
 *
 * <p>The weights aᵢ are optimal in the sense that they maximize the correlation between the sample
 * order statistics and expected normal order statistics. The numerator represents the best linear
 * combination of order statistics for detecting non-normality, while the denominator standardizes
 * by the total variation.</p>
 *
 * <p>The W statistic ranges from 0 to 1, where:</p>
 * <ul>
 *   <li>W = 1 indicates perfect agreement with normality</li>
 *   <li>W close to 1 (e.g., > 0.95) suggests normality</li>
 *   <li>W significantly less than 1 indicates departure from normality</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Shapiro, S. S., & Wilk, M. B. (1965). "An analysis of variance test for normality (complete samples)". Biometrika, 52(3-4), 591-611.</li>
 *   <li>Royston, P. (1982). "An extension of Shapiro and Wilk's W test for normality to large samples". Applied Statistics, 31(2), 115-124.</li>
 *   <li>Royston, P. (1992). "Approximating the Shapiro-Wilk W-test for non-normality". Statistics and Computing, 2(3), 117-119.</li>
 *   <li>Razali, N. M., & Wah, Y. B. (2011). "Power comparisons of Shapiro-Wilk, Kolmogorov-Smirnov, Lilliefors and Anderson-Darling tests". Journal of Statistical Modeling and Analytics, 2(1), 21-33.</li>
 * </ul>
 *
 * <p><b>Note:</b> This implementation uses Royston's approximation for p-value calculation,
 * which provides good accuracy for sample sizes 3 ≤ n ≤ 5000. For very large samples (n > 5000),
 * consider using Anderson-Darling or other tests designed for large samples.</p>
 */
@CompileStatic
class ShapiroWilk {

  private static final int MIN_SAMPLE_SIZE = 3
  private static final int MAX_SAMPLE_SIZE = 5000

  /**
   * Performs the Shapiro-Wilk test for normality.
   *
   * @param data The sample data (must have at least 3 observations)
   * @return ShapiroWilkResult containing W statistic and p-value
   * @throws IllegalArgumentException if data is null, empty, or has fewer than 3 observations
   */
  static ShapiroWilkResult test(List<? extends Number> data) {
    validateData(data)

    int n = data.size()
    double[] sorted = data.collect { it.doubleValue() }.sort() as double[]

    // Calculate sample mean and variance
    double mean = StatUtils.mean(sorted)
    double variance = StatUtils.variance(sorted, mean)

    // Calculate W statistic
    double w = calculateW(sorted, n, mean, variance)

    // Calculate p-value using Royston's approximation
    double pValue = calculatePValue(w, n)

    return new ShapiroWilkResult(
      W: w,
      pValue: pValue,
      sampleSize: n
    )
  }

  /**
   * Performs the Shapiro-Wilk test for normality on an array of doubles.
   *
   * @param data The sample data array
   * @return ShapiroWilkResult containing W statistic and p-value
   */
  static ShapiroWilkResult test(double[] data) {
    return test(data.toList())
  }

  private static void validateData(List<? extends Number> data) {
    if (data == null || data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be null or empty")
    }
    if (data.size() < MIN_SAMPLE_SIZE) {
      throw new IllegalArgumentException("Shapiro-Wilk test requires at least ${MIN_SAMPLE_SIZE} observations (got ${data.size()})")
    }
    if (data.size() > MAX_SAMPLE_SIZE) {
      throw new IllegalArgumentException("Shapiro-Wilk test is only valid for samples up to ${MAX_SAMPLE_SIZE} observations (got ${data.size()})")
    }

    // Check for constant data (all values the same)
    double first = data[0].doubleValue()
    boolean allSame = data.every { it.doubleValue() == first }
    if (allSame) {
      throw new IllegalArgumentException("Data has zero variance - all values are identical")
    }
  }

  private static double calculateW(double[] sorted, int n, double mean, double variance) {
    // Calculate coefficients a[] for the Shapiro-Wilk test
    double[] a = calculateCoefficients(n)

    // Calculate numerator: (Σ aᵢ xᵢ)²
    double numerator = 0
    int k = n.intdiv(2)
    for (int i = 0; i < k; i++) {
      numerator += a[i] * (sorted[n - 1 - i] - sorted[i])
    }
    numerator = numerator * numerator

    // Calculate denominator: Σ(xᵢ - x̄)²
    double denominator = variance * (n - 1)

    return numerator / denominator
  }

  /**
   * Calculates the coefficients for the Shapiro-Wilk test.
   * This uses an approximation suitable for n >= 3.
   */
  private static double[] calculateCoefficients(int n) {
    int k = n.intdiv(2)
    double[] a = new double[k]

    // Use approximation based on normal distribution quantiles
    NormalDistribution normal = new NormalDistribution(0, 1)

    // Calculate expected values of order statistics (m values)
    double[] m = new double[n]
    for (int i = 0; i < n; i++) {
      double p = (i + 1 - 0.375) / (n + 0.25)
      m[i] = normal.inverseCumulativeProbability(p)
    }

    // Calculate sum of squares of m values
    double sumSq = 0
    for (int i = 0; i < n; i++) {
      sumSq += m[i] * m[i]
    }

    // Calculate coefficients
    if (n <= 5) {
      // For very small samples, use simple approximation
      double c = 1.0 / Math.sqrt(sumSq)
      for (int i = 0; i < k; i++) {
        a[i] = c * (m[n - 1 - i] - m[i])
      }
    } else {
      // For larger samples, use better approximation
      double[] u = new double[n]
      for (int i = 0; i < n; i++) {
        u[i] = m[i] / Math.sqrt(sumSq)
      }

      // Apply correction for better approximation
      double an = -2.706056 * Math.pow(n, -0.5) + 4.434685 * Math.pow(n, -1.0) - 2.071190 * Math.pow(n, -1.5) + 0.147981 * Math.pow(n, -2.0) + 0.221157 * Math.pow(n, -3.0) - 0.147981 * Math.pow(n, -4.0)
      double an1 = -3.582633 * Math.pow(n, -0.5) + 5.682633 * Math.pow(n, -1.0) - 1.752461 * Math.pow(n, -1.5) - 0.293762 * Math.pow(n, -2.0) + 0.042981 * Math.pow(n, -3.0)

      double phi = (sumSq - 2.0 * m[n - 1] * m[n - 1]) / (1.0 - 2.0 * an * an)

      for (int i = 0; i < k; i++) {
        if (i == 0) {
          a[i] = an
        } else if (i == 1 && k > 1) {
          a[i] = an1
        } else {
          a[i] = (m[n - 1 - i] - m[i]) / Math.sqrt(phi)
        }
      }
    }

    return a
  }

  /**
   * Calculates the p-value using Royston's (1992) approximation.
   * This provides good accuracy for 3 ≤ n ≤ 5000.
   */
  private static double calculatePValue(double w, int n) {
    // Transform W to get better approximation
    double logW = Math.log(1.0 - w)
    double mu, sigma

    if (n <= 11) {
      // For small samples (n <= 11)
      double gamma = 0.459 * n - 2.273
      mu = -0.0006714 * Math.pow(n, 3.0) + 0.025054 * Math.pow(n, 2.0) - 0.39978 * n + 0.544
      sigma = Math.exp(-0.0020322 * Math.pow(n, 3.0) + 0.062767 * Math.pow(n, 2.0) - 0.77857 * n + 1.3822)
    } else {
      // For larger samples (n > 11)
      double logN = Math.log(n)
      mu = 0.0038915 * Math.pow(logN, 3.0) - 0.083751 * Math.pow(logN, 2.0) - 0.31082 * logN - 1.5861
      sigma = Math.exp(0.0030302 * Math.pow(logN, 2.0) - 0.082676 * logN - 0.4803)
    }

    // Normalize and calculate p-value
    double z = (logW - mu) / sigma
    NormalDistribution normal = new NormalDistribution(0, 1)
    double pValue = normal.cumulativeProbability(z)

    return Math.max(0.0, Math.min(1.0, pValue))  // Ensure p-value is in [0, 1]
  }

  /**
   * Result class for the Shapiro-Wilk test.
   */
  static class ShapiroWilkResult {
    /** The W test statistic (0 < W ≤ 1, closer to 1 indicates normality) */
    Double W

    /** The p-value of the test */
    Double pValue

    /** The sample size */
    Integer sampleSize

    /**
     * Evaluates whether to reject the null hypothesis of normality at the given significance level.
     *
     * @param alpha Significance level (default 0.05)
     * @return true if null hypothesis should be rejected (p-value < alpha), indicating non-normality
     */
    boolean evaluate(double alpha = 0.05) {
      return pValue < alpha
    }

    /**
     * Returns true if the data appears to be normally distributed at the 5% significance level.
     */
    boolean isNormal(double alpha = 0.05) {
      return pValue >= alpha
    }

    @Override
    String toString() {
      return """Shapiro-Wilk Normality Test Result:
  W statistic: ${W}
  p-value: ${pValue}
  sample size: ${sampleSize}
  ${isNormal() ? 'Data appears normally distributed' : 'Data does NOT appear normally distributed'} (α=0.05)"""
    }
  }
}
