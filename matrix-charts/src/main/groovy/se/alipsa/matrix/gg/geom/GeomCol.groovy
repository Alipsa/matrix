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
 * Column geometry for bar charts with pre-computed heights.
 * Uses stat_identity by default - expects y values to be provided directly.
 * For automatic counting, use GeomBar instead.
 *
 * Difference from GeomBar:
 * - GeomBar: stat = COUNT, only requires x aesthetic, counts occurrences
 * - GeomCol: stat = IDENTITY, requires both x and y aesthetics
 */
@CompileStatic
class GeomCol extends GeomBar {

  GeomCol() {
    super()
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
  }

  GeomCol(Map params) {
    super(params)
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = data.columnNames().contains('count') ? 'count' : aes.yColName
    String fillCol = aes.fillColName

    if (xCol == null) {
      throw new IllegalArgumentException("GeomCol requires x aesthetic")
    }
    if (yCol == null) {
      throw new IllegalArgumentException("GeomCol requires y aesthetic")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    BigDecimal barWidth = calculateBarWidth(xScale)

    int elementIndex = 0
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) {
        elementIndex++
        return
      }

      BigDecimal xCenter = xScale?.transform(xVal) as BigDecimal
      BigDecimal yTop
      BigDecimal yBottom
      if (data.columnNames().contains('ymin') && data.columnNames().contains('ymax')) {
        Number yMinVal = row['ymin'] as Number
        Number yMaxVal = row['ymax'] as Number
        yTop = yScale?.transform(yMaxVal) as BigDecimal
        yBottom = yScale?.transform(yMinVal) as BigDecimal
      } else {
        yTop = yScale?.transform(yVal) as BigDecimal
        yBottom = yScale?.transform(0) as BigDecimal
      }

      if (xCenter == null || yTop == null || yBottom == null) {
        elementIndex++
        return
      }

      BigDecimal xPx = xCenter - barWidth / 2
      BigDecimal yPx = yTop.min(yBottom)
      BigDecimal heightPx = (yBottom - yTop).abs()

      String barFill = this.fill
      if (fillCol && row[fillCol] != null) {
        if (fillScale) {
          barFill = fillScale.transform(row[fillCol])?.toString() ?: this.fill
        } else {
          barFill = getDefaultColor(row[fillCol])
        }
      } else if (aes.fill instanceof Identity) {
        barFill = (aes.fill as Identity).value.toString()
      }
      barFill = ColorUtil.normalizeColor(barFill) ?: barFill

      def rect = group.addRect(barWidth, heightPx)
          .x(xPx)
          .y(yPx)
          .fill(barFill)

      if (color != null) {
        String strokeColor = ColorUtil.normalizeColor(color) ?: color
        rect.stroke(strokeColor)
        rect.addAttribute('stroke-width', linewidth)
      }

      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Apply CSS attributes
      GeomUtils.applyAttributes(rect, ctx, 'col', 'gg-col', elementIndex)
      elementIndex++
    }
  }
}
