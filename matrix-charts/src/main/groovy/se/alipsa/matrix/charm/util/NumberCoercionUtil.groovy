package se.alipsa.matrix.charm.util

import groovy.transform.CompileStatic

/**
 * Shared numeric coercion helpers used by Charm and gg scale paths.
 */
@CompileStatic
class NumberCoercionUtil {

  /**
   * Coerces an arbitrary value to BigDecimal.
   *
   * @param value value to coerce
   * @return BigDecimal value or null when conversion is not possible
   */
  static BigDecimal coerceToBigDecimal(Object value) {
    if (value == null) return null
    if (value instanceof BigDecimal) return value as BigDecimal
    if (value instanceof Number) {
      if (value instanceof Double || value instanceof Float) {
        double v = value as double
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
}
