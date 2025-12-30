package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
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
    if (params.color) this.color = params.color
    if (params.colour) this.color = params.colour
    if (params.fill) this.fill = params.fill
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
    String sizeCol = aes.size instanceof String ? aes.size : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomPoint requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

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

      // Determine size
      Number pointSize = this.size
      if (sizeCol && row[sizeCol] != null && row[sizeCol] instanceof Number) {
        pointSize = row[sizeCol] as Number
      } else if (aes.size instanceof Identity) {
        pointSize = (aes.size as Identity).value as Number
      }

      // Draw the point
      def circle = group.addCircle()
          .cx(xPx)
          .cy(yPx)
          .r(pointSize)
          .fill(pointColor)
          .stroke(pointColor)

      if (alpha < 1.0) {
        circle.addAttribute('fill-opacity', alpha)
      }
    }
  }

  /**
   * Get a default color from a discrete palette based on a value.
   */
  private String getDefaultColor(Object value) {
    // Default ggplot2-like color palette
    List<String> palette = [
      '#F8766D', '#00BA38', '#619CFF',
      '#F564E3', '#00BFC4', '#B79F00',
      '#DE8C00', '#7CAE00', '#00B4F0',
      '#C77CFF'
    ]

    int index = Math.abs(value.hashCode()) % palette.size()
    return palette[index]
  }
}
