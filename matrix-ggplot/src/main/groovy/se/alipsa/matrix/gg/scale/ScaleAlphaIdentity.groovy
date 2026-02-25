package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Identity scale for alpha aesthetic - uses data values directly without mapping.
 *
 * When the data contains numeric alpha/transparency values, this scale uses those
 * exact values rather than mapping them through a range. Values are clamped to [0, 1].
 *
 * Example:
 * <pre>
 * // Data has a column 'transparency' with values [0.3, 0.7, 1.0]
 * chart + geom_point(aes(alpha: 'transparency')) + scale_alpha_identity()
 * </pre>
 */
@CompileStatic
class ScaleAlphaIdentity extends Scale {
  String aesthetic = 'alpha'
  BigDecimal naValue = 1.0

  ScaleAlphaIdentity(Map params = [:]) {
    if (params.naValue) this.naValue = params.naValue as BigDecimal
    if (params.name) this.name = params.name as String
    if (params.guide) this.guide = params.guide
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    BigDecimal alpha = ScaleUtils.coerceToNumber(value)
    if (alpha == null) return naValue
    // Clamp to [0, 1] range
    BigDecimal zero = 0.0
    BigDecimal one = 1.0
    return zero.max(alpha.min(one))
  }
}
