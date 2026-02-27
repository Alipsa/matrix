package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Error bar geometry for intervals (ymin to ymax) at each x position.
 * Mirrors ggplot2's geom_errorbar contract as closely as possible.
 */
@CompileStatic
class GeomErrorbar extends Geom {

  /** Bar width as fraction of bandwidth (discrete) or data units (continuous) */
  BigDecimal width = null

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  GeomErrorbar() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomErrorbar(Map params) {
    this()
    this.width = params.width as BigDecimal ?: this.width
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.linewidth = (params.linewidth ?: params.size) as BigDecimal ?: this.linewidth
    this.linetype = params.linetype as String ?: this.linetype
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.params = params
  }

}
