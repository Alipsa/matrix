package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Rug geometry for showing marginal distributions as tick marks along axes.
 *
 * Usage:
 * - geom_rug() - rug marks on both x and y axes
 * - geom_rug(sides: 'b') - only bottom (x-axis)
 * - geom_rug(sides: 'l') - only left (y-axis)
 * - geom_rug(sides: 'bl') - both bottom and left
 */
@CompileStatic
class GeomRug extends Geom {

  /** Which sides to draw: 't'=top, 'b'=bottom, 'l'=left, 'r'=right */
  String sides = 'bl'

  /** Rug mark color */
  String color = 'black'

  /** Rug mark length (in pixels) */
  Number length = 10

  /** Line width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 0.5

  /** Position outside plot area */
  boolean outside = false

  GeomRug() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // x and/or y depending on sides
    defaultAes = [color: 'black', alpha: 0.5] as Map<String, Object>
  }

  GeomRug(Map params) {
    this()
    if (params.sides) this.sides = params.sides as String
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.length != null) this.length = params.length as Number
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.outside != null) this.outside = params.outside as boolean
    this.params = params
  }

}
