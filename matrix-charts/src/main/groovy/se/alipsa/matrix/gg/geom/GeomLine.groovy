package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.geom.Point

/**
 * Line geometry for line charts.
 * Connects data points with lines, optionally grouped by a variable.
 */
@CompileStatic
class GeomLine extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  GeomLine() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomLine(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.size = params.linewidth as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomLine requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale alphaScale = scales['alpha']

    // Group data if a group aesthetic is specified - use idiomatic groupBy
    def groups = data.rows().groupBy { row ->
      groupCol ? row[groupCol] : '__all__'
    }

    // Render each group as a separate line
    groups.each { groupKey, rows ->
      renderLine(group, rows*.toMap(), xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
                 xScale, yScale, colorScale, sizeScale, alphaScale, aes)
    }
  }

  private void renderLine(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, String sizeCol, String alphaCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale,
                          Scale sizeScale, Scale alphaScale, Aes aes) {
    // Collect points and sort by x value
    List<Map> sortedRows = sortRowsByX(rows, xCol)

    // Collect transformed points - use idiomatic collect instead of each
    List<Point> points = sortedRows.collect { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal == null || yVal == null) return null

      // Transform using scales
      Number xTransformed = xScale?.transform(xVal) as Number
      Number yTransformed = yScale?.transform(yVal) as Number
      if (xTransformed == null || yTransformed == null) return null

      new Point(xTransformed, yTransformed)
    }.findAll() // Remove nulls

    if (points.size() < 2) return

    // Determine line color
    String lineColor = this.color
    if (colorCol && groupKey != '__all__') {
      if (colorScale) {
        lineColor = colorScale.transform(groupKey)?.toString() ?: this.color
      } else {
        lineColor = GeomUtils.getDefaultColor(groupKey)
      }
    } else if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    BigDecimal lineSize = GeomUtils.extractLineSize(this.size, aes, sizeCol, rows, sizeScale)
    BigDecimal lineAlpha = GeomUtils.extractLineAlpha(this.alpha, aes, alphaCol, rows, alphaScale)

    // Get stroke-dasharray for line type
    String dashArray = GeomUtils.getDashArray(linetype)

    // Draw connected line segments
    for (int i = 0; i < points.size() - 1; i++) {
      Point p1 = points[i]
      Point p2 = points[i + 1]

      def line = group.addLine(p1.x, p1.y, p2.x, p2.y)
          .stroke(lineColor)
          .strokeWidth(lineSize)

      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }

      if (lineAlpha < 1.0) {
        line.addAttribute('stroke-opacity', lineAlpha)
      }
    }
  }

  /**
   * Sort rows by x value for proper line connection.
   */
  private List<Map> sortRowsByX(List<Map> rows, String xCol) {
    return rows.sort { a, b ->
      def xA = a[xCol]
      def xB = b[xCol]

      // Handle numeric comparison
      if (xA instanceof Number && xB instanceof Number) {
        return (xA as Number) <=> (xB as Number)
      }

      // Handle comparable types
      if (xA instanceof Comparable && xB instanceof Comparable) {
        return (xA as Comparable) <=> (xB as Comparable)
      }

      // Fall back to string comparison
      return xA?.toString() <=> xB?.toString()
    }
  }
}
