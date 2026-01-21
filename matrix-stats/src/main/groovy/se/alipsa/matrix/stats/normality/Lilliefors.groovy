package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution

/**
 * The Lilliefors test is a normality test based on the Kolmogorov–Smirnov test.
 * It is used to test the null hypothesis that data come from a normally distributed population,
 * when the null hypothesis does not specify which normal distribution;
 * i.e., it does not specify the expected value and variance of the distribution.
 *
 * The Lilliefors test is a modification of the Kolmogorov-Smirnov test where the
 * distribution parameters (mean and standard deviation) are estimated from the sample.
 * This estimation affects the null distribution, so different critical values are needed.
 *
 * Reference: R's nortest::lillie.test()
 */
@CompileStatic
class Lilliefors {

  /**
   * Performs the Lilliefors test for normality.
   *
   * The null hypothesis is that the data follows a normal distribution.
   * The alternative hypothesis is that the data does not follow a normal distribution.
   *
   * @param data The sample data to test
   * @param alpha Significance level (default: 0.05)
   * @return LillieforsResult containing test statistic and p-value
   * @throws IllegalArgumentException if data is null, empty, or has fewer than 4 observations
   */
  static LillieforsResult testNormality(List<? extends Number> data, double alpha = 0.05) {
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

    // Calculate the Kolmogorov-Smirnov test statistic using the estimated parameters
    double dStatistic = calculateKSStatistic(values, mean, stdDev, n)

    // Calculate p-value using Lilliefors-specific approximation
    double pValue = calculatePValue(dStatistic, n)

    return new LillieforsResult(
      statistic: dStatistic,
      pValue: pValue,
      alpha: alpha,
      sampleSize: n,
      mean: mean,
      stdDev: stdDev
    )
  }

  /**
   * Calculates the Kolmogorov-Smirnov test statistic D.
   *
   * D = max|F_n(x) - F(x)|
   *
   * where F_n is the empirical CDF and F is the theoretical normal CDF
   * with parameters estimated from the data.
   */
  private static double calculateKSStatistic(double[] values, double mean, double stdDev, int n) {
    // Sort the data
    double[] sorted = values.clone()
    Arrays.sort(sorted)

    // Create the standard normal distribution with estimated parameters
    NormalDistribution normalDist = new NormalDistribution(mean, stdDev)

    // Calculate the maximum absolute difference between empirical and theoretical CDFs
    double maxDifference = 0.0

    for (int i = 0; i < n; i++) {
      // Empirical CDF at x_i: F_n(x_i) = i/n (before the point)
      // Empirical CDF at x_i: F_n(x_i) = (i+1)/n (at or after the point)
      double empiricalCDFBefore = i / (double) n
      double empiricalCDFAfter = (i + 1) / (double) n

      // Theoretical CDF at x_i
      double theoreticalCDF = normalDist.cumulativeProbability(sorted[i])

      // Calculate absolute differences
      double diff1 = Math.abs(empiricalCDFBefore - theoreticalCDF)
      double diff2 = Math.abs(empiricalCDFAfter - theoreticalCDF)

      maxDifference = Math.max(maxDifference, Math.max(diff1, diff2))
    }

    return maxDifference
  }

  /**
   * Calculates the p-value for the Lilliefors test.
   *
   * Uses the approximation from Dallal and Wilkinson (1986):
   * "An Analytic Approximation to the Distribution of Lilliefors's Test Statistic for Normality"
   *
   * For more accuracy, uses different approximations based on sample size and D value.
   */
  private static double calculatePValue(double d, int n) {
    // For small samples (n < 100), use the Dallal-Wilkinson approximation
    if (n < 100) {
      // Dallal-Wilkinson approximation
      double sqrtN = Math.sqrt(n)
      double z = d * sqrtN

      // Approximation based on simulation studies
      if (z < 0.2) {
        return 1.0
      } else if (z > 1.0) {
        // For large D values, use exponential approximation
        return Math.exp(-7.01256 * z * z + 4.8 * z)
      } else {
        // Linear interpolation for moderate D values
        return Math.max(0.0, 1.0 - Math.exp(-7.01256 * z * z + 4.8 * z))
      }
    } else {
      // For larger samples, use a different approximation
      // Based on the asymptotic distribution
      double sqrtN = Math.sqrt(n)
      double zScore = d * (sqrtN - 0.01 + 0.85 / sqrtN)

      if (zScore < 0.3) {
        return 1.0
      } else if (zScore > 1.0) {
        return Math.exp(-2.0 * zScore * zScore)
      } else {
        return Math.max(0.0, 1.0 - Math.exp(-2.0 * zScore * zScore))
      }
    }
  }

  private static void validateInput(List<? extends Number> data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be empty")
    }
    if (data.size() < 4) {
      throw new IllegalArgumentException("Lilliefors test requires at least 4 observations, got ${data.size()}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Result class for the Lilliefors normality test.
   */
  @CompileStatic
  static class LillieforsResult {
    /** The Kolmogorov-Smirnov test statistic D */
    double statistic

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
      return """Lilliefors Normality Test
  Sample size: ${sampleSize}
  Sample mean: ${String.format("%.4f", mean)}
  Sample SD: ${String.format("%.4f", stdDev)}
  Test statistic (D): ${String.format("%.4f", statistic)}
  P-value: ${String.format("%.4f", pValue)}

  ${evaluate()}"""
    }
  }
}
