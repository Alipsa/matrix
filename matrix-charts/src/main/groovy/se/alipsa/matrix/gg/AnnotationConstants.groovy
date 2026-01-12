package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Shared constants and utility methods for annotation implementations.
 */
@CompileStatic
class AnnotationConstants {

  /**
   * Marker value used to represent positive/negative infinity for position bounds.
   * BigDecimal cannot directly represent infinity, so we use this large value as a marker
   * that annotation geoms will recognize and replace with actual panel bounds.
   *
   * Uses 1e15 (1 quadrillion) to avoid conflicts with typical data values while
   * remaining safely below Double.MAX_VALUE (approximately 1.8e308).
   */
  static final BigDecimal INFINITY_MARKER = 1e15 as BigDecimal

  private AnnotationConstants() {
    // Utility class - prevent instantiation
  }

  /**
   * Handle infinite position values for annotation bounds.
   * Converts null, 'Inf', '-Inf' to appropriate infinity marker constants.
   *
   * @param value the input value (can be null, String, or Number)
   * @param isMin whether this is a minimum bound (affects default for null)
   * @return BigDecimal representing the position or infinity marker
   */
  static BigDecimal handleInfValue(Object value, boolean isMin) {
    if (value == null) {
      return isMin ? -INFINITY_MARKER : INFINITY_MARKER
    }

    // Handle string representations of infinity
    if (value instanceof String) {
      String str = (value as String).toLowerCase()
      if (str == 'inf' || str == '+inf') {
        return INFINITY_MARKER
      }
      if (str == '-inf') {
        return -INFINITY_MARKER
      }
      // Try to parse as number
      try {
        return new BigDecimal(str)
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid position value: ${value}")
      }
    }

    // Handle numeric infinity
    if (value instanceof Number) {
      double d = (value as Number).doubleValue()
      if (Double.isInfinite(d)) {
        return d > 0 ? INFINITY_MARKER : -INFINITY_MARKER
      }
      return value as BigDecimal
    }

    // Default conversion
    return value as BigDecimal
  }

  /**
   * Get position value from data matrix for annotation bounds.
   * Returns appropriate INFINITY_MARKER for missing or null values based on column name:
   * - Negative infinity for 'min' columns (xmin, ymin)
   * - Positive infinity for 'max' columns (xmax, ymax)
   *
   * @param data the data matrix containing position bounds
   * @param colName the column name to retrieve (e.g., 'xmin', 'xmax', 'ymin', 'ymax')
   * @param rowIdx the row index to retrieve from
   * @return BigDecimal representing the position value or infinity marker
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
    return value as BigDecimal
  }

  /**
   * Check if a value represents infinity (using marker values).
   *
   * @param value the value to check
   * @return true if the value is null or represents infinity
   */
  static boolean isInfinite(BigDecimal value) {
    if (value == null) return true
    return value.abs() >= INFINITY_MARKER
  }
}
