package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.math.MathContext

/**
 * Utility methods for scale operations.
 */
@CompileStatic
class ScaleUtils {

  static final BigDecimal TWO = BigDecimal.valueOf(2)
  static final MathContext MATH_CONTEXT = MathContext.DECIMAL128

  /**
   * Coerce a value to a BigDecimal, handling null/NaN and string representations.
   *
   * @param value the value to coerce
   * @return the BigDecimal value, or null if the value cannot be converted
   *
   * String handling:
   * - Trims input
   * - Treats empty, 'NA', 'NaN', and 'null' (case-insensitive) as null
   * - Otherwise attempts BigDecimal parsing; failures return null
   */
  static BigDecimal coerceToNumber(Object value) {
    if (value == null) return null
    if (value instanceof BigDecimal) return value as BigDecimal
    if (value instanceof Number) {
      if (value instanceof Double || value instanceof Float) {
        double v = (value as Number).doubleValue()
        if (Double.isNaN(v) || Double.isInfinite(v)) return null
      }
      try {
        return new BigDecimal(value.toString())
      } catch (NumberFormatException ignored) {
        return null
      }
    }
    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty() || s.equalsIgnoreCase('NA') || s.equalsIgnoreCase('NaN') || s.equalsIgnoreCase('null')) {
        return null
      }
      try {
        return new BigDecimal(s)
      } catch (NumberFormatException ignored) {
        return null
      }
    }
    return null
  }

  /**
   * Build a list of evenly spaced values across a range.
   * Used by discrete scales to map n discrete levels to a continuous range.
   *
   * @param n the number of values to generate
   * @param range the output range [min, max]
   * @return list of interpolated Number values (as BigDecimal internally)
   */
  static List<Number> interpolateRange(int n, List<? extends Number> range) {
    if (n <= 0) return []

    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal
    List<Number> values = []

    if (n == 1) {
      values << (rMin + rMax).divide(TWO, MATH_CONTEXT)
      return values
    }

    BigDecimal divisor = BigDecimal.valueOf(n - 1)
    for (int i = 0; i < n; i++) {
      BigDecimal t = BigDecimal.valueOf(i).divide(divisor, MATH_CONTEXT)
      values << (rMin + t * (rMax - rMin))
    }
    return values
  }

  /**
   * Perform linear interpolation from domain to range.
   * Handles the zero-range domain edge case by returning the midpoint of the range.
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

    // Handle edge case: zero-range domain
    if (domainMax == domainMin) {
      return (rangeMin + rangeMax).divide(TWO, MATH_CONTEXT)
    }

    // Linear interpolation
    BigDecimal normalized = (value - domainMin).divide((domainMax - domainMin), MATH_CONTEXT)
    return rangeMin + normalized * (rangeMax - rangeMin)
  }

  /**
   * Perform inverse linear interpolation from range to domain.
   * Handles the zero-range output edge case by returning the midpoint of the domain.
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

    // Handle edge case: zero-range output
    if (rangeMax == rangeMin) {
      return (domainMin + domainMax).divide(TWO, MATH_CONTEXT)
    }

    // Inverse linear interpolation
    BigDecimal normalized = (value - rangeMin).divide((rangeMax - rangeMin), MATH_CONTEXT)
    return domainMin + normalized * (domainMax - domainMin)
  }

  /**
   * Perform reversed linear interpolation from domain to range.
   * Handles the zero-range domain edge case by returning the midpoint of the range.
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

    // Handle edge case: zero-range domain
    if (domainMax == domainMin) {
      return (rangeMin + rangeMax).divide(TWO, MATH_CONTEXT)
    }

    // Reversed linear interpolation: rMax - normalized * (rMax - rMin)
    BigDecimal normalized = (value - domainMin).divide((domainMax - domainMin), MATH_CONTEXT)
    return rangeMax - normalized * (rangeMax - rangeMin)
  }

  /**
   * Perform reversed inverse linear interpolation from range to domain.
   * Handles the zero-range output edge case by returning the midpoint of the domain.
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

    // Handle edge case: zero-range output
    if (rangeMax == rangeMin) {
      return (domainMin + domainMax).divide(TWO, MATH_CONTEXT)
    }

    // Reversed inverse linear interpolation
    BigDecimal normalized = (rangeMax - value).divide((rangeMax - rangeMin), MATH_CONTEXT)
    return domainMin + normalized * (domainMax - domainMin)
  }

  /**
   * Calculate the midpoint of two BigDecimal values.
   *
   * @param a first value
   * @param b second value
   * @return the midpoint (a + b) / 2
   */
  static BigDecimal midpoint(BigDecimal a, BigDecimal b) {
    return (a + b).divide(TWO, MATH_CONTEXT)
  }

  /**
   * Find a "nice" number approximately equal to x for axis labeling.
   * Based on Wilkinson's algorithm - rounds to 1, 2, 5, or 10 times a power of 10.
   *
   * This is useful for generating readable axis tick values (e.g., 0.5, 1, 2, 5, 10, 20, 50, 100).
   *
   * @param x the number to round
   * @param round if true, round the number; if false, take the ceiling
   * @return a "nice" number close to x
   */
  static BigDecimal niceNum(BigDecimal x, boolean round) {
    if (x == 0) return BigDecimal.ZERO

    BigDecimal exp = x.abs().log10().floor()
    BigDecimal f = x / (10 ** exp)

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

    return nf * (10 ** exp)
  }
}
