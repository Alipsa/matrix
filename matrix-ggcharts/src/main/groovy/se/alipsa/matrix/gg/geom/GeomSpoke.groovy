package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Spoke geometry for drawing radial line segments from a point.
 * Each spoke is defined by a center point (x, y), an angle, and a radius.
 * Useful for wind roses, direction plots, and radial visualizations.
 *
 * Required aesthetics: x, y, angle, radius
 * Optional aesthetics: color, size/linewidth, linetype, alpha
 *
 * Note: Angles are in radians by default. Use angle * pi / 180 to convert from degrees.
 *
 * Usage:
 * - geom_spoke() - with data containing x, y, angle, radius columns
 * - geom_spoke(aes('x', 'y', angle: 'direction', radius: 'strength'))
 */
@CompileStatic
class GeomSpoke extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Default radius if not specified in data */
  Number radius = 1

  GeomSpoke() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomSpoke(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.radius != null) this.radius = params.radius as Number
    this.params = params
  }

}
