package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.pictura.util.ColorUtil

/**
 * 2D binning geometry for creating heatmaps from point data.
 * Divides the plotting area into a grid and counts observations in each cell.
 *
 * Usage:
 * - geom_bin_2d() - default 30x30 bins
 * - geom_bin_2d(bins: 20) - 20x20 bins
 * - geom_bin_2d(binwidth: [0.5, 0.5]) - specify bin widths
 *
 * Similar to geom_tile() but computes counts automatically.
 */
@CompileStatic
class GeomBin2d extends Geom {

  /** Number of bins in x and y directions */
  int bins = 30

  /** Bin width for x and y (overrides bins if set) */
  List<Number> binwidth

  /** Fill color for bins (used if no fill scale) */
  String fill = 'steelblue'

  /** Border color for bins */
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

  GeomBin2d() {
    defaultStat = StatType.IDENTITY  // We compute bins internally
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'white'] as Map<String, Object>
  }

  GeomBin2d(Map params) {
    this()
    this.bins = params.bins as Integer ?: this.bins
    this.binwidth = params.binwidth as List<Number> ?: this.binwidth
    this.fill = params.fill as String ?: this.fill
    this.color = (params.color ?: params.colour) as String ?: this.color
    this.linewidth = params.linewidth as BigDecimal ?: this.linewidth
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    if (params.drop != null) this.drop = params.drop as boolean
    this.fillColors = (params.fillColors ?: params.fill_colors) as List<String> ?: this.fillColors
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    this.fillColors = this.fillColors.collect { ColorUtil.normalizeColor(it) }
    this.params = params
  }

}
