package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Arbitrary line geometry defined by slope and intercept (y = slope * x + intercept).
 *
 * Usage:
 * - geom_abline(slope: 1, intercept: 0) - diagonal line through origin
 * - geom_abline(slope: 2, intercept: 5) - line with slope 2, y-intercept 5
 * - With data mapping: aes(slope: 'slope_col', intercept: 'intercept_col')
 */
@CompileStatic
class GeomAbline extends Geom {

  /** Slope of the line */
  Number slope = 1

  /** Y-intercept of the line */
  Number intercept = 0

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomAbline() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // slope/intercept can be specified as parameters
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomAbline(Map params) {
    this()
    if (params.slope != null) this.slope = params.slope as Number
    if (params.intercept != null) this.intercept = params.intercept as Number
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

}
