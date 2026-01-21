package se.alipsa.matrix.stats.normality

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution
import se.alipsa.matrix.core.Matrix

/**
 * D'Agostino's K² test (also known as K-squared test) is a goodness-of-fit measure of departure from normality.
 * Named for Ralph D'Agostino, the test gauges the compatibility of given data with the null hypothesis that
 * the data is a realization of independent, identically distributed Gaussian random variables.
 *
 * The test is based on transformations of the sample kurtosis and skewness, and has power only against the
 * alternatives that the distribution is skewed and/or kurtic (heavy or light tails).
 *
 * The K² statistic is the sum of squares of Z-scores for skewness and kurtosis:
 * K² = Z(skewness)² + Z(kurtosis)²
 *
 * Under the null hypothesis of normality, K² follows a chi-squared distribution with 2 degrees of freedom.
 *
 * Example:
 * <pre>
 * def data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 2.7, 3.0, 3.3, 2.6] as double[]
 * def result = KSquared.test(data)
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - D'Agostino, R. B., & Pearson, E. S. (1973). "Tests for departure from normality"
 * - D'Agostino, R. B., Belanger, A., & D'Agostino, R. B. Jr. (1990). "A suggestion for using powerful and informative tests of normality"
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
