package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/**
 * The Jarque–Bera test is a goodness-of-fit test of whether sample data have the skewness and kurtosis
 * matching a normal distribution. The test is named after Carlos Jarque and Anil K. Bera.
 * The test statistic is always nonnegative. If it is far from zero, it signals the data do not have a
 * normal distribution.
 *
 * <p>The test statistic JB is defined as:</p>
 * <pre>
 * JB = (n/6) * [S² + (K-3)²/4]
 * </pre>
 * <p>where:</p>
 * <ul>
 *   <li>n = sample size</li>
 *   <li>S = sample skewness</li>
 *   <li>K = sample kurtosis</li>
 * </ul>
 *
 * <p>Under the null hypothesis of normality, the JB statistic asymptotically follows a
 * chi-squared distribution with 2 degrees of freedom.</p>
 *
 * <p>A normal distribution has skewness = 0 and (excess) kurtosis = 0 (or kurtosis = 3).</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]
 * def result = JarqueBera.test(data)
 * println "JB statistic: ${result.jbStatistic}"
 * println "p-value: ${result.pValue}"
 * println "Skewness: ${result.skewness}"
 * println "Kurtosis: ${result.kurtosis}"
 * </pre>
 *
 * <p><strong>Note:</strong> This test is suitable for large samples (n ≥ 2000 recommended).
 * For smaller samples, consider using Shapiro-Wilk or other normality tests.</p>
 */
@CompileStatic
class JarqueBera {

  private static final int MIN_SAMPLE_SIZE = 4  // Need at least 4 observations to calculate kurtosis

  /**
   * Performs the Jarque-Bera test for normality.
   *
   * @param data The sample data (must have at least 4 observations)
   * @return JarqueBeraResult containing the test statistic, skewness, kurtosis, and p-value
   * @throws IllegalArgumentException if data is null, empty, or has fewer than 4 observations
   */
  static JarqueBeraResult test(List<? extends Number> data) {
    validateData(data)

    int n = data.size()
    double[] values = data.collect { it.doubleValue() } as double[]

    // Calculate mean
    double mean = 0
    for (double v : values) {
      mean += v
    }
    mean /= n

    // Calculate moments
    double m2 = 0  // Second moment (variance)
    double m3 = 0  // Third moment (for skewness)
    double m4 = 0  // Fourth moment (for kurtosis)

    for (double v : values) {
      double diff = v - mean
      double diff2 = diff * diff
      m2 += diff2
      m3 += diff * diff2
      m4 += diff2 * diff2
    }

    m2 /= n
    m3 /= n
    m4 /= n

    // Calculate skewness and kurtosis
    double skewness = m3 / Math.pow(m2, 1.5)
    double kurtosis = m4 / (m2 * m2)

    // Calculate JB statistic
    // JB = (n/6) * [S² + (K-3)²/4]
    double jbStatistic = (n / 6.0) * (skewness * skewness + Math.pow(kurtosis - 3, 2) / 4.0)

    // Calculate p-value using chi-squared distribution with 2 degrees of freedom
    ChiSquaredDistribution chiSq = new ChiSquaredDistribution(2)
    double pValue = 1.0 - chiSq.cumulativeProbability(jbStatistic)

    return new JarqueBeraResult(
      jbStatistic: jbStatistic,
      skewness: skewness,
      kurtosis: kurtosis,
      excessKurtosis: kurtosis - 3,
      pValue: pValue,
      sampleSize: n
    )
  }

  /**
   * Performs the Jarque-Bera test on a double array.
   *
   * @param data The sample data array
   * @return JarqueBeraResult containing the test statistic and p-value
   */
  static JarqueBeraResult test(double[] data) {
    return test(data.toList())
  }

  private static void validateData(List<? extends Number> data) {
    if (data == null || data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be null or empty")
    }
    if (data.size() < MIN_SAMPLE_SIZE) {
      throw new IllegalArgumentException("Jarque-Bera test requires at least ${MIN_SAMPLE_SIZE} observations (got ${data.size()})")
    }

    // Check for constant data (all values the same)
    double first = data[0].doubleValue()
    boolean allSame = data.every { it.doubleValue() == first }
    if (allSame) {
      throw new IllegalArgumentException("Data has zero variance - all values are identical")
    }
  }

  /**
   * Result class for the Jarque-Bera test.
   */
  static class JarqueBeraResult {
    /** The Jarque-Bera test statistic */
    Double jbStatistic

    /** The sample skewness */
    Double skewness

    /** The sample kurtosis */
    Double kurtosis

    /** The excess kurtosis (kurtosis - 3) */
    Double excessKurtosis

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
     * Returns true if the data appears to be normally distributed at the given significance level.
     */
    boolean isNormal(double alpha = 0.05) {
      return pValue >= alpha
    }

    @Override
    String toString() {
      return """Jarque-Bera Normality Test Result:
  JB statistic: ${jbStatistic}
  skewness: ${skewness}
  kurtosis: ${kurtosis} (excess: ${excessKurtosis})
  p-value: ${pValue}
  sample size: ${sampleSize}
  ${isNormal() ? 'Data appears normally distributed' : 'Data does NOT appear normally distributed'} (α=0.05)"""
    }
  }
}
