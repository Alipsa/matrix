package se.alipsa.matrix.stats.ttest

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.distribution.TDistribution

import java.math.RoundingMode

/**
 * Student's t-test is a statistical hypothesis test used to compare the means of two populations.
 * This class implements the orthodox Student's t-test (also known as the independent samples t-test)
 * which assumes equal variances between the two populations.
 *
 * <p>For the two-sample t-test with equal variance assumption, the pooled variance formula is used:</p>
 *
 * <p>s<sub>p</sub><sup>2</sup> = [(n<sub>1</sub>-1)s<sub>1</sub><sup>2</sup> + (n<sub>2</sub>-1)s<sub>2</sub><sup>2</sup>] / (n<sub>1</sub>+n<sub>2</sub>-2)</p>
 *
 * <p>The t-statistic is then calculated as:</p>
 *
 * <p>t = (x̄<sub>1</sub> - x̄<sub>2</sub>) / (s<sub>p</sub> × √(1/n<sub>1</sub> + 1/n<sub>2</sub>))</p>
 *
 * <p>Degrees of freedom: df = n<sub>1</sub> + n<sub>2</sub> - 2</p>
 *
 * <p>where:</p>
 * <ul>
 *   <li>x̄<sub>1</sub> is the mean of the first sample</li>
 *   <li>x̄<sub>2</sub> is the mean of the second sample</li>
 *   <li>s<sub>1</sub><sup>2</sup> is the variance of the first sample</li>
 *   <li>s<sub>2</sub><sup>2</sup> is the variance of the second sample</li>
 *   <li>s<sub>p</sub><sup>2</sup> is the pooled variance</li>
 *   <li>n<sub>1</sub> is the size of the first sample</li>
 *   <li>n<sub>2</sub> is the size of the second sample</li>
 * </ul>
 *
 * <p>Note: The tTest() method can also perform Welch's t-test when equalVariance=false is specified,
 * which does not assume equal variances. For a cleaner API that always uses Welch's t-test,
 * see the {@link Welch} class.</p>
 */
@CompileStatic
class Student {

  private static final int SCALE = 15

  /**
   * The paired samples t-test is used to compare the means between two related groups of samples.
   * In this case, you have two values (i.e., pair of values) for the same samples.
   *
   * @param first the values for the first condition
   * @param second the values for the second condition
   * @return a PairedResult containing all relevant statistics of the t-test
   * @throws IllegalArgumentException if inputs are null, empty, or of different sizes
   */
  static PairedResult pairedTTest(List<? extends Number> first, List<? extends Number> second) {
    if (first == null || second == null) {
      throw new IllegalArgumentException("Input lists cannot be null")
    }
    if (first.isEmpty() || second.isEmpty()) {
      throw new IllegalArgumentException("Input lists cannot be empty")
    }
    Integer n1 = first.size()
    Integer n2 = second.size()
    if (n1 != n2) {
      throw new IllegalArgumentException("The two lists of values are of different size")
    }
    if (n1 < 2) {
      throw new IllegalArgumentException("Paired t-test requires at least 2 pairs of observations, got: $n1")
    }
    BigDecimal mean1 = Stat.mean(first, SCALE)
    BigDecimal mean2 = Stat.mean(second, SCALE)
    BigDecimal var1 = Stat.variance(first, true)
    BigDecimal var2 = Stat.variance(second, true)
    BigDecimal sd1 = var1.sqrt()
    BigDecimal sd2 = var2.sqrt()
    def df = n1 - 1

    BigDecimal cov = 0
    for (int j = 0; j < n1; j++) {
      cov += (first[j] as BigDecimal - mean1) * (second[j] as BigDecimal - mean2)
    }
    cov /= df
    // sample standard deviation of the differences
    BigDecimal sd = ((var1 + var2 - 2 * cov) / n1).sqrt()
    BigDecimal t = (mean1 - mean2) / sd

    // Use native TDistribution for p-value calculation
    def p = TDistribution.pValue(t as double, df as double)
    PairedResult result = new PairedResult()
    result.description = "Paired t-test"
    result.tVal = t
    result.n1 = n1
    result.n2 = n2
    result.mean1 = mean1.stripTrailingZeros()
    result.mean2 = mean2.stripTrailingZeros()
    result.var1 = var1
    result.var2 = var2
    result.sd = sd
    result.sd1 = sd1
    result.sd2 = sd2
    result.pVal = p
    result.df = df
    return result
  }

  /**
   * Performs Student's t-test comparing the means of two independent samples.
   *
   * <p>This method implements the orthodox Student's t-test using pooled variance,
   * which assumes equal population variances. If variances are unequal, use {@link Welch#tTest} instead.</p>
   *
   * <p>The equalVariance parameter controls the behavior:</p>
   * <ul>
   *   <li><b>true</b>: Performs Student's t-test with pooled variance (assumes equal variances)</li>
   *   <li><b>false</b>: Throws exception directing user to use {@link Welch#tTest}</li>
   *   <li><b>null</b>: Auto-detect using rule of thumb |var1 - var2| < 4. If unequal variance detected, throws exception</li>
   * </ul>
   *
   * @param first the first sample
   * @param second the second sample
   * @param equalVariance whether to assume equal variance (default: null = auto-detect)
   * @return a Result containing all relevant statistics of the t-test
   * @throws IllegalArgumentException if inputs are invalid, if equalVariance=false, or if auto-detect determines unequal variance
   * @see Welch#tTest for Welch's t-test when variances are unequal
   */
  static Result tTest(List<? extends Number> first, List<? extends Number> second, Boolean equalVariance = null) {
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

    // Determine if we should assume equal variance
    boolean isEqualVariance
    if (equalVariance == null) {
      // Auto-detect using rule of thumb: https://www.statology.org/equal-variance-assumption/
      isEqualVariance = (var1 - var2).abs() < 4
      if (!isEqualVariance) {
        throw new IllegalArgumentException(
          "Student's t-test requires equal variances. Variances differ significantly (var1=${var1}, var2=${var2}). " +
          "Use se.alipsa.matrix.stats.ttest.Welch.tTest() instead for unequal variances."
        )
      }
    } else if (!equalVariance) {
      throw new IllegalArgumentException(
        "Student's t-test requires equal variances (equalVariance=true). " +
        "Use se.alipsa.matrix.stats.ttest.Welch.tTest() instead for unequal variances."
      )
    } else {
      isEqualVariance = true
    }

    Result result = new Result()

    // Student's t-test with pooled variance
    result.description = "Student's two sample t-test"
    BigDecimal pooledVar = ((n1 - 1) * var1 + (n2 - 1) * var2) / (n1 + n2 - 2)
    BigDecimal t = (mean1 - mean2) / (pooledVar * (1.0/n1 + 1.0/n2)).sqrt()
    result.tVal = t
    result.df = n1 + n2 - 2

    result.mean1 = mean1
    result.mean2 = mean2
    result.var1 = var1
    result.var2 = var2
    result.sd1 = sd1
    result.sd2 = sd2
    result.n1 = n1
    result.n2 = n2

    // Use native TDistribution for p-value calculation
    result.pVal = TDistribution.pValue(result.tVal as double, result.df as double)
    return result
  }

  /**
   * A One sample t-test tests the mean of a single group against a known mean.
   * t= (m−μ) / s/sqrt(n)
   * where
   * <ul>
   *  <li>m is the sample mean</li>
   *  <li>n is the sample size</li>
   *  <li>s is the sample standard deviation with n−1 degrees of freedom</li>
   *  <li>μ is the theoretical mean value</li>
   * </ul>
   *
   * @param values the sample values
   * @param comparison the theoretical mean to test against
   * @return a SingleResult containing all relevant statistics of the t-test
   * @throws IllegalArgumentException if input is null, empty, or has insufficient observations
   */
  static SingleResult tTest(List<? extends Number> values, Number comparison) {
    if (values == null) {
      throw new IllegalArgumentException("Input list cannot be null")
    }
    if (values.isEmpty()) {
      throw new IllegalArgumentException("Input list cannot be empty")
    }
    if (comparison == null) {
      throw new IllegalArgumentException("Comparison value cannot be null")
    }

    int n = values.size()
    if (n < 2) {
      throw new IllegalArgumentException("One-sample t-test requires at least 2 observations, got: $n")
    }
    BigDecimal mean = Stat.mean(values, SCALE)
    BigDecimal variance = Stat.variance(values)
    BigDecimal sd = variance.sqrt()
    BigDecimal dividend = mean - (comparison as BigDecimal)
    BigDecimal divisor = sd / (n as BigDecimal).sqrt()
    SingleResult result = new SingleResult("One Sample t-test")
    result.tVal = dividend / divisor
    result.mean = mean
    result.var = variance
    result.sd = sd
    result.n = n
    result.df = n - 1
    // Use native TDistribution for p-value calculation
    result.pVal = TDistribution.pValue(result.tVal as double, result.df as double)
    return result
  }

  /**
   * Result object for Student's two-sample t-test.
   * Extends TtestResult to maintain backwards compatibility.
   */
  static class Result extends TtestResult {
    // All properties and methods inherited from TtestResult
  }

  static class SingleResult {
    Integer n
    BigDecimal mean
    BigDecimal var
    BigDecimal sd
    BigDecimal tVal
    BigDecimal pVal
    Integer df
    String description

    SingleResult(String description) {
      this.description = description
    }

    BigDecimal getT() {
      return tVal
    }

    BigDecimal getT(int numberOfDecimals) {
      return getT().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getP() {
      return pVal
    }

    BigDecimal getP(int numberOfDecimals) {
      return getP().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    Integer getDf() {
      return df
    }

    BigDecimal getMean() {
      return mean
    }

    BigDecimal getMean(int numberOfDecimals) {
      return getMean().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }


    BigDecimal getVar() {
      return var
    }

    BigDecimal getVar(int numberOfDecimals) {
      return getVar().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getSd() {
      return sd
    }

    BigDecimal getSd(int numberOfDecimals) {
      return getSd().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    Integer getN() {
      return n
    }

    String getDescription() {
      return description
    }

    @Override
    String toString() {
      return """
      ${getDescription()}
      t = ${getT(3)}, df = ${getDf()}, p = ${getP(3)}
      mean = ${getMean(3)}, size = ${getN()}, sd = ${getSd(3)}
      """.stripIndent()
    }
  }

  static class PairedResult extends Result {
    BigDecimal sd

    /**
     * @return the sample standard deviation of the differences
     */
    BigDecimal getSd() {
      return sd
    }

    BigDecimal getSd(int numberOfDecimals) {
      return getSd().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    @Override
    String toString() {
      return """
      ${getDescription()}
      t = ${getT(3)}, df = ${getDf()}, p = ${getP(4)}, sd diff = ${getSd(3)}
      x: mean = ${getMean1(3)}, size = ${getN1()}, sd = ${getSd1(3)}
      y: mean = ${getMean2(3)}, size = ${getN2()}, sd = ${getSd2(3)}
      """.stripIndent()
    }
  }
}
