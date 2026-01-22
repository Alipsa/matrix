package se.alipsa.matrix.stats.distribution

import groovy.transform.CompileStatic

/**
 * Student's t-distribution implementation.
 * Provides CDF and p-value calculations for t-tests.
 *
 * <p>Uses custom high-precision implementation with no external dependencies.</p>
 */
@CompileStatic
class TDistribution {

  private final double degreesOfFreedom

  TDistribution(double degreesOfFreedom) {
    if (degreesOfFreedom <= 0) {
      throw new IllegalArgumentException("Degrees of freedom must be positive, got: $degreesOfFreedom")
    }
    this.degreesOfFreedom = degreesOfFreedom
  }

  /**
   * Computes the cumulative distribution function (CDF) of the t-distribution.
   * P(T <= t) where T follows a t-distribution with the specified degrees of freedom.
   *
   * @param t the t-value
   * @return probability P(T <= t)
   */
  double cdf(double t) {
    double x = degreesOfFreedom / (degreesOfFreedom + t * t)
    double beta = SpecialFunctions.regularizedIncompleteBeta(x, degreesOfFreedom / 2.0d, 0.5d)

    if (t >= 0) {
      return 1.0d - 0.5d * beta
    } else {
      return 0.5d * beta
    }
  }

  /**
   * Computes the two-tailed p-value for a t-statistic.
   * This is 2 * P(T > |t|) = 2 * (1 - CDF(|t|))
   *
   * @param t the t-statistic
   * @return two-tailed p-value
   */
  double twoTailedPValue(double t) {
    double absT = Math.abs(t)
    return 2.0d * (1.0d - cdf(absT))
  }

  /**
   * Computes the one-tailed p-value for a t-statistic (upper tail).
   * This is P(T > t) = 1 - CDF(t)
   *
   * @param t the t-statistic
   * @return one-tailed p-value (upper)
   */
  double oneTailedPValueUpper(double t) {
    return 1.0d - cdf(t)
  }

  /**
   * Computes the one-tailed p-value for a t-statistic (lower tail).
   * This is P(T < t) = CDF(t)
   *
   * @param t the t-statistic
   * @return one-tailed p-value (lower)
   */
  double oneTailedPValueLower(double t) {
    return cdf(t)
  }

  /**
   * Static convenience method for computing two-tailed p-value.
   *
   * @param t the t-statistic
   * @param df degrees of freedom
   * @return two-tailed p-value
   */
  static double pValue(double t, double df) {
    return new TDistribution(df).twoTailedPValue(t)
  }

  /**
   * Computes the two-sample t-test p-value (two-tailed).
   *
   * @param sample1 first sample data
   * @param sample2 second sample data
   * @return two-tailed p-value for the difference in means
   */
  static double twoSampleTTest(double[] sample1, double[] sample2) {
    int n1 = sample1.length
    int n2 = sample2.length

    double mean1 = mean(sample1)
    double mean2 = mean(sample2)
    double var1 = variance(sample1, mean1)
    double var2 = variance(sample2, mean2)

    // Welch's t-test (unequal variances)
    double se = Math.sqrt(var1 / n1 + var2 / n2 as double)
    double t = (mean1 - mean2) / se

    // Welch-Satterthwaite degrees of freedom
    double num = Math.pow(var1 / n1 + var2 / n2 as double, 2)
    double denom = Math.pow(var1 / n1 as double, 2) / (n1 - 1) + Math.pow(var2 / n2 as double, 2) / (n2 - 1)
    double df = num / denom

    return pValue(t, df)
  }

  /**
   * Computes the one-sample t-test p-value (two-tailed).
   *
   * @param mu the hypothesized population mean
   * @param sample the sample data
   * @return two-tailed p-value
   */
  static double oneSampleTTest(double mu, double[] sample) {
    int n = sample.length
    double mean = mean(sample)
    double sd = Math.sqrt(variance(sample, mean))
    double t = (mean - mu) / (sd / Math.sqrt(n))
    double df = n - 1
    return pValue(t, df)
  }

  private static double mean(double[] values) {
    double sum = 0.0d
    for (double v : values) {
      sum += v
    }
    return sum / values.length
  }

  private static double variance(double[] values, double mean) {
    double sum = 0.0d
    for (double v : values) {
      double diff = v - mean
      sum += diff * diff
    }
    return sum / (values.length - 1)
  }
}
