package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Curved line segment geometry for drawing smooth curves from (x, y) to (xend, yend).
 * Uses cubic Bezier curves for smooth transitions.
 *
 * Required aesthetics: x, y, xend, yend
 * Optional aesthetics: color, size/linewidth, linetype, alpha, curvature
 *
 * The curvature parameter controls the bend of the curve:
 * - 0: straight line (like geom_segment)
 * - Positive values: curve bends to the right
 * - Negative values: curve bends to the left
 * - Default: 0.5
 *
 * Usage:
 * - geom_curve(aes('x', 'y', xend: 'x2', yend: 'y2'))
 * - geom_curve(curvature: 1.0, color: 'blue')
 */
@CompileStatic
class GeomCurve extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Curvature of the curve (0 = straight, positive = right bend, negative = left bend) */
  Number curvature = 0.5

  GeomCurve() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'xend', 'yend']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomCurve(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.curvature != null) this.curvature = params.curvature as Number
    this.params = params
  }

}
