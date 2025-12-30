package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Histogram geometry for visualizing the distribution of continuous data.
 * Uses stat_bin by default to bin continuous data into intervals.
 *
 * After stat_bin transformation, data has columns:
 * - x: bin center
 * - xmin: bin left edge
 * - xmax: bin right edge
 * - count: number of observations in bin
 * - density: count / (n * binwidth)
 */
@CompileStatic
class GeomHistogram extends Geom {

  /** Bar fill color */
  String fill = '#595959'

  /** Bar outline color */
  String color = 'white'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Outline width */
  Number linewidth = 0.5

  /** Number of bins (default 30, overridden if binwidth is set) */
  Integer bins = 30

  /** Width of each bin (if set, overrides bins parameter) */
  Number binwidth = null

  GeomHistogram() {
    defaultStat = StatType.BIN
    requiredAes = ['x']
    defaultAes = [fill: '#595959', color: 'white', alpha: 1.0] as Map<String, Object>
  }

  GeomHistogram(Map params) {
    this()
    if (params.fill) this.fill = params.fill
    if (params.color) this.color = params.color
    if (params.colour) this.color = params.colour
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.linewidth) this.linewidth = params.linewidth as Number
    if (params.bins) this.bins = params.bins as Integer
    if (params.binwidth) this.binwidth = params.binwidth as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    // After stat_bin, data has: x (center), xmin, xmax, count, density
    if (!data.columnNames().contains('xmin') || !data.columnNames().contains('xmax')) {
      throw new IllegalArgumentException("GeomHistogram requires stat_bin transformation (xmin, xmax columns)")
    }

    String fillCol = aes.fillColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Render each bin as a rectangle
    data.each { row ->
      def xmin = row['xmin']
      def xmax = row['xmax']
      def count = row['count']

      if (xmin == null || xmax == null || count == null) return

      // Transform coordinates
      def xLeft = xScale?.transform(xmin)
      def xRight = xScale?.transform(xmax)
      def yTop = yScale?.transform(count)
      def yBottom = yScale?.transform(0)

      if (xLeft == null || xRight == null || yTop == null || yBottom == null) return

      double xPx = xLeft as double
      double widthPx = (xRight as double) - (xLeft as double)
      double yPx = yTop as double
      double heightPx = Math.abs((yBottom as double) - (yTop as double))

      // Skip empty bins
      if (heightPx <= 0) return

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

      // Draw histogram bar
      def rect = group.addRect(widthPx, heightPx)
          .x(xPx)
          .y(yPx)
          .fill(barFill)

      // Add stroke
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
   * Get a default color from a discrete palette based on a value.
   */
  private String getDefaultColor(Object value) {
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
