package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Shared constants and utility methods for annotation implementations.
 */
@CompileStatic
class AnnotationConstants {

  /**
   * Marker value used to represent positive/negative infinity for position bounds.
   *
   * Uses 1e15 to avoid conflicts with typical data values while remaining safely
   * below Double.MAX_VALUE.
   */
  static final BigDecimal INFINITY_MARKER = 1e15 as BigDecimal

  private AnnotationConstants() {
    // Utility class
  }

  /**
   * Converts null and infinity values to finite marker values.
   *
   * @param value input bound value
   * @param isMin true for lower bounds
   * @return converted numeric value or infinity marker
   */
  static BigDecimal handleInfValue(Object value, boolean isMin) {
    if (value == null) {
      return isMin ? -INFINITY_MARKER : INFINITY_MARKER
    }

    if (value instanceof String) {
      String str = (value as String).toLowerCase()
      if (str == 'inf' || str == '+inf') {
        return INFINITY_MARKER
      }
      if (str == '-inf') {
        return -INFINITY_MARKER
      }
      try {
        return new BigDecimal(str)
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid position value: ${value}")
      }
    }

    if (value instanceof Number) {
      double d = value as double
      if (Double.isInfinite(d)) {
        return d > 0 ? INFINITY_MARKER : -INFINITY_MARKER
      }
      return value as BigDecimal
    }

    value as BigDecimal
  }

  /**
   * Reads a bound value from matrix data or returns infinity marker defaults.
   *
   * @param data source matrix
   * @param colName bound column name
   * @param rowIdx row index
   * @return bound value or infinity marker
   */
  static BigDecimal getPositionValue(Matrix data, String colName, int rowIdx) {
    if (data == null || !data.columnNames().contains(colName)) {
      boolean isMinBound = colName?.toLowerCase()?.contains('min') ?: true
      return isMinBound ? -INFINITY_MARKER : INFINITY_MARKER
    }
    Object value = data[colName][rowIdx]
    if (value == null) {
      boolean isMinBound = colName?.toLowerCase()?.contains('min') ?: true
      return isMinBound ? -INFINITY_MARKER : INFINITY_MARKER
    }
    value as BigDecimal
  }

  /**
   * Returns true when the bound should be treated as infinity.
   *
   * @param value bound value
   * @return true when value is null or uses infinity marker
   */
  static boolean isInfinite(BigDecimal value) {
    if (value == null) {
      return true
    }
    value.abs() >= INFINITY_MARKER
  }
}
