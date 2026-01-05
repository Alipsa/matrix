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
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Crossbar geometry for displaying a horizontal line with a box around it.
 * Shows a rectangle from ymin to ymax with a horizontal line at y (the middle value).
 * Commonly used for showing means with confidence intervals.
 *
 * Required aesthetics: x, y, ymin, ymax
 * Optional aesthetics: fill, color, linewidth, alpha, width
 *
 * Usage:
 * - geom_crossbar(aes(ymin: 'lower', ymax: 'upper'))
 * - geom_crossbar(fill: 'lightblue', color: 'blue', width: 0.5)
 */
@CompileStatic
class GeomCrossbar extends Geom {

  /** Fill color for the box */
  String fill = null

  /** Stroke color for the box and middle line */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Width of the crossbar (as fraction of bandwidth for discrete, or data units for continuous) */
  Number width = null

  /** Fatten factor for the middle line (multiplier on linewidth) */
  Number fatten = 2.5

  GeomCrossbar() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomCrossbar(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.width != null) this.width = params.width as Number
    if (params.fatten != null) this.fatten = params.fatten as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes?.xColName ?: 'x'
    String yCol = aes?.yColName ?: 'y'
    String yminCol = params?.get('ymin')?.toString() ?: 'ymin'
    String ymaxCol = params?.get('ymax')?.toString() ?: 'ymax'
    String fillCol = aes.fillColName
    String colorCol = aes.colorColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']
    Scale colorScale = scales['color']

    double defaultWidth = resolveDefaultWidth(xScale, data, xCol)
    double widthValue = width != null ? (width as double) : defaultWidth

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]
      def fillVal = fillCol ? row[fillCol] : null
      def colorVal = colorCol ? row[colorCol] : null

      if (xVal == null || yVal == null || yminVal == null || ymaxVal == null) return

      def xPx = xScale?.transform(xVal)
      def yPx = yScale?.transform(yVal)
      def yMinPx = yScale?.transform(yminVal)
      def yMaxPx = yScale?.transform(ymaxVal)

      if (xPx == null || yPx == null || yMinPx == null || yMaxPx == null) return

      double xCenter = xPx as double
      double yCenter = yPx as double
      double yMin = yMinPx as double
      double yMax = yMaxPx as double

      // Calculate half width in pixels
      double halfWidthPx
      if (xScale instanceof ScaleDiscrete) {
        double bandwidth = (xScale as ScaleDiscrete).getBandwidth()
        halfWidthPx = bandwidth * widthValue / 2.0d
      } else {
        double xNum = (xVal as Number).doubleValue()
        double halfWidthData = widthValue / 2.0d
        def xLeft = xScale.transform(xNum - halfWidthData)
        def xRight = xScale.transform(xNum + halfWidthData)
        if (xLeft == null || xRight == null) return
        halfWidthPx = Math.abs((xRight as double) - (xLeft as double)) / 2.0d
      }

      // Determine fill color
      String boxFill = this.fill
      if (fillCol && fillVal != null) {
        if (fillScale) {
          boxFill = fillScale.transform(fillVal)?.toString() ?: this.fill
        } else {
          boxFill = getDefaultColor(fillVal)
        }
      } else if (aes.fill instanceof Identity) {
        boxFill = (aes.fill as Identity).value.toString()
      }
      if (boxFill) boxFill = ColorUtil.normalizeColor(boxFill)

      // Determine stroke color
      String boxStroke = this.color
      if (colorCol && colorVal != null) {
        if (colorScale) {
          boxStroke = colorScale.transform(colorVal)?.toString() ?: this.color
        } else {
          boxStroke = getDefaultColor(colorVal)
        }
      } else if (aes.color instanceof Identity) {
        boxStroke = (aes.color as Identity).value.toString()
      }
      boxStroke = ColorUtil.normalizeColor(boxStroke) ?: boxStroke

      // Draw the box (rectangle from ymin to ymax)
      double boxX = xCenter - halfWidthPx
      double boxY = Math.min(yMin, yMax)
      double boxWidth = halfWidthPx * 2
      double boxHeight = Math.abs(yMax - yMin)

      def rect = group.addRect()
          .x(boxX as int)
          .y(boxY as int)
          .width(boxWidth as int)
          .height(boxHeight as int)
          .stroke(boxStroke)

      rect.addAttribute('stroke-width', linewidth)

      if (boxFill) {
        rect.fill(boxFill)
      } else {
        rect.fill('none')
      }

      if ((alpha as double) < 1.0) {
        if (boxFill) rect.addAttribute('fill-opacity', alpha)
        rect.addAttribute('stroke-opacity', alpha)
      }

      // Draw the middle line (at y position, with fattened stroke)
      double middleLineWidth = (linewidth as double) * (fatten as double)
      def line = group.addLine()
          .x1((xCenter - halfWidthPx) as int)
          .y1(yCenter as int)
          .x2((xCenter + halfWidthPx) as int)
          .y2(yCenter as int)
          .stroke(boxStroke)

      line.addAttribute('stroke-width', middleLineWidth)

      if ((alpha as double) < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
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

  /**
   * Get a default color from a discrete palette.
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
