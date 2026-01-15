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
 * Filled 2D density contours geometry.
 * Computes 2D kernel density estimation and renders filled contour regions.
 *
 * Usage:
 * - geom_density_2d_filled() - default filled density contours
 * - geom_density_2d_filled(bins: 10) - specify number of contour levels
 * - geom_density_2d_filled(h: [0.5, 0.5]) - specify bandwidth for KDE
 * - geom_density_2d_filled(n: 100) - number of grid points
 */
@CompileStatic
class GeomDensity2dFilled extends Geom {

  /** Fill colors (will be interpolated based on levels) */
  List<String> fillColors = [
      '#f7fbff', '#deebf7', '#c6dbef', '#9ecae1', '#6baed6',
      '#4292c6', '#2171b5', '#08519c', '#08306b'
  ]

  /** Stroke color for contour borders */
  String color = null

  /** Line width for borders */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Number of contour bins/levels */
  int bins = 10

  /** Grid size for density estimation */
  int n = 100

  /** Bandwidth for KDE [h_x, h_y]. If null, auto-calculated */
  List<Number> h = null

  GeomDensity2dFilled() {
    defaultStat = StatType.DENSITY_2D
    requiredAes = ['x', 'y']
    defaultAes = [alpha: 1.0] as Map<String, Object>
  }

  GeomDensity2dFilled(Map params) {
    this()
    if (params.fillColors) this.fillColors = params.fillColors as List<String>
    if (params.fill_colors) this.fillColors = params.fill_colors as List<String>
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.bins != null) this.bins = params.bins as int
    if (params.n != null) this.n = params.n as int
    if (params.h) this.h = params.h as List<Number>
    this.fillColors = this.fillColors.collect { ColorUtil.normalizeColor(it) }
    if (this.color) this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 3) return

    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomDensity2dFilled requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']

    // Collect data points
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

    if (points.size() < 3) return

    // Calculate bandwidth if not specified
    BigDecimal hx, hy
    if (h != null && h.size() >= 2) {
      hx = h[0] as BigDecimal
      hy = h[1] as BigDecimal
    } else {
      hx = GeomDensity2d.calculateBandwidth(points*.getAt(0))
      hy = GeomDensity2d.calculateBandwidth(points*.getAt(1))
    }

    // Create density grid
    BigDecimal xRange = xMax - xMin
    BigDecimal yRange = yMax - yMin
    BigDecimal xStep = xRange / (n - 1)
    BigDecimal yStep = yRange / (n - 1)

    BigDecimal[][] densityGrid = new BigDecimal[n][n]
    BigDecimal maxDensity = 0

    // Compute 2D KDE on grid
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        BigDecimal gridX = xMin + i * xStep
        BigDecimal gridY = yMin + j * yStep

        BigDecimal density = GeomDensity2d.compute2dKde(gridX, gridY, points, hx, hy)
        densityGrid[i][j] = density

        if (density > maxDensity) {
          maxDensity = density
        }
      }
    }

    if (maxDensity == 0) return

    // Render filled contours as tiles
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < n - 1; j++) {
        BigDecimal density = densityGrid[i][j]

        // Determine fill color based on density level
        String fillColor
        if (fillScale) {
          fillColor = fillScale.transform(density)?.toString() ?: getFillColor(density, maxDensity)
        } else {
          fillColor = getFillColor(density, maxDensity)
        }
        fillColor = ColorUtil.normalizeColor(fillColor) ?: fillColor

        // Grid cell bounds in data space
        BigDecimal x0 = xMin + i * xStep
        BigDecimal x1 = x0 + xStep
        BigDecimal y0 = yMin + j * yStep
        BigDecimal y1 = y0 + yStep

        // Transform to pixel space
        def x0Px = xScale?.transform(x0)
        def x1Px = xScale?.transform(x1)
        def y0Px = yScale?.transform(y0)
        def y1Px = yScale?.transform(y1)

        if (x0Px == null || x1Px == null || y0Px == null || y1Px == null) continue

        int left = (x0Px as BigDecimal).min(x1Px as BigDecimal).intValue()
        int right = (x0Px as BigDecimal).max(x1Px as BigDecimal).intValue()
        int top = (y0Px as BigDecimal).min(y1Px as BigDecimal).intValue()
        int bottom = (y0Px as BigDecimal).max(y1Px as BigDecimal).intValue()

        int width = 1.max(right - left).intValue()
        int height = 1.max(bottom - top).intValue()

        // Draw filled rectangle
        def rect = group.addRect()
            .x(left)
            .y(top)
            .width(width)
            .height(height)
            .fill(fillColor)

        if (alpha < 1.0) {
          rect.addAttribute('fill-opacity', alpha)
        }

        // Optional border
        if (color != null && linewidth > 0) {
          rect.stroke(color)
          rect.addAttribute('stroke-width', linewidth)
        } else {
          rect.stroke('none')
        }
      }
    }
  }

  /**
   * Get fill color based on density value.
   */
  private String getFillColor(BigDecimal density, BigDecimal maxDensity) {
    if (maxDensity == 0 || density == 0) return fillColors[0]

    BigDecimal ratio = density / maxDensity
    BigDecimal rawIdx = ratio * (fillColors.size() - 1)
    BigDecimal colorIdx = 0.max(rawIdx.min(fillColors.size() - 1))
    return fillColors[colorIdx]
  }
}
