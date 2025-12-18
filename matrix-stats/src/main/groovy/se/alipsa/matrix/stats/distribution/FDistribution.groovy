package se.alipsa.matrix.stats.distribution

import groovy.transform.CompileStatic

/**
 * F-distribution (Fisher-Snedecor distribution) implementation.
 * Provides CDF and p-value calculations for ANOVA and variance ratio tests.
 */
@CompileStatic
class FDistribution {

  private final double dfNumerator
  private final double dfDenominator

  /**
   * Creates an F-distribution with the specified degrees of freedom.
   *
   * @param dfNumerator degrees of freedom for the numerator (between groups)
   * @param dfDenominator degrees of freedom for the denominator (within groups)
   */
  FDistribution(double dfNumerator, double dfDenominator) {
    if (dfNumerator <= 0 || dfDenominator <= 0) {
      throw new IllegalArgumentException(
          "Degrees of freedom must be positive, got: dfNumerator=$dfNumerator, dfDenominator=$dfDenominator"
      )
    }
    this.dfNumerator = dfNumerator
    this.dfDenominator = dfDenominator
  }

  /**
   * Computes the cumulative distribution function (CDF) of the F-distribution.
   * P(F <= f) where F follows an F-distribution.
   *
   * @param f the F-value (must be >= 0)
   * @return probability P(F <= f)
   */
  double cdf(double f) {
    if (f < 0) {
      throw new IllegalArgumentException("f must be non-negative, got: $f")
    }
    if (f == 0) return 0.0

    // F-distribution CDF is related to the regularized incomplete beta function
    double x = dfNumerator * f / (dfNumerator * f + dfDenominator)
    return SpecialFunctions.regularizedIncompleteBeta(
        x,
        dfNumerator / 2.0d,
        dfDenominator / 2.0d
    )
  }

  /**
   * Computes the p-value (upper tail probability) for an F-statistic.
   * This is P(F > f) = 1 - CDF(f)
   *
   * @param f the F-statistic
   * @return p-value (probability of observing a value at least as extreme)
   */
  double pValue(double f) {
    return 1.0 - cdf(f)
  }

  /**
   * Static convenience method for computing F-test p-value.
   *
   * @param f the F-statistic
   * @param dfNumerator degrees of freedom for the numerator
   * @param dfDenominator degrees of freedom for the denominator
   * @return p-value
   */
  static double pValue(double f, double dfNumerator, double dfDenominator) {
    return new FDistribution(dfNumerator, dfDenominator).pValue(f)
  }

  /**
   * Computes the one-way ANOVA F-statistic.
   *
   * @param groups list of double arrays, each representing a group
   * @return the F-statistic
   */
  static double oneWayAnovaFValue(List<double[]> groups) {
    int k = groups.size()  // number of groups
    int n = 0  // total observations

    // Calculate group means and sizes
    double[] groupMeans = new double[k]
    int[] groupSizes = new int[k]
    double grandSum = 0.0

    for (int i = 0; i < k; i++) {
      double[] group = groups[i]
      groupSizes[i] = group.length
      n += group.length
      double sum = 0.0
      for (double v : group) {
        sum += v
        grandSum += v
      }
      groupMeans[i] = sum / group.length
    }

    double grandMean = grandSum / n

    // Sum of squares between groups (SSB)
    double ssb = 0.0
    for (int i = 0; i < k; i++) {
      double diff = groupMeans[i] - grandMean
      ssb += groupSizes[i] * diff * diff
    }

    // Sum of squares within groups (SSW)
    double ssw = 0.0
    for (int i = 0; i < k; i++) {
      double[] group = groups[i]
      for (double v : group) {
        double diff = v - groupMeans[i]
        ssw += diff * diff
      }
    }

    // Degrees of freedom
    double dfBetween = k - 1
    double dfWithin = n - k

    // Mean squares
    double msb = ssb / dfBetween
    double msw = ssw / dfWithin

    // F-statistic
    return msb / msw
  }

  /**
   * Computes the one-way ANOVA p-value.
   *
   * @param groups list of double arrays, each representing a group
   * @return the p-value for the ANOVA test
   */
  static double oneWayAnovaPValue(List<double[]> groups) {
    int k = groups.size()
    int n = 0
    for (double[] group : groups) {
      n += group.length
    }

    double f = oneWayAnovaFValue(groups)
    double dfBetween = k - 1
    double dfWithin = n - k

    return pValue(f, dfBetween, dfWithin)
  }
}
