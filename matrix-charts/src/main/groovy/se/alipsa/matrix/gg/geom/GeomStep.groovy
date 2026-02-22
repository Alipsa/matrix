package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Step geometry for step plots (staircase pattern).
 * Creates a step function plot where horizontal segments connect to vertical segments.
 * Useful for visualizing piecewise constant functions or cumulative distributions.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: color, size/linewidth, linetype, alpha, group
 *
 * The 'direction' parameter controls where the step occurs:
 * - 'hv' (default): horizontal then vertical (step happens at the end)
 * - 'vh': vertical then horizontal (step happens at the start)
 * - 'mid': step happens at the midpoint between x values
 *
 * Usage:
 * - geom_step() - default step plot (hv direction)
 * - geom_step(direction: 'vh') - vertical first
 * - geom_step(color: 'blue', linewidth: 2) - styled step
 */
@CompileStatic
class GeomStep extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Step direction: 'hv' (horizontal-vertical), 'vh' (vertical-horizontal), 'mid' (midpoint) */
  String direction = 'hv'

  GeomStep() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomStep(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.size = params.linewidth as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.direction = params.direction as String ?: this.direction
    this.params = params
  }

}
