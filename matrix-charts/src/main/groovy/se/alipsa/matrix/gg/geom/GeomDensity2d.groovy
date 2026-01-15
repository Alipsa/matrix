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
 * 2D density contour lines geometry.
 * Computes 2D kernel density estimation and renders contour lines.
 *
 * Usage:
 * - geom_density_2d() - default density contours
 * - geom_density_2d(bins: 10) - specify number of contour levels
 * - geom_density_2d(h: [0.5, 0.5]) - specify bandwidth for KDE
 * - geom_density_2d(n: 100) - number of grid points
 */
@CompileStatic
class GeomDensity2d extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Line type */
  String linetype = 'solid'

  /** Number of contour bins/levels */
  int bins = 10

  /** Grid size for density estimation */
  int n = 100

  /** Bandwidth for KDE [h_x, h_y]. If null, auto-calculated */
  List<Number> h = null

  GeomDensity2d() {
    defaultStat = StatType.DENSITY_2D
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', linewidth: 0.5] as Map<String, Object>
  }

  GeomDensity2d(Map params) {
    this()
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.bins != null) this.bins = params.bins as int
    if (params.n != null) this.n = params.n as int
    if (params.h) this.h = params.h as List<Number>
    this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 3) return

    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomDensity2d requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

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

    // Calculate bandwidth if not specified (Silverman's rule of thumb)
    BigDecimal hx, hy
    if (h != null && h.size() >= 2) {
      hx = h[0] as BigDecimal
      hy = h[1] as BigDecimal
    } else {
      hx = calculateBandwidth(points*.getAt(0))
      hy = calculateBandwidth(points*.getAt(1))
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

        BigDecimal density = compute2dKde(gridX, gridY, points, hx, hy)
        densityGrid[i][j] = density

        if (density > maxDensity) {
          maxDensity = density
        }
      }
    }

    if (maxDensity == 0) return

    // Determine contour levels
    List<BigDecimal> levels = []
    for (int i = 1; i <= bins; i++) {
      levels << (maxDensity * i / bins)
    }

    // Draw contours using marching squares
    levels.eachWithIndex { BigDecimal level, int idx ->
      // Determine color for this level
      String lineColor = this.color
      if (colorScale) {
        lineColor = colorScale.transform(level)?.toString() ?: this.color
      }
      lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

      // Find contours for this level
      List<List<BigDecimal[]>> contours = marchingSquares(
          densityGrid, xMin, xMax, yMin, yMax, n, n, level
      )

      // Render each contour path
      contours.each { List<BigDecimal[]> contour ->
        if (contour.size() < 2) return

        StringBuilder pathData = new StringBuilder()

        // Transform first point
        def x0 = xScale?.transform(contour[0][0])
        def y0 = yScale?.transform(contour[0][1])
        if (x0 == null || y0 == null) return

        pathData.append("M ${x0 as int},${y0 as int}")

        // Add remaining points
        for (int i = 1; i < contour.size(); i++) {
          def x = xScale?.transform(contour[i][0])
          def y = yScale?.transform(contour[i][1])
          if (x == null || y == null) continue

          pathData.append(" L ${x as int},${y as int}")
        }

        // Create path element
        def path = group.addPath()
            .d(pathData.toString())
            .fill('none')
            .stroke(lineColor)

        path.addAttribute('stroke-width', linewidth)

        if (alpha < 1.0) {
          path.addAttribute('stroke-opacity', alpha)
        }

        // Add linetype
        if (linetype && linetype != 'solid') {
          String dashArray = getDashArray(linetype)
          if (dashArray) {
            path.addAttribute('stroke-dasharray', dashArray)
          }
        }
      }
    }
  }

  /**
   * Calculate bandwidth using Silverman's rule of thumb.
   */
  static BigDecimal calculateBandwidth(List<BigDecimal> values) {
    if (values.isEmpty()) return 1.0

    // Calculate standard deviation
    BigDecimal sum = values.sum() as BigDecimal
    BigDecimal mean = sum / values.size()
    BigDecimal varianceSum = values.collect { (it - mean) ** 2 }.sum() as BigDecimal
    BigDecimal variance = varianceSum / values.size()
    BigDecimal sd = variance.sqrt()

    // Silverman's rule: h = 1.06 * sd * n^(-1/5)
    int n = values.size()
    BigDecimal bandwidth = 1.06 * sd * (n ** (-0.2))

    return bandwidth > 0 ? bandwidth : 1.0
  }

  /**
   * Compute 2D kernel density estimate at a point.
   */
  static BigDecimal compute2dKde(BigDecimal x, BigDecimal y, List<BigDecimal[]> points,
                                   BigDecimal hx, BigDecimal hy) {
    BigDecimal sum = 0
    int n = points.size()

    for (BigDecimal[] point : points) {
      // Gaussian kernel
      BigDecimal dx = (x - point[0]) / hx
      BigDecimal dy = (y - point[1]) / hy
      double exponent = (-(dx * dx + dy * dy) / 2) as double
      BigDecimal kernelValue = Math.exp(exponent)
      sum = sum + kernelValue
    }

    // Normalize by bandwidth product and sample size
    return sum / (n * hx * hy * 2 * Math.PI)
  }

  /**
   * Marching squares algorithm to find contour lines.
   */
  private static List<List<BigDecimal[]>> marchingSquares(
      BigDecimal[][] grid, BigDecimal xMin, BigDecimal xMax,
      BigDecimal yMin, BigDecimal yMax, int nx, int ny, BigDecimal level) {

    List<List<BigDecimal[]>> contours = []
    boolean[][] visited = new boolean[ny - 1][nx - 1]

    BigDecimal dx = (xMax - xMin) / (nx - 1)
    BigDecimal dy = (yMax - yMin) / (ny - 1)

    // Scan grid for contour segments
    for (int j = 0; j < ny - 1; j++) {
      for (int i = 0; i < nx - 1; i++) {
        if (visited[j][i]) continue

        // Get cell values
        BigDecimal v00 = grid[i][j]       // bottom-left
        BigDecimal v10 = grid[i + 1][j]   // bottom-right
        BigDecimal v01 = grid[i][j + 1]   // top-left
        BigDecimal v11 = grid[i + 1][j + 1] // top-right

        // Check if contour passes through this cell
        int caseIdx = 0
        if (v00 >= level) caseIdx |= 1
        if (v10 >= level) caseIdx |= 2
        if (v11 >= level) caseIdx |= 4
        if (v01 >= level) caseIdx |= 8

        // Skip if no contour or all same
        if (caseIdx == 0 || caseIdx == 15) {
          visited[j][i] = true
          continue
        }

        // Build contour segment for this cell
        List<BigDecimal[]> segment = buildContourSegment(
            i, j, v00, v10, v01, v11, level, caseIdx, xMin, yMin, dx, dy
        )

        if (segment.size() > 0) {
          contours << segment
        }

        visited[j][i] = true
      }
    }

    return contours
  }

  /**
   * Build a contour segment for a marching squares cell.
   */
  private static List<BigDecimal[]> buildContourSegment(
      int i, int j, BigDecimal v00, BigDecimal v10, BigDecimal v01, BigDecimal v11,
      BigDecimal level, int caseIdx,
      BigDecimal xMin, BigDecimal yMin, BigDecimal dx, BigDecimal dy) {

    List<BigDecimal[]> segment = []

    // Cell corner positions
    BigDecimal x0 = xMin + i * dx
    BigDecimal x1 = x0 + dx
    BigDecimal y0 = yMin + j * dy
    BigDecimal y1 = y0 + dy

    // Edge midpoints (linear interpolation for better accuracy)
    BigDecimal xBot = lerp(x0, x1, (level - v00) / (v10 - v00))  // bottom edge
    BigDecimal xTop = lerp(x0, x1, (level - v01) / (v11 - v01))  // top edge
    BigDecimal yLeft = lerp(y0, y1, (level - v00) / (v01 - v00))  // left edge
    BigDecimal yRight = lerp(y0, y1, (level - v10) / (v11 - v10))  // right edge

    // Determine which edges to connect based on case
    switch (caseIdx) {
      case 1: case 14:  // bottom-left corner
        segment << ([xBot, y0] as BigDecimal[])
        segment << ([x0, yLeft] as BigDecimal[])
        break
      case 2: case 13:  // bottom-right corner
        segment << ([x1, yRight] as BigDecimal[])
        segment << ([xBot, y0] as BigDecimal[])
        break
      case 3: case 12:  // bottom edge
        segment << ([x0, yLeft] as BigDecimal[])
        segment << ([x1, yRight] as BigDecimal[])
        break
      case 4: case 11:  // top-right corner
        segment << ([xTop, y1] as BigDecimal[])
        segment << ([x1, yRight] as BigDecimal[])
        break
      case 6: case 9:   // right edge
        segment << ([xBot, y0] as BigDecimal[])
        segment << ([xTop, y1] as BigDecimal[])
        break
      case 7: case 8:   // top-left corner
        segment << ([x0, yLeft] as BigDecimal[])
        segment << ([xTop, y1] as BigDecimal[])
        break
    }

    return segment
  }

  /**
   * Linear interpolation helper.
   */
  private static BigDecimal lerp(BigDecimal a, BigDecimal b, BigDecimal t) {
    double tValue = t as double
    if (Double.isNaN(tValue) || Double.isInfinite(tValue)) {
      return (a + b) / 2
    }
    BigDecimal clamped = 0.max(1.min(t))
    return a + (b - a) * clamped
  }

  /**
   * Get SVG dash array for linetype.
   */
  private static String getDashArray(String linetype) {
    final Map<String, String> dashArrays = [
        dashed: '5,5',
        dotted: '2,2',
        longdash: '10,5',
        twodash: '10,5,2,5'
    ]
    return dashArrays[linetype?.toLowerCase()]
  }
}
