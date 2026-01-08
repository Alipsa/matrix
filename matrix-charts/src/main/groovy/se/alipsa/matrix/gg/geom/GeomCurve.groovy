package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
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

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    List<String> colNames = data.columnNames()
    String xCol = aes?.xColName ?: 'x'
    String yCol = aes?.yColName ?: 'y'
    String xendCol = params?.get('xend')?.toString() ?: 'xend'
    String yendCol = params?.get('yend')?.toString() ?: 'yend'

    boolean hasAllCols = [xCol, yCol, xendCol, yendCol].every { colNames.contains(it) }
    if (!hasAllCols) return

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def xendVal = row[xendCol]
      def yendVal = row[yendCol]

      if (!(xVal instanceof Number) || !(yVal instanceof Number) ||
          !(xendVal instanceof Number) || !(yendVal instanceof Number)) {
        return
      }

      // Transform coordinates
      def x1Px = xScale.transform(xVal)
      def y1Px = yScale.transform(yVal)
      def x2Px = xScale.transform(xendVal)
      def y2Px = yScale.transform(yendVal)

      if (x1Px == null || y1Px == null || x2Px == null || y2Px == null) return

      BigDecimal x1 = x1Px as BigDecimal
      BigDecimal y1 = y1Px as BigDecimal
      BigDecimal x2 = x2Px as BigDecimal
      BigDecimal y2 = y2Px as BigDecimal

      // Calculate control points for cubic Bezier curve
      List<BigDecimal> controlPoints = calculateControlPoints(
          x1, y1, x2, y2, curvature as BigDecimal)
      BigDecimal cx1 = controlPoints[0]
      BigDecimal cy1 = controlPoints[1]
      BigDecimal cx2 = controlPoints[2]
      BigDecimal cy2 = controlPoints[3]

      // Build SVG path with cubic Bezier
      String pathData = "M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}"

      // Draw curve
      String lineColor = ColorUtil.normalizeColor(color) ?: color
      def path = group.addPath()
          .d(pathData)
          .fill('none')
          .stroke(lineColor)

      path.addAttribute('stroke-width', linewidth)

      // Apply line type
      String dashArray = getLineDashArray(linetype)
      if (dashArray) {
        path.addAttribute('stroke-dasharray', dashArray)
      }

      // Apply alpha
      if (alpha < 1.0) {
        path.addAttribute('stroke-opacity', alpha)
      }
    }
  }

  /**
   * Calculate control points for a cubic Bezier curve.
   * Based on ggplot2's curve geometry algorithm.
   */
  private List<BigDecimal> calculateControlPoints(BigDecimal x1, BigDecimal y1, BigDecimal x2, BigDecimal y2,
                                                   BigDecimal curvature) {
    // Calculate midpoint
    BigDecimal mx = (x1 + x2) / 2
    BigDecimal my = (y1 + y2) / 2

    // Calculate perpendicular offset based on curvature
    BigDecimal dx = x2 - x1
    BigDecimal dy = y2 - y1
    BigDecimal dist = (dx * dx + dy * dy).sqrt()

    // Perpendicular direction (rotate 90 degrees)
    BigDecimal px = -dy
    BigDecimal py = dx

    // Normalize perpendicular vector
    if (dist > 0) {
      px = px / dist
      py = py / dist
    }

    // Apply curvature
    BigDecimal offset = dist * curvature * 0.5G
    BigDecimal offsetX = px * offset
    BigDecimal offsetY = py * offset

    // Control point at midpoint offset perpendicular to line
    BigDecimal cx = mx + offsetX
    BigDecimal cy = my + offsetY

    // For cubic Bezier, use the same control point twice (gives smooth curve)
    // Or split the offset for smoother curves
    BigDecimal cx1 = x1 + (cx - x1) * 0.67G
    BigDecimal cy1 = y1 + (cy - y1) * 0.67G
    BigDecimal cx2 = x2 + (cx - x2) * 0.67G
    BigDecimal cy2 = y2 + (cy - y2) * 0.67G

    return [cx1, cy1, cx2, cy2]
  }

  /**
   * Convert line type name to SVG stroke-dasharray value.
   */
  private String getLineDashArray(String type) {
    final Map<String, String> dashArray = [
        dashed: '5,5',
        dotted: '2,2',
        longdash: '10,5',
        twodash: '10,5,2,5'
    ]
    dashArray[type?.toLowerCase()]
  }
}
