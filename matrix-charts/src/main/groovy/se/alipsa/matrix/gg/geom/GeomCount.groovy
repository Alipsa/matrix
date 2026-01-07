package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Count geometry for showing point counts at each location.
 * Useful for dealing with overplotting - shows points sized by count.
 *
 * Usage:
 * - geom_count() - points sized by number of observations
 * - geom_count(color: 'blue') - colored count points
 *
 * The n (count) aesthetic is computed automatically.
 */
@CompileStatic
class GeomCount extends Geom {

  /** Point color */
  String color = 'black'

  /** Point fill color */
  String fill = 'steelblue'

  /** Alpha transparency (0-1) */
  Number alpha = 0.7

  /** Minimum point size (for count=1) */
  Number sizeMin = 3

  /** Maximum point size (for max count) */
  Number sizeMax = 15

  /** Point shape: 'circle', 'square', 'triangle' */
  String shape = 'circle'

  /** Stroke width for point border */
  Number stroke = 1

  GeomCount() {
    defaultStat = StatType.IDENTITY  // We compute counts internally
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomCount(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.sizeMin != null) this.sizeMin = params.sizeMin as Number
    if (params.size_min != null) this.sizeMin = params.size_min as Number
    if (params.sizeMax != null) this.sizeMax = params.sizeMax as Number
    if (params.size_max != null) this.sizeMax = params.size_max as Number
    if (params.shape) this.shape = params.shape as String
    if (params.stroke != null) this.stroke = params.stroke as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 1) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String colorCol = aes.colorColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomCount requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale fillScale = scales['fill']

    // Count occurrences at each unique (x, y) location
    Map<String, CountData> counts = [:]

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal == null || yVal == null) return

      String key = "${xVal}|${yVal}"
      if (!counts.containsKey(key)) {
        counts[key] = new CountData(
            x: xVal,
            y: yVal,
            count: 0,
            color: colorCol ? row[colorCol] : null,
            fill: fillCol ? row[fillCol] : null
        )
      }
      counts[key].count++
    }

    if (counts.isEmpty()) return

    // Find max count for size scaling
    int maxCount = counts.values().max { it.count }.count
    if (maxCount == 0) return

    // Render each unique location as a sized point
    counts.values().each { CountData cd ->
      def xPx = xScale?.transform(cd.x)
      def yPx = yScale?.transform(cd.y)

      if (xPx == null || yPx == null) return

      int cx = xPx as int
      int cy = yPx as int

      // Calculate size based on count
      BigDecimal sizeRange = (sizeMax as BigDecimal) - (sizeMin as BigDecimal)
      BigDecimal size
      if (maxCount == 1) {
        size = (sizeMin as BigDecimal) + sizeRange / 2
      } else {
        size = (sizeMin as BigDecimal) + (cd.count - 1) * sizeRange / (maxCount - 1)
      }

      // Determine colors
      String pointColor = this.color
      String pointFill = this.fill

      if (cd.color != null && colorScale != null) {
        pointColor = colorScale.transform(cd.color)?.toString() ?: this.color
      }

      if (cd.fill != null && fillScale != null) {
        pointFill = fillScale.transform(cd.fill)?.toString() ?: this.fill
      }
      pointColor = ColorUtil.normalizeColor(pointColor) ?: pointColor
      pointFill = ColorUtil.normalizeColor(pointFill) ?: pointFill

      // Draw the point
      drawPoint(group, cx, cy, size, pointFill, pointColor)
    }
  }

  /**
   * Draw a point of the specified shape.
   */
  private void drawPoint(G group, int cx, int cy, Number size, String fillColor, String strokeColor) {
    double halfSize = size / 2

    switch (shape?.toLowerCase()) {
      case 'square':
        def rect = group.addRect()
            .x((cx - halfSize) as int)
            .y((cy - halfSize) as int)
            .width(size as int)
            .height(size as int)
            .fill(fillColor)
            .stroke(strokeColor)
        rect.addAttribute('stroke-width', stroke)
        if ((alpha as double) < 1.0) {
          rect.addAttribute('fill-opacity', alpha)
        }
        break

      case 'triangle':
        // Equilateral triangle pointing up - use path instead of polygon
        double h = size * Math.sqrt(3) / 2
        double topY = cy - h * 2 / 3
        double bottomY = cy + h / 3
        double leftX = cx - halfSize
        double rightX = cx + halfSize
        String pathD = "M ${cx} ${topY as int} L ${leftX as int} ${bottomY as int} L ${rightX as int} ${bottomY as int} Z"
        def path = group.addPath().d(pathD)
            .fill(fillColor)
            .stroke(strokeColor)
        path.addAttribute('stroke-width', stroke)
        if ((alpha as double) < 1.0) {
          path.addAttribute('fill-opacity', alpha)
        }
        break

      case 'circle':
      default:
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(halfSize as int)
            .fill(fillColor)
            .stroke(strokeColor)
        circle.addAttribute('stroke-width', stroke)
        if ((alpha as double) < 1.0) {
          circle.addAttribute('fill-opacity', alpha)
        }
        break
    }
  }

  /**
   * Helper class to hold count data.
   */
  @CompileStatic
  private static class CountData {
    Object x
    Object y
    int count
    Object color
    Object fill
  }
}
