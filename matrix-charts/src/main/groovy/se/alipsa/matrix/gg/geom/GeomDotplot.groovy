package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Dot plot geometry for displaying distributions.
 * Stacks dots for each observation to show the distribution of data.
 *
 * Usage:
 * - geom_dotplot() - vertical dotplot
 * - geom_dotplot(binaxis: 'y') - horizontal dotplot
 * - geom_dotplot(binwidth: 0.5) - specify bin width
 * - geom_dotplot(method: 'histodot') - histodot stacking method
 */
@CompileStatic
class GeomDotplot extends Geom {

  /** Axis to bin along: 'x' (vertical) or 'y' (horizontal) */
  String binaxis = 'x'

  /** Method for stacking: 'dotdensity' or 'histodot' */
  String method = 'dotdensity'

  /** Bin width (auto-calculated if null) */
  Number binwidth = null

  /** Number of bins (used if binwidth not specified) */
  int bins = 30

  /** Stacking direction: 'up', 'down', 'center', 'centerwhole' */
  String stackdir = 'up'

  /** Ratio of dot height to bin width */
  Number dotsize = 1.0

  /** Dot fill color */
  String fill = 'black'

  /** Dot stroke color */
  String color = 'black'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Width of stack relative to binwidth */
  Number stackratio = 1.0

  GeomDotplot() {
    defaultStat = StatType.COUNT
    requiredAes = ['x']
    defaultAes = [fill: 'black', color: 'black', alpha: 1.0] as Map<String, Object>
  }

  GeomDotplot(Map params) {
    this()
    if (params.binaxis) this.binaxis = params.binaxis as String
    if (params.method) this.method = params.method as String
    if (params.binwidth != null) this.binwidth = params.binwidth as Number
    if (params.bins != null) this.bins = params.bins as int
    if (params.stackdir) this.stackdir = params.stackdir as String
    if (params.dotsize != null) this.dotsize = params.dotsize as Number
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.stackratio != null) this.stackratio = params.stackratio as Number
    this.params = params
  }

}
