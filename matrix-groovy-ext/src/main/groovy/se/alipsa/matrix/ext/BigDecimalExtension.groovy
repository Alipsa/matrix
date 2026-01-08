package se.alipsa.matrix.ext

import groovy.transform.CompileStatic

import java.math.RoundingMode

/**
 * Extension methods for BigDecimal to support idiomatic Groovy floor(), ceil() and log10() operations.
 */
@CompileStatic
class BigDecimalExtension {

  /**
   * Returns the largest integer value less than or equal to this BigDecimal.
   *
   * @param self the BigDecimal value
   * @return a BigDecimal representing the floor of this value
   */
  static BigDecimal floor(BigDecimal self) {
    return self.setScale(0, RoundingMode.FLOOR)
  }

  /**
   * Returns the smallest integer value greater than or equal to this BigDecimal.
   *
   * @param self the BigDecimal value
   * @return a BigDecimal representing the ceiling of this value
   */
  static BigDecimal ceil(BigDecimal self) {
    return self.setScale(0, RoundingMode.CEILING)
  }

  /**
   * Returns the base-10 logarithm (log10) of this number as a BigDecimal.
   *
   * @param self the Number value
   * @return  a BigDecimal representing the base-10 logarithm (log10) of this value
   */
  static BigDecimal log10(Number self) {
    return Math.log10(self.doubleValue()) as BigDecimal
  }

  /**
   * Returns the size of an ulp (unit in the last place) of this BigDecimal value.
   * An ulp is the positive distance between this floating-point value and the next larger magnitude value.
   *
   * @param self the BigDecimal value
   * @return a BigDecimal representing the size of an ulp
   */
  static BigDecimal ulp(BigDecimal self) {
    return Math.ulp(self.doubleValue()) as BigDecimal
  }

  /**
   * Returns the size of an ulp (unit in the last place) of this Number value.
   * An ulp is the positive distance between this floating-point value and the next larger magnitude value.
   *
   * @param self the Number value
   * @return a BigDecimal representing the size of an ulp
   */
  static BigDecimal ulp(Number self) {
    return Math.ulp(self.doubleValue()) as BigDecimal
  }

  /**
   * Returns the smaller of this BigDecimal and the given Number.
   *
   * @param self the BigDecimal value
   * @param other the Number to compare with
   * @return the smaller value as a BigDecimal
   */
  static BigDecimal min(BigDecimal self, Number other) {
    BigDecimal otherBD = other as BigDecimal
    return self < otherBD ? self : otherBD
  }

  /**
   * Returns the larger of this BigDecimal and the given Number.
   *
   * @param self the BigDecimal value
   * @param other the Number to compare with
   * @return the larger value as a BigDecimal
   */
  static BigDecimal max(BigDecimal self, Number other) {
    BigDecimal otherBD = other as BigDecimal
    return self > otherBD ? self : otherBD
  }

  /**
   * Returns the smaller of this Number and the given BigDecimal.
   *
   * @param self the Number value
   * @param other the BigDecimal to compare with
   * @return the smaller value as a BigDecimal
   */
  static BigDecimal min(Number self, BigDecimal other) {
    BigDecimal selfBD = self as BigDecimal
    return selfBD < other ? selfBD : other
  }

  /**
   * Returns the larger of this Number and the given BigDecimal.
   *
   * @param self the Number value
   * @param other the BigDecimal to compare with
   * @return the larger value as a BigDecimal
   */
  static BigDecimal max(Number self, BigDecimal other) {
    BigDecimal selfBD = self as BigDecimal
    return selfBD > other ? selfBD : other
  }
}
