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
   * Coerce a value to a BigDecimal, handling null, NaN, and string representations.
   *
   * @param value the value to coerce
   * @return the BigDecimal value, or null if the value cannot be converted
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
      if (s.isEmpty() || s.equalsIgnoreCase('NA') || s.equalsIgnoreCase('NaN')) {
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
   * @return list of interpolated values
   */
  static List<Number> interpolateRange(int n, List<Number> range) {
    if (n <= 0) return []
    double rMin = range[0] as double
    double rMax = range[1] as double
    List<Number> values = []
    if (n == 1) {
      values << ((rMin + rMax) / 2.0d)
      return values
    }
    for (int i = 0; i < n; i++) {
      double t = i / (double) (n - 1)
      values << (rMin + t * (rMax - rMin))
    }
    return values
  }
}
