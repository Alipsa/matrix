package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/**
 * The Jarque-Bera test is a goodness-of-fit test that determines whether sample data have skewness
 * and kurtosis matching a normal distribution. Named after Carlos Jarque and Anil K. Bera, this test
 * provides a simple and computationally efficient method for testing normality by examining the third
 * and fourth moments of the distribution.
 *
 * <p><b>What is the Jarque-Bera test?</b></p>
 * The Jarque-Bera test evaluates normality by measuring how much the sample skewness and kurtosis
 * deviate from those of a normal distribution (skewness = 0, excess kurtosis = 0). The test combines
 * these two measures into a single test statistic that follows a chi-squared distribution under the
 * null hypothesis of normality.
 *
 * <p><b>When to use the Jarque-Bera test:</b></p>
 * <ul>
 *   <li>When you have large sample sizes (n ≥ 2000 recommended for reliable asymptotic approximation)</li>
 *   <li>When computational simplicity is important</li>
 *   <li>For financial and economic data analysis (very popular in econometrics)</li>
 *   <li>When you specifically want to test for departures from normality due to skewness or heavy/light tails</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Simple to compute and interpret</li>
 *   <li>Provides information about the nature of non-normality (skewness vs. kurtosis)</li>
 *   <li>Widely used and well-understood in econometrics and finance</li>
 *   <li>Asymptotically efficient for large samples</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Requires very large samples (n ≥ 2000) for the asymptotic chi-squared approximation to be reliable</li>
 *   <li>Poor power for small to moderate sample sizes</li>
 *   <li>Less powerful than Shapiro-Wilk or Anderson-Darling for detecting non-normality in small samples</li>
 *   <li>Can be sensitive to outliers which may inflate skewness and kurtosis</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): The data are normally distributed (skewness = 0 and excess kurtosis = 0)</li>
 *   <li>H₁ (alternative hypothesis): The data are not normally distributed (skewness ≠ 0 or excess kurtosis ≠ 0)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test normality of a dataset
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]
 * def result = JarqueBera.test(data)
 * println "JB statistic: ${result.jbStatistic}"
 * println "p-value: ${result.pValue}"
 * println "Skewness: ${result.skewness}"
 * println "Excess kurtosis: ${result.excessKurtosis}"
 * println result.isNormal() ? "Data appears normal" : "Data appears non-normal"
 *
 * // Example output:
 * // JB statistic: 0.3254
 * // p-value: 0.8502
 * // Skewness: 0.1234
 * // Excess kurtosis: -0.4321
 * // Data appears normal
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic is calculated as:
 * <pre>
 * JB = (n/6) * [S² + (K-3)²/4]
 * </pre>
 * where:
 * <ul>
 *   <li>n = sample size</li>
 *   <li>S = sample skewness</li>
 *   <li>K = sample kurtosis (K - 3 = excess kurtosis)</li>
 * </ul>
 * The test statistic is always non-negative. Under the null hypothesis of normality, JB asymptotically
 * follows a chi-squared distribution with 2 degrees of freedom (one for skewness, one for kurtosis).
 * Large values of JB indicate departure from normality.
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Jarque, C. M., & Bera, A. K. (1980). "Efficient tests for normality, homoscedasticity and serial independence of regression residuals". Economics Letters, 6(3), 255-259.</li>
 *   <li>Jarque, C. M., & Bera, A. K. (1987). "A test for normality of observations and regression residuals". International Statistical Review, 55(2), 163-172.</li>
 *   <li>Thadewald, T., & Büning, H. (2007). "Jarque–Bera test and its competitors for testing normality – A power comparison". Journal of Applied Statistics, 34(1), 87-105.</li>
 * </ul>
 *
 * <p><b>Note:</b> This test is most suitable for large samples (n ≥ 2000). For smaller samples (n < 50),
 * consider using the Shapiro-Wilk test. For medium samples (50 ≤ n < 2000), consider Anderson-Darling
 * or Shapiro-Francia tests.</p>
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
