package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Identity scale for size aesthetic - uses data values directly without mapping.
 *
 * When the data contains numeric size values, this scale uses those exact values
 * rather than mapping them through a range.
 *
 * Example:
 * <pre>
 * // Data has a column 'pointSize' with values [3, 5, 8]
 * chart + geom_point(aes(size: 'pointSize')) + scale_size_identity()
 * </pre>
 */
@CompileStatic
class ScaleSizeIdentity extends Scale {
  String aesthetic = 'size'
  BigDecimal naValue = 3.0

  ScaleSizeIdentity(Map params = [:]) {
    if (params.naValue) this.naValue = params.naValue as BigDecimal
    if (params.name) this.name = params.name as String
    if (params.guide) this.guide = params.guide
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    BigDecimal result = ScaleUtils.coerceToNumber(value)
    if (result == null) return naValue

    // Clamp negative values to minimum size (0.1 to ensure visibility)
    if (result < 0.1) {
      return 0.1
    }

    return result
  }
}
