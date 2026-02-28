package se.alipsa.matrix.gg.geom


import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Smooth geometry for trend lines (regression, loess, etc.).
 * Renders a fitted line through the data points.
 */
@CompileStatic
class GeomSmooth extends Geom {

  /** Line color */
  String color = '#3366FF'

  /** Line width */
  Number size = 1.5

  /** Line type (solid, dashed, dotted) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Smoothing method: 'lm' (linear), 'loess', 'gam' */
  String method = 'lm'

  /** Number of points to generate for the smoothed line */
  int n = 80

  /** Whether to show standard error band */
  boolean se = true

  /** Fill color for SE band */
  String fill = '#3366FF'

  /** Alpha for SE band */
  Number fillAlpha = 0.2

  GeomSmooth() {
    defaultStat = StatType.SMOOTH
    requiredAes = ['x', 'y']
    defaultAes = [color: '#3366FF', size: 1.5] as Map<String, Object>
  }

  GeomSmooth(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size) this.size = params.size as Number
    if (params.linetype) this.linetype = params.linetype
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.method) this.method = params.method
    if (params.n) this.n = params.n as int
    if (params.containsKey('se')) this.se = params.se
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    this.params = params
  }

}
