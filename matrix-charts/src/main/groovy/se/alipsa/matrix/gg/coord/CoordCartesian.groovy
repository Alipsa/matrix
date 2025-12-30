package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Cartesian coordinate system (default).
 * Maps x and y data values to pixel positions using linear transformation.
 */
@CompileStatic
class CoordCartesian extends Coord {

  /** X-axis limits [min, max] - null means auto-detect from data */
  List<Number> xlim

  /** Y-axis limits [min, max] - null means auto-detect from data */
  List<Number> ylim

  /** Whether to expand the axis limits slightly beyond the data range */
  boolean expand = true

  /** Expansion multiplier (default 5% on each side) */
  double expandMult = 0.05

  /** Plot width in pixels (set by renderer) */
  int plotWidth = 640

  /** Plot height in pixels (set by renderer) */
  int plotHeight = 480

  CoordCartesian() {}

  CoordCartesian(Map params) {
    if (params.xlim) this.xlim = params.xlim as List<Number>
    if (params.ylim) this.ylim = params.ylim as List<Number>
    if (params.containsKey('expand')) this.expand = params.expand
    if (params.expandMult) this.expandMult = params.expandMult as double
  }

  @CompileDynamic
  @Override
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    // Get x and y scales
    def xScale = scales['x']
    def yScale = scales['y']

    Number xPx = xScale ? xScale.transform(x) : x
    Number yPx = yScale ? yScale.transform(y) : y

    return [xPx, yPx]
  }

  @CompileDynamic
  @Override
  List<Number> inverse(Number xPx, Number yPx, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']

    Number x = xScale ? xScale.inverse(xPx) : xPx
    Number y = yScale ? yScale.inverse(yPx) : yPx

    return [x, y]
  }
}
