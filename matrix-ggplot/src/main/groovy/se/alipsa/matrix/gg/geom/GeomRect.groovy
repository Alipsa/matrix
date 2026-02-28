package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Rectangle geometry for drawing rectangles defined by corner coordinates.
 * Unlike geom_tile which is centered, geom_rect uses explicit corner positions.
 *
 * Required aesthetics: xmin, xmax, ymin, ymax
 * Optional aesthetics: fill, color, alpha, linewidth, linetype
 *
 * Usage:
 * - geom_rect(aes(xmin: 'x1', xmax: 'x2', ymin: 'y1', ymax: 'y2'))
 * - geom_rect(fill: 'blue', alpha: 0.3)
 */
@CompileStatic
class GeomRect extends Geom {

  /** Fill color for rectangles */
  String fill = 'gray'

  /** Stroke color for rectangle borders */
  String color = 'black'

  /** Line width for borders */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Line type for border */
  String linetype = 'solid'

  GeomRect() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['xmin', 'xmax', 'ymin', 'ymax']
    defaultAes = [fill: 'gray', color: 'black', alpha: 1.0] as Map<String, Object>
  }

  GeomRect(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    this.params = params
  }

}
