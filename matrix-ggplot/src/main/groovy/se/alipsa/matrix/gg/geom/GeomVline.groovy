package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Vertical line geometry for drawing reference lines across the plot.
 *
 * Usage:
 * - geom_vline(xintercept: 5) - single vertical line at x=5
 * - geom_vline(xintercept: [2, 4, 6]) - multiple vertical lines
 * - With data mapping: aes(xintercept: 'threshold_column')
 */
@CompileStatic
class GeomVline extends Geom {

  /** X-intercept value(s) for the vertical line(s) */
  def xintercept

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomVline() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // xintercept can be specified as parameter
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomVline(Map params) {
    this()
    if (params.xintercept != null) this.xintercept = params.xintercept
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

}
