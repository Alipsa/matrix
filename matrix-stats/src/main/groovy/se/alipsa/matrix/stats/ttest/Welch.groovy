package se.alipsa.matrix.stats.ttest

import groovy.transform.CompileStatic
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
@CompileStatic
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
      throw new IllegalArgumentException("Input lists cannot be null")
    }
    if (first.isEmpty() || second.isEmpty()) {
      throw new IllegalArgumentException("Input lists cannot be empty")
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

    TtestResult result = new TtestResult()
    result.description = "Welch's two sample t-test"

    // Welch's t-statistic
    BigDecimal t = (mean1 - mean2) / ((var1 / n1) + (var2 / n2)).sqrt()
    result.tVal = t

    // Welch-Satterthwaite degrees of freedom
    BigDecimal dividend = ((var1/n1) + (var2/n2)) ** 2
    BigDecimal n1Sq = (n1 as BigDecimal) ** 2
    BigDecimal n2Sq = (n2 as BigDecimal) ** 2
    BigDecimal divisor1 = ((var1 ** 2) as BigDecimal).divide(n1Sq * (n1-1), SCALE, RoundingMode.HALF_UP)
    BigDecimal divisor2 = ((var2 ** 2) as BigDecimal).divide(n2Sq * (n2 -1), SCALE, RoundingMode.HALF_UP)
    result.df = dividend.divide(divisor1 + divisor2, SCALE, RoundingMode.HALF_UP)

    result.mean1 = mean1
    result.mean2 = mean2
    result.var1 = var1
    result.var2 = var2
    result.sd1 = sd1
    result.sd2 = sd2
    result.n1 = n1
    result.n2 = n2

    // Use native TDistribution for p-value calculation
    result.pVal = TDistribution.pValue(t as double, result.df as double)
    return result
  }
}
