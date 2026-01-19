package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale

/**
 * Jittered point geometry.
 * Convenience wrapper for points with position_jitter applied.
 */
@CompileStatic
class GeomJitter extends GeomPoint {

  /**
   * Create a jittered point geom with default settings.
   */
  GeomJitter() {
    super()
    defaultPosition = PositionType.JITTER
  }

  /**
   * Create a jittered point geom with parameters.
   *
   * @param params geom parameters
   */
  GeomJitter(Map params) {
    super(params)
    defaultPosition = PositionType.JITTER
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String shapeCol = aes.shape instanceof String ? aes.shape as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomJitter requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale shapeScale = scales['shape']
    Scale alphaScale = scales['alpha']

    // Track element index for CSS IDs
    int elementIndex = 0

    // Render each point
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) {
        elementIndex++
        return
      }

      // Transform to pixel coordinates using scales
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      // Skip if scale couldn't transform the value
      if (xTransformed == null || yTransformed == null) {
        elementIndex++
        return
      }

      BigDecimal xPx = xTransformed as BigDecimal
      BigDecimal yPx = yTransformed as BigDecimal

      // Determine color
      String pointColor = this.color
      if (colorCol && row[colorCol] != null) {
        if (colorScale) {
          pointColor = colorScale.transform(row[colorCol])?.toString() ?: this.color
        } else {
          pointColor = GeomUtils.getDefaultColor(row[colorCol])
        }
      } else if (aes.color instanceof Identity) {
        pointColor = (aes.color as Identity).value.toString()
      }
      pointColor = ColorUtil.normalizeColor(pointColor) ?: pointColor

      // Determine size
      BigDecimal pointSize = GeomUtils.extractPointSize(this.size, aes, sizeCol, row.toMap(), sizeScale)

      // Determine shape
      String pointShape = this.shape
      if (shapeCol && row[shapeCol] != null) {
        pointShape = shapeScale?.transform(row[shapeCol])?.toString() ?: row[shapeCol].toString()
      } else if (aes.shape instanceof Identity) {
        pointShape = (aes.shape as Identity).value.toString()
      }

      // Determine alpha
      BigDecimal pointAlpha = GeomUtils.extractPointAlpha(this.alpha, aes, alphaCol, row.toMap(), alphaScale)

      // Draw the point based on shape
      SvgElement pointElement = drawPointElement(group, xPx, yPx, pointSize, pointColor, pointShape, pointAlpha)

      // Apply CSS attributes if element was created
      if (pointElement != null) {
        GeomUtils.applyAttributes(pointElement, ctx, 'jitter', 'gg-jitter', elementIndex)
      }

      elementIndex++
    }
  }

  /**
   * Draw a point element and return it for CSS attribute application.
   * This is a simplified version of GeomUtils.drawPoint that returns the element.
   */
  private SvgElement drawPointElement(G group, BigDecimal cx, BigDecimal cy, BigDecimal radius,
                                       String color, String shape, BigDecimal alphaVal) {
    BigDecimal size = radius * 2
    BigDecimal halfSize = size / 2

    SvgElement element
    switch (shape?.toLowerCase()) {
      case 'square' -> {
        element = group.addRect(size as int, size as int)
            .x((cx - halfSize) as int)
            .y((cy - halfSize) as int)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          element.addAttribute('fill-opacity', alphaVal)
        }
      }
      case 'plus', 'cross' -> {
        // For compound shapes, wrap in a group
        G pointGroup = group.addG()
        pointGroup.addLine((cx - halfSize) as int, cy as int, (cx + halfSize) as int, cy as int)
            .stroke(color)
            .addAttribute('stroke-opacity', alphaVal < 1.0d ? alphaVal : null)
        pointGroup.addLine(cx as int, (cy - halfSize) as int, cx as int, (cy + halfSize) as int)
            .stroke(color)
            .addAttribute('stroke-opacity', alphaVal < 1.0d ? alphaVal : null)
        element = pointGroup
      }
      case 'x' -> {
        // For compound shapes, wrap in a group
        G pointGroup = group.addG()
        pointGroup.addLine((cx - halfSize) as int, (cy - halfSize) as int, (cx + halfSize) as int, (cy + halfSize) as int)
            .stroke(color)
            .addAttribute('stroke-opacity', alphaVal < 1.0d ? alphaVal : null)
        pointGroup.addLine((cx - halfSize) as int, (cy + halfSize) as int, (cx + halfSize) as int, (cy - halfSize) as int)
            .stroke(color)
            .addAttribute('stroke-opacity', alphaVal < 1.0d ? alphaVal : null)
        element = pointGroup
      }
      case 'triangle' -> {
        double h = size * 3.sqrt() / 2
        double topY = cy - h * 2 / 3
        double bottomY = cy + h / 3
        double leftX = cx - halfSize
        double rightX = cx + halfSize
        String pathD = "M ${cx} ${topY as int} L ${leftX as int} ${bottomY as int} L ${rightX as int} ${bottomY as int} Z"
        element = group.addPath().d(pathD)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          element.addAttribute('fill-opacity', alphaVal)
        }
      }
      case 'diamond' -> {
        String diamond = "M ${cx} ${(cy - halfSize) as int} " +
            "L ${(cx + halfSize) as int} ${cy} " +
            "L ${cx} ${(cy + halfSize) as int} " +
            "L ${(cx - halfSize) as int} ${cy} Z"
        element = group.addPath().d(diamond)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          element.addAttribute('fill-opacity', alphaVal)
        }
      }
      case 'circle' -> {
        element = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(radius)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          element.addAttribute('fill-opacity', alphaVal)
        }
      }
      default -> {
        element = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(radius)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          element.addAttribute('fill-opacity', alphaVal)
        }
      }
    }
    return element
  }
}
