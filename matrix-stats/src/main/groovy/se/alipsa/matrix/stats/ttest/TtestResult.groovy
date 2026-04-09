package se.alipsa.matrix.stats.ttest

import java.math.RoundingMode

/**
 * Result object for two-sample t-tests (Student's and Welch's).
 * Contains all relevant statistics from a t-test comparison of two independent samples.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class TtestResult {
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
    tVal
  }

  BigDecimal getT(int numberOfDecimals) {
    getT().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getP() {
    pVal
  }

  BigDecimal getP(int numberOfDecimals) {
    getP().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getDf() {
    df
  }

  BigDecimal getDf(int numberOfDecimals) {
    getDf().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getMean1() {
    mean1
  }

  BigDecimal getMean1(int numberOfDecimals) {
    getMean1().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getMean2() {
    mean2
  }

  BigDecimal getMean2(int numberOfDecimals) {
    getMean2().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getVar1() {
    var1
  }

  BigDecimal getVar1(int numberOfDecimals) {
    getVar1().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getVar2() {
    var2
  }

  BigDecimal getVar2(int numberOfDecimals) {
    getVar2().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getSd1() {
    sd1
  }

  BigDecimal getSd1(int numberOfDecimals) {
    getSd1().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  BigDecimal getSd2() {
    sd2
  }

  BigDecimal getSd2(int numberOfDecimals) {
    getSd2().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  Integer getN1() {
    n1
  }

  Integer getN2() {
    n2
  }

  String getDescription() {
    description
  }

  @Override
  String toString() {
    """
      ${getDescription()}
      t = ${getT(3)}, df = ${getDf(3)}, p = ${getP(3)}
      x: mean = ${getMean1(3)}, size = ${getN1()}, sd = ${getSd1(3)}
      y: mean = ${getMean2(3)}, size = ${getN2()}, sd = ${getSd2(3)}
      """.stripIndent()
  }
}
