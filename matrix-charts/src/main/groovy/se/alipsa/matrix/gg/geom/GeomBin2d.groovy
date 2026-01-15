package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

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
  BigDecimal linewidth = 0.5

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

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
    this.bins = params.bins as Integer ?: this.bins
    this.binwidth = params.binwidth as List<Number> ?: this.binwidth
    this.fill = params.fill as String ?: this.fill
    this.color = (params.color ?: params.colour) as String ?: this.color
    this.linewidth = params.linewidth as BigDecimal ?: this.linewidth
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    if (params.drop != null) this.drop = params.drop as boolean
    this.fillColors = (params.fillColors ?: params.fill_colors) as List<String> ?: this.fillColors
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    this.fillColors = this.fillColors.collect { ColorUtil.normalizeColor(it) }
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
    List<BigDecimal[]> points = []
    BigDecimal xMin = null, xMax = null
    BigDecimal yMin = null, yMax = null

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal instanceof Number && yVal instanceof Number) {
        BigDecimal x = xVal as BigDecimal
        BigDecimal y = yVal as BigDecimal
        points << ([x, y] as BigDecimal[])

        xMin = xMin == null ? x : xMin.min(x)
        xMax = xMax == null ? x : xMax.max(x)
        yMin = yMin == null ? y : yMin.min(y)
        yMax = yMax == null ? y : yMax.max(y)
      }
    }

    if (points.isEmpty()) return

    // Compute bin sizes
    BigDecimal xRange = xMax - xMin
    BigDecimal yRange = yMax - yMin

    // Prevent zero range
    if (xRange == 0) xRange = 1
    if (yRange == 0) yRange = 1

    BigDecimal binWidthX, binWidthY
    int nBinsX, nBinsY

    if (binwidth != null && binwidth.size() >= 2) {
      binWidthX = binwidth[0] as BigDecimal
      binWidthY = binwidth[1] as BigDecimal
      nBinsX = (xRange / binWidthX).ceil() as int
      nBinsY = (yRange / binWidthY).ceil() as int
    } else {
      nBinsX = bins
      nBinsY = bins
      binWidthX = xRange / nBinsX
      binWidthY = yRange / nBinsY
    }

    // Count points in each bin
    int[][] counts = new int[nBinsY][nBinsX]
    int maxCount = 0

    for (BigDecimal[] point : points) {
      int binX = ((((point[0] - xMin) / binWidthX) as int).min(nBinsX - 1)) as int
      int binY = ((((point[1] - yMin) / binWidthY) as int).min(nBinsY - 1)) as int

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
        BigDecimal x0 = xMin + i * binWidthX
        BigDecimal x1 = x0 + binWidthX
        BigDecimal y0 = yMin + j * binWidthY
        BigDecimal y1 = y0 + binWidthY

        // Transform to pixel coordinates
        BigDecimal x0Px = xScale?.transform(x0) as BigDecimal
        BigDecimal x1Px = xScale?.transform(x1) as BigDecimal
        BigDecimal y0Px = yScale?.transform(y0) as BigDecimal
        BigDecimal y1Px = yScale?.transform(y1) as BigDecimal

        if (x0Px == null || x1Px == null || y0Px == null || y1Px == null) continue

        BigDecimal left = x0Px.min(x1Px)
        BigDecimal right = x0Px.max(x1Px)
        BigDecimal top = y0Px.min(y1Px)
        BigDecimal bottom = y0Px.max(y1Px)

        BigDecimal width = 1.max(right - left)
        BigDecimal height = 1.max(bottom - top)

        // Get fill color based on count
        String binFill
        if (fillScale != null) {
          binFill = fillScale.transform(count)?.toString() ?: getFillColor(count, maxCount)
        } else {
          binFill = getFillColor(count, maxCount)
        }
        binFill = ColorUtil.normalizeColor(binFill)

        def rect = group.addRect()
            .x(left)
            .y(top)
            .width(width)
            .height(height)
            .fill(binFill)

        if (color != null && linewidth > 0) {
          rect.stroke(ColorUtil.normalizeColor(color))
          rect.addAttribute('stroke-width', linewidth)
        } else {
          rect.stroke('none')
        }

        if (alpha < 1.0) {
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

    BigDecimal ratio = (count / maxCount) as BigDecimal
    int colorIdx = (ratio * (fillColors.size() - 1)) as int
    colorIdx = (0.max(colorIdx.min(fillColors.size() - 1))) as int
    return fillColors[colorIdx]
  }
}
