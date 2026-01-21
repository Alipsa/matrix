package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution

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
@CompileStatic
class CramerVonMises {

  /**
   * Performs the Cramér-von Mises test for normality.
   *
   * @param data The sample data
   * @param alpha The significance level (default: 0.05)
   * @return CramerVonMisesResult containing test statistic, p-value, and conclusion
   */
  static CramerVonMisesResult testNormality(List<? extends Number> data, double alpha = 0.05) {
    validateInput(data, alpha)

    int n = data.size()

    // Convert to array and calculate sample mean and standard deviation
    double[] values = data.collect { it.doubleValue() } as double[]

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

    if (stdDev < 1e-10) {
      throw new IllegalArgumentException("Data has no variation (constant or near-constant values)")
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
    double criticalValue = getCriticalValue(alpha)

    return new CramerVonMisesResult(
      statistic: modifiedWSquared,
      rawStatistic: wSquared,
      sampleSize: n,
      mean: mean,
      stdDev: stdDev,
      alpha: alpha,
      pValue: pValue,
      criticalValue: criticalValue
    )
  }

  /**
   * Calculates an approximate p-value for the Cramér-von Mises test.
   * Uses the approximation from Stephens (1974).
   */
  private static double calculatePValue(double wSquared) {
    // For W²* (modified statistic with estimated parameters)
    // Approximation from Stephens (1974): "EDF Statistics for Goodness of Fit and Some Comparisons"

    if (wSquared < 0.0275) {
      return 1.0
    } else if (wSquared < 0.051) {
      // Linear interpolation in the lower range
      return 1.0 - (wSquared - 0.0275) / (0.051 - 0.0275) * 0.75
    } else if (wSquared < 0.093) {
      return 0.25 - (wSquared - 0.051) / (0.093 - 0.051) * 0.15
    } else if (wSquared < 0.158) {
      return 0.10 - (wSquared - 0.093) / (0.158 - 0.093) * 0.05
    } else if (wSquared < 0.241) {
      return 0.05 - (wSquared - 0.158) / (0.241 - 0.158) * 0.025
    } else if (wSquared < 0.347) {
      return 0.025 - (wSquared - 0.241) / (0.347 - 0.241) * 0.015
    } else if (wSquared < 0.461) {
      return 0.01 - (wSquared - 0.347) / (0.461 - 0.347) * 0.005
    } else {
      // For very large values, use asymptotic approximation
      // P ≈ exp(-W²*/2) for large W²*
      double p = Math.exp(-wSquared / 2.0)
      return Math.max(0.001, p)
    }
  }

  /**
   * Gets the critical value for the Cramér-von Mises test at a given significance level.
   * These are for the modified statistic W²* when parameters are estimated from the data.
   * Values from Stephens (1974).
   */
  private static double getCriticalValue(double alpha) {
    if (alpha >= 0.25) {
      return 0.051
    } else if (alpha >= 0.10) {
      return 0.093
    } else if (alpha >= 0.05) {
      return 0.158
    } else if (alpha >= 0.025) {
      return 0.241
    } else if (alpha >= 0.01) {
      return 0.347
    } else {
      return 0.461  // alpha = 0.005
    }
  }

  private static void validateInput(List<? extends Number> data, double alpha) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be empty")
    }
    if (data.size() < 3) {
      throw new IllegalArgumentException("Cramér-von Mises test requires at least 3 observations, got ${data.size()}")
    }
    if (alpha <= 0 || alpha >= 1) {
      throw new IllegalArgumentException("Alpha must be between 0 and 1, got ${alpha}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Result class for the Cramér-von Mises test.
   */
  @CompileStatic
  static class CramerVonMisesResult {
    /** The modified Cramér-von Mises test statistic W²* */
    double statistic

    /** The raw W² statistic before modification */
    double rawStatistic

    /** The sample size */
    int sampleSize

    /** The sample mean */
    double mean

    /** The sample standard deviation */
    double stdDev

    /** The significance level */
    double alpha

    /** The approximate p-value */
    double pValue

    /** The critical value at the specified alpha level */
    double criticalValue

    /**
     * Interprets the Cramér-von Mises test result.
     *
     * @return A string describing whether the data appears normally distributed
     */
    String interpret() {
      if (statistic > criticalValue) {
        return "Reject H0: Data does not appear to be normally distributed (p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: Data appears to be consistent with a normal distribution (p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate() {
      String conclusion = statistic > criticalValue ?
        "not normally distributed" :
        "consistent with a normal distribution"

      return String.format(
        "Cramér-von Mises W²*: %.4f (critical value: %.3f at %.0f%% level)\n" +
        "p-value: %.4f\n" +
        "Sample: n=%d, mean=%.4f, sd=%.4f\n" +
        "Conclusion: Data appears %s",
        statistic, criticalValue, alpha * 100, pValue,
        sampleSize, mean, stdDev, conclusion
      )
    }

    @Override
    String toString() {
      return """Cramér-von Mises Normality Test
  Sample size: ${sampleSize}
  Mean: ${String.format('%.4f', mean)}
  Std Dev: ${String.format('%.4f', stdDev)}
  W²* statistic: ${String.format('%.4f', statistic)}
  Critical value (${String.format('%.0f', alpha * 100)}%%): ${String.format('%.3f', criticalValue)}
  p-value: ${String.format('%.4f', pValue)}

  ${interpret()}"""
    }
  }
}
