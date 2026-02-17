package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.render.scale.ScaleUtils as CharmScaleUtils

/**
 * Utility methods for scale operations.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.render.scale.ScaleUtils} instead.
 *             This stub delegates all calls to the charm implementation.
 */
@Deprecated
@CompileStatic
class ScaleUtils {

  /** @see CharmScaleUtils#coerceToNumber(Object) */
  static BigDecimal coerceToNumber(Object value) {
    CharmScaleUtils.coerceToNumber(value)
  }

  /** @see CharmScaleUtils#interpolateRange(int, List) */
  static List<Number> interpolateRange(int n, List<? extends Number> range) {
    CharmScaleUtils.interpolateRange(n, range)
  }

  /** @see CharmScaleUtils#linearTransform(BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal) */
  static BigDecimal linearTransform(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                     BigDecimal rangeMin, BigDecimal rangeMax) {
    CharmScaleUtils.linearTransform(value, domainMin, domainMax, rangeMin, rangeMax)
  }

  /** @see CharmScaleUtils#linearInverse(BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal) */
  static BigDecimal linearInverse(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                   BigDecimal rangeMin, BigDecimal rangeMax) {
    CharmScaleUtils.linearInverse(value, domainMin, domainMax, rangeMin, rangeMax)
  }

  /** @see CharmScaleUtils#linearTransformReversed(BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal) */
  static BigDecimal linearTransformReversed(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                             BigDecimal rangeMin, BigDecimal rangeMax) {
    CharmScaleUtils.linearTransformReversed(value, domainMin, domainMax, rangeMin, rangeMax)
  }

  /** @see CharmScaleUtils#linearInverseReversed(BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal) */
  static BigDecimal linearInverseReversed(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                           BigDecimal rangeMin, BigDecimal rangeMax) {
    CharmScaleUtils.linearInverseReversed(value, domainMin, domainMax, rangeMin, rangeMax)
  }

  /** @see CharmScaleUtils#midpoint(BigDecimal, BigDecimal) */
  static BigDecimal midpoint(BigDecimal a, BigDecimal b) {
    CharmScaleUtils.midpoint(a, b)
  }

  /** @see CharmScaleUtils#niceNum(BigDecimal, boolean) */
  static BigDecimal niceNum(BigDecimal x, boolean round) {
    CharmScaleUtils.niceNum(x, round)
  }
}
