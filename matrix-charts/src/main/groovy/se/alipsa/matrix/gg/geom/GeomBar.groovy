package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

/**
 * Bar geometry for bar charts (counts/frequencies).
 * Uses stat_count by default to count observations in each category.
 * For pre-computed heights, use GeomCol instead.
 */
@CompileStatic
class GeomBar extends Geom {

  /** Bar fill color */
  String fill = '#595959'

  /** Bar outline color */
  String color = null

  /** Bar width as fraction of bandwidth (0-1), null = auto */
  Number width = null

  /** Alpha transparency */
  Number alpha = 1.0

  /** Outline width */
  Number linewidth = 0.5

  GeomBar() {
    defaultStat = StatType.COUNT
    requiredAes = ['x']
    defaultAes = [fill: '#595959', alpha: 1.0] as Map<String, Object>
  }

  GeomBar(Map params) {
    this()
    if (params.fill) this.fill = params.fill
    if (params.color) this.color = params.color
    if (params.colour) this.color = params.colour
    if (params.width) this.width = params.width as Number
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.linewidth) this.linewidth = params.linewidth as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    // After stat_count, y values are in 'count' column
    // For identity stat (GeomCol), use the aes y column
    String yCol = data.columnNames().contains('count') ? 'count' : aes.yColName
    String fillCol = aes.fillColName

    if (xCol == null) {
      throw new IllegalArgumentException("GeomBar requires x aesthetic")
    }
    if (yCol == null) {
      throw new IllegalArgumentException("GeomBar requires y aesthetic or stat_count")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Calculate bar width
    double barWidth = calculateBarWidth(xScale)

    // Render each bar
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform coordinates
      def xCenter = xScale?.transform(xVal)
      def yTop = yScale?.transform(yVal)
      def yBottom = yScale?.transform(0)

      if (xCenter == null || yTop == null || yBottom == null) return

      double xPx = (xCenter as double) - barWidth / 2
      double yPx = yTop as double
      double heightPx = Math.abs((yBottom as double) - (yTop as double))

      // Determine fill color
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

      // Draw bar
      def rect = group.addRect(barWidth, heightPx)
          .x(xPx)
          .y(yPx)
          .fill(barFill)

      // Add stroke if specified
      if (color != null) {
        rect.stroke(color)
        rect.addAttribute('stroke-width', linewidth)
      }

      // Add transparency
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
    }
  }

  /**
   * Calculate bar width based on scale bandwidth.
   */
  protected double calculateBarWidth(Scale xScale) {
    if (width != null) {
      // User specified width as fraction
      if (xScale instanceof ScaleDiscrete) {
        return (xScale as ScaleDiscrete).getBandwidth() * (width as double)
      }
      return 20 * (width as double)  // Fallback for continuous
    }

    // Default: 90% of bandwidth for discrete, 20px for continuous
    if (xScale instanceof ScaleDiscrete) {
      return (xScale as ScaleDiscrete).getBandwidth() * 0.9
    }
    return 20
  }

  /**
   * Get a default color from a discrete palette based on a value.
   */
  protected String getDefaultColor(Object value) {
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
