package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Utility methods for scale operations.
 */
@CompileStatic
class ScaleUtils {

  /**
   * Coerce a value to a Double, handling null, NaN, and string representations.
   *
   * @param value the value to coerce
   * @return the Double value, or null if the value cannot be converted
   */
  static Double coerceToNumber(Object value) {
    if (value == null) return null
    if (value instanceof Number) {
      double v = (value as Number).doubleValue()
      return Double.isNaN(v) ? null : v
    }
    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty() || s.equalsIgnoreCase('NA') || s.equalsIgnoreCase('NaN') || s.equalsIgnoreCase('null')) {
        return null
      }
      try {
        return Double.parseDouble(s)
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
