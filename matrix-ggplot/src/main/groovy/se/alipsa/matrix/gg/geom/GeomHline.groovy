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
 * Horizontal line geometry for drawing reference lines across the plot.
 *
 * Usage:
 * - geom_hline(yintercept: 5) - single horizontal line at y=5
 * - geom_hline(yintercept: [2, 4, 6]) - multiple horizontal lines
 * - With data mapping: aes(yintercept: 'threshold_column')
 */
@CompileStatic
class GeomHline extends Geom {

  /** Y-intercept value(s) for the horizontal line(s) */
  def yintercept

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomHline() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // yintercept can be specified as parameter
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomHline(Map params) {
    this()
    if (params.yintercept != null) this.yintercept = params.yintercept
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

}
