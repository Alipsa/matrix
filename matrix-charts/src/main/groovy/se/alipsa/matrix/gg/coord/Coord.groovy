package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Base class for coordinate systems.
 * Coordinate systems define how data coordinates are mapped to pixel positions.
 */
@CompileStatic
abstract class Coord {

  /**
   * Transform data coordinates to pixel coordinates.
   * @param data Matrix with x, y columns in data space
   * @param plotWidth Width of the plot area in pixels
   * @param plotHeight Height of the plot area in pixels
   * @param xRange Data range for x [min, max]
   * @param yRange Data range for y [min, max]
   * @return Matrix with px, py columns in pixel space
   */
  Matrix transform(Matrix data, int plotWidth, int plotHeight,
                   List<Number> xRange, List<Number> yRange) {
    // Default implementation - subclasses override
    return data
  }

  /**
   * Inverse transform: pixel coordinates to data coordinates.
   */
  List<Number> inverse(Number px, Number py, int plotWidth, int plotHeight,
                       List<Number> xRange, List<Number> yRange) {
    // Default implementation - subclasses override
    return [px, py]
  }

  /**
   * Transform a single point using scale objects.
   * @param x Data x value
   * @param y Data y value
   * @param scales Map of aesthetic to Scale
   * @return [xPx, yPx] pixel coordinates
   */
  @CompileDynamic
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']
    Number xPx = xScale ? xScale.transform(x) : x
    Number yPx = yScale ? yScale.transform(y) : y
    return [xPx, yPx]
  }

  /**
   * Inverse transform using scale objects.
   * @param xPx Pixel x value
   * @param yPx Pixel y value
   * @param scales Map of aesthetic to Scale
   * @return [x, y] data coordinates
   */
  @CompileDynamic
  List<Number> inverse(Number xPx, Number yPx, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']
    Number x = xScale ? xScale.inverse(xPx) : xPx
    Number y = yScale ? yScale.inverse(yPx) : yPx
    return [x, y]
  }
}
