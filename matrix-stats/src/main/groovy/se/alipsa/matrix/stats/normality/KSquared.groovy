package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution
import se.alipsa.matrix.core.Matrix

/**
 * D'Agostino's K² test (K-squared test) is a powerful omnibus test for normality that combines
 * normalized measures of sample skewness and kurtosis into a single test statistic. Named after
 * Ralph D'Agostino, this test provides a comprehensive assessment of departure from normality by
 * examining both the asymmetry and tail behavior of the distribution.
 *
 * <p><b>What is D'Agostino's K² test?</b></p>
 * The K² test transforms sample skewness and kurtosis into approximately normally distributed
 * Z-scores, then combines them into a chi-squared statistic with 2 degrees of freedom. This
 * approach allows the test to detect departures from normality due to asymmetry (skewness),
 * heavy or light tails (kurtosis), or both. The transformation makes the test more powerful
 * than simple moment-based tests for moderate sample sizes.
 *
 * <p><b>When to use D'Agostino's K² test:</b></p>
 * <ul>
 *   <li>For sample sizes from about 20 to several thousand observations</li>
 *   <li>When you want to test for departures from normality due to both skewness and kurtosis</li>
 *   <li>As an omnibus test that has good power against a wide range of alternatives</li>
 *   <li>When you need diagnostic information about the nature of non-normality</li>
 *   <li>For balanced assessment of both symmetry and tail behavior</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Good power for moderate to large sample sizes (n ≥ 20)</li>
 *   <li>Provides information about which aspect (skewness or kurtosis) contributes to non-normality</li>
 *   <li>More powerful than simple Jarque-Bera test due to better normalizing transformations</li>
 *   <li>Well-calibrated for sample sizes as small as 20</li>
 *   <li>Computationally simple and fast</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Less powerful than Shapiro-Wilk or Anderson-Darling tests for small samples (n < 50)</li>
 *   <li>Requires minimum sample size of 8 observations</li>
 *   <li>Like all moment-based tests, can be affected by outliers</li>
 *   <li>May have reduced power against certain alternatives (e.g., contaminated normals)</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): The data follow a normal distribution</li>
 *   <li>H₁ (alternative hypothesis): The data do not follow a normal distribution (due to non-zero skewness, excess kurtosis, or both)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test normality of a dataset
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 2.7, 3.0, 3.3, 2.6] as double[]
 * def result = KSquared.test(data)
 * println "K² statistic: ${result.statistic}"
 * println "p-value: ${result.pValue}"
 * println "Skewness: ${result.skewness} (Z = ${result.zSkewness})"
 * println "Kurtosis: ${result.kurtosis} (Z = ${result.zKurtosis})"
 * println result.interpret()
 *
 * // Example output:
 * // K² statistic: 1.2345
 * // p-value: 0.5395
 * // Skewness: 0.1234 (Z = 0.4567)
 * // Kurtosis: 2.8901 (Z = -0.2345)
 * // Fail to reject H0: Data is consistent with normality
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic is calculated as:
 * <pre>
 * K² = Z(skewness)² + Z(kurtosis)²
 * </pre>
 * where Z(skewness) and Z(kurtosis) are normalized transformations of the sample skewness and
 * kurtosis using D'Agostino's method. These transformations account for the sampling distributions
 * and make the components approximately normally distributed. Under H₀, K² follows a chi-squared
 * distribution with 2 degrees of freedom.
 *
 * <p>The sample skewness and kurtosis are calculated from the third and fourth standardized moments,
 * then transformed using correction factors that depend on sample size to achieve approximate normality
 * of the Z-scores.</p>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>D'Agostino, R. B., & Pearson, E. S. (1973). "Tests for departure from normality. Empirical results for the distributions of b2 and √b1". Biometrika, 60(3), 613-622.</li>
 *   <li>D'Agostino, R. B., Belanger, A., & D'Agostino, R. B. Jr. (1990). "A suggestion for using powerful and informative tests of normality". The American Statistician, 44(4), 316-321.</li>
 *   <li>D'Agostino, R. B., & Stephens, M. A. (1986). Goodness-of-Fit Techniques. New York: Marcel Dekker.</li>
 * </ul>
 *
 * <p><b>Note:</b> For small samples (n < 20), consider using the Shapiro-Wilk test instead.
 * For very large samples (n > 2000), the Jarque-Bera test is also appropriate and computationally
 * simpler, though K² generally has better finite-sample properties.</p>
 */
@CompileStatic
class KSquared {

  /**
   * Performs D'Agostino's K² test for normality.
   *
   * @param data The sample data as double array
   * @return KSquaredResult containing test statistic, p-value, and component Z-scores
   */
  static KSquaredResult test(double[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.length < 8) {
      throw new IllegalArgumentException("Sample size must be at least 8 (got ${data.length})")
    }

    int n = data.length

    // Calculate mean
    double mean = 0.0
    for (double x : data) {
      mean += x
    }
    mean /= n

    // Calculate moments
    double m2 = 0.0  // Second moment (variance)
    double m3 = 0.0  // Third moment (for skewness)
    double m4 = 0.0  // Fourth moment (for kurtosis)

    for (double x : data) {
      double diff = x - mean
      double diff2 = diff * diff
      m2 += diff2
      m3 += diff2 * diff
      m4 += diff2 * diff2
    }

    m2 /= n
    m3 /= n
    m4 /= n

    double sd = Math.sqrt(m2)

    if (sd < 1e-10) {
      throw new IllegalArgumentException("Data has zero variance")
    }

    // Calculate sample skewness (b1) and kurtosis (b2)
    double b1 = m3 / (sd * sd * sd)
    double b2 = m4 / (m2 * m2)

    // Transform skewness to Z-score using D'Agostino's method
    double y = b1 * Math.sqrt((n + 1) * (n + 3) / (6.0 * (n - 2)))
    double beta2 = 3.0 * (n * n + 27 * n - 70) * (n + 1) * (n + 3) /
                   ((n - 2) * (n + 5) * (n + 7) * (n + 9))
    double w2 = -1.0 + Math.sqrt(2 * (beta2 - 1))
    double delta = 1.0 / Math.sqrt(Math.log(Math.sqrt(w2)))
    double alpha = Math.sqrt(2.0 / (w2 - 1))

    double zSkewness = delta * Math.log(y / alpha + Math.sqrt((y / alpha) * (y / alpha) + 1))

    // Transform kurtosis to Z-score using D'Agostino's method
    double eb2 = 3.0 * (n - 1) / (n + 1)
    double varb2 = 24.0 * n * (n - 2) * (n - 3) / ((n + 1) * (n + 1) * (n + 3) * (n + 5))
    double x = (b2 - eb2) / Math.sqrt(varb2)
    double sqrtbeta1 = 6.0 * (n * n - 5 * n + 2) / ((n + 7) * (n + 9)) *
                       Math.sqrt(6.0 * (n + 3) * (n + 5) / (n * (n - 2) * (n - 3)))
    double A = 6.0 + 8.0 / sqrtbeta1 * (2.0 / sqrtbeta1 + Math.sqrt(1 + 4.0 / (sqrtbeta1 * sqrtbeta1)))

    double zKurtosis = Math.sqrt(9 * A / 2.0) *
                       ((1 - 2.0 / (9 * A)) - Math.pow(Math.abs(1 - 2.0 / A + x * Math.sqrt(2.0 / (A - 4))), 1.0 / 3))

    // Calculate K² statistic
    double kSquared = zSkewness * zSkewness + zKurtosis * zKurtosis

    // Calculate p-value using chi-squared distribution with 2 df
    ChiSquaredDistribution chiSq = new ChiSquaredDistribution(2)
    double pValue = 1.0 - chiSq.cumulativeProbability(kSquared)

    return new KSquaredResult(
      statistic: kSquared,
      pValue: pValue,
      skewness: b1,
      kurtosis: b2,
      zSkewness: zSkewness,
      zKurtosis: zKurtosis,
      sampleSize: n
    )
  }

  /**
   * Performs D'Agostino's K² test on a Matrix column.
   *
   * @param matrix The Matrix containing the data
   * @param columnName The name of the column to test
   * @return KSquaredResult containing test statistic, p-value, and component Z-scores
   */
  static KSquaredResult test(Matrix matrix, String columnName) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }

    List<Object> column = matrix.column(columnName)
    double[] data = column.collect { it as double } as double[]

    return test(data)
  }

  /**
   * Performs D'Agostino's K² test on a Matrix column by index.
   *
   * @param matrix The Matrix containing the data
   * @param columnIndex The index of the column to test
   * @return KSquaredResult containing test statistic, p-value, and component Z-scores
   */
  static KSquaredResult test(Matrix matrix, int columnIndex) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }

    List<Object> column = matrix.column(columnIndex)
    double[] data = column.collect { it as double } as double[]

    return test(data)
  }

  /**
   * Result class for D'Agostino's K² test.
   */
  @CompileStatic
  static class KSquaredResult {
    /** The K² test statistic */
    double statistic

    /** The p-value */
    double pValue

    /** Sample skewness */
    double skewness

    /** Sample kurtosis (excess kurtosis relative to normal distribution) */
    double kurtosis

    /** Z-score for skewness */
    double zSkewness

    /** Z-score for kurtosis */
    double zKurtosis

    /** Sample size */
    int sampleSize

    /**
     * Interprets the K² test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether the data appears normal
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        String reason = determineReason()
        return "Reject H0: Data significantly departs from normality (K² = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)}) - ${reason}"
      } else {
        return "Fail to reject H0: Data is consistent with normality (K² = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Determines the primary reason for departure from normality.
     */
    private String determineReason() {
      double absZSkew = Math.abs(zSkewness)
      double absZKurt = Math.abs(zKurtosis)

      if (absZSkew > 1.96 && absZKurt > 1.96) {
        return "both skewness and kurtosis"
      } else if (absZSkew > absZKurt) {
        return skewness > 0 ? "positive skewness (right tail)" : "negative skewness (left tail)"
      } else {
        return kurtosis > 3 ? "heavy tails (leptokurtic)" : "light tails (platykurtic)"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A detailed description of the test result
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = pValue < alpha ? "significant departure from normality" : "consistent with normality"

      return String.format(
        "D'Agostino's K² test:\\n" +
        "K² statistic: %.4f\\n" +
        "p-value: %.4f\\n" +
        "Sample size: %d\\n" +
        "Skewness: %.4f (Z = %.4f)\\n" +
        "Kurtosis: %.4f (Z = %.4f)\\n" +
        "Conclusion: Data shows %s at %.0f%% significance level",
        statistic, pValue, sampleSize, skewness, zSkewness, kurtosis, zKurtosis,
        conclusion, alpha * 100
      )
    }

    @Override
    String toString() {
      return """D'Agostino's K² Test
  Sample size: ${sampleSize}
  K² statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}
  Skewness: ${String.format('%.4f', skewness)} (Z = ${String.format('%.4f', zSkewness)})
  Kurtosis: ${String.format('%.4f', kurtosis)} (Z = ${String.format('%.4f', zKurtosis)})

  ${interpret()}"""
    }
  }
}
