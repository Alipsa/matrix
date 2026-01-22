package se.alipsa.matrix.stats.ttest

import groovy.transform.CompileStatic

import java.math.RoundingMode

/**
 * Result object for two-sample t-tests (Student's and Welch's).
 * Contains all relevant statistics from a t-test comparison of two independent samples.
 */
@CompileStatic
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
