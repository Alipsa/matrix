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
 * Rectangle geometry for drawing rectangles defined by corner coordinates.
 * Unlike geom_tile which is centered, geom_rect uses explicit corner positions.
 *
 * Required aesthetics: xmin, xmax, ymin, ymax
 * Optional aesthetics: fill, color, alpha, linewidth, linetype
 *
 * Usage:
 * - geom_rect(aes(xmin: 'x1', xmax: 'x2', ymin: 'y1', ymax: 'y2'))
 * - geom_rect(fill: 'blue', alpha: 0.3)
 */
@CompileStatic
class GeomRect extends Geom {

  /** Fill color for rectangles */
  String fill = 'gray'

  /** Stroke color for rectangle borders */
  String color = 'black'

  /** Line width for borders */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Line type for border */
  String linetype = 'solid'

  GeomRect() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['xmin', 'xmax', 'ymin', 'ymax']
    defaultAes = [fill: 'gray', color: 'black', alpha: 1.0] as Map<String, Object>
  }

  GeomRect(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xminCol = params?.get('xmin')?.toString() ?: 'xmin'
    String xmaxCol = params?.get('xmax')?.toString() ?: 'xmax'
    String yminCol = params?.get('ymin')?.toString() ?: 'ymin'
    String ymaxCol = params?.get('ymax')?.toString() ?: 'ymax'
    String fillCol = aes.fillColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    data.each { row ->
      def xminVal = row[xminCol]
      def xmaxVal = row[xmaxCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]
      def fillVal = fillCol ? row[fillCol] : null

      if (xminVal == null || xmaxVal == null || yminVal == null || ymaxVal == null) return

      // Transform coordinates
      def xminPx = xScale?.transform(xminVal)
      def xmaxPx = xScale?.transform(xmaxVal)
      def yminPx = yScale?.transform(yminVal)
      def ymaxPx = yScale?.transform(ymaxVal)

      if (xminPx == null || xmaxPx == null || yminPx == null || ymaxPx == null) return

      // Calculate rectangle bounds (note: in SVG, y increases downward)
      double x = Math.min(xminPx as double, xmaxPx as double)
      double y = Math.min(yminPx as double, ymaxPx as double)
      double w = Math.abs((xmaxPx as double) - (xminPx as double))
      double h = Math.abs((ymaxPx as double) - (yminPx as double))

      // Determine fill color
      String rectFill = this.fill
      if (fillCol && fillVal != null) {
        if (fillScale) {
          rectFill = fillScale.transform(fillVal)?.toString() ?: this.fill
        } else {
          rectFill = getDefaultColor(fillVal)
        }
      } else if (aes.fill instanceof Identity) {
        rectFill = (aes.fill as Identity).value.toString()
      }
      rectFill = ColorUtil.normalizeColor(rectFill) ?: rectFill

      // Draw the rectangle
      def rect = group.addRect()
          .x(x as int)
          .y(y as int)
          .width(w as int)
          .height(h as int)
          .fill(rectFill)

      // Apply alpha
      if ((alpha as double) < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Apply stroke
      if (color != null && (linewidth as double) > 0) {
        String strokeColor = ColorUtil.normalizeColor(color) ?: color
        rect.stroke(strokeColor)
        rect.addAttribute('stroke-width', linewidth)

        String dashArray = getDashArray(linetype)
        if (dashArray) {
          rect.addAttribute('stroke-dasharray', dashArray)
        }
      } else {
        rect.stroke('none')
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
