package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import java.util.Locale

/**
 * Radial coordinate system for circular plots with support for partial arcs and inner radius.
 *
 * Unlike coord_polar(), coord_radial() supports:
 * - Partial circles via start/end parameters
 * - Inner radius for donut-style charts
 * - Automatic label rotation
 */
@CompileStatic
class CoordRadial extends Coord {

  /** Which variable to map to angle: 'x' or 'y' (default: 'x') */
  String theta = 'x'

  /** Starting angle offset in radians (0 = 12 o'clock position) */
  BigDecimal start = 0

  /** Ending angle in radians (null = full circle) */
  BigDecimal end

  /** Whether to expand scale limits */
  boolean expand = true

  /** Direction: true for clockwise, false for anticlockwise */
  boolean clockwise = true

  /** Whether to clip drawing to the extent of the plot panel */
  boolean clip = false

  /** Inner radius ratio (0-1): 0 = pie, >0 = donut */
  BigDecimal innerRadius = 0

  /** Whether to automatically rotate theta labels */
  boolean rotateAngle = false

  /** Whether to place the r-axis labels inside the plot */
  Boolean rAxisInside

  /** Plot width in pixels (set by renderer) */
  int plotWidth = 640

  /** Plot height in pixels (set by renderer) */
  int plotHeight = 480

  /** Flag to indicate this is radial coordinates */
  final boolean radial = true

  CoordRadial() {}

  CoordRadial(Map params) {
    if (params.theta) this.theta = params.theta as String
    if (params.start != null) this.start = params.start as BigDecimal
    if (params.containsKey('end')) this.end = params.end != null ? params.end as BigDecimal : null
    if (params.containsKey('expand')) this.expand = params.expand as boolean
    if (params.containsKey('direction')) this.clockwise = (params.direction as int) == 1
    if (params.containsKey('clockwise')) this.clockwise = params.clockwise as boolean
    if (params.containsKey('clip')) {
      def clipParam = params.clip
      if (clipParam instanceof CharSequence) {
        this.clip = clipParam.toString().toLowerCase() == 'on'
      } else {
        this.clip = clipParam as boolean
      }
    }
    if (params.containsKey('innerRadius')) {
      this.innerRadius = params.innerRadius != null ? params.innerRadius as BigDecimal : 0
    }
    if (params.containsKey('inner.radius')) {
      this.innerRadius = params['inner.radius'] != null ? params['inner.radius'] as BigDecimal : 0
    }
    if (params.containsKey('rotateAngle')) this.rotateAngle = params.rotateAngle as boolean
    if (params.containsKey('rotate.angle')) this.rotateAngle = params['rotate.angle'] as boolean
    if (params.containsKey('rAxisInside')) this.rAxisInside = params.rAxisInside as Boolean
    if (params.containsKey('r.axis.inside')) this.rAxisInside = params['r.axis.inside'] as Boolean

    if (theta != 'x' && theta != 'y') {
      theta = 'x'
    }

    if (innerRadius < 0) {
      innerRadius = 0
    }
    if (innerRadius > 1) {
      innerRadius = 1
    }
  }

  /**
   * Get the center point of the radial plot in pixels.
   */
  List<BigDecimal> getCenter() {
    return [plotWidth / 2, plotHeight / 2]
  }

  /**
   * Get the maximum radius that fits in the plot area.
   */
  BigDecimal getMaxRadius() {
    return plotWidth.min(plotHeight) / 2 * 0.9
  }

  /**
   * Get the inner radius in pixels.
   */
  BigDecimal getInnerRadiusPx() {
    return innerRadius * getMaxRadius()
  }

  /**
   * Get the angular range in radians.
   */
  List<BigDecimal> getAngularRange() {
    BigDecimal fullCircle = (2.0d * Math.PI) as BigDecimal
    BigDecimal effectiveEnd = (end != null) ? end : (start + fullCircle)
    return [start, effectiveEnd]
  }

  /**
   * Get the angular span in radians.
   */
  BigDecimal getAngularSpan() {
    List<BigDecimal> range = getAngularRange()
    return (range[1] - range[0]).abs()
  }

  /**
   * Transform Cartesian to radial coordinates.
   * For theta='x': x becomes angle, y becomes radius
   * For theta='y': y becomes angle, x becomes radius
   */
  @CompileDynamic
  @Override
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']

    BigDecimal xNorm = xScale ? getNormalizedValue(x, xScale) : (x ?: 0) as BigDecimal
    BigDecimal yNorm = yScale ? getNormalizedValue(y, yScale) : (y ?: 0) as BigDecimal

    BigDecimal thetaNorm
    BigDecimal rNorm
    if (theta == 'y') {
      thetaNorm = yNorm
      rNorm = xNorm
    } else {
      thetaNorm = xNorm
      rNorm = yNorm
    }

    BigDecimal span = getAngularSpan()
    BigDecimal angle = clockwise ? (start + thetaNorm * span) : (start - thetaNorm * span)

    BigDecimal maxRadius = getMaxRadius()
    BigDecimal minRadius = getInnerRadiusPx()
    BigDecimal radius = minRadius + rNorm * (maxRadius - minRadius)

    List<BigDecimal> center = getCenter()
    BigDecimal px = center[0] + radius * angle.sin()
    BigDecimal py = center[1] - radius * angle.cos()

    return [px, py]
  }

  /**
   * Inverse transform from pixel coordinates back to data coordinates.
   */
  @CompileDynamic
  @Override
  List<Number> inverse(Number px, Number py, Map<String, ?> scales) {
    List<BigDecimal> center = getCenter()
    BigDecimal cx = center[0]
    BigDecimal cy = center[1]

    BigDecimal dx = (px as BigDecimal) - cx
    BigDecimal dy = cy - (py as BigDecimal)

    BigDecimal radius = (dx * dx + dy * dy).sqrt()
    BigDecimal angle = Math.atan2(dx as double, dy as double) as BigDecimal

    if (!clockwise) {
      angle = -angle
    }
    angle = angle - start

    double angleVal = angle as double
    double spanVal = getAngularSpan() as double
    double fullCircle = 2 * Math.PI

    while (angleVal < 0) angleVal += fullCircle
    while (angleVal >= fullCircle) angleVal -= fullCircle

    double thetaNormVal = spanVal == 0 ? 0.0d : angleVal / spanVal
    BigDecimal thetaNorm = thetaNormVal as BigDecimal
    thetaNorm = 0.max(thetaNorm.min(1))

    BigDecimal minRadius = getInnerRadiusPx()
    BigDecimal maxRadius = getMaxRadius()
    BigDecimal denom = maxRadius - minRadius
    BigDecimal rNorm = denom == 0 ? 0 : (radius - minRadius) / denom
    rNorm = 0.max(rNorm.min(1))

    def xScale = scales['x']
    def yScale = scales['y']

    Number xValue
    Number yValue
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
   * Create an arc path for a radial slice.
   *
   * @param startAngle Starting angle offset in radians
   * @param endAngle Ending angle offset in radians
   * @param innerRadius Inner radius (0 for pie, >0 for donut)
   * @param outerRadius Outer radius
   * @return SVG path string
   */
  String createArcPath(double startAngle, double endAngle, double innerRadius, double outerRadius) {
    List<BigDecimal> center = getCenter()
    double cx = center[0] as double
    double cy = center[1] as double

    BigDecimal span = getAngularSpan()
    BigDecimal startOffset = (startAngle as BigDecimal).max(0).min(span)
    BigDecimal endOffset = (endAngle as BigDecimal).max(0).min(span)

    BigDecimal base = getAngularRange()[0]
    BigDecimal adjStart = clockwise ? (base + startOffset) : (base - startOffset)
    BigDecimal adjEnd = clockwise ? (base + endOffset) : (base - endOffset)

    double startRad = adjStart as double
    double endRad = adjEnd as double

    double x1 = cx + outerRadius * Math.sin(startRad)
    double y1 = cy - outerRadius * Math.cos(startRad)
    double x2 = cx + outerRadius * Math.sin(endRad)
    double y2 = cy - outerRadius * Math.cos(endRad)

    double x3 = cx + innerRadius * Math.sin(endRad)
    double y3 = cy - innerRadius * Math.cos(endRad)
    double x4 = cx + innerRadius * Math.sin(startRad)
    double y4 = cy - innerRadius * Math.cos(startRad)

    double angleDiff = (endOffset - startOffset).abs() as double
    int largeArc = angleDiff > Math.PI ? 1 : 0
    int sweepFlag = clockwise ? 1 : 0

    StringBuilder path = new StringBuilder()

    if (innerRadius > 0) {
      path << "M ${formatNumber(x1)} ${formatNumber(y1)}"
      path << " A ${formatNumber(outerRadius)} ${formatNumber(outerRadius)} 0 ${largeArc} ${sweepFlag} ${formatNumber(x2)} ${formatNumber(y2)}"
      path << " L ${formatNumber(x3)} ${formatNumber(y3)}"
      path << " A ${formatNumber(innerRadius)} ${formatNumber(innerRadius)} 0 ${largeArc} ${1 - sweepFlag} ${formatNumber(x4)} ${formatNumber(y4)}"
      path << " Z"
    } else {
      path << "M ${formatNumber(cx)} ${formatNumber(cy)}"
      path << " L ${formatNumber(x1)} ${formatNumber(y1)}"
      path << " A ${formatNumber(outerRadius)} ${formatNumber(outerRadius)} 0 ${largeArc} ${sweepFlag} ${formatNumber(x2)} ${formatNumber(y2)}"
      path << " Z"
    }

    return path.toString()
  }

  /**
   * Calculate text rotation angle for a given theta position.
   */
  BigDecimal getTextRotation(Number thetaNorm) {
    if (!rotateAngle) return 0

    BigDecimal span = getAngularSpan()
    BigDecimal angle = clockwise ? (start + (thetaNorm as BigDecimal) * span) : (start - (thetaNorm as BigDecimal) * span)
    BigDecimal degrees = angle.toDegrees()

    if (degrees > 90 && degrees < 270) {
      degrees += 180
    }

    return degrees
  }

  /**
   * Get a normalized value (0-1) from a scale.
   */
  @CompileDynamic
  private BigDecimal getNormalizedValue(Number value, def scale) {
    if (scale == null) return value as BigDecimal

    def transformed = scale.transform(value)
    if (!(transformed instanceof Number)) return 0.0

    def range = scale.range
    if (range == null || range.size() < 2) return transformed as BigDecimal

    BigDecimal min = range[0] as BigDecimal
    BigDecimal max = range[1] as BigDecimal
    BigDecimal rangeSize = (max - min).abs()

    if (rangeSize == 0) return 0.0

    BigDecimal low = min < max ? min : max
    return ((transformed as BigDecimal) - low) / rangeSize
  }

  /**
   * Format numeric SVG coordinates with fixed precision.
   */
  private static String formatNumber(double value) {
    return String.format(Locale.US, "%.3f", value)
  }
}
