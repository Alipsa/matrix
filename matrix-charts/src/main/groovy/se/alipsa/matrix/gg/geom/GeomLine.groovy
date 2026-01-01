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

/**
 * Line geometry for line charts.
 * Connects data points with lines, optionally grouped by a variable.
 */
@CompileStatic
class GeomLine extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  GeomLine() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomLine(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size) this.size = params.size as Number
    if (params.linewidth) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype
    if (params.alpha) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomLine requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    // Group data if a group aesthetic is specified
    Map<Object, List<Map>> groups = [:]
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

    // Render each group as a separate line
    groups.each { groupKey, rows ->
      renderLine(group, rows, xCol, yCol, colorCol, groupKey,
                 xScale, yScale, colorScale, aes)
    }
  }

  private void renderLine(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale, Aes aes) {
    // Collect points and sort by x value
    List<Map> sortedRows = sortRowsByX(rows, xCol)

    // Collect transformed points
    List<double[]> points = []
    sortedRows.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform using scales
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      if (xTransformed == null || yTransformed == null) return

      double xPx = xTransformed as double
      double yPx = yTransformed as double

      points << ([xPx, yPx] as double[])
    }

    if (points.size() < 2) return

    // Determine line color
    String lineColor = this.color
    if (colorCol && groupKey != '__all__') {
      if (colorScale) {
        lineColor = colorScale.transform(groupKey)?.toString() ?: this.color
      } else {
        lineColor = getDefaultColor(groupKey)
      }
    } else if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    // Get stroke-dasharray for line type
    String dashArray = getDashArray(linetype)

    // Draw connected line segments
    for (int i = 0; i < points.size() - 1; i++) {
      double[] p1 = points[i]
      double[] p2 = points[i + 1]

      def line = group.addLine(p1[0], p1[1], p2[0], p2[1])
          .stroke(lineColor)
          .strokeWidth(size)

      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }

      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
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

  /**
   * Convert line type to SVG stroke-dasharray.
   */
  private String getDashArray(String type) {
    switch (type) {
      case 'dashed':
        return '8,4'
      case 'dotted':
        return '2,2'
      case 'dotdash':
        return '2,2,8,2'
      case 'longdash':
        return '12,4'
      case 'twodash':
        return '4,2,8,2'
      case 'solid':
      default:
        return null
    }
  }

  /**
   * Get a default color from a discrete palette based on a value.
   */
  private String getDefaultColor(Object value) {
    List<String> palette = [
        '#F8766D', '#C49A00', '#53B400',
        '#00C094', '#00B6EB', '#A58AFF',
        '#FB61D7'
    ]

    int index = Math.abs(value.hashCode()) % palette.size()
    return palette[index]
  }
}
