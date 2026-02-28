package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.pict.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Q-Q plot reference line.
 * Uses stat_qq_line to compute line endpoints.
 */
@CompileStatic
class GeomQqLine extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /**
   * Create a Q-Q line geom with default settings.
   */
  GeomQqLine() {
    defaultStat = StatType.QQ_LINE
    requiredAes = ['x']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  /**
   * Create a Q-Q line geom with parameters.
   *
   * @param params geom parameters
   */
  GeomQqLine(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size) this.size = params.size as Number
    if (params.linewidth) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype
    if (params.alpha) this.alpha = params.alpha as Number
    this.params = params
  }

}
