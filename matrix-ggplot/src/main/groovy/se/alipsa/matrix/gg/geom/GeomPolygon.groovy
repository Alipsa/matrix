package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Polygon geometry for closed filled shapes.
 * Unlike geom_path which draws an open path, geom_polygon automatically
 * closes the path and fills it. Useful for drawing custom shapes,
 * regions, and map polygons.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: fill, color, size/linewidth, linetype, alpha, group
 *
 * Usage:
 * - geom_polygon() - basic filled polygon
 * - geom_polygon(fill: 'blue', color: 'black', linewidth: 2) - styled polygon
 */
@CompileStatic
class GeomPolygon extends Geom {

  /** Fill color */
  String fill = 'gray'

  /** Border color */
  String color = 'black'

  /** Border width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Line end style (butt, round, square) */
  String lineend = 'butt'

  /** Line join style (round, mitre, bevel) */
  String linejoin = 'round'

  GeomPolygon() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomPolygon(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
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
