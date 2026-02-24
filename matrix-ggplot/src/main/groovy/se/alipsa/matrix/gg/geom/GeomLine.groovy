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
 * Line geometry for line charts.
 * Connects data points with lines, optionally grouped by a variable.
 */
@CompileStatic
class GeomLine extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  GeomLine() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomLine(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.size = params.linewidth as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.params = params
  }

}
