package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Crossbar geometry for displaying a horizontal line with a box around it.
 * Shows a rectangle from ymin to ymax with a horizontal line at y (the middle value).
 * Commonly used for showing means with confidence intervals.
 *
 * Required aesthetics: x, y, ymin, ymax
 * Optional aesthetics: fill, color, linewidth, alpha, width
 *
 * Usage:
 * - geom_crossbar(aes(ymin: 'lower', ymax: 'upper'))
 * - geom_crossbar(fill: 'lightblue', color: 'blue', width: 0.5)
 */
@CompileStatic
class GeomCrossbar extends Geom {

  /** Fill color for the box */
  String fill = null

  /** Stroke color for the box and middle line */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Width of the crossbar (as fraction of bandwidth for discrete, or data units for continuous) */
  BigDecimal width = null

  /** Fatten factor for the middle line (multiplier on linewidth) */
  BigDecimal fatten = 2.5

  GeomCrossbar() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomCrossbar(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.width != null) this.width = params.width as BigDecimal
    if (params.fatten != null) this.fatten = params.fatten as BigDecimal
    this.params = params
  }

}
