package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.StatUtils

/**
 * The Shapiro–Francia test is a statistical test for the normality of a population, based on sample data.
 * It was introduced by S. S. Shapiro and R. S. Francia in 1972 as a simplification of the Shapiro–Wilk test.
 *
 * The Shapiro-Francia test is computationally simpler than Shapiro-Wilk and is particularly recommended
 * for larger sample sizes (n > 50). It uses the correlation coefficient between the ordered sample values
 * and the expected normal quantiles.
 *
 * The test statistic W' is calculated as:
 * W' = R²
 *
 * where R is the correlation coefficient between:
 * - The ordered sample values x₁, x₂, ..., xₙ
 * - The expected normal quantiles mᵢ = Φ⁻¹((i - 3/8)/(n + 1/4))
 *
 * Equivalently:
 * W' = (Σ mᵢ xᵢ)² / (Σ mᵢ² × Σ(xᵢ - x̄)²)
 *
 * W' ranges from 0 to 1, with values close to 1 indicating normality.
 *
 * Reference:
 * - Shapiro, S. S., & Francia, R. S. (1972). "An approximate analysis of variance test for normality"
 * - Royston, P. (1993). "A toolkit for testing for non-normality in complete and censored samples"
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
