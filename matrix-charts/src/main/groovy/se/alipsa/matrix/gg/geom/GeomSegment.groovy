package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

import static se.alipsa.matrix.ext.NumberExtension.PI

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
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
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
      Number x1Px = xScale.transform(seg.x) as Number
      Number y1Px = yScale.transform(seg.y) as Number
      Number x2Px = xScale.transform(seg.xend) as Number
      Number y2Px = yScale.transform(seg.yend) as Number

      if (x1Px == null || y1Px == null || x2Px == null || y2Px == null) return

      String lineColor = ColorUtil.normalizeColor(color) ?: color
      def line = group.addLine()
          .x1(x1Px)
          .y1(y1Px)
          .x2(x2Px)
          .y2(y2Px)
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

      // Draw arrow if requested
      if (arrow) {
        drawArrow(group, x2Px, y2Px, x1Px, y1Px)
      }
    }
  }

  /**
   * Draw an arrowhead at the end point.
   */
  private void drawArrow(G group, Number x2, Number y2, Number x1, Number y1) {
    // Calculate angle of the line
    BigDecimal dy = (y2 as BigDecimal) - (y1 as BigDecimal)
    BigDecimal dx = (x2 as BigDecimal) - (x1 as BigDecimal)
    BigDecimal angle = Math.atan2(dy as double, dx as double) as BigDecimal
    BigDecimal arrowAngle = PI / 6  // 30 degrees
    BigDecimal size = arrowSize as BigDecimal

    // Calculate arrowhead points
    BigDecimal ax1 = (x2 as BigDecimal) - size * (angle - arrowAngle).cos()
    BigDecimal ay1 = (y2 as BigDecimal) - size * (angle - arrowAngle).sin()
    BigDecimal ax2 = (x2 as BigDecimal) - size * (angle + arrowAngle).cos()
    BigDecimal ay2 = (y2 as BigDecimal) - size * (angle + arrowAngle).sin()

    // Draw arrowhead as two lines
    String lineColor = ColorUtil.normalizeColor(color) ?: color
    def arrow1 = group.addLine()
        .x1(x2)
        .y1(y2)
        .x2(ax1)
        .y2(ay1)
        .stroke(lineColor)
    arrow1.addAttribute('stroke-width', linewidth)

    def arrow2 = group.addLine()
        .x1(x2)
        .y1(y2)
        .x2(ax2)
        .y2(ay2)
        .stroke(lineColor)
    arrow2.addAttribute('stroke-width', linewidth)

    // Apply alpha to arrows
    if (alpha < 1.0) {
      arrow1.addAttribute('stroke-opacity', alpha)
      arrow2.addAttribute('stroke-opacity', alpha)
    }
  }

  /**
   * Convert line type name to SVG stroke-dasharray value.
   */
  private String getLineDashArray(String type) {
    switch (type?.toLowerCase()) {
      case 'dashed' -> '5,5'
      case 'dotted' -> '2,2'
      case 'longdash' -> '10,5'
      case 'twodash' -> '10,5,2,5'
      case 'solid' -> null
      default -> null
    }
  }
}
