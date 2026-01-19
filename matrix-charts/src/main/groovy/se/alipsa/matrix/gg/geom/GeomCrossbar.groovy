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
  BigDecimal linewidth = 1

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Width of the crossbar (as fraction of bandwidth for discrete, or data units for continuous) */
  BigDecimal width = null

  /** Fatten factor for the middle line (multiplier on linewidth) */
  BigDecimal fatten = 2.5

  GeomCrossbar() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomCrossbar(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.width != null) this.width = params.width as BigDecimal
    if (params.fatten != null) this.fatten = params.fatten as BigDecimal
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
    String yCol = aes?.yColName ?: 'y'
    String yminCol = params?.get('ymin')?.toString() ?: 'ymin'
    String ymaxCol = params?.get('ymax')?.toString() ?: 'ymax'
    String fillCol = aes.fillColName
    String colorCol = aes.colorColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']
    Scale colorScale = scales['color']

    BigDecimal defaultWidth = resolveDefaultWidth(xScale, data, xCol)
    BigDecimal widthValue = width ?: defaultWidth

    int elementIndex = 0
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]
      def fillVal = fillCol ? row[fillCol] : null
      def colorVal = colorCol ? row[colorCol] : null

      if (xVal == null || yVal == null || yminVal == null || ymaxVal == null) {
        elementIndex++
        return
      }

      BigDecimal xCenter = xScale?.transform(xVal) as BigDecimal
      BigDecimal yCenter = yScale?.transform(yVal) as BigDecimal
      BigDecimal yMin = yScale?.transform(yminVal) as BigDecimal
      BigDecimal yMax = yScale?.transform(ymaxVal) as BigDecimal

      if (xCenter == null || yCenter == null || yMin == null || yMax == null) {
        elementIndex++
        return
      }

      // Calculate half width in pixels
      BigDecimal halfWidthPx
      if (xScale instanceof ScaleDiscrete) {
        BigDecimal bandwidth = (xScale as ScaleDiscrete).getBandwidth()
        halfWidthPx = bandwidth * widthValue / 2
      } else {
        BigDecimal xNum = xVal as BigDecimal
        BigDecimal halfWidthData = widthValue / 2
        BigDecimal xLeft = xScale.transform(xNum - halfWidthData) as BigDecimal
        BigDecimal xRight = xScale.transform(xNum + halfWidthData) as BigDecimal
        if (xLeft == null || xRight == null) {
          elementIndex++
          return
        }
        halfWidthPx = (xRight - xLeft).abs() / 2
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
      BigDecimal boxX = xCenter - halfWidthPx
      BigDecimal boxY = yMin.min(yMax)
      BigDecimal boxWidth = halfWidthPx * 2
      BigDecimal boxHeight = (yMax - yMin).abs()

      def rect = group.addRect()
          .x(boxX)
          .y(boxY)
          .width(boxWidth)
          .height(boxHeight)
          .stroke(boxStroke)

      rect.addAttribute('stroke-width', linewidth)

      if (boxFill) {
        rect.fill(boxFill)
      } else {
        rect.fill('none')
      }

      if (alpha < 1.0) {
        if (boxFill) rect.addAttribute('fill-opacity', alpha)
        rect.addAttribute('stroke-opacity', alpha)
      }

      // Apply CSS attributes to rectangle
      GeomUtils.applyAttributes(rect, ctx, 'crossbar', 'gg-crossbar', elementIndex)

      // Draw the middle line (at y position, with fattened stroke)
      BigDecimal middleLineWidth = linewidth * fatten
      def line = group.addLine()
          .x1(xCenter - halfWidthPx)
          .y1(yCenter)
          .x2(xCenter + halfWidthPx)
          .y2(yCenter)
          .stroke(boxStroke)

      line.addAttribute('stroke-width', middleLineWidth)

      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }

      // Apply CSS attributes to middle line
      GeomUtils.applyAttributes(line, ctx, 'crossbar', 'gg-crossbar', elementIndex)

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
    return resolution > 0 ? resolution * 0.9 : 0.9
  }

  private BigDecimal computeResolution(List<Number> values) {
    List<BigDecimal> sorted = values.collect { it as BigDecimal }.unique().sort()
    if (sorted.size() < 2) {
      return 0
    }
    BigDecimal minDiff = null
    for (int i = 1; i < sorted.size(); i++) {
      BigDecimal diff = sorted[i] - sorted[i - 1]
      if (diff > 0 && (minDiff == null || diff < minDiff)) {
        minDiff = diff
      }
    }
    return minDiff ?: 0
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

    int index = value.hashCode().abs() % palette.size()
    return palette[index]
  }
}
