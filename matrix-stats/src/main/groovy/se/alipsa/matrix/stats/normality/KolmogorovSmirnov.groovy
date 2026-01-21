package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest as ApacheKSTest

/**
 * The Kolmogorov–Smirnov test (K–S test or KS test) is a nonparametric test of the equality of
 * one-dimensional probability distributions that can be used to compare a sample with a reference probability
 * distribution (one-sample K–S test), or to compare two samples (two-sample K–S test).
 *
 * <p>The one-sample K-S test compares a sample with a reference distribution (e.g., normal distribution).</p>
 * <p>The two-sample K-S test compares two samples to determine if they come from the same distribution.</p>
 *
 * <p>The test statistic D is the maximum absolute difference between the empirical cumulative
 * distribution functions (ECDFs) of the samples.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test if data is normally distributed
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]
 * def result = KolmogorovSmirnov.testNormality(data)
 * println "D statistic: ${result.dStatistic}"
 * println "p-value: ${result.pValue}"
 *
 * // Compare two samples
 * def sample1 = [1.2, 1.5, 1.3, 1.8, 1.6]
 * def sample2 = [2.1, 2.3, 2.0, 2.4, 2.2]
 * def result2 = KolmogorovSmirnov.twoSampleTest(sample1, sample2)
 * println "p-value: ${result2.pValue}"
 * </pre>
 */
@CompileStatic
class KolmogorovSmirnov {

  private static final ApacheKSTest ksTest = new ApacheKSTest()

  /**
   * Performs a one-sample Kolmogorov-Smirnov test to check if the data follows a normal distribution.
   * The test uses the sample mean and standard deviation to construct the reference normal distribution.
   *
   * @param data The sample data
   * @return KSResult containing the D statistic and p-value
   * @throws IllegalArgumentException if data is null or has fewer than 2 observations
   */
  static KSResult testNormality(List<? extends Number> data) {
    validateData(data)

    double[] values = data.collect { it.doubleValue() } as double[]

    // Calculate sample mean and standard deviation
    double mean = values.sum() / values.length
    double variance = 0
    for (double v : values) {
      variance += (v - mean) * (v - mean)
    }
    double stdDev = Math.sqrt(variance / values.length)

    // Create normal distribution with sample mean and std dev
    NormalDistribution normalDist = new NormalDistribution(mean, stdDev)

    return testAgainstDistribution(data, normalDist, "Normality")
  }

  /**
   * Performs a one-sample Kolmogorov-Smirnov test to check if the data follows a standard normal distribution (μ=0, σ=1).
   *
   * @param data The sample data
   * @return KSResult containing the D statistic and p-value
   * @throws IllegalArgumentException if data is null or has fewer than 2 observations
   */
  static KSResult testStandardNormality(List<? extends Number> data) {
    validateData(data)
    NormalDistribution standardNormal = new NormalDistribution(0, 1)
    return testAgainstDistribution(data, standardNormal, "Standard Normality")
  }

  /**
   * Performs a one-sample Kolmogorov-Smirnov test to check if the data follows the specified distribution.
   *
   * @param data The sample data
   * @param distribution The reference distribution to test against
   * @param testName Optional name for the test (for display purposes)
   * @return KSResult containing the D statistic and p-value
   * @throws IllegalArgumentException if data is null or has fewer than 2 observations
   */
  static KSResult testAgainstDistribution(List<? extends Number> data, RealDistribution distribution, String testName = "K-S Test") {
    validateData(data)

    double[] values = data.collect { it.doubleValue() } as double[]

    // Calculate D statistic
    double dStatistic = ksTest.kolmogorovSmirnovStatistic(distribution, values)

    // Calculate p-value
    double pValue = ksTest.kolmogorovSmirnovTest(distribution, values)

    return new KSResult(
      dStatistic: dStatistic,
      pValue: pValue,
      sampleSize: values.length,
      testType: testName
    )
  }

  /**
   * Performs a two-sample Kolmogorov-Smirnov test to determine if two samples come from the same distribution.
   *
   * @param sample1 The first sample
   * @param sample2 The second sample
   * @return KSResult containing the D statistic and p-value
   * @throws IllegalArgumentException if either sample is null or has fewer than 2 observations
   */
  static KSResult twoSampleTest(List<? extends Number> sample1, List<? extends Number> sample2) {
    validateData(sample1)
    validateData(sample2)

    double[] values1 = sample1.collect { it.doubleValue() } as double[]
    double[] values2 = sample2.collect { it.doubleValue() } as double[]

    // Calculate D statistic
    double dStatistic = ksTest.kolmogorovSmirnovStatistic(values1, values2)

    // Calculate p-value
    double pValue = ksTest.kolmogorovSmirnovTest(values1, values2)

    return new KSResult(
      dStatistic: dStatistic,
      pValue: pValue,
      sampleSize: values1.length + values2.length,
      testType: "Two-Sample K-S"
    )
  }

  /**
   * Performs a two-sample Kolmogorov-Smirnov test on double arrays.
   *
   * @param sample1 The first sample
   * @param sample2 The second sample
   * @return KSResult containing the D statistic and p-value
   */
  static KSResult twoSampleTest(double[] sample1, double[] sample2) {
    return twoSampleTest(sample1.toList(), sample2.toList())
  }

  private static void validateData(List<? extends Number> data) {
    if (data == null || data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be null or empty")
    }
    if (data.size() < 2) {
      throw new IllegalArgumentException("Kolmogorov-Smirnov test requires at least 2 observations (got ${data.size()})")
    }
  }

  /**
   * Result class for the Kolmogorov-Smirnov test.
   */
  static class KSResult {
    /** The D test statistic (maximum distance between ECDFs) */
    Double dStatistic

    /** The p-value of the test */
    Double pValue

    /** The total sample size */
    Integer sampleSize

    /** The type of test performed */
    String testType

    /**
     * Evaluates whether to reject the null hypothesis at the given significance level.
     *
     * @param alpha Significance level (default 0.05)
     * @return true if null hypothesis should be rejected (p-value < alpha)
     */
    boolean evaluate(double alpha = 0.05) {
      return pValue < alpha
    }

    /**
     * For normality tests, returns true if data appears normally distributed.
     */
    boolean isNormal(double alpha = 0.05) {
      return pValue >= alpha
    }

    @Override
    String toString() {
      String result
      if (testType.contains("Normality")) {
        result = isNormal() ? "Data appears normally distributed" : "Data does NOT appear normally distributed"
      } else {
        result = evaluate() ? "Samples appear to differ" : "Samples do not significantly differ"
      }

      return """Kolmogorov-Smirnov Test Result (${testType}):
  D statistic: ${dStatistic}
  p-value: ${pValue}
  sample size: ${sampleSize}
  ${result} (α=0.05)"""
    }
  }
}
