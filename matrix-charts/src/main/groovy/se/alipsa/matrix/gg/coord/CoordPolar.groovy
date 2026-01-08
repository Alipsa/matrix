package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import java.util.Locale

/**
 * Polar coordinate system for creating pie charts, rose diagrams, and other circular plots.
 *
 * The polar coordinate system transforms Cartesian coordinates to polar form:
 * - One variable maps to angle (theta)
 * - Another variable maps to radius (r)
 *
 * Usage:
 * - coord_polar() - default: x maps to angle
 * - coord_polar(theta: 'y') - y maps to angle instead
 * - coord_polar(start: Math.PI/2) - rotate starting position
 *
 * Example (pie chart):
 * ggplot(data, aes(x: '', y: 'value', fill: 'category')) +
 *     geom_bar(stat: 'identity', width: 1) +
 *     coord_polar(theta: 'y')
 */
@CompileStatic
class CoordPolar extends Coord {

  /** Which variable to map to angle: 'x' or 'y' (default: 'x') */
  String theta = 'x'

  /** Starting angle offset in radians (0 = 12 o'clock position) */
  BigDecimal start = 0

  /** Direction: true for clockwise, false for anticlockwise */
  boolean clockwise = true

  /** Whether to clip drawing to the extent of the plot panel */
  boolean clip = true

  /** Plot width in pixels (set by renderer) */
  int plotWidth = 640

  /** Plot height in pixels (set by renderer) */
  int plotHeight = 480

  /** Flag to indicate this is polar coordinates */
  final boolean polar = true

  CoordPolar() {}

  CoordPolar(String theta, BigDecimal start, Boolean clockwise, Boolean clip) {
    this.theta = theta ?: 'x'
    this.start = start ?: 0
    this.clockwise = clockwise != null ? clockwise : true
    this.clip = clip != null ? clip : true
  }

  CoordPolar(Map params) {
    if (params.theta) this.theta = params.theta as String
    if (params.start != null) this.start = params.start as BigDecimal
    if (params.containsKey('clockwise')) this.clockwise = params.clockwise as boolean
    if (params.direction != null) this.clockwise = (params.direction as int) == 1
    if (params.containsKey('clip')) this.clip = params.clip as boolean
  }

  /**
   * Get the center point of the polar plot in pixels.
   */
  List<BigDecimal> getCenter() {
    return [plotWidth / 2, plotHeight / 2]
  }

  /**
   * Get the maximum radius that fits in the plot area.
   */
  BigDecimal getMaxRadius() {
    return plotWidth.min(plotHeight) / 2 * 0.9  // 90% to leave margin
  }

  /**
   * Transform Cartesian to polar coordinates.
   * For theta='x': x becomes angle, y becomes radius
   * For theta='y': y becomes angle, x becomes radius
   */
  @CompileDynamic
  @Override
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']

    // Get normalized values (0-1 range from scales)
    Number xNorm = xScale ? getNormalizedValue(x, xScale) : (x ?: 0)
    Number yNorm = yScale ? getNormalizedValue(y, yScale) : (y ?: 0)

    // Determine which is theta (angle) and which is r (radius)
    double thetaValue, rValue
    if (theta == 'y') {
      thetaValue = yNorm as double
      rValue = xNorm as double
    } else {
      thetaValue = xNorm as double
      rValue = yNorm as double
    }

    // Convert to polar
    double angle = (start as double) + thetaValue * 2 * Math.PI
    if (!clockwise) {
      angle = (start as double) - thetaValue * 2 * Math.PI
    }

    double radius = rValue * getMaxRadius()

    // Convert polar to Cartesian pixel coordinates
    def center = getCenter()
    double cx = center[0] as double
    double cy = center[1] as double

    // In SVG, y increases downward, so adjust angle accordingly
    double px = cx + radius * Math.sin(angle)
    double py = cy - radius * Math.cos(angle)

    return [px, py]
  }

  /**
   * Get a normalized value (0-1) from a scale.
   */
  @CompileDynamic
  private double getNormalizedValue(Number value, def scale) {
    if (scale == null) return value as double

    // Get the transformed pixel value
    def transformed = scale.transform(value)
    if (transformed == null) return 0

    // Get the range to normalize
    def range = scale.range
    if (range == null || range.size() < 2) return transformed as double

    double min = range[0] as double
    double max = range[1] as double
    double rangeSize = Math.abs(max - min)

    if (rangeSize == 0) return 0

    // Normalize to 0-1
    return ((transformed as double) - Math.min(min, max)) / rangeSize
  }

  @CompileDynamic
  @Override
  List<Number> inverse(Number px, Number py, Map<String, ?> scales) {
    // Convert pixel coordinates back to data coordinates
    def center = getCenter()
    double cx = center[0] as double
    double cy = center[1] as double

    double dx = (px as double) - cx
    double dy = cy - (py as double)  // Invert y for SVG

    double radius = Math.sqrt(dx * dx + dy * dy)
    double angle = Math.atan2(dx, dy)  // Note: atan2(x,y) for our angle convention

    // Adjust for start offset and direction
    if (!clockwise) {
      angle = -angle
    }
    angle = angle - (start as double)

    // Normalize angle to 0-2π
    while (angle < 0) angle += 2 * Math.PI
    while (angle >= 2 * Math.PI) angle -= 2 * Math.PI

    double thetaNorm = angle / (2 * Math.PI)
    double rNorm = radius / getMaxRadius()

    // Convert back using scales
    def xScale = scales['x']
    def yScale = scales['y']

    Number xValue, yValue
    if (theta == 'y') {
      yValue = yScale ? yScale.inverse(thetaNorm * (yScale.range[1] - yScale.range[0]) + yScale.range[0]) : thetaNorm
      xValue = xScale ? xScale.inverse(rNorm * (xScale.range[1] - xScale.range[0]) + xScale.range[0]) : rNorm
    } else {
      xValue = xScale ? xScale.inverse(thetaNorm * (xScale.range[1] - xScale.range[0]) + xScale.range[0]) : thetaNorm
      yValue = yScale ? yScale.inverse(rNorm * (yScale.range[1] - yScale.range[0]) + yScale.range[0]) : rNorm
    }

    return [xValue, yValue]
  }

  /**
   * Create an arc path for a pie/donut slice.
   * @param startAngle Starting angle in radians
   * @param endAngle Ending angle in radians
   * @param innerRadius Inner radius (0 for pie, >0 for donut)
   * @param outerRadius Outer radius
   * @return SVG path string
   */
  String createArcPath(double startAngle, double endAngle, double innerRadius, double outerRadius) {
    def center = getCenter()
    double cx = center[0] as double
    double cy = center[1] as double

    // Adjust angles for SVG coordinate system and apply start offset
    double adjStart = (start as double) + startAngle
    double adjEnd = (start as double) + endAngle

    if (!clockwise) {
      adjStart = (start as double) - startAngle
      adjEnd = (start as double) - endAngle
    }

    // Calculate arc endpoints
    double x1 = cx + outerRadius * Math.sin(adjStart)
    double y1 = cy - outerRadius * Math.cos(adjStart)
    double x2 = cx + outerRadius * Math.sin(adjEnd)
    double y2 = cy - outerRadius * Math.cos(adjEnd)

    double x3 = cx + innerRadius * Math.sin(adjEnd)
    double y3 = cy - innerRadius * Math.cos(adjEnd)
    double x4 = cx + innerRadius * Math.sin(adjStart)
    double y4 = cy - innerRadius * Math.cos(adjStart)

    // Determine if arc is greater than 180 degrees
    double angleDiff = Math.abs(endAngle - startAngle)
    int largeArc = angleDiff > Math.PI ? 1 : 0
    int sweepFlag = clockwise ? 1 : 0

    StringBuilder path = new StringBuilder()

    if (innerRadius > 0) {
      // Donut slice
      path << "M ${formatNumber(x1)} ${formatNumber(y1)}"
      path << " A ${formatNumber(outerRadius)} ${formatNumber(outerRadius)} 0 ${largeArc} ${sweepFlag} ${formatNumber(x2)} ${formatNumber(y2)}"
      path << " L ${formatNumber(x3)} ${formatNumber(y3)}"
      path << " A ${formatNumber(innerRadius)} ${formatNumber(innerRadius)} 0 ${largeArc} ${1 - sweepFlag} ${formatNumber(x4)} ${formatNumber(y4)}"
      path << " Z"
    } else {
      // Pie slice
      path << "M ${formatNumber(cx)} ${formatNumber(cy)}"
      path << " L ${formatNumber(x1)} ${formatNumber(y1)}"
      path << " A ${formatNumber(outerRadius)} ${formatNumber(outerRadius)} 0 ${largeArc} ${sweepFlag} ${formatNumber(x2)} ${formatNumber(y2)}"
      path << " Z"
    }

    return path.toString()
  }

  /**
   * Format numeric SVG coordinates with fixed precision.
   * <p>
   * Uses fixed precision to ensure consistent decimal representation in SVG path data,
   * avoiding floating-point artifacts and reducing file size. Three decimal places
   * provides sub-pixel accuracy (0.001px) which is sufficient for typical display
   * resolutions while keeping coordinates compact.
   *
   * @param value numeric value to format
   * @return formatted string with 3 decimal places
   */
  private static String formatNumber(double value) {
    return String.format(Locale.US, "%.3f", value)
  }
}
