package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Ribbon geometry for displaying confidence bands or ranges.
 * Draws a filled area between ymin and ymax values.
 *
 * Required aesthetics: x, ymin, ymax
 * Optional aesthetics: fill, color, alpha, linetype, linewidth
 *
 * Usage:
 * - geom_ribbon(aes(ymin: 'lower', ymax: 'upper')) - basic ribbon
 * - geom_ribbon(fill: 'blue', alpha: 0.3) - semi-transparent blue ribbon
 */
@CompileStatic
class GeomRibbon extends Geom {

  /** Fill color for the ribbon */
  String fill = 'gray'

  /** Stroke color for the outline */
  String color = null

  /** Line width for outline (0 for no outline) */
  BigDecimal linewidth = 0

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 0.5

  /** Line type for outline */
  String linetype = 'solid'

  GeomRibbon() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [fill: 'gray', alpha: 0.5] as Map<String, Object>
  }

  GeomRibbon(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = params.color ? ColorUtil.normalizeColor((params.color ?: params.colour) as String) : this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    this.params = params
  }

}
