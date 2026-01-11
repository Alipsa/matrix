package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Identity scale for shape aesthetic - uses data values directly without mapping.
 *
 * When the data contains shape names (e.g., 'circle', 'square', 'triangle'), this scale
 * uses those exact values rather than mapping them from a palette.
 *
 * Example:
 * <pre>
 * // Data has a column 'pointShape' with values ['circle', 'square', 'triangle']
 * chart + geom_point(aes(shape: 'pointShape')) + scale_shape_identity()
 * </pre>
 */
@CompileStatic
class ScaleShapeIdentity extends Scale {
  String aesthetic = 'shape'
  String naValue = 'circle'

  ScaleShapeIdentity(Map params = [:]) {
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
