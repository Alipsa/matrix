package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.charm.util.ColorUtil

/**
 * Box plot geometry for visualizing distributions through quartiles.
 * Uses stat_boxplot by default to compute quartiles and whiskers.
 *
 * After stat_boxplot transformation, data has columns:
 * - x: group key (category)
 * - ymin: lower whisker
 * - lower: Q1 (25th percentile, bottom of box)
 * - middle: median (50th percentile, line inside box)
 * - upper: Q3 (75th percentile, top of box)
 * - ymax: upper whisker
 * - outliers: list of outlier values beyond whiskers
 * - width: box width in data units
 * - xmin/xmax: box width extents in data units
 * - relvarwidth: relative width scaling factor (sqrt(n))
 * - xresolution: minimum non-zero x resolution for width calculation
 */
@CompileStatic
class GeomBoxplot extends Geom {

  /** Box fill color */
  String fill = 'white'

  /** Box and whisker outline color */
  String color = 'black'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Line width for box outline and whiskers */
  Number linewidth = 0.5

  /** Width of boxes relative to spacing (0-1) */
  Number width = 0.75

  /** Whether to scale box widths based on sample size (null/false = disabled, true = enabled) */
  Boolean varwidth = null

  /** Whether to show outlier points */
  boolean outliers = true

  /** Size of outlier points */
  Number outlierSize = 1.5

  /** Shape of outlier points (circle, square, etc.) */
  String outlierShape = 'circle'

  /** Color of outlier points (null = use color) */
  String outlierColor = null

  /** Width of whisker caps relative to box width */
  Number stapleWidth = 0.0

  GeomBoxplot() {
    defaultStat = StatType.BOXPLOT
    defaultPosition = PositionType.DODGE2
    requiredAes = ['y']  // x is optional for single boxplot
    defaultAes = [fill: 'white', color: 'black', alpha: 1.0] as Map<String, Object>
  }

  GeomBoxplot(Aes aes) {
    this()
    if (aes != null) {
      this.params = [mapping: aes]
    }
  }

  GeomBoxplot(Map params) {
    this()
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.width != null) this.width = params.width as Number
    if (params.varwidth != null) this.varwidth = params.varwidth as Boolean
    if (params.var_width != null) this.varwidth = params.var_width as Boolean
    if (params.outliers != null) this.outliers = params.outliers as boolean
    if (params.outlierSize != null) this.outlierSize = params.outlierSize as Number
    if (params.outlier_size != null) this.outlierSize = params.outlier_size as Number
    if (params.outlierShape) this.outlierShape = params.outlierShape as String
    if (params.outlier_shape) this.outlierShape = params.outlier_shape as String
    if (params.outlierColor) this.outlierColor = params.outlierColor as String
    if (params.outlier_color) this.outlierColor = params.outlier_color as String
    if (params.stapleWidth != null) this.stapleWidth = params.stapleWidth as Number
    if (params.staple_width != null) this.stapleWidth = params.staple_width as Number
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    if (this.outlierColor != null) {
      this.outlierColor = ColorUtil.normalizeColor(this.outlierColor)
    }
    this.params = params
  }

}
