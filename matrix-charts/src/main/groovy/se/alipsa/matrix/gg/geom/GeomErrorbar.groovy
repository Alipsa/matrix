package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Error bar geometry for intervals (ymin to ymax) at each x position.
 * Mirrors ggplot2's geom_errorbar contract as closely as possible.
 */
@CompileStatic
class GeomErrorbar extends Geom {

  /** Bar width as fraction of bandwidth (discrete) or data units (continuous) */
  Number width = null

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomErrorbar() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomErrorbar(Map params) {
    this()
    if (params.width != null) this.width = params.width as Number
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes?.xColName ?: 'x'
    String yminCol = params?.get('ymin')?.toString() ?: 'ymin'
    String ymaxCol = params?.get('ymax')?.toString() ?: 'ymax'

    if (!data.columnNames().containsAll([xCol, yminCol, ymaxCol])) {
      return
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    double defaultWidth = resolveDefaultWidth(xScale, data, xCol)
    double widthValue = width != null ? (width as double) : defaultWidth

    data.each { row ->
      def xVal = row[xCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]

      if (!(xVal instanceof Number) && !(xScale instanceof ScaleDiscrete)) return
      if (!(yminVal instanceof Number) || !(ymaxVal instanceof Number)) return

      def xCenter = xScale.transform(xVal)
      def yMinPx = yScale.transform(yminVal)
      def yMaxPx = yScale.transform(ymaxVal)

      if (xCenter == null || yMinPx == null || yMaxPx == null) return

      double xCenterPx = xCenter as double
      double yMin = yMinPx as double
      double yMax = yMaxPx as double

      double halfWidthPx
      if (xScale instanceof ScaleDiscrete) {
        double bandwidth = (xScale as ScaleDiscrete).getBandwidth()
        halfWidthPx = bandwidth * (widthValue as double) / 2.0d
      } else {
        double xNum = (xVal as Number).doubleValue()
        double halfWidthData = widthValue / 2.0d
        def xLeft = xScale.transform(xNum - halfWidthData)
        def xRight = xScale.transform(xNum + halfWidthData)
        if (xLeft == null || xRight == null) return
        halfWidthPx = Math.abs((xRight as double) - (xLeft as double)) / 2.0d
      }

      drawLine(group, xCenterPx, yMin, xCenterPx, yMax)
      drawLine(group, xCenterPx - halfWidthPx, yMin, xCenterPx + halfWidthPx, yMin)
      drawLine(group, xCenterPx - halfWidthPx, yMax, xCenterPx + halfWidthPx, yMax)
    }
  }

  private double resolveDefaultWidth(Scale xScale, Matrix data, String xCol) {
    if (xScale instanceof ScaleDiscrete) {
      return 0.9d
    }
    List<Number> values = data[xCol].findAll { it instanceof Number } as List<Number>
    if (values.isEmpty()) {
      return 0.9d
    }
    double resolution = computeResolution(values)
    return resolution > 0.0d ? resolution * 0.9d : 0.9d
  }

  private double computeResolution(List<Number> values) {
    List<Double> sorted = values.collect { it.doubleValue() }.unique().sort()
    if (sorted.size() < 2) {
      return 0.0d
    }
    double minDiff = Double.POSITIVE_INFINITY
    for (int i = 1; i < sorted.size(); i++) {
      double diff = sorted[i] - sorted[i - 1]
      if (diff > 0 && diff < minDiff) {
        minDiff = diff
      }
    }
    return Double.isInfinite(minDiff) ? 0.0d : minDiff
  }

  private void drawLine(G group, double x1, double y1, double x2, double y2) {
    String lineColor = ColorUtil.normalizeColor(color) ?: color
    def line = group.addLine()
        .x1(x1 as int)
        .y1(y1 as int)
        .x2(x2 as int)
        .y2(y2 as int)
        .stroke(lineColor)
    line.addAttribute('stroke-width', linewidth)

    String dashArray = getLineDashArray(linetype)
    if (dashArray) {
      line.addAttribute('stroke-dasharray', dashArray)
    }
    if ((alpha as double) < 1.0d) {
      line.addAttribute('stroke-opacity', alpha)
    }
  }

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
