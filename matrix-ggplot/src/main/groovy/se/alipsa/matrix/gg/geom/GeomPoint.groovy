package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil

/**
 * Point geometry for scatter plots.
 * Renders data points as circles.
 */
@CompileStatic
class GeomPoint extends Geom {

  /** Default point color */
  String color = 'black'

  /** Default point fill */
  String fill = 'black'

  /** Default point size (radius) */
  BigDecimal size = 3

  /** Default point shape */
  String shape = 'circle'

  /** Default alpha (transparency) */
  BigDecimal alpha = 1.0

  GeomPoint() {
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 3, alpha: 1.0] as Map<String, Object>
  }

  GeomPoint(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    if (params.size != null) this.size = params.size as BigDecimal
    this.shape = params.shape as String ?: this.shape
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.params = params
  }

}
