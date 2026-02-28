package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Line segment geometry for drawing lines from (x, y) to (xend, yend).
 *
 * Usage:
 * - geom_segment(x: 1, y: 1, xend: 5, yend: 5) - single segment
 * - With data mapping: aes(x: 'x1', y: 'y1', xend: 'x2', yend: 'y2')
 */
@CompileStatic
class GeomSegment extends Geom {

  /** Starting x coordinate */
  BigDecimal x

  /** Starting y coordinate */
  BigDecimal y

  /** Ending x coordinate */
  BigDecimal xend

  /** Ending y coordinate */
  BigDecimal yend

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Arrow at end of segment */
  boolean arrow = false

  /** Arrow size (width in pixels) */
  BigDecimal arrowSize = 10

  GeomSegment() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'xend', 'yend']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomSegment(Map params) {
    this()
    if (params.x != null) this.x = params.x as BigDecimal
    if (params.y != null) this.y = params.y as BigDecimal
    if (params.xend != null) this.xend = params.xend as BigDecimal
    if (params.yend != null) this.yend = params.yend as BigDecimal
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.arrow != null) this.arrow = params.arrow as boolean
    if (params.arrowSize != null) this.arrowSize = params.arrowSize as BigDecimal
    this.params = params
  }

}
