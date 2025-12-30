package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * 2D binning geometry for creating heatmaps from point data.
 * Divides the plotting area into a grid and counts observations in each cell.
 *
 * Usage:
 * - geom_bin_2d() - default 30x30 bins
 * - geom_bin_2d(bins: 20) - 20x20 bins
 * - geom_bin_2d(binwidth: [0.5, 0.5]) - specify bin widths
 *
 * Similar to geom_tile() but computes counts automatically.
 */
@CompileStatic
class GeomBin2d extends Geom {

  /** Number of bins in x and y directions */
  int bins = 30

  /** Bin width for x and y (overrides bins if set) */
  List<Number> binwidth

  /** Fill color for bins (used if no fill scale) */
  String fill = 'steelblue'

  /** Border color for bins */
  String color = 'white'

  /** Border width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Whether to drop bins with zero count */
  boolean drop = true

  /** Default fill colors for gradient (low to high count) */
  List<String> fillColors = [
      '#f7fbff', '#deebf7', '#c6dbef', '#9ecae1', '#6baed6',
      '#4292c6', '#2171b5', '#08519c', '#08306b'
  ]

  GeomBin2d() {
    defaultStat = StatType.IDENTITY  // We compute bins internally
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'white'] as Map<String, Object>
  }

  GeomBin2d(Map params) {
    this()
    if (params.bins != null) this.bins = params.bins as int
    if (params.binwidth) this.binwidth = params.binwidth as List<Number>
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.drop != null) this.drop = params.drop as boolean
    if (params.fillColors) this.fillColors = params.fillColors as List<String>
    if (params.fill_colors) this.fillColors = params.fill_colors as List<String>
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 1) return

    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomBin2d requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']

    // Collect numeric x,y values
    List<double[]> points = []
    double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE
    double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal instanceof Number && yVal instanceof Number) {
        double x = xVal as double
        double y = yVal as double
        points << ([x, y] as double[])

        if (x < xMin) xMin = x
        if (x > xMax) xMax = x
        if (y < yMin) yMin = y
        if (y > yMax) yMax = y
      }
    }

    if (points.isEmpty()) return

    // Compute bin sizes
    double xRange = xMax - xMin
    double yRange = yMax - yMin

    // Prevent zero range
    if (xRange == 0) xRange = 1
    if (yRange == 0) yRange = 1

    double binWidthX, binWidthY
    int nBinsX, nBinsY

    if (binwidth != null && binwidth.size() >= 2) {
      binWidthX = binwidth[0] as double
      binWidthY = binwidth[1] as double
      nBinsX = Math.ceil(xRange / binWidthX) as int
      nBinsY = Math.ceil(yRange / binWidthY) as int
    } else {
      nBinsX = bins
      nBinsY = bins
      binWidthX = xRange / nBinsX
      binWidthY = yRange / nBinsY
    }

    // Count points in each bin
    int[][] counts = new int[nBinsY][nBinsX]
    int maxCount = 0

    for (double[] point : points) {
      int binX = Math.min((int) ((point[0] - xMin) / binWidthX), nBinsX - 1)
      int binY = Math.min((int) ((point[1] - yMin) / binWidthY), nBinsY - 1)

      if (binX >= 0 && binX < nBinsX && binY >= 0 && binY < nBinsY) {
        counts[binY][binX]++
        if (counts[binY][binX] > maxCount) {
          maxCount = counts[binY][binX]
        }
      }
    }

    if (maxCount == 0) return

    // Render bins as rectangles
    for (int j = 0; j < nBinsY; j++) {
      for (int i = 0; i < nBinsX; i++) {
        int count = counts[j][i]
        if (drop && count == 0) continue

        // Bin boundaries in data coordinates
        double x0 = xMin + i * binWidthX
        double x1 = x0 + binWidthX
        double y0 = yMin + j * binWidthY
        double y1 = y0 + binWidthY

        // Transform to pixel coordinates
        def x0Px = xScale?.transform(x0)
        def x1Px = xScale?.transform(x1)
        def y0Px = yScale?.transform(y0)
        def y1Px = yScale?.transform(y1)

        if (x0Px == null || x1Px == null || y0Px == null || y1Px == null) continue

        int left = Math.min(x0Px as int, x1Px as int)
        int right = Math.max(x0Px as int, x1Px as int)
        int top = Math.min(y0Px as int, y1Px as int)
        int bottom = Math.max(y0Px as int, y1Px as int)

        int width = Math.max(1, right - left)
        int height = Math.max(1, bottom - top)

        // Get fill color based on count
        String binFill
        if (fillScale != null) {
          binFill = fillScale.transform(count)?.toString() ?: getFillColor(count, maxCount)
        } else {
          binFill = getFillColor(count, maxCount)
        }

        def rect = group.addRect()
            .x(left)
            .y(top)
            .width(width)
            .height(height)
            .fill(binFill)

        if (color != null && (linewidth as double) > 0) {
          rect.stroke(color)
          rect.addAttribute('stroke-width', linewidth)
        } else {
          rect.stroke('none')
        }

        if ((alpha as double) < 1.0) {
          rect.addAttribute('fill-opacity', alpha)
        }
      }
    }
  }

  /**
   * Get fill color based on count value.
   */
  private String getFillColor(int count, int maxCount) {
    if (maxCount == 0 || count == 0) return fillColors[0]

    double ratio = count / (double) maxCount
    int colorIdx = (int) (ratio * (fillColors.size() - 1))
    colorIdx = Math.max(0, Math.min(colorIdx, fillColors.size() - 1))
    return fillColors[colorIdx]
  }
}
