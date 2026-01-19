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
  BigDecimal alpha = 0.7

  /** Minimum point size (for count=1) */
  BigDecimal sizeMin = 3

  /** Maximum point size (for max count) */
  BigDecimal sizeMax = 15

  /** Point shape: 'circle', 'square', 'triangle' */
  String shape = 'circle'

  /** Stroke width for point border */
  BigDecimal stroke = 1

  GeomCount() {
    defaultStat = StatType.IDENTITY  // We compute counts internally
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomCount(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.sizeMin = (params.sizeMin ?: params.size_min) as BigDecimal ?: this.sizeMin
    this.sizeMax = (params.sizeMax ?: params.size_max) as BigDecimal ?: this.sizeMax
    this.shape = params.shape as String ?: this.shape
    this.stroke = params.stroke as BigDecimal ?: this.stroke
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
    Map<String, CountData> counts = data.rows()
        .findAll { row -> row[xCol] != null && row[yCol] != null }
        .groupBy { row -> "${row[xCol]}|${row[yCol]}" }
        .collectEntries { key, rows ->
          def firstRow = rows[0]
          [(key): new CountData(
              x: firstRow[xCol],
              y: firstRow[yCol],
              count: rows.size(),
              color: colorCol ? firstRow[colorCol] : null,
              fill: fillCol ? firstRow[fillCol] : null
          )]
        } as Map<String, CountData>

    if (counts.isEmpty()) return

    // Find max count for size scaling
    int maxCount = counts.values().max { it.count }.count
    if (maxCount == 0) return

    // Render each unique location as a sized point
    counts.values().each { CountData cd ->
      BigDecimal xPx = xScale?.transform(cd.x) as BigDecimal
      BigDecimal yPx = yScale?.transform(cd.y) as BigDecimal

      if (xPx == null || yPx == null) return

      // Calculate size based on count
      BigDecimal sizeRange = sizeMax - sizeMin
      BigDecimal size
      if (maxCount == 1) {
        size = sizeMin + sizeRange / 2
      } else {
        size = sizeMin + (cd.count - 1) * sizeRange / (maxCount - 1)
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
      drawPoint(group, xPx, yPx, size, pointFill, pointColor)
    }
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
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
    Map<String, CountData> counts = data.rows()
        .findAll { row -> row[xCol] != null && row[yCol] != null }
        .groupBy { row -> "${row[xCol]}|${row[yCol]}" }
        .collectEntries { key, rows ->
          def firstRow = rows[0]
          [(key): new CountData(
              x: firstRow[xCol],
              y: firstRow[yCol],
              count: rows.size(),
              color: colorCol ? firstRow[colorCol] : null,
              fill: fillCol ? firstRow[fillCol] : null
          )]
        } as Map<String, CountData>

    if (counts.isEmpty()) return

    // Find max count for size scaling
    int maxCount = counts.values().max { it.count }.count
    if (maxCount == 0) return

    int elementIndex = 0
    // Render each unique location as a sized point
    counts.values().each { CountData cd ->
      BigDecimal xPx = xScale?.transform(cd.x) as BigDecimal
      BigDecimal yPx = yScale?.transform(cd.y) as BigDecimal

      if (xPx == null || yPx == null) {
        elementIndex++
        return
      }

      // Calculate size based on count
      BigDecimal sizeRange = sizeMax - sizeMin
      BigDecimal size
      if (maxCount == 1) {
        size = sizeMin + sizeRange / 2
      } else {
        size = sizeMin + (cd.count - 1) * sizeRange / (maxCount - 1)
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
      def pointElement = drawPointWithContext(group, xPx, yPx, size, pointFill, pointColor)

      // Apply CSS attributes
      GeomUtils.applyAttributes(pointElement, ctx, 'count', 'gg-count', elementIndex)
      elementIndex++
    }
  }

  /**
   * Draw a point of the specified shape.
   */
  private void drawPoint(G group, BigDecimal cx, BigDecimal cy, BigDecimal size, String fillColor, String strokeColor) {
    BigDecimal halfSize = size / 2

    switch (shape?.toLowerCase()) {
      case 'square':
        def rect = group.addRect()
            .x(cx - halfSize)
            .y(cy - halfSize)
            .width(size)
            .height(size)
            .fill(fillColor)
            .stroke(strokeColor)
        rect.addAttribute('stroke-width', stroke)
        if (alpha < 1.0) {
          rect.addAttribute('fill-opacity', alpha)
        }
        break

      case 'triangle':
        // Equilateral triangle pointing up - use path instead of polygon
        BigDecimal h = size * 3.0.sqrt() / 2
        BigDecimal topY = cy - h * 2 / 3
        BigDecimal bottomY = cy + h / 3
        BigDecimal leftX = cx - halfSize
        BigDecimal rightX = cx + halfSize
        String pathD = "M ${cx} ${topY} L ${leftX} ${bottomY} L ${rightX} ${bottomY} Z"
        def path = group.addPath().d(pathD)
            .fill(fillColor)
            .stroke(strokeColor)
        path.addAttribute('stroke-width', stroke)
        if (alpha < 1.0) {
          path.addAttribute('fill-opacity', alpha)
        }
        break

      case 'circle':
      default:
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(halfSize)
            .fill(fillColor)
            .stroke(strokeColor)
        circle.addAttribute('stroke-width', stroke)
        if (alpha < 1.0) {
          circle.addAttribute('fill-opacity', alpha)
        }
        break
    }
  }

  /**
   * Draw a point of the specified shape and return it for CSS attribute application.
   */
  private se.alipsa.groovy.svg.SvgElement drawPointWithContext(G group, BigDecimal cx, BigDecimal cy, BigDecimal size, String fillColor, String strokeColor) {
    BigDecimal halfSize = size / 2

    switch (shape?.toLowerCase()) {
      case 'square':
        def rect = group.addRect()
            .x(cx - halfSize)
            .y(cy - halfSize)
            .width(size)
            .height(size)
            .fill(fillColor)
            .stroke(strokeColor)
        rect.addAttribute('stroke-width', stroke)
        if (alpha < 1.0) {
          rect.addAttribute('fill-opacity', alpha)
        }
        return rect

      case 'triangle':
        // Equilateral triangle pointing up - use path instead of polygon
        BigDecimal h = size * 3.0.sqrt() / 2
        BigDecimal topY = cy - h * 2 / 3
        BigDecimal bottomY = cy + h / 3
        BigDecimal leftX = cx - halfSize
        BigDecimal rightX = cx + halfSize
        String pathD = "M ${cx} ${topY} L ${leftX} ${bottomY} L ${rightX} ${bottomY} Z"
        def path = group.addPath().d(pathD)
            .fill(fillColor)
            .stroke(strokeColor)
        path.addAttribute('stroke-width', stroke)
        if (alpha < 1.0) {
          path.addAttribute('fill-opacity', alpha)
        }
        return path

      case 'circle':
      default:
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(halfSize)
            .fill(fillColor)
            .stroke(strokeColor)
        circle.addAttribute('stroke-width', stroke)
        if (alpha < 1.0) {
          circle.addAttribute('fill-opacity', alpha)
        }
        return circle
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
