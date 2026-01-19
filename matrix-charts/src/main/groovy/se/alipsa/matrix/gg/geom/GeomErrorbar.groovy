package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Error bar geometry for intervals (ymin to ymax) at each x position.
 * Mirrors ggplot2's geom_errorbar contract as closely as possible.
 */
@CompileStatic
class GeomErrorbar extends Geom {

  /** Bar width as fraction of bandwidth (discrete) or data units (continuous) */
  BigDecimal width = null

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  GeomErrorbar() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomErrorbar(Map params) {
    this()
    this.width = params.width as BigDecimal ?: this.width
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.linewidth = (params.linewidth ?: params.size) as BigDecimal ?: this.linewidth
    this.linetype = params.linetype as String ?: this.linetype
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
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

    BigDecimal defaultWidth = resolveDefaultWidth(xScale, data, xCol)
    BigDecimal widthValue = width ?: defaultWidth

    int elementIndex = 0
    data.each { row ->
      def xVal = row[xCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]

      if (!(xVal instanceof Number) && !(xScale instanceof ScaleDiscrete)) {
        elementIndex++
        return
      }
      if (!(yminVal instanceof Number) || !(ymaxVal instanceof Number)) {
        elementIndex++
        return
      }

      def xCenter = xScale.transform(xVal)
      def yMinPx = yScale.transform(yminVal)
      def yMaxPx = yScale.transform(ymaxVal)

      if (xCenter == null || yMinPx == null || yMaxPx == null) {
        elementIndex++
        return
      }

      BigDecimal xCenterPx = xCenter as BigDecimal
      BigDecimal yMin = yMinPx as BigDecimal
      BigDecimal yMax = yMaxPx as BigDecimal

      BigDecimal halfWidthPx
      if (xScale instanceof ScaleDiscrete) {
        BigDecimal bandwidth = (xScale as ScaleDiscrete).getBandwidth()
        halfWidthPx = bandwidth * widthValue / 2.0
      } else {
        BigDecimal xNum = xVal as BigDecimal
        BigDecimal halfWidthData = widthValue / 2.0
        def xLeft = xScale.transform(xNum - halfWidthData)
        def xRight = xScale.transform(xNum + halfWidthData)
        if (xLeft == null || xRight == null) {
          elementIndex++
          return
        }
        halfWidthPx = ((xRight as BigDecimal) - (xLeft as BigDecimal)).abs() / 2.0
      }

      drawLine(group, xCenterPx, yMin, xCenterPx, yMax, ctx, elementIndex)
      drawLine(group, xCenterPx - halfWidthPx, yMin, xCenterPx + halfWidthPx, yMin, ctx, elementIndex)
      drawLine(group, xCenterPx - halfWidthPx, yMax, xCenterPx + halfWidthPx, yMax, ctx, elementIndex)
      elementIndex++
    }
  }

  private BigDecimal resolveDefaultWidth(Scale xScale, Matrix data, String xCol) {
    if (xScale instanceof ScaleDiscrete) {
      return 0.9
    }
    List<Number> values = data[xCol].findAll { it instanceof Number } as List<Number>
    if (values.isEmpty()) {
      return 0.9
    }
    BigDecimal resolution = computeResolution(values)
    return resolution > 0.0 ? resolution * 0.9 : 0.9
  }

  private BigDecimal computeResolution(List<Number> values) {
    def sorted = values.collect { it as BigDecimal }.unique().sort()
    if (sorted.size() < 2) {
      return 0.0
    }
    BigDecimal minDiff = null
    for (int i = 1; i < sorted.size(); i++) {
      BigDecimal diff = sorted[i] - sorted[i - 1]
      if (diff > 0 && (minDiff == null || diff < minDiff)) {
        minDiff = diff
      }
    }
    return minDiff ?: 0.0
  }

  private void drawLine(G group, BigDecimal x1, BigDecimal y1, BigDecimal x2, BigDecimal y2, RenderContext ctx, int elementIndex) {
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
    if (alpha < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }

    // Apply CSS attributes
    GeomUtils.applyAttributes(line, ctx, 'errorbar', 'gg-errorbar', elementIndex)
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
