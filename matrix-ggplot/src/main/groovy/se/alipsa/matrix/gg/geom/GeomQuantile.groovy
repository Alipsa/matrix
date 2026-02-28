package se.alipsa.matrix.gg.geom


import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Quantile regression geometry.
 * Renders quantile regression lines that estimate conditional quantiles
 * rather than the conditional mean.
 *
 * Example:
 * <pre>
 * // Fit 25th, 50th, and 75th percentile lines
 * chart + geom_quantile()
 *
 * // Fit only the median (50th percentile)
 * chart + geom_quantile(quantiles: [0.5])
 *
 * // Fit 10th, 50th, and 90th percentiles with custom styling
 * chart + geom_quantile(quantiles: [0.1, 0.5, 0.9], color: 'blue', linetype: 'dashed')
 * </pre>
 */
@CompileStatic
class GeomQuantile extends Geom {

  /** Line color */
  String color = '#3366FF'

  /** Line width */
  Number size = 1.0

  /** Line type (solid, dashed, dotted, etc.) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Quantiles to compute [0.25, 0.5, 0.75] */
  List<Number> quantiles = [0.25, 0.5, 0.75]

  /** Number of fitted points */
  int n = 80

  GeomQuantile() {
    defaultStat = StatType.QUANTILE
    requiredAes = ['x', 'y']
    defaultAes = [color: '#3366FF', size: 1.0] as Map<String, Object>
  }

  GeomQuantile(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size) this.size = params.size as Number
    if (params.linetype) this.linetype = params.linetype
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.quantiles) this.quantiles = params.quantiles as List<Number>
    if (params.n) this.n = params.n as int
    this.params = params
  }

}
