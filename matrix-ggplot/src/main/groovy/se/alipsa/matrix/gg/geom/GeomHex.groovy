package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.pict.util.ColorUtil

/**
 * Hexagonal binning geometry for creating hexbin plots from point data.
 * Divides the plotting area into a hexagonal grid and counts observations in each hexagon.
 *
 * Usage:
 * - geom_hex() - default 30 bins
 * - geom_hex(bins: 20) - 20 bins in x direction
 * - geom_hex(binwidth: 0.5) - specify hexagon width
 *
 * Similar to geom_bin_2d() but uses hexagonal bins instead of rectangular.
 */
@CompileStatic
class GeomHex extends Geom {

  /** Number of bins in x direction */
  int bins = 30

  /** Hexagon width (overrides bins if set) */
  BigDecimal binwidth

  /** Fill color for hexagons (used if no fill scale) */
  String fill = 'steelblue'

  /** Border color for hexagons */
  String color = 'white'

  /** Border width */
  BigDecimal linewidth = 0.5

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Whether to drop bins with zero count */
  boolean drop = true

  /** Default fill colors for gradient (low to high count) */
  List<String> fillColors = [
      '#f7fbff', '#deebf7', '#c6dbef', '#9ecae1', '#6baed6',
      '#4292c6', '#2171b5', '#08519c', '#08306b'
  ]

  GeomHex() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'white'] as Map<String, Object>
  }

  GeomHex(Map params) {
    this()
    if (params.bins != null) this.bins = params.bins as int
    if (params.binwidth != null) this.binwidth = params.binwidth as BigDecimal
    this.fill = params.fill as String ?: this.fill
    this.color = (params.color ?: params.colour) as String ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.drop != null) this.drop = params.drop as boolean
    this.fillColors = (params.fillColors ?: params.fill_colors) as List<String> ?: this.fillColors
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    this.fillColors = this.fillColors.collect { ColorUtil.normalizeColor(it) }
    this.params = params
  }

}
