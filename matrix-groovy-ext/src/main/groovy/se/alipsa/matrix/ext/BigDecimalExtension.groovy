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
   * Returns the log10
   *
   * @param self the Number value
   * @return  a BigDecimal representing the log10 of this value
   */
  static BigDecimal log10(Number self) {
    return Math.log10(self.doubleValue()) as BigDecimal
  }
}
