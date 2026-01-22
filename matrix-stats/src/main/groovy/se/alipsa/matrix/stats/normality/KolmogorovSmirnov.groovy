package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest as ApacheKSTest

/**
 * The Kolmogorov-Smirnov (K-S) test is a nonparametric goodness-of-fit test that compares a sample
 * with a reference probability distribution (one-sample K-S test), or compares two samples to determine
 * if they come from the same distribution (two-sample K-S test). The test is based on the maximum
 * distance between empirical cumulative distribution functions.
 *
 * <p><b>What is the Kolmogorov-Smirnov test?</b></p>
 * The K-S test measures the maximum vertical distance between the empirical cumulative distribution
 * function (ECDF) of the sample and the theoretical CDF of the reference distribution. For normality
 * testing, it compares the sample's ECDF to that of a normal distribution with the same mean and
 * standard deviation. Unlike moment-based tests, K-S examines the entire distribution shape.
 *
 * <p><b>When to use the Kolmogorov-Smirnov test:</b></p>
 * <ul>
 *   <li>When testing goodness-of-fit to any continuous distribution (not limited to normal)</li>
 *   <li>When you want a distribution-free (nonparametric) test</li>
 *   <li>For moderate to large sample sizes (n ≥ 50)</li>
 *   <li>When you need to compare two samples without assuming a parametric distribution</li>
 *   <li>When the data are measured on at least an ordinal scale</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Distribution-free (nonparametric) - makes no assumptions about the distribution shape</li>
 *   <li>Can be used to test fit to any continuous distribution, not just normal</li>
 *   <li>Provides an intuitive measure (maximum distance between CDFs)</li>
 *   <li>Sensitive to differences in both location and shape of distributions</li>
 *   <li>Well-established with exact p-value calculations available</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Less powerful than Shapiro-Wilk or Anderson-Darling for detecting departures from normality</li>
 *   <li>Most sensitive to differences near the center of the distribution, less so in the tails</li>
 *   <li>When parameters are estimated from the data (composite hypothesis), the test becomes conservative</li>
 *   <li>Requires continuous distributions (not appropriate for discrete data)</li>
 *   <li>For normality testing with estimated parameters, Lilliefors correction is more appropriate</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * For one-sample normality test:
 * <ul>
 *   <li>H₀ (null hypothesis): The data follow a normal distribution</li>
 *   <li>H₁ (alternative hypothesis): The data do not follow a normal distribution</li>
 * </ul>
 * For two-sample test:
 * <ul>
 *   <li>H₀ (null hypothesis): The two samples come from the same distribution</li>
 *   <li>H₁ (alternative hypothesis): The two samples come from different distributions</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test if data is normally distributed
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]
 * def result = KolmogorovSmirnov.testNormality(data)
 * println "D statistic: ${result.dStatistic}"
 * println "p-value: ${result.pValue}"
 * println result.isNormal() ? "Data appears normal" : "Data appears non-normal"
 *
 * // Compare two independent samples
 * def sample1 = [1.2, 1.5, 1.3, 1.8, 1.6]
 * def sample2 = [2.1, 2.3, 2.0, 2.4, 2.2]
 * def result2 = KolmogorovSmirnov.twoSampleTest(sample1, sample2)
 * println "p-value: ${result2.pValue}"
 * println result2.evaluate() ? "Samples differ" : "Samples do not differ"
 *
 * // Example output:
 * // D statistic: 0.1523
 * // p-value: 0.9234
 * // Data appears normal
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic D is calculated as:
 * <pre>
 * D = sup|F_n(x) - F(x)|
 * </pre>
 * where F_n(x) is the empirical CDF of the sample and F(x) is the theoretical CDF of the reference
 * distribution. For the two-sample test:
 * <pre>
 * D = sup|F_1,n(x) - F_2,m(x)|
 * </pre>
 * where F_1,n and F_2,m are the empirical CDFs of the two samples. The statistic ranges from 0 to 1,
 * with larger values indicating greater discrepancy.
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Kolmogorov, A. N. (1933). "Sulla determinazione empirica di una legge di distribuzione". Giornale dell'Istituto Italiano degli Attuari, 4, 83-91.</li>
 *   <li>Smirnov, N. V. (1948). "Table for estimating the goodness of fit of empirical distributions". Annals of Mathematical Statistics, 19(2), 279-281.</li>
 *   <li>Massey, F. J. (1951). "The Kolmogorov-Smirnov test for goodness of fit". Journal of the American Statistical Association, 46(253), 68-78.</li>
 *   <li>Stephens, M. A. (1974). "EDF statistics for goodness of fit and some comparisons". Journal of the American Statistical Association, 69(347), 730-737.</li>
 * </ul>
 *
 * <p><b>Note:</b> When testing normality with parameters estimated from the data, the test becomes
 * conservative (actual significance level is lower than nominal). For this case, consider using the
 * Lilliefors test instead, which provides a corrected version of the K-S test specifically for normality
 * testing with estimated parameters.</p>
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
