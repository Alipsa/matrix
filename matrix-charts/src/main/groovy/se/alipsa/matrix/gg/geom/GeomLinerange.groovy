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

/**
 * Line range geometry for displaying vertical line ranges.
 * Shows a vertical line from ymin to ymax at each x position.
 * Unlike geom_errorbar, this does not have horizontal end caps.
 *
 * Required aesthetics: x, ymin, ymax
 * Optional aesthetics: color, linewidth, linetype, alpha
 *
 * Usage:
 * - geom_linerange(aes(ymin: 'lower', ymax: 'upper'))
 * - geom_linerange(color: 'blue', linewidth: 2)
 */
@CompileStatic
class GeomLinerange extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type (solid, dashed, dotted, etc.) */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomLinerange() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomLinerange(Map params) {
    this()
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
    if (data == null || data.rowCount() == 0) return

    String xCol = aes?.xColName ?: 'x'
    String yminCol = params?.get('ymin')?.toString() ?: 'ymin'
    String ymaxCol = params?.get('ymax')?.toString() ?: 'ymax'
    String colorCol = aes.colorColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    data.each { row ->
      def xVal = row[xCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]
      def colorVal = colorCol ? row[colorCol] : null

      if (xVal == null || yminVal == null || ymaxVal == null) return

      def xPx = xScale?.transform(xVal)
      def yMinPx = yScale?.transform(yminVal)
      def yMaxPx = yScale?.transform(ymaxVal)

      if (xPx == null || yMinPx == null || yMaxPx == null) return

      double xCenter = xPx as double
      double yMin = yMinPx as double
      double yMax = yMaxPx as double

      // Determine color
      String lineColor = this.color
      if (colorCol && colorVal != null) {
        if (colorScale) {
          lineColor = colorScale.transform(colorVal)?.toString() ?: this.color
        } else {
          lineColor = getDefaultColor(colorVal)
        }
      } else if (aes.color instanceof Identity) {
        lineColor = (aes.color as Identity).value.toString()
      }
      lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

      // Draw the vertical line
      def line = group.addLine()
          .x1(xCenter as int)
          .y1(yMin as int)
          .x2(xCenter as int)
          .y2(yMax as int)
          .stroke(lineColor)

      line.addAttribute('stroke-width', linewidth)

      // Apply line type
      String dashArray = getDashArray(linetype)
      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }

      if ((alpha as double) < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
    }
  }

  /**
   * Convert line type to SVG stroke-dasharray.
   */
  private String getDashArray(String type) {
    switch (type?.toLowerCase()) {
      case 'dashed': return '8,4'
      case 'dotted': return '2,2'
      case 'dotdash': return '2,2,8,2'
      case 'longdash': return '12,4'
      case 'twodash': return '4,2,8,2'
      case 'solid':
      default: return null
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

    int index = Math.abs(value.hashCode()) % palette.size()
    return palette[index]
  }
}
