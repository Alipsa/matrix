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
 * Point range geometry for displaying a point with a vertical line range.
 * Commonly used for showing means with confidence intervals or error bars.
 *
 * Required aesthetics: x, y, ymin, ymax
 * Optional aesthetics: color, size, linewidth, alpha, shape
 *
 * Usage:
 * - geom_pointrange(aes(ymin: 'lower', ymax: 'upper'))
 * - geom_pointrange(color: 'blue', size: 3)
 */
@CompileStatic
class GeomPointrange extends Geom {

  /** Point and line color */
  String color = 'black'

  /** Point size (diameter) */
  BigDecimal size = 4

  /** Line width for the range line */
  BigDecimal linewidth = 1

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Point shape */
  BigDecimal shape = 19  // filled circle

  /** Fill color for the point (for fillable shapes) */
  String fill = null

  GeomPointrange() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'ymin', 'ymax']
    defaultAes = [color: 'black', size: 4, linewidth: 1] as Map<String, Object>
  }

  GeomPointrange(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.shape != null) this.shape = params.shape as BigDecimal
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
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
    String colorCol = aes.colorColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    int elementIndex = 0
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]
      def colorVal = colorCol ? row[colorCol] : null

      if (xVal == null || yVal == null) {
        elementIndex++
        return
      }
      if (yminVal == null || ymaxVal == null) {
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

      // Determine color
      String pointColor = this.color
      if (colorCol && colorVal != null) {
        if (colorScale) {
          pointColor = colorScale.transform(colorVal)?.toString() ?: this.color
        } else {
          pointColor = getDefaultColor(colorVal)
        }
      } else if (aes.color instanceof Identity) {
        pointColor = (aes.color as Identity).value.toString()
      }
      pointColor = ColorUtil.normalizeColor(pointColor) ?: pointColor

      // Draw the vertical line (range)
      def line = group.addLine()
          .x1(xCenter)
          .y1(yMin)
          .x2(xCenter)
          .y2(yMax)
          .stroke(pointColor)

      line.addAttribute('stroke-width', linewidth)

      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }

      // Apply CSS attributes to line
      GeomUtils.applyAttributes(line, ctx, 'pointrange', 'gg-pointrange', elementIndex)

      // Draw the point
      BigDecimal radius = size / 2
      String pointFill = fill ?: pointColor

      def circle = group.addCircle()
          .cx(xCenter)
          .cy(yCenter)
          .r(radius)
          .fill(pointFill)
          .stroke(pointColor)

      circle.addAttribute('stroke-width', 1)

      if (alpha < 1.0) {
        circle.addAttribute('fill-opacity', alpha)
        circle.addAttribute('stroke-opacity', alpha)
      }

      // Apply CSS attributes to circle
      GeomUtils.applyAttributes(circle, ctx, 'pointrange', 'gg-pointrange', elementIndex)

      elementIndex++
    }
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
