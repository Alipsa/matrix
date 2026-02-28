package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.pict.util.ColorUtil

/**
 * Identity scale for fill aesthetic - uses data values directly without mapping.
 *
 * When the data contains color values (e.g., 'red', 'blue', '#FF0000'), this scale
 * uses those exact values for fill rather than mapping them through a palette.
 *
 * Example:
 * <pre>
 * // Data has a column 'barFill' with values ['red', 'blue', 'green']
 * chart + geom_bar(aes(fill: 'barFill')) + scale_fill_identity()
 * </pre>
 */
@CompileStatic
class ScaleFillIdentity extends Scale {
  String aesthetic = 'fill'
  String naValue = 'grey50'

  ScaleFillIdentity(Map params = [:]) {
    if (params.naValue) {
      this.naValue = ColorUtil.normalizeColor(params.naValue as String) ?: params.naValue as String
    }
    if (params.name) this.name = params.name as String
    if (params.guide) this.guide = params.guide
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    String colorValue = value.toString()
    return ColorUtil.normalizeColor(colorValue) ?: colorValue
  }

  /**
   * Converts this gg scale to a charm Scale spec.
   *
   * @return charm Scale with identity color configuration
   */
  CharmScale toCharmScale() {
    CharmScale.identity(naValue)
  }
}
