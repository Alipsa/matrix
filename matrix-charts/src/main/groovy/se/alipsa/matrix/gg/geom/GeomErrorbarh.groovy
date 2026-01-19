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
 * Horizontal error bar geometry for intervals (xmin to xmax) at each y position.
 * Useful for displaying horizontal confidence intervals or ranges.
 *
 * Required aesthetics: y, xmin, xmax
 * Optional aesthetics: color, size/linewidth, linetype, alpha, height
 */
@CompileStatic
class GeomErrorbarh extends Geom {

  /** Bar height as fraction of bandwidth (discrete) or data units (continuous) */
  Number height = null

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomErrorbarh() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['y', 'xmin', 'xmax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomErrorbarh(Map params) {
    this()
    if (params.height != null) this.height = params.height as Number
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
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() == 0) return

    String yCol = aes?.yColName ?: 'y'
    String xminCol = params?.get('xmin')?.toString() ?: 'xmin'
    String xmaxCol = params?.get('xmax')?.toString() ?: 'xmax'

    if (!data.columnNames().containsAll([yCol, xminCol, xmaxCol])) {
      return
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    double defaultHeight = resolveDefaultHeight(yScale, data, yCol)
    double heightValue = height != null ? (height as double) : defaultHeight

    int elementIndex = 0
    data.each { row ->
      def yVal = row[yCol]
      def xminVal = row[xminCol]
      def xmaxVal = row[xmaxCol]

      // For continuous y scale, y value must be numeric
      // For discrete y scale, any value works
      if (yScale instanceof ScaleDiscrete) {
        // Discrete y scale - any value is ok
      } else if (!(yVal instanceof Number)) {
        // Continuous y scale requires numeric value
        elementIndex++
        return
      }
      if (!(xminVal instanceof Number) || !(xmaxVal instanceof Number)) {
        elementIndex++
        return
      }

      def yCenter = yScale.transform(yVal)
      def xMinPx = xScale.transform(xminVal)
      def xMaxPx = xScale.transform(xmaxVal)

      if (yCenter == null || xMinPx == null || xMaxPx == null) {
        elementIndex++
        return
      }

      double yCenterPx = yCenter as double
      double xMin = xMinPx as double
      double xMax = xMaxPx as double

      double halfHeightPx
      if (yScale instanceof ScaleDiscrete) {
        double bandwidth = (yScale as ScaleDiscrete).getBandwidth()
        halfHeightPx = bandwidth * (heightValue as double) / 2.0d
      } else {
        double yNum = yVal as double
        double halfHeightData = heightValue / 2.0d
        def yBottom = yScale.transform(yNum - halfHeightData)
        def yTop = yScale.transform(yNum + halfHeightData)
        if (yBottom == null || yTop == null) {
          elementIndex++
          return
        }
        halfHeightPx = ((yTop as BigDecimal) - (yBottom as BigDecimal)).abs() / 2
      }

      // Draw horizontal error bar: horizontal line and vertical end caps
      drawLine(group, xMin, yCenterPx, xMax, yCenterPx, ctx, elementIndex)  // Main horizontal line
      drawLine(group, xMin, yCenterPx - halfHeightPx, xMin, yCenterPx + halfHeightPx, ctx, elementIndex)  // Left cap
      drawLine(group, xMax, yCenterPx - halfHeightPx, xMax, yCenterPx + halfHeightPx, ctx, elementIndex)  // Right cap
      elementIndex++
    }
  }

  private double resolveDefaultHeight(Scale yScale, Matrix data, String yCol) {
    if (yScale instanceof ScaleDiscrete) {
      return 0.9d
    }
    List<Number> values = data[yCol].findAll { it instanceof Number } as List<Number>
    if (values.isEmpty()) {
      return 0.9d
    }
    double resolution = computeResolution(values)
    return resolution > 0.0d ? resolution * 0.9d : 0.9d
  }

  private double computeResolution(List<Number> values) {
    List<Double> sorted = values.collect { it as double }.unique().sort()
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

  private void drawLine(G group, double x1, double y1, double x2, double y2, RenderContext ctx, int elementIndex) {
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

    // Apply CSS attributes
    GeomUtils.applyAttributes(line, ctx, 'errorbarh', 'gg-errorbarh', elementIndex)
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
