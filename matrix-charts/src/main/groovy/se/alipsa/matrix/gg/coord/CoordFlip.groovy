package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Flipped Cartesian coordinate system - swaps x and y axes.
 * Useful for creating horizontal bar charts.
 *
 * Usage:
 * - coord_flip() - flip axes
 *
 * Example:
 * ggplot(data, aes(x: 'category', y: 'value')) +
 *     geom_bar(stat: 'identity') +
 *     coord_flip()  // Creates horizontal bars
 */
@CompileStatic
class CoordFlip extends Coord {

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

  /** Flag to indicate this coordinate system flips axes */
  final boolean flipped = true

  CoordFlip() {}

  CoordFlip(Map params) {
    if (params.xlim) this.xlim = params.xlim as List<Number>
    if (params.ylim) this.ylim = params.ylim as List<Number>
    if (params.containsKey('expand')) this.expand = params.expand as boolean
    if (params.expandMult) this.expandMult = params.expandMult as double
  }

  /**
   * Transform swaps x and y - the x scale is used for vertical positioning
   * and y scale is used for horizontal positioning.
   */
  @CompileDynamic
  @Override
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    // For flipped coordinates, swap which scale is used for which axis
    // x data -> y pixel position (vertical)
    // y data -> x pixel position (horizontal)
    def xScale = scales['x']
    def yScale = scales['y']

    // Use y scale for horizontal (returns xPx), x scale for vertical (returns yPx)
    Number xPx = yScale ? yScale.transform(y) : y
    Number yPx = xScale ? xScale.transform(x) : x

    return [xPx, yPx]
  }

  @CompileDynamic
  @Override
  List<Number> inverse(Number xPx, Number yPx, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']

    // Inverse of the flip
    Number x = xScale ? xScale.inverse(yPx) : yPx
    Number y = yScale ? yScale.inverse(xPx) : xPx

    return [x, y]
  }
}
