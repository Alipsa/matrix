package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale

/**
 * Frequency polygon geometry.
 * Uses stat_bin to compute counts per bin and draws a line through the bin centers.
 */
@CompileStatic
class GeomFreqpoly extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /**
   * Create a frequency polygon geom with default settings.
   */
  GeomFreqpoly() {
    defaultStat = StatType.BIN
    requiredAes = ['x']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  /**
   * Create a frequency polygon geom with parameters.
   *
   * @param params geom parameters
   */
  GeomFreqpoly(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size != null) this.size = params.size as Number
    if (params.linewidth != null) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = data.columnNames().contains('x') ? 'x' : aes.xColName
    String yCol = resolveYColumn(data, aes)
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomFreqpoly requires stat_bin output with x and count/density columns")
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

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() < 2) return

    String xCol = data.columnNames().contains('x') ? 'x' : aes.xColName
    String yCol = resolveYColumn(data, aes)
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomFreqpoly requires stat_bin output with x and count/density columns")
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
      renderLineWithContext(group, rows, xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
          xScale, yScale, colorScale, sizeScale, alphaScale, aes, ctx)
    }
  }

  private String resolveYColumn(Matrix data, Aes aes) {
    if (aes.isAfterStat('y')) {
      String statCol = aes.getAfterStatName('y')
      if (statCol && data.columnNames().contains(statCol)) {
        return statCol
      }
    }
    String yCol = aes.yColName
    if (yCol && data.columnNames().contains(yCol)) {
      return yCol
    }
    if (data.columnNames().contains('count')) {
      return 'count'
    }
    if (data.columnNames().contains('density')) {
      return 'density'
    }
    if (data.columnNames().contains('y')) {
      return 'y'
    }
    return null
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

  private void renderLineWithContext(G group, List<Map> rows, String xCol, String yCol,
                                     String colorCol, String sizeCol, String alphaCol, Object groupKey,
                                     Scale xScale, Scale yScale, Scale colorScale,
                                     Scale sizeScale, Scale alphaScale, Aes aes, RenderContext ctx) {
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

    int elementIndex = 0
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

      // Apply CSS attributes
      GeomUtils.applyAttributes(line, ctx, 'freqpoly', 'gg-freqpoly', elementIndex)
      elementIndex++
    }
  }
}
