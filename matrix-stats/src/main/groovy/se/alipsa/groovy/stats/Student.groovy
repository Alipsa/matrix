package se.alipsa.groovy.stats

import org.apache.commons.math3.stat.inference.TTest
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.matrix.Stat

import java.math.RoundingMode

/**
 * A t-test is any statistical hypothesis test in which the test statistic follows a Student's t-distribution
 * under the null hypothesis. It is most commonly applied when the test statistic would follow a normal
 * distribution if the value of a scaling term in the test statistic were known (typically,
 * the scaling term is unknown and therefore a nuisance parameter). When the scaling term is estimated based
 * on the data, the test statistic—under certain conditions—follows a Student's t distribution.
 * The t-test's most common application is to test whether the means of two populations are different.
 *
 * The formula is:
 * t = ( x̄<sub>1</sub> - x̄<sub>2</sub> ) /
 * ( sqrt(S<sub>1</sub><sup>2</sup> / N<sub>1</sub> * S<sub>2</sub><sup>2</sup> / N<sub>2</sub>)
 * where
 * <ul>
 *   <li>x̄<sub>1</sub> is the mean of first data set</li>
 *   <li>x̄<sub>2</sub> is the mean of second data set</li>
 *   <li>S<sub>1</sub><sup>2</sup> is the standard deviation of first data set</li>
 *   <li>S<sub>2</sub><sup>2</sup> is the standard deviation of second data set</li>
 *   <li>N<sub>1</sub> is the number of elements in the first data set</li>
 *   <li>N<sub>2</sub> is the number of elements in the second data set</li>
 * </ul>
 * @return
 */
class Student {

  /**
   * The paired samples t-test is used to compare the means between two related groups of samples.
   * In this case, you have two values (i.e., pair of values) for the same samples.
   *
   * @param first the values for the first condition
   * @param second the values for the second condition
   * @return a PairedResult containing all relevant statistics of the t-test
   */
  static PairedResult pairedTTest(List<? extends Number> first, List<? extends Number> second) {
    Integer n1 = first.size()
    Integer n2 = second.size()
    if (n1 != n2) {
      throw new IllegalArgumentException("The two lists of values are of different size")
    }
    BigDecimal mean1 = Stat.mean(first)
    BigDecimal mean2 = Stat.mean(second)
    BigDecimal var1 = Stat.variance(first, true)
    BigDecimal var2 = Stat.variance(second, true)
    BigDecimal sd1 = Math.sqrt(var1 as double)
    BigDecimal sd2 = Math.sqrt(var2 as double)
    def df = n1 - 1

    double cov = 0.0
    for (int j = 0; j < n1; j++) {
      cov += (first[j] - mean1) * (second[j] - mean2)
    }
    cov /= df
    // sample standard deviation of the differences
    double sd = Math.sqrt((var1 + var2 - 2.0 * cov) / n1)
    def t = (mean1 - mean2) / sd

    def tTest = new TTest()
    def p = tTest.pairedTTest(ListConverter.toDoubleArray(first), ListConverter.toDoubleArray(second))
    PairedResult result = new PairedResult()
    result.description = "Paired t-test"
    result.tVal = t
    result.n1 = n1
    result.n2 = n2
    result.mean1 = mean1
    result.mean2 = mean2
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
   * An Independent Samples t-test compares the means for two groups.
   * @param first
   * @param second
   * @param equalVariance
   * @return
   */
  static Result tTest(List<? extends Number> first, List<? extends Number> second, Boolean equalVariance = null) {
    BigDecimal mean1 = Stat.mean(first)
    BigDecimal mean2 = Stat.mean(second)
    BigDecimal var1 = Stat.variance(first, true)
    BigDecimal var2 = Stat.variance(second, true)
    BigDecimal sd1 = Math.sqrt(var1 as double)
    BigDecimal sd2 = Math.sqrt(var2 as double)
    Integer n1 = first.size()
    Integer n2 = second.size()
    Result result = new Result()
    def t = (mean1 - mean2) / Math.sqrt((sd1 ** 2) / n1 + (sd2 ** 2) / n2)
    result.tVal = t
    // To be sure, perform an F test test to check, 4 is general rule of thumb, https://www.statology.org/equal-variance-assumption/
    boolean isEqualVariance
    if (equalVariance == null) {
      isEqualVariance = (var1 - var2).abs() < 4
    } else {
      isEqualVariance = equalVariance
    }
    if (isEqualVariance) {
      result.df = n1 + n2 - 2
      result.description = "Welch two sample t-test with equal variance"
    } else {
      def dividend = (var1/n1 + var2/n2) ** 2
      def divisor1 = (var1 ** 2) / ((n1 ** 2) * (n1-1))
      def divisor2 = (var2 ** 2) / ((n2 ** 2) * (n2 -1))
      result.df =  dividend / (divisor1 + divisor2)
      result.description = "Welch two sample t-test with unequal variance"
    }
    result.mean1 = mean1
    result.mean2 = mean2
    result.var1 = var1
    result.var2 = var2
    result.sd1 = sd1
    result.sd2 = sd2
    result.n1 = n1
    result.n2 = n2
    // TODO: implement p value function, relying on commons math3 as a temporary solution
    TTest tTest = new TTest()
    result.pVal = tTest.tTest(ListConverter.toDoubleArray(first), ListConverter.toDoubleArray(second))
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
   */
  static SingleResult tTest(List<? extends Number> values, Number comparison) {
    def mean = Stat.mean(values)
    def variance = Stat.variance(values)
    def sd = Math.sqrt(variance as double)
    def n = values.size()
    def dividend = mean - comparison
    def divisor = sd / Math.sqrt(n)
    SingleResult result = new SingleResult("One Sample t-test")
    result.tVal = dividend / divisor
    result.mean = mean
    result.var = variance
    result.sd = sd
    result.n = n
    result.df = n -1
    // TODO: implement p value function, relying on commons math3 as a temporary solution
    TTest tTest = new TTest()
    result.pVal = tTest.tTest(comparison.doubleValue(), ListConverter.toDoubleArray(values))
    return result
  }



  static class Result {
    Integer n1
    Integer n2
    BigDecimal mean1
    BigDecimal mean2
    BigDecimal var1
    BigDecimal var2
    BigDecimal sd1
    BigDecimal sd2
    BigDecimal tVal
    BigDecimal pVal
    BigDecimal df
    String description

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

    BigDecimal getDf() {
      return df
    }

    BigDecimal getDf(int numberOfDecimals) {
      return getDf().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getMean1() {
      return mean1
    }

    BigDecimal getMean1(int numberOfDecimals) {
      return getMean1().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getMean2() {
      return mean2
    }

    BigDecimal getMean2(int numberOfDecimals) {
      return getMean2().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getVar1() {
      return var1
    }

    BigDecimal getVar1(int numberOfDecimals) {
      return getVar1().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getVar2() {
      return var2
    }

    BigDecimal getVar2(int numberOfDecimals) {
      return getVar2().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getSd1() {
      return sd1
    }

    BigDecimal getSd1(int numberOfDecimals) {
      return getSd1().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    BigDecimal getSd2() {
      return sd2
    }

    BigDecimal getSd2(int numberOfDecimals) {
      return getSd2().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
    }

    Integer getN1() {
      return n1
    }

    Integer getN2() {
      return n2
    }

    String getDescription() {
      return description
    }

    @Override
    String toString() {
      return """
      ${getDescription()}
      t = ${getT(3)}, df = ${getDf(3)}, p = ${getP(3)}
      x: mean = ${getMean1(3)}, size = ${getN1()}, sd = ${getSd1(3)}
      y: mean = ${getMean2(3)}, size = ${getN2()}, sd = ${getSd2(3)} 
      """.stripIndent()
    }
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
