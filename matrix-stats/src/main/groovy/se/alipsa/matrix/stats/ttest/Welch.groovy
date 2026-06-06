package se.alipsa.matrix.stats.ttest

import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.distribution.TDistribution

import java.math.RoundingMode

/**
 * Welch's t-test (also called unequal variances t-test) is a two-sample location test
 * which is used to test the hypothesis that two populations have equal means.
 *
 * <p>Unlike Student's t-test, Welch's t-test does not assume that the two populations
 * have equal variances, making it more robust in practice. This is the default behavior
 * of R's t.test() function.</p>
 *
 * <p>The test uses the Welch-Satterthwaite equation to compute the degrees of freedom,
 * which may result in non-integer degrees of freedom.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
class Welch {

  private static final int SCALE = 15

  /**
   * Performs Welch's t-test on two independent samples.
   *
   * <p>This method always uses Welch's t-test which does not assume equal variances.
   * This is generally recommended and follows R's default behavior.</p>
   *
   * @param first the first sample
   * @param second the second sample
   * @return a TtestResult containing all relevant statistics of the t-test
   * @throws IllegalArgumentException if inputs are null, empty, or have insufficient observations
   */
  static TtestResult tTest(List<? extends Number> first, List<? extends Number> second) {
    if (first == null || second == null) {
      throw new IllegalArgumentException('Input lists cannot be null')
    }
    if (first.isEmpty() || second.isEmpty()) {
      throw new IllegalArgumentException('Input lists cannot be empty')
    }
    if (first.size() < 2 || second.size() < 2) {
      throw new IllegalArgumentException("Each sample must have at least 2 observations, got: first=${first.size()}, second=${second.size()}")
    }

    BigDecimal mean1 = Stat.mean(first, SCALE)
    BigDecimal mean2 = Stat.mean(second, SCALE)
    BigDecimal var1 = Stat.variance(first, true)
    BigDecimal var2 = Stat.variance(second, true)
    BigDecimal sd1 = var1.sqrt()
    BigDecimal sd2 = var2.sqrt()
    int n1 = first.size()
    int n2 = second.size()

    // Welch's t-statistic
    BigDecimal t = (mean1 - mean2) / ((var1 / n1) + (var2 / n2)).sqrt()

    // Welch-Satterthwaite degrees of freedom
    BigDecimal dividend = ((var1/n1) + (var2/n2)) ** 2
    BigDecimal n1Sq = (n1 as BigDecimal) ** 2
    BigDecimal n2Sq = (n2 as BigDecimal) ** 2
    BigDecimal divisor1 = ((var1 ** 2) as BigDecimal).divide(n1Sq * (n1-1), SCALE, RoundingMode.HALF_UP)
    BigDecimal divisor2 = ((var2 ** 2) as BigDecimal).divide(n2Sq * (n2 -1), SCALE, RoundingMode.HALF_UP)

    BigDecimal degreesFreedom = dividend.divide(divisor1 + divisor2, SCALE, RoundingMode.HALF_UP)
    def p = TDistribution.pValue(t as double, degreesFreedom as double)
    BigDecimal firstMean = mean1
    BigDecimal secondMean = mean2
    BigDecimal firstVariance = var1
    BigDecimal secondVariance = var2
    BigDecimal firstSd = sd1
    BigDecimal secondSd = sd2
    int firstCount = n1
    int secondCount = n2
    new TtestResult(
      description: "Welch's two sample t-test",
      tVal: t,
      df: degreesFreedom,
      mean1: firstMean,
      mean2: secondMean,
      var1: firstVariance,
      var2: secondVariance,
      sd1: firstSd,
      sd2: secondSd,
      n1: firstCount,
      n2: secondCount,
      pVal: p
    )
  }
}
