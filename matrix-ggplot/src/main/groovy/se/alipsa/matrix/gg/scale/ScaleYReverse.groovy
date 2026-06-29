package se.alipsa.matrix.gg.scale


/**
 * Reversed continuous scale for the y-axis.
 * Maps values in reverse order (high values at bottom, low values at top).
 *
 * Usage:
 * - scale_y_reverse() - reversed y-axis
 * - scale_y_reverse(limits: [0, 100]) - with explicit limits
 */
@SuppressWarnings('DuplicateStringLiteral')
class ScaleYReverse extends ScaleXReverse {

  ScaleYReverse() {
    aesthetic = 'y'
  }

  ScaleYReverse(Map params) {
    super(params)
    aesthetic = 'y'
  }
}
