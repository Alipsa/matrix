package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Identity scale for linetype aesthetic - uses data values directly without mapping.
 *
 * When the data contains linetype names (e.g., 'solid', 'dashed', 'dotted'), this scale
 * uses those exact values rather than mapping them from a palette.
 *
 * Example:
 * <pre>
 * // Data has a column 'lineStyle' with values ['solid', 'dashed', 'dotted']
 * chart + geom_line(aes(linetype: 'lineStyle')) + scale_linetype_identity()
 * </pre>
 */
@CompileStatic
class ScaleLinetypeIdentity extends Scale {
  String aesthetic = 'linetype'
  String naValue = 'solid'

  ScaleLinetypeIdentity(Map params = [:]) {
    if (params.naValue) this.naValue = params.naValue as String
    if (params.name) this.name = params.name as String
    if (params.guide) this.guide = params.guide
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    return value.toString()
  }
}
