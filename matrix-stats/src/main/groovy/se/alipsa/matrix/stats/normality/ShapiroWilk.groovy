package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.StatUtils

/**
 * The Shapiro–Wilk test tests the null hypothesis that a sample x₁, ..., xₙ came from a
 * normally distributed population.
 *
 * <p>The Shapiro-Wilk test is one of the most powerful normality tests, especially for small sample sizes
 * (n < 50), though it is valid for larger samples as well.</p>
 *
 * <p>The test statistic W is calculated as:</p>
 * <pre>
 * W = (Σ aᵢ xᵢ)² / Σ(xᵢ - x̄)²
 * </pre>
 * <p>where x₁, ..., xₙ are the ordered sample values, x̄ is the sample mean,
 * and aᵢ are weights calculated from the expected values of order statistics.</p>
 *
 * <p>W ranges from 0 to 1, with values close to 1 indicating normality.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]
 * def result = ShapiroWilk.test(data)
 * println "W statistic: ${result.W}"
 * println "p-value: ${result.pValue}"
 * </pre>
 *
 * <p><strong>Note:</strong> This implementation uses an approximation for p-value calculation
 * based on the algorithm described in Royston (1992) for sample sizes 3 ≤ n ≤ 5000.</p>
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
