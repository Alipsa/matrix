package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Point range geometry for displaying a point with a vertical line range.
 * Commonly used for showing means with confidence intervals or error bars.
 *
 * Required aesthetics: x, y, ymin, ymax
 * Optional aesthetics: color, size, linewidth, alpha, shape
 *
 * Usage:
 * - geom_pointrange(aes(ymin: 'lower', ymax: 'upper'))
 * - geom_pointrange(color: 'blue', size: 3)
 */
@CompileStatic
class GeomPointrange extends Geom {

  /** Point and line color */
  String color = 'black'

  /** Point size (diameter) */
  BigDecimal size = 4

  /** Line width for the range line */
  BigDecimal linewidth = 1

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Point shape */
  BigDecimal shape = 19  // filled circle

  /** Fill color for the point (for fillable shapes) */
  String fill = null

  GeomPointrange() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'ymin', 'ymax']
    defaultAes = [color: 'black', size: 4, linewidth: 1] as Map<String, Object>
  }

  GeomPointrange(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.shape != null) this.shape = params.shape as BigDecimal
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.params = params
  }

}
