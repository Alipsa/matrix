package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Horizontal error bar geometry for intervals (xmin to xmax) at each y position.
 * Useful for displaying horizontal confidence intervals or ranges.
 *
 * Required aesthetics: y, xmin, xmax
 * Optional aesthetics: color, size/linewidth, linetype, alpha, height
 */
@CompileStatic
class GeomErrorbarh extends Geom {

  /** Bar height as fraction of bandwidth (discrete) or data units (continuous) */
  Number height = null

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomErrorbarh() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['y', 'xmin', 'xmax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomErrorbarh(Map params) {
    this()
    if (params.height != null) this.height = params.height as Number
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

}
