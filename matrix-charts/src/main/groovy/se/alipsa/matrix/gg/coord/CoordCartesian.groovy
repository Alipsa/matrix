package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CoordSpec

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

  /** Plot width in pixels (set by renderer) */
  int plotWidth = 640

  /** Plot height in pixels (set by renderer) */
  int plotHeight = 480

  CoordCartesian() {}

  CoordCartesian(Map params) {
    if (params.xlim) this.xlim = params.xlim as List<Number>
    if (params.ylim) this.ylim = params.ylim as List<Number>
  }

  @Override
  CoordSpec toCharmCoordSpec() {
    Map<String, Object> p = [:]
    if (xlim) p.xlim = xlim
    if (ylim) p.ylim = ylim
    new CoordSpec(type: CharmCoordType.CARTESIAN, params: p)
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
