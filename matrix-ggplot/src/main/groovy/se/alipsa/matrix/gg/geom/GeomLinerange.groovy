package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Line range geometry for displaying vertical line ranges.
 * Shows a vertical line from ymin to ymax at each x position.
 * Unlike geom_errorbar, this does not have horizontal end caps.
 *
 * Required aesthetics: x, ymin, ymax
 * Optional aesthetics: color, linewidth, linetype, alpha
 *
 * Usage:
 * - geom_linerange(aes(ymin: 'lower', ymax: 'upper'))
 * - geom_linerange(color: 'blue', linewidth: 2)
 */
@CompileStatic
class GeomLinerange extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Line type (solid, dashed, dotted, etc.) */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  GeomLinerange() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomLinerange(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.params = params
  }

}
