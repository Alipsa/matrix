package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ValueConverter

/**
 * Utility methods for scale operations.
 */
@CompileStatic
class ScaleUtils {

  /**
   * Coerce a value to a BigDecimal, handling null/NaN and string representations.
   *
   * @param value the value to coerce
   * @return the BigDecimal value, or null if the value cannot be converted
   */
  static BigDecimal coerceToNumber(Object value) {
    ValueConverter.asBigDecimal(value)
  }

  /**
   * Build a list of evenly spaced values across a range.
   *
   * @param n the number of values to generate
   * @param range the output range [min, max]
   * @return list of interpolated Number values
   */
  static List<Number> interpolateRange(int n, List<? extends Number> range) {
    if (n <= 0) return []

    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal
    List<Number> values = []

    if (n == 1) {
      values << (rMin + rMax) / 2
      return values
    }

    def divisor = n - 1
    for (int i = 0; i < n; i++) {
      BigDecimal t = i / divisor
      values << rMin + t * (rMax - rMin)
    }
    values
  }

  /**
   * Perform linear interpolation from domain to range.
   *
   * @param value the value to transform (in domain space)
   * @param domainMin the minimum of the domain
   * @param domainMax the maximum of the domain
   * @param rangeMin the minimum of the range
   * @param rangeMax the maximum of the range
   * @return the transformed value in range space, or null if value is null
   */
  static BigDecimal linearTransform(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                     BigDecimal rangeMin, BigDecimal rangeMax) {
    if (value == null) return null
    if (domainMax == domainMin) {
      return (rangeMin + rangeMax) / 2
    }
    BigDecimal normalized = (value - domainMin) / (domainMax - domainMin)
    rangeMin + normalized * (rangeMax - rangeMin)
  }

  /**
   * Perform inverse linear interpolation from range to domain.
   *
   * @param value the value to inverse transform (in range space)
   * @param domainMin the minimum of the domain
   * @param domainMax the maximum of the domain
   * @param rangeMin the minimum of the range
   * @param rangeMax the maximum of the range
   * @return the inverse transformed value in domain space, or null if value is null
   */
  static BigDecimal linearInverse(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                   BigDecimal rangeMin, BigDecimal rangeMax) {
    if (value == null) return null
    if (rangeMax == rangeMin) {
      return (domainMin + domainMax) / 2
    }
    BigDecimal normalized = (value - rangeMin) / (rangeMax - rangeMin)
    domainMin + normalized * (domainMax - domainMin)
  }

  /**
   * Perform reversed linear interpolation from domain to range.
   *
   * @param value the value to transform (in domain space)
   * @param domainMin the minimum of the domain
   * @param domainMax the maximum of the domain
   * @param rangeMin the minimum of the range
   * @param rangeMax the maximum of the range
   * @return the transformed value in range space (reversed), or null if value is null
   */
  static BigDecimal linearTransformReversed(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                             BigDecimal rangeMin, BigDecimal rangeMax) {
    if (value == null) return null
    if (domainMax == domainMin) {
      return (rangeMin + rangeMax) / 2
    }
    BigDecimal normalized = (value - domainMin) / (domainMax - domainMin)
    rangeMax - normalized * (rangeMax - rangeMin)
  }

  /**
   * Perform reversed inverse linear interpolation from range to domain.
   *
   * @param value the value to inverse transform (in range space)
   * @param domainMin the minimum of the domain
   * @param domainMax the maximum of the domain
   * @param rangeMin the minimum of the range
   * @param rangeMax the maximum of the range
   * @return the inverse transformed value in domain space, or null if value is null
   */
  static BigDecimal linearInverseReversed(BigDecimal value, BigDecimal domainMin, BigDecimal domainMax,
                                           BigDecimal rangeMin, BigDecimal rangeMax) {
    if (value == null) return null
    if (rangeMax == rangeMin) {
      return (domainMin + domainMax) / 2
    }
    BigDecimal normalized = (rangeMax - value) / (rangeMax - rangeMin)
    domainMin + normalized * (domainMax - domainMin)
  }

  /**
   * Calculate the midpoint of two BigDecimal values.
   *
   * @param a first value
   * @param b second value
   * @return the midpoint (a + b) / 2
   */
  static BigDecimal midpoint(BigDecimal a, BigDecimal b) {
    (a + b) / 2
  }

  /**
   * Build a stable break-to-label map where label indices correspond to original configured break indices.
   *
   * <p>When breaks are filtered later (for example, to trained levels), this mapping preserves
   * break/label pairing and avoids index drift.</p>
   *
   * @param breaks configured break values as strings
   * @param labels configured labels
   * @return map of break string to configured label
   */
  static Map<String, String> labelMapForConfiguredBreaks(List<String> breaks, List<String> labels) {
    if (breaks == null || breaks.isEmpty() || labels == null || labels.isEmpty()) {
      return [:]
    }
    Map<String, String> labelByBreak = [:]
    breaks.eachWithIndex { String breakValue, int idx ->
      if (breakValue == null || labelByBreak.containsKey(breakValue)) {
        return
      }
      if (idx < labels.size() && labels[idx] != null) {
        labelByBreak[breakValue] = labels[idx]
      }
    }
    labelByBreak
  }

  /**
   * Find a "nice" number approximately equal to x for axis labeling.
   * Based on Wilkinson's algorithm.
   *
   * @param x the number to round
   * @param round if true, round the number; if false, take the ceiling
   * @return a "nice" number close to x
   */
  static BigDecimal niceNum(BigDecimal x, boolean round) {
    if (x == 0) return BigDecimal.ZERO

    BigDecimal absX = x.abs()
    BigDecimal exp = absX.log10().floor()
    BigDecimal f = absX / (10 ** exp)

    BigDecimal nf
    if (round) {
      if (f < 1.5) nf = 1
      else if (f < 3) nf = 2
      else if (f < 7) nf = 5
      else nf = 10
    } else {
      if (f <= 1) nf = 1
      else if (f <= 2) nf = 2
      else if (f <= 5) nf = 5
      else nf = 10
    }

    BigDecimal result = nf * (10 ** exp)
    x < 0 ? -result : result
  }
}
