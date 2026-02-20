package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Backward-compatible annotation constants facade for gg package code.
 *
 * The canonical implementation now lives in {@code se.alipsa.matrix.charm.AnnotationConstants}.
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
  static final BigDecimal INFINITY_MARKER = se.alipsa.matrix.charm.AnnotationConstants.INFINITY_MARKER

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
    se.alipsa.matrix.charm.AnnotationConstants.handleInfValue(value, isMin)
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
    se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(data, colName, rowIdx)
  }

  /**
   * Check if a value represents infinity (using marker values).
   *
   * @param value the value to check
   * @return true if the value is null or represents infinity
   */
  static boolean isInfinite(BigDecimal value) {
    se.alipsa.matrix.charm.AnnotationConstants.isInfinite(value)
  }
}
