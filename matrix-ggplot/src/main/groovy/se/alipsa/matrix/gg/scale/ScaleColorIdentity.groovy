package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil

/**
 * Identity scale for color aesthetic - uses data values directly without mapping.
 *
 * When the data contains color values (e.g., 'red', 'blue', '#FF0000'), this scale
 * uses those exact values rather than mapping them through a palette.
 *
 * Supported color formats:
 * <ul>
 *   <li>Named colors: 'red', 'blue', 'green', etc. (standard SVG/CSS color names)</li>
 *   <li>Hex colors: '#RGB', '#RRGGBB', or '#RRGGBBAA'</li>
 *   <li>Gray shades: 'grey50', 'gray75', etc. (0-100)</li>
 * </ul>
 *
 * Invalid colors will fall back to the naValue (default: 'grey50').
 *
 * Example:
 * <pre>
 * // Data has a column 'pointColor' with values ['red', 'blue', 'green']
 * chart + geom_point(aes(color: 'pointColor')) + scale_color_identity()
 * </pre>
 */
@CompileStatic
class ScaleColorIdentity extends Scale {
  String aesthetic = 'color'
  String naValue = 'grey50'

  ScaleColorIdentity(Map params = [:]) {
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
}
