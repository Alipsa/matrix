package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.geom.Point

/**
 * Area geometry for area charts.
 * Like geom_line but fills the area under the line.
 *
 * Usage:
 * - geom_area() - basic area chart
 * - geom_area(fill: 'blue', alpha: 0.5) - semi-transparent blue area
 * - With grouping: aes(fill: 'category') for stacked areas
 */
@CompileStatic
class GeomArea extends Geom {

  /** Fill color for the area */
  String fill = 'gray'

  /** Stroke color for the outline */
  String color = 'black'

  /** Line width for outline (0 for no outline) */
  BigDecimal linewidth = 0.5

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 0.7

  /** Line type for outline */
  String linetype = 'solid'

  /** Position adjustment: 'identity', 'stack', 'fill' */
  String position = 'stack'

  GeomArea() {
    defaultStat = StatType.ALIGN
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomArea(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.linewidth = (params.linewidth ?: params.size) as BigDecimal ?: this.linewidth
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.linetype = params.linetype as String ?: this.linetype
    this.position = params.position as String ?: this.position
    this.params = params
  }

}
