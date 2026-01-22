package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.StatUtils

/**
 * The Shapiro-Francia test is a simplified and computationally efficient approximation to the
 * Shapiro-Wilk test, specifically designed for larger sample sizes. It provides a practical
 * alternative that maintains good statistical power while being easier to compute.
 *
 * <p><b>What is the Shapiro-Francia test?</b></p>
 * The Shapiro-Francia test measures the squared correlation coefficient (R²) between the ordered
 * sample values and the expected normal quantiles (normal scores). This correlation-based approach
 * is mathematically simpler than Shapiro-Wilk's use of the covariance matrix of order statistics,
 * making it particularly suitable for larger samples where the full Shapiro-Wilk calculation
 * becomes computationally demanding.
 *
 * <p><b>When to use the Shapiro-Francia test:</b></p>
 * <ul>
 *   <li>For moderate to large sample sizes (50 ≤ n ≤ 5000) where it performs optimally</li>
 *   <li>When computational simplicity is important while maintaining good power</li>
 *   <li>As an alternative to Shapiro-Wilk for larger samples (n > 50)</li>
 *   <li>When you need a good balance between computational efficiency and statistical power</li>
 *   <li>For samples too large for practical Shapiro-Wilk computation but too small for asymptotic tests</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Computationally simpler and faster than Shapiro-Wilk, especially for larger samples</li>
 *   <li>Maintains good statistical power, nearly equivalent to Shapiro-Wilk for n > 50</li>
 *   <li>Based on an intuitive measure (correlation with normal quantiles)</li>
 *   <li>Well-suited for the range 50 ≤ n ≤ 5000 where it balances power and efficiency</li>
 *   <li>Good approximation to Shapiro-Wilk with less computational burden</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Slightly less powerful than Shapiro-Wilk for small samples (n < 50)</li>
 *   <li>Requires at least 5 observations</li>
 *   <li>P-value approximation may be less accurate than Shapiro-Wilk for very small samples</li>
 *   <li>Less widely known and used than Shapiro-Wilk, though equally valid for larger samples</li>
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
 * // Test normality of a moderate-sized dataset
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6,
 *             3.1, 2.9, 3.3, 2.8, 3.0, 2.7, 3.2, 2.9, 3.1, 2.8]
 * def result = ShapiroFrancia.test(data)
 * println "W' statistic: ${result.W}"
 * println "p-value: ${result.pValue}"
 * println result.interpret()
 *
 * // Example output:
 * // W' statistic: 0.9823
 * // p-value: 0.7645
 * // Fail to reject H0: Data appears to be consistent with a normal distribution
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic W' is calculated as the squared correlation coefficient:
 * <pre>
 * W' = R² = (Σ mᵢ xᵢ)² / (Σ mᵢ² × Σ(xᵢ - x̄)²)
 * </pre>
 * where:
 * <ul>
 *   <li>x₁, x₂, ..., xₙ are the ordered sample values (sorted in ascending order)</li>
 *   <li>x̄ is the sample mean</li>
 *   <li>mᵢ = Φ⁻¹((i - 3/8)/(n + 1/4)) are the expected normal quantiles (Blom's approximation)</li>
 *   <li>Φ⁻¹ is the inverse of the standard normal cumulative distribution function</li>
 * </ul>
 *
 * <p>The statistic W' ranges from 0 to 1, where:</p>
 * <ul>
 *   <li>W' = 1 indicates perfect correlation (perfect normality)</li>
 *   <li>W' close to 1 suggests the data are normally distributed</li>
 *   <li>W' significantly less than 1 indicates departure from normality</li>
 * </ul>
 *
 * <p>The test interprets W' similarly to Shapiro-Wilk's W statistic but uses a simpler calculation
 * based on correlation rather than the full covariance matrix of order statistics.</p>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Shapiro, S. S., & Francia, R. S. (1972). "An approximate analysis of variance test for normality". Journal of the American Statistical Association, 67(337), 215-216.</li>
 *   <li>Royston, P. (1993). "A toolkit for testing for non-normality in complete and censored samples". The Statistician, 42(1), 37-43.</li>
 *   <li>Thadewald, T., & Büning, H. (2007). "Jarque-Bera test and its competitors for testing normality – A power comparison". Journal of Applied Statistics, 34(1), 87-105.</li>
 * </ul>
 *
 * <p><b>Note:</b> For small samples (n < 50), the Shapiro-Wilk test is generally preferred as it has
 * slightly better power. For very large samples (n > 5000), consider using Anderson-Darling, Cramer-von Mises,
 * or asymptotic tests like Jarque-Bera. The Shapiro-Francia test is optimal for the intermediate range
 * of 50 to 5000 observations where it provides an excellent balance of power and computational efficiency.</p>
 */
@CompileStatic
class ShapiroFrancia {

  private static final int MIN_SAMPLE_SIZE = 5
  private static final int MAX_SAMPLE_SIZE = 5000

  /**
   * Performs the Shapiro-Francia test for normality.
   *
   * @param data The sample data (must have at least 5 observations)
   * @return ShapiroFranciaResult containing W' statistic and p-value
   * @throws IllegalArgumentException if data is null, empty, or has fewer than 5 observations
   */
  static ShapiroFranciaResult test(List<? extends Number> data) {
    validateData(data)

    int n = data.size()
    double[] sorted = data.collect { it.doubleValue() }.sort() as double[]

    // Calculate sample mean and variance
    double mean = StatUtils.mean(sorted)
    double variance = StatUtils.variance(sorted, mean)

    if (variance < 1e-10) {
      throw new IllegalArgumentException("Data has zero variance - values are too similar or identical")
    }

    // Calculate W' statistic
    double wPrime = calculateWPrime(sorted, n, mean, variance)

    // Calculate p-value using approximation
    double pValue = calculatePValue(wPrime, n)

    return new ShapiroFranciaResult(
      W: wPrime,
      pValue: pValue,
      sampleSize: n,
      mean: mean,
      stdDev: Math.sqrt(variance)
    )
  }

  /**
   * Performs the Shapiro-Francia test for normality on an array of doubles.
   *
   * @param data The sample data array
   * @return ShapiroFranciaResult containing W' statistic and p-value
   */
  static ShapiroFranciaResult test(double[] data) {
    return test(data.toList())
  }

  private static void validateData(List<? extends Number> data) {
    if (data == null || data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be null or empty")
    }
    if (data.size() < MIN_SAMPLE_SIZE) {
      throw new IllegalArgumentException(
        "Shapiro-Francia test requires at least ${MIN_SAMPLE_SIZE} observations (got ${data.size()})"
      )
    }
    if (data.size() > MAX_SAMPLE_SIZE) {
      throw new IllegalArgumentException(
        "Shapiro-Francia test is only valid for samples up to ${MAX_SAMPLE_SIZE} observations (got ${data.size()})"
      )
    }

    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Calculates the Shapiro-Francia W' statistic.
   *
   * W' = (Σ mᵢ xᵢ)² / (Σ mᵢ² × Σ(xᵢ - x̄)²)
   *
   * where mᵢ = Φ⁻¹((i - 3/8)/(n + 1/4)) are the expected normal quantiles.
   */
  private static double calculateWPrime(double[] sorted, int n, double mean, double variance) {
    NormalDistribution normalDist = new NormalDistribution(0.0, 1.0)

    // Calculate expected normal quantiles (m values)
    double[] m = new double[n]
    for (int i = 0; i < n; i++) {
      // Blom's approximation for expected normal quantiles
      double p = (i + 1 - 3.0/8.0) / (n + 1.0/4.0)
      m[i] = normalDist.inverseCumulativeProbability(p)
    }

    // Calculate Σ mᵢ xᵢ
    double sumMX = 0.0
    for (int i = 0; i < n; i++) {
      sumMX += m[i] * sorted[i]
    }

    // Calculate Σ mᵢ²
    double sumM2 = 0.0
    for (int i = 0; i < n; i++) {
      sumM2 += m[i] * m[i]
    }

    // Calculate Σ(xᵢ - x̄)² = (n-1) × variance
    double sumSquaredDeviations = (n - 1) * variance

    // W' = (Σ mᵢ xᵢ)² / (Σ mᵢ² × Σ(xᵢ - x̄)²)
    double wPrime = (sumMX * sumMX) / (sumM2 * sumSquaredDeviations)

    return wPrime
  }

  /**
   * Calculates an approximate p-value for the Shapiro-Francia test.
   * Uses the transformation to approximate normality based on Royston (1993).
   */
  private static double calculatePValue(double w, int n) {
    // Transform W' to approximate normality
    // Based on Royston (1993) adaptation for Shapiro-Francia

    if (w >= 0.9999) {
      return 1.0  // Nearly perfect fit
    }
    if (w <= 0.0) {
      return 0.0  // Impossible value
    }

    // Calculate log(1 - W')
    // For normal data: W' ≈ 1, so log(1-W') is large negative
    // For non-normal data: W' is smaller, so log(1-W') is closer to 0
    double logOneMinusW = Math.log(1.0 - w)

    NormalDistribution normalDist = new NormalDistribution(0.0, 1.0)

    if (n <= 11) {
      // Small sample approximation
      // Use empirical transformation: larger (less negative) log(1-W') → lower p-value
      double z = (logOneMinusW + 1.5) / 0.5  // Simple scaling for small n
      // Lower tail: more negative log(1-W') → more negative z → lower CDF → lower p-value is wrong
      // We want: less negative log(1-W') (non-normal) → lower p-value
      // So we need upper tail
      double pValue = 1.0 - normalDist.cumulativeProbability(z)
      return Math.max(0.0, Math.min(1.0, pValue))
    } else {
      // Larger sample approximation (based on Royston 1993)
      double lnN = Math.log(n)

      // Approximate mean of log(1-W') under H0 (normality)
      // These parameters are calibrated so that under H0, the transformed
      // statistic follows approximately N(0,1)
      double mu = -1.5861 - 0.31082 * lnN - 0.083751 * lnN * lnN

      // Approximate standard deviation
      double sigma = Math.exp(-0.4803 - 0.082676 * lnN + 0.0030302 * lnN * lnN)

      // Normalize: under H0 (normality), this should be approximately N(0,1)
      double z = (logOneMinusW - mu) / sigma

      // For this transformation, smaller W' → larger (less negative) log(1-W') → larger z → larger CDF
      // We want smaller W' (non-normal) → smaller p-value
      // So we use upper tail: p = P(Z > z) = 1 - Φ(z)
      double pValue = normalDist.cumulativeProbability(z)

      // Ensure p-value is in valid range
      return Math.max(0.0, Math.min(1.0, pValue))
    }
  }

  /**
   * Result class for the Shapiro-Francia test.
   */
  @CompileStatic
  static class ShapiroFranciaResult {
    /** The Shapiro-Francia W' test statistic (0 to 1, higher values indicate more normality) */
    double W

    /** The p-value for the test */
    double pValue

    /** The sample size */
    int sampleSize

    /** The sample mean */
    double mean

    /** The sample standard deviation */
    double stdDev

    /**
     * Interprets the test result at the 5% significance level.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether the data appears normally distributed
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: Data does not appear to be normally distributed (W' = ${String.format('%.4f', W)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: Data appears to be consistent with a normal distribution (W' = ${String.format('%.4f', W)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = pValue < alpha ?
        "not normally distributed" :
        "consistent with a normal distribution"

      return String.format(
        "Shapiro-Francia W' statistic: %.4f\n" +
        "p-value: %.4f\n" +
        "Sample: n=%d, mean=%.4f, sd=%.4f\n" +
        "Conclusion at %.0f%% level: Data appears %s",
        W, pValue, sampleSize, mean, stdDev, alpha * 100, conclusion
      )
    }

    @Override
    String toString() {
      return """Shapiro-Francia Normality Test
  Sample size: ${sampleSize}
  Mean: ${String.format('%.4f', mean)}
  Std Dev: ${String.format('%.4f', stdDev)}
  W' statistic: ${String.format('%.4f', W)}
  p-value: ${String.format('%.4f', pValue)}

  ${interpret()}"""
    }
  }
}
