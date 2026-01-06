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
 * Q-Q plot reference line.
 * Uses stat_qq_line to compute line endpoints.
 */
@CompileStatic
class GeomQqLine extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /**
   * Create a Q-Q line geom with default settings.
   */
  GeomQqLine() {
    defaultStat = StatType.QQ_LINE
    requiredAes = ['x']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  /**
   * Create a Q-Q line geom with parameters.
   *
   * @param params geom parameters
   */
  GeomQqLine(Map params) {
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

    String xCol = data.columnNames().contains('x') ? 'x' : aes.xColName
    String yCol = data.columnNames().contains('y') ? 'y' : aes.yColName
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomQqLine requires stat_qq_line output with x and y columns")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale alphaScale = scales['alpha']

    Map<Object, List<Map>> groups = [:]
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

    groups.each { groupKey, rows ->
      renderLine(group, rows, xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
          xScale, yScale, colorScale, sizeScale, alphaScale, aes)
    }
  }

  private void renderLine(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, String sizeCol, String alphaCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale,
                          Scale sizeScale, Scale alphaScale, Aes aes) {
    List<Map> sortedRows = rows.sort { a, b ->
      def xA = a[xCol]
      def xB = b[xCol]
      if (xA instanceof Number && xB instanceof Number) {
        return (xA as Number) <=> (xB as Number)
      }
      if (xA instanceof Comparable && xB instanceof Comparable) {
        return (xA as Comparable) <=> (xB as Comparable)
      }
      return xA?.toString() <=> xB?.toString()
    }

    List<double[]> points = []
    sortedRows.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal == null || yVal == null) return

      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)
      if (xTransformed == null || yTransformed == null) return

      points << ([xTransformed as double, yTransformed as double] as double[])
    }

    if (points.size() < 2) return

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

    Number lineSize = GeomUtils.extractLineSize(this.size, aes, sizeCol, rows, sizeScale)
    Number lineAlpha = GeomUtils.extractLineAlpha(this.alpha, aes, alphaCol, rows, alphaScale)

    String dashArray = GeomUtils.getDashArray(linetype)

    for (int i = 0; i < points.size() - 1; i++) {
      double[] p1 = points[i]
      double[] p2 = points[i + 1]

      def line = group.addLine(p1[0], p1[1], p2[0], p2[1])
          .stroke(lineColor)
          .strokeWidth(lineSize)

      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }
      if ((lineAlpha as double) < 1.0) {
        line.addAttribute('stroke-opacity', lineAlpha)
      }
    }
  }
}
