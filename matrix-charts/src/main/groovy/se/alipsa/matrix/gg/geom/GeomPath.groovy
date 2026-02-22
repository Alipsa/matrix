package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.geom.Point

/**
 * Path geometry for connecting observations in data order.
 * Unlike geom_line which connects points sorted by x value,
 * geom_path connects points in the order they appear in the data.
 * This is useful for trajectories, polygons, and other ordered paths.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: color, size/linewidth, linetype, alpha, group
 *
 * Usage:
 * - geom_path() - basic path in data order
 * - geom_path(color: 'red', linewidth: 2) - styled path
 */
@CompileStatic
class GeomPath extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Line end style (butt, round, square) */
  String lineend = 'butt'

  /** Line join style (round, mitre, bevel) */
  String linejoin = 'round'

  GeomPath() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomPath(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.size = params.linewidth as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.lineend = params.lineend as String ?: this.lineend
    this.linejoin = params.linejoin as String ?: this.linejoin
    this.params = params
  }

}
