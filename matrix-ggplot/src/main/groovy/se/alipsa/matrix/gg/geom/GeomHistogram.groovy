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
 * Histogram geometry for visualizing the distribution of continuous data.
 * Uses stat_bin by default to bin continuous data into intervals.
 *
 * After stat_bin transformation, data has columns:
 * - x: bin center
 * - xmin: bin left edge
 * - xmax: bin right edge
 * - count: number of observations in bin
 * - density: count / (n * binwidth)
 */
@CompileStatic
class GeomHistogram extends Geom {

  /** Bar fill color */
  String fill = '#595959'

  /** Bar outline color */
  String color = 'white'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Outline width */
  Number linewidth = 0.5

  /** Number of bins (default 30, overridden if binwidth is set) */
  Integer bins = 30

  /** Width of each bin (if set, overrides bins parameter) */
  Number binwidth = null

  GeomHistogram() {
    defaultStat = StatType.BIN
    requiredAes = ['x']
    defaultAes = [fill: '#595959', color: 'white', alpha: 1.0] as Map<String, Object>
  }

  GeomHistogram(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.linewidth) this.linewidth = params.linewidth as Number
    if (params.bins) this.bins = params.bins as Integer
    if (params.binwidth) this.binwidth = params.binwidth as Number
    this.params = params
  }

}
