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
 * Spoke geometry for drawing radial line segments from a point.
 * Each spoke is defined by a center point (x, y), an angle, and a radius.
 * Useful for wind roses, direction plots, and radial visualizations.
 *
 * Required aesthetics: x, y, angle, radius
 * Optional aesthetics: color, size/linewidth, linetype, alpha
 *
 * Note: Angles are in radians by default. Use angle * pi / 180 to convert from degrees.
 *
 * Usage:
 * - geom_spoke() - with data containing x, y, angle, radius columns
 * - geom_spoke(aes('x', 'y', angle: 'direction', radius: 'strength'))
 */
@CompileStatic
class GeomSpoke extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Default radius if not specified in data */
  Number radius = 1

  GeomSpoke() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomSpoke(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.radius != null) this.radius = params.radius as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    String xCol = aes?.xColName ?: 'x'
    String yCol = aes?.yColName ?: 'y'
    String angleCol = params?.get('angle')?.toString() ?: 'angle'
    String radiusCol = params?.get('radius')?.toString() ?: 'radius'

    List<String> colNames = data.columnNames()
    if (!colNames.contains(xCol) || !colNames.contains(yCol)) return

    boolean hasAngle = colNames.contains(angleCol)
    boolean hasRadius = colNames.contains(radiusCol)

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (!(xVal instanceof Number) || !(yVal instanceof Number)) return

      // Get angle (in radians)
      double angle = 0
      if (hasAngle) {
        def angleVal = row[angleCol]
        if (angleVal instanceof Number) {
          angle = angleVal as double
        } else {
          return
        }
      }

      // Get radius
      double rad = this.radius as double
      if (hasRadius) {
        def radiusVal = row[radiusCol]
        if (radiusVal instanceof Number) {
          rad = radiusVal as double
        }
      }

      // Transform center point
      def x0Px = xScale.transform(xVal)
      def y0Px = yScale.transform(yVal)

      if (x0Px == null || y0Px == null) return

      double x0 = x0Px as double
      double y0 = y0Px as double

      // Calculate end point in data space
      double xEnd = (xVal as double) + rad * Math.cos(angle)
      double yEnd = (yVal as double) + rad * Math.sin(angle)

      // Transform end point
      def x1Px = xScale.transform(xEnd)
      def y1Px = yScale.transform(yEnd)

      if (x1Px == null || y1Px == null) return

      double x1 = x1Px as double
      double y1 = y1Px as double

      // Draw spoke
      String lineColor = ColorUtil.normalizeColor(color) ?: color
      def line = group.addLine()
          .x1(x0 as int)
          .y1(y0 as int)
          .x2(x1 as int)
          .y2(y1 as int)
          .stroke(lineColor)

      line.addAttribute('stroke-width', linewidth)

      // Apply line type
      String dashArray = getLineDashArray(linetype)
      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }

      // Apply alpha
      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
    }
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
