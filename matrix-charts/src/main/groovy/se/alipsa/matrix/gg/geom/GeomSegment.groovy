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
  BigDecimal x

  /** Starting y coordinate */
  BigDecimal y

  /** Ending x coordinate */
  BigDecimal xend

  /** Ending y coordinate */
  BigDecimal yend

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Arrow at end of segment */
  boolean arrow = false

  /** Arrow size (width in pixels) */
  BigDecimal arrowSize = 10

  GeomSegment() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'xend', 'yend']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomSegment(Map params) {
    this()
    if (params.x != null) this.x = params.x as BigDecimal
    if (params.y != null) this.y = params.y as BigDecimal
    if (params.xend != null) this.xend = params.xend as BigDecimal
    if (params.yend != null) this.yend = params.yend as BigDecimal
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.arrow != null) this.arrow = params.arrow as boolean
    if (params.arrowSize != null) this.arrowSize = params.arrowSize as BigDecimal
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    // Collect segments
    List<Map<String, BigDecimal>> segments = []

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
                x: xVal as BigDecimal,
                y: yVal as BigDecimal,
                xend: xendVal as BigDecimal,
                yend: yendVal as BigDecimal
            ]
          }
        }
      }
    }

    if (segments.isEmpty()) return

    // Draw segments
    segments.each { Map<String, BigDecimal> seg ->
      BigDecimal x1Px = xScale.transform(seg.x) as BigDecimal
      BigDecimal y1Px = yScale.transform(seg.y) as BigDecimal
      BigDecimal x2Px = xScale.transform(seg.xend) as BigDecimal
      BigDecimal y2Px = yScale.transform(seg.yend) as BigDecimal

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
  private void drawArrow(G group, BigDecimal x2, BigDecimal y2, BigDecimal x1, BigDecimal y1) {
    // Calculate angle of the line
    BigDecimal dy = y2 - y1
    BigDecimal dx = x2 - x1
    BigDecimal angle = dy.atan2(dx)
    BigDecimal arrowAngle = PI / 6  // 30 degrees
    BigDecimal size = arrowSize

    // Calculate arrowhead points
    BigDecimal ax1 = x2 - size * (angle - arrowAngle).cos()
    BigDecimal ay1 = y2 - size * (angle - arrowAngle).sin()
    BigDecimal ax2 = x2 - size * (angle + arrowAngle).cos()
    BigDecimal ay2 = y2 - size * (angle + arrowAngle).sin()

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
      case 'solid', null -> null
      default -> null
    }
  }
}
