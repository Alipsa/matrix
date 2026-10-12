package se.alipsa.matrix.stats.normality

import se.alipsa.matrix.stats.distribution.NormalDistribution
import se.alipsa.matrix.stats.util.NumericConversion

import se.alipsa.matrix.stats.util.NumericTolerance

/**
 * Cramér-von Mises test for normality.
 *
 * The Cramér-von Mises test is a goodness-of-fit test that measures the discrepancy between
 * the empirical distribution function and the hypothesized theoretical distribution.
 * It is generally more powerful than the Kolmogorov-Smirnov test, especially for detecting
 * deviations in the tails of the distribution.
 *
 * The test statistic is:
 * W² = (1/12n) + Σ[(2i-1)/(2n) - Φ((xᵢ - μ̂)/σ̂)]²
 *
 * where Φ is the standard normal CDF and μ̂, σ̂ are estimated from the data.
 *
 * The modified statistic W²* = W² × (1 + 0.5/n) is used when parameters are estimated.
 *
 * Reference:
 * - Cramér, H. (1928). "On the composition of elementary errors"
 * - von Mises, R. (1928). "Wahrscheinlichkeit, Statistik und Wahrheit"
 * - Anderson, T.W. (1962). "On the Distribution of the Two-Sample Cramer-von Mises Criterion"
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class CramerVonMises {

  /**
   * Performs the Cramér-von Mises test for normality.
   *
   * @param data The sample data
   * @param alpha The significance level (default: 0.05)
   * @return CramerVonMisesResult containing test statistic, p-value, and conclusion
   */
  static CramerVonMisesResult testNormality(List<? extends Number> data, Number alpha = 0.05) {
    BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
    validateInput(data)

    int n = data.size()

    // Convert to array and calculate sample mean and standard deviation
    double[] values = data*.doubleValue() as double[]

    double mean = 0.0
    for (double val : values) {
      mean += val
    }
    mean /= n

    double variance = 0.0
    for (double val : values) {
      double dev = val - mean
      variance += dev * dev
    }
    variance /= (n - 1)  // Use sample variance
    double stdDev = Math.sqrt(variance)

    if (NumericTolerance.isNegligible(stdDev, NumericTolerance.maxMagnitude(values))) {
      throw new IllegalArgumentException('Data has no variation (constant or near-constant values)')
    }

    // Sort the data
    double[] sorted = values.clone()
    Arrays.sort(sorted)

    // Calculate the Cramér-von Mises statistic
    NormalDistribution normalDist = new NormalDistribution(0.0, 1.0)

    double wSquared = 1.0 / (12.0 * n)  // Initial term

    for (int i = 0; i < n; i++) {
      // Standardize the observation
      double z = (sorted[i] - mean) / stdDev

      // Calculate the theoretical CDF value
      double theoreticalCDF = normalDist.cumulativeProbability(z)

      // Calculate the expected CDF value for this position
      double empiricalMid = (2.0 * (i + 1) - 1.0) / (2.0 * n)

      // Add the squared difference
      double diff = empiricalMid - theoreticalCDF
      wSquared += diff * diff
    }

    // Apply modification for estimated parameters
    // W²* = W² × (1 + 0.5/n)
    double modifiedWSquared = wSquared * (1.0 + 0.5 / n)

    // Calculate p-value and critical value
    double pValue = calculatePValue(modifiedWSquared)
    double criticalValue = getCriticalValue(alphaValue as double)

    new CramerVonMisesResult(
      statistic: BigDecimal.valueOf(modifiedWSquared),
      rawStatistic: BigDecimal.valueOf(wSquared),
      sampleSize: n,
      mean: BigDecimal.valueOf(mean),
      stdDev: BigDecimal.valueOf(stdDev),
      alpha: alphaValue,
      pValue: BigDecimal.valueOf(pValue),
      criticalValue: BigDecimal.valueOf(criticalValue)
    )
  }

  /**
   * Calculates an approximate p-value for the Cramér-von Mises test.
   * Uses the approximation from Stephens (1974).
   */
  private static double calculatePValue(double wSquared) {
    double w = wSquared
    if (w < 0.0275d) {
      return 1 - Math.exp(-13.953d + 775.5d * w - 12542.61d * w * w)
    }
    if (w < 0.051d) {
      return 1 - Math.exp(-5.903d + 179.546d * w - 1515.29d * w * w)
    }
    if (w < 0.092d) {
      return Math.exp(0.886d - 31.62d * w + 10.897d * w * w)
    }
    if (w < 1.1d) {
      return Math.exp(1.111d - 34.242d * w + 12.832d * w * w)
    }
    7.37e-10d
  }

  /**
   * Gets the critical value for the Cramér-von Mises test at a given significance level.
   * These are for the modified statistic W²* when parameters are estimated from the data.
   * Values from Stephens (1974).
   */
  private static double getCriticalValue(double alpha) {
    if (alpha >= 0.15d) {
      return 0.091d
    }
    if (alpha >= 0.10d) {
      return 0.104d
    }
    if (alpha >= 0.05d) {
      return 0.126d
    }
    if (alpha >= 0.025d) {
      return 0.148d
    }
    0.178d
  }

  private static void validateInput(List<? extends Number> data) {
    if (data == null) {
      throw new IllegalArgumentException('Data cannot be null')
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException('Data cannot be empty')
    }
    if (data.size() < 3) {
      throw new IllegalArgumentException("Cramér-von Mises test requires at least 3 observations, got ${data.size()}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException('Data contains null values')
      }
    }
  }

  /**
   * Result class for the Cramér-von Mises test.
   */
  static class CramerVonMisesResult {
    /** The modified Cramér-von Mises test statistic W²* */
    BigDecimal statistic

    /** The raw W² statistic before modification */
    BigDecimal rawStatistic

    /** The sample size */
    int sampleSize

    /** The sample mean */
    BigDecimal mean

    /** The sample standard deviation */
    BigDecimal stdDev

    /** The significance level */
    BigDecimal alpha

    /** The approximate p-value */
    BigDecimal pValue

    /** The critical value at the specified alpha level */
    BigDecimal criticalValue

    /**
     * Interprets the Cramér-von Mises test result.
     *
     * @return A string describing whether the data appears normally distributed
     */
    String interpret() {
      if (pValue < alpha) {
        return "Reject H0: Data does not appear to be normally distributed (p = ${String.format('%.4f', pValue)})"
      }
      return "Fail to reject H0: Data appears to be consistent with a normal distribution (p = ${String.format('%.4f', pValue)})"
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate() {
      String conclusion = pValue < alpha ?
        'not normally distributed' :
        'consistent with a normal distribution'

      String.format(
        'Cramér-von Mises W²*: %.4f (critical value: %.3f at %.0f%% level)\n' +
        'p-value: %.4f\n' +
        'Sample: n=%d, mean=%.4f, sd=%.4f\n' +
        'Conclusion: Data appears %s',
        statistic, criticalValue, alpha * 100, pValue,
        sampleSize, mean, stdDev, conclusion
      )
    }

    @Override
    String toString() {
      """Cramér-von Mises Normality Test
  Sample size: ${sampleSize}
  Mean: ${String.format('%.4f', mean)}
  Std Dev: ${String.format('%.4f', stdDev)}
  W²* statistic: ${String.format('%.4f', statistic)}
  Critical value (${String.format('%.0f', alpha * 100)}%): ${String.format('%.3f', criticalValue)}
  p-value: ${String.format('%.4f', pValue)}

  ${interpret()}"""
    }
  }
}
