package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.scale.Scale

/**
 * Point geometry for scatter plots.
 * Renders data points as circles.
 */
@CompileStatic
class GeomPoint extends Geom {

  /** Default point color */
  String color = 'black'

  /** Default point fill */
  String fill = 'black'

  /** Default point size (radius) */
  Number size = 3

  /** Default point shape */
  String shape = 'circle'

  /** Default alpha (transparency) */
  Number alpha = 1.0

  GeomPoint() {
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 3, alpha: 1.0] as Map<String, Object>
  }

  GeomPoint(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.size) this.size = params.size as Number
    if (params.shape) this.shape = params.shape
    if (params.alpha) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String shapeCol = aes.shape instanceof String ? aes.shape as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomPoint requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale shapeScale = scales['shape']
    Scale alphaScale = scales['alpha']

    // Render each point
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform to pixel coordinates using scales
      // Scales handle both continuous (numeric) and discrete (string) values
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      // Skip if scale couldn't transform the value
      if (xTransformed == null || yTransformed == null) return

      double xPx = xTransformed as double
      double yPx = yTransformed as double

      // Determine color
      String pointColor = this.color
      if (colorCol && row[colorCol] != null) {
        if (colorScale) {
          pointColor = colorScale.transform(row[colorCol])?.toString() ?: this.color
        } else {
          // Use default color palette
          pointColor = getDefaultColor(row[colorCol])
        }
      } else if (aes.color instanceof Identity) {
        pointColor = (aes.color as Identity).value.toString()
      }
      pointColor = ColorUtil.normalizeColor(pointColor) ?: pointColor

      // Determine size
      Number pointSize = this.size
      if (sizeCol && row[sizeCol] != null) {
        if (sizeScale) {
          def scaled = sizeScale.transform(row[sizeCol])
          if (scaled instanceof Number) {
            pointSize = scaled as Number
          }
        } else if (row[sizeCol] instanceof Number) {
          pointSize = row[sizeCol] as Number
        }
      } else if (aes.size instanceof Identity) {
        pointSize = (aes.size as Identity).value as Number
      }

      // Determine shape
      String pointShape = this.shape
      if (shapeCol && row[shapeCol] != null) {
        pointShape = shapeScale?.transform(row[shapeCol])?.toString() ?: row[shapeCol].toString()
      } else if (aes.shape instanceof Identity) {
        pointShape = (aes.shape as Identity).value.toString()
      }

      // Determine alpha
      Number pointAlpha = this.alpha
      if (aes.alpha instanceof Identity) {
        pointAlpha = (aes.alpha as Identity).value as Number
      } else if (alphaCol && row[alphaCol] != null) {
        if (alphaScale) {
          def scaledAlpha = alphaScale.transform(row[alphaCol])
          if (scaledAlpha instanceof Number) {
            pointAlpha = scaledAlpha as Number
          }
        } else if (row[alphaCol] instanceof Number) {
          pointAlpha = row[alphaCol] as Number
        }
      }

      // Draw the point
      drawPoint(group, xPx, yPx, (pointSize as Number).doubleValue(), pointColor, pointShape,
          (pointAlpha as Number).doubleValue())
    }
  }

  /**
   * Draw a point of the specified shape.
   */
  private void drawPoint(G group, double cx, double cy, double radius, String color, String shape, double alphaVal) {
    double size = radius * 2
    double halfSize = size / 2.0d

    switch (shape?.toLowerCase()) {
      case 'square':
        def rect = group.addRect(size as int, size as int)
            .x((cx - halfSize) as int)
            .y((cy - halfSize) as int)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0) {
          rect.addAttribute('fill-opacity', alphaVal)
        }
        break
      case 'plus':
      case 'cross':
        def hLine = group.addLine((cx - halfSize) as int, cy as int, (cx + halfSize) as int, cy as int)
            .stroke(color)
        def vLine = group.addLine(cx as int, (cy - halfSize) as int, cx as int, (cy + halfSize) as int)
            .stroke(color)
        if (alphaVal < 1.0) {
          hLine.addAttribute('stroke-opacity', alphaVal)
          vLine.addAttribute('stroke-opacity', alphaVal)
        }
        break
      case 'x':
        def diag1 = group.addLine((cx - halfSize) as int, (cy - halfSize) as int, (cx + halfSize) as int, (cy + halfSize) as int)
            .stroke(color)
        def diag2 = group.addLine((cx - halfSize) as int, (cy + halfSize) as int, (cx + halfSize) as int, (cy - halfSize) as int)
            .stroke(color)
        if (alphaVal < 1.0) {
          diag1.addAttribute('stroke-opacity', alphaVal)
          diag2.addAttribute('stroke-opacity', alphaVal)
        }
        break
      case 'triangle':
        double h = size * Math.sqrt(3) / 2
        double topY = cy - h * 2 / 3
        double bottomY = cy + h / 3
        double leftX = cx - halfSize
        double rightX = cx + halfSize
        String pathD = "M ${cx} ${topY as int} L ${leftX as int} ${bottomY as int} L ${rightX as int} ${bottomY as int} Z"
        def path = group.addPath().d(pathD)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0) {
          path.addAttribute('fill-opacity', alphaVal)
        }
        break
      case 'diamond':
        String diamond = "M ${cx} ${(cy - halfSize) as int} " +
            "L ${(cx + halfSize) as int} ${cy} " +
            "L ${cx} ${(cy + halfSize) as int} " +
            "L ${(cx - halfSize) as int} ${cy} Z"
        def diamondPath = group.addPath().d(diamond)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0) {
          diamondPath.addAttribute('fill-opacity', alphaVal)
        }
        break
      case 'circle':
      default:
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(radius)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0) {
          circle.addAttribute('fill-opacity', alphaVal)
        }
        break
    }
  }

  /**
   * Get a default color from a discrete palette based on a value.
   */
  private String getDefaultColor(Object value) {
    // Default ggplot2-like color palette
    List<String> palette = [
      '#F8766D', '#C49A00', '#53B400',
      '#00C094', '#00B6EB', '#A58AFF',
      '#FB61D7'
    ]

    int index = Math.abs(value.hashCode()) % palette.size()
    return palette[index]
  }
}
