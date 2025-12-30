package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Line segment geometry for drawing lines from (x, y) to (xend, yend).
 *
 * Usage:
 * - geom_segment(x: 1, y: 1, xend: 5, yend: 5) - single segment
 * - With data mapping: aes(x: 'x1', y: 'y1', xend: 'x2', yend: 'y2')
 */
@CompileStatic
class GeomSegment extends Geom {

  /** Starting x coordinate */
  Number x

  /** Starting y coordinate */
  Number y

  /** Ending x coordinate */
  Number xend

  /** Ending y coordinate */
  Number yend

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Arrow at end of segment */
  boolean arrow = false

  /** Arrow size (width in pixels) */
  Number arrowSize = 10

  GeomSegment() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'xend', 'yend']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomSegment(Map params) {
    this()
    if (params.x != null) this.x = params.x as Number
    if (params.y != null) this.y = params.y as Number
    if (params.xend != null) this.xend = params.xend as Number
    if (params.yend != null) this.yend = params.yend as Number
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.arrow != null) this.arrow = params.arrow as boolean
    if (params.arrowSize != null) this.arrowSize = params.arrowSize as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    // Collect segments
    List<Map<String, Number>> segments = []

    // From parameters
    if (x != null && y != null && xend != null && yend != null) {
      segments << [x: x, y: y, xend: xend, yend: yend]
    }

    // From data mapping
    if (data != null && data.rowCount() > 0) {
      List<String> colNames = data.columnNames()
      String xCol = aes?.xColName ?: 'x'
      String yCol = aes?.yColName ?: 'y'
      // xend/yend come from params passed to geom, or default column names
      String xendCol = params?.get('xend')?.toString() ?: 'xend'
      String yendCol = params?.get('yend')?.toString() ?: 'yend'

      boolean hasAllCols = [xCol, yCol, xendCol, yendCol].every { colNames.contains(it) }

      if (hasAllCols) {
        for (int i = 0; i < data.rowCount(); i++) {
          def xVal = data[xCol][i]
          def yVal = data[yCol][i]
          def xendVal = data[xendCol][i]
          def yendVal = data[yendCol][i]

          if (xVal instanceof Number && yVal instanceof Number &&
              xendVal instanceof Number && yendVal instanceof Number) {
            segments << [
                x: xVal as Number,
                y: yVal as Number,
                xend: xendVal as Number,
                yend: yendVal as Number
            ]
          }
        }
      }
    }

    if (segments.isEmpty()) return

    // Draw segments
    segments.each { Map<String, Number> seg ->
      def x1Px = xScale.transform(seg.x)
      def y1Px = yScale.transform(seg.y)
      def x2Px = xScale.transform(seg.xend)
      def y2Px = yScale.transform(seg.yend)

      if (x1Px == null || y1Px == null || x2Px == null || y2Px == null) return

      def line = group.addLine()
          .x1(x1Px as int)
          .y1(y1Px as int)
          .x2(x2Px as int)
          .y2(y2Px as int)
          .stroke(color)

      line.addAttribute('stroke-width', linewidth)

      // Apply line type
      String dashArray = getLineDashArray(linetype)
      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }

      // Apply alpha
      if ((alpha as double) < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }

      // Draw arrow if requested
      if (arrow) {
        drawArrow(group, x2Px as double, y2Px as double,
                  x1Px as double, y1Px as double)
      }
    }
  }

  /**
   * Draw an arrowhead at the end point.
   */
  private void drawArrow(G group, double x2, double y2, double x1, double y1) {
    // Calculate angle of the line
    double angle = Math.atan2(y2 - y1, x2 - x1)
    double arrowAngle = Math.PI / 6  // 30 degrees
    double size = arrowSize as double

    // Calculate arrowhead points
    double ax1 = x2 - size * Math.cos(angle - arrowAngle)
    double ay1 = y2 - size * Math.sin(angle - arrowAngle)
    double ax2 = x2 - size * Math.cos(angle + arrowAngle)
    double ay2 = y2 - size * Math.sin(angle + arrowAngle)

    // Draw arrowhead as two lines
    def arrow1 = group.addLine()
        .x1(x2 as int)
        .y1(y2 as int)
        .x2(ax1 as int)
        .y2(ay1 as int)
        .stroke(color)
    arrow1.addAttribute('stroke-width', linewidth)

    def arrow2 = group.addLine()
        .x1(x2 as int)
        .y1(y2 as int)
        .x2(ax2 as int)
        .y2(ay2 as int)
        .stroke(color)
    arrow2.addAttribute('stroke-width', linewidth)

    // Apply alpha to arrows
    if ((alpha as double) < 1.0) {
      arrow1.addAttribute('stroke-opacity', alpha)
      arrow2.addAttribute('stroke-opacity', alpha)
    }
  }

  /**
   * Convert line type name to SVG stroke-dasharray value.
   */
  private String getLineDashArray(String type) {
    switch (type?.toLowerCase()) {
      case 'dashed': return '5,5'
      case 'dotted': return '2,2'
      case 'longdash': return '10,5'
      case 'twodash': return '10,5,2,5'
      case 'solid':
      default: return null
    }
  }
}
