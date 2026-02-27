package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.pictura.util.ColorUtil

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Violin geometry for showing kernel density estimates.
 * Like a mirrored density plot, useful for comparing distributions.
 *
 * Usage:
 * - geom_violin() - basic violin plot
 * - geom_violin(fill: 'lightblue', alpha: 0.7)
 * - geom_violin(draw_quantiles: [0.25, 0.5, 0.75]) - with quantile lines
 */
@CompileStatic
class GeomViolin extends Geom {

  /**
   * Data class to hold kernel density estimation points.
   */
  @CompileStatic
  static class DensityPoint {
    final BigDecimal position
    final BigDecimal density

    DensityPoint(BigDecimal position, BigDecimal density) {
      this.position = position
      this.density = density
    }
  }

  /** Fill color */
  String fill = 'gray'

  /** Outline color */
  String color = 'black'

  /** Line width for outline */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 0.7

  /** Width of violin (relative, 0-1) */
  Number width = 0.9

  /** Whether to trim violin to data range */
  boolean trim = true

  /** Whether to scale all violins to same max width ('area', 'count', 'width') */
  String scale = 'area'

  /** Quantiles to draw as lines */
  List<Number> draw_quantiles = []

  /** Number of points for density estimation */
  int n = 512

  /** Bandwidth adjustment factor (multiplier for default bandwidth) */
  Number adjust = 1.0

  GeomViolin() {
    defaultStat = StatType.YDENSITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomViolin(Aes aes) {
    this()
  }

  GeomViolin(Map params) {
    this()
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.width != null) this.width = params.width as Number
    if (params.trim != null) this.trim = params.trim as boolean
    if (params.scale) this.scale = params.scale as String
    if (params.draw_quantiles) this.draw_quantiles = params.draw_quantiles as List<Number>
    if (params.n != null) this.n = params.n as int
    if (params.adjust != null) this.adjust = params.adjust as Number
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

}
