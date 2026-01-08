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
 * Hexagonal binning geometry for creating hexbin plots from point data.
 * Divides the plotting area into a hexagonal grid and counts observations in each hexagon.
 *
 * Usage:
 * - geom_hex() - default 30 bins
 * - geom_hex(bins: 20) - 20 bins in x direction
 * - geom_hex(binwidth: 0.5) - specify hexagon width
 *
 * Similar to geom_bin_2d() but uses hexagonal bins instead of rectangular.
 */
@CompileStatic
class GeomHex extends Geom {

  /** Number of bins in x direction */
  int bins = 30

  /** Hexagon width (overrides bins if set) */
  Number binwidth

  /** Fill color for hexagons (used if no fill scale) */
  String fill = 'steelblue'

  /** Border color for hexagons */
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

  GeomHex() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'white'] as Map<String, Object>
  }

  GeomHex(Map params) {
    this()
    if (params.bins != null) this.bins = params.bins as int
    if (params.binwidth != null) this.binwidth = params.binwidth as Number
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.drop != null) this.drop = params.drop as boolean
    if (params.fillColors) this.fillColors = params.fillColors as List<String>
    if (params.fill_colors) this.fillColors = params.fill_colors as List<String>
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
      throw new IllegalArgumentException("GeomHex requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']

    // Collect numeric x,y values
    List<BigDecimal[]> points = []
    BigDecimal xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE
    BigDecimal yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal instanceof Number && yVal instanceof Number) {
        BigDecimal x = xVal as BigDecimal
        BigDecimal y = yVal as BigDecimal
        points << ([x, y] as BigDecimal[])

        if (x < xMin) xMin = x
        if (x > xMax) xMax = x
        if (y < yMin) yMin = y
        if (y > yMax) yMax = y
      }
    }

    if (points.isEmpty()) return

    // Compute hex dimensions
    BigDecimal xRange = xMax - xMin

    // Prevent zero range
    if (xRange == 0) xRange = 1

    // Hexagon geometry: width and height
    BigDecimal hexWidth
    if (binwidth != null) {
      hexWidth = binwidth as BigDecimal
    } else {
      hexWidth = xRange / bins
    }

    // For regular hexagons (flat-top), height = width * sqrt(3) / 2
    BigDecimal hexHeight = hexWidth * (Math.sqrt(3) / 2)

    // Hexagon spacing
    BigDecimal dx = hexWidth * 0.75  // horizontal spacing between hex centers
    BigDecimal dy = hexHeight        // vertical spacing between hex centers

    // Count points in each hexagon using a map
    Map<String, Integer> hexCounts = [:] as Map<String, Integer>
    int maxCount = 0

    for (BigDecimal[] point : points) {
      // Find hexagon coordinates for this point
      def hexCoord = pointToHex(point[0], point[1], xMin, yMin, dx, dy)
      String hexKey = "${hexCoord[0]},${hexCoord[1]}"

      hexCounts[hexKey] = (hexCounts[hexKey] ?: 0) + 1
      if (hexCounts[hexKey] > maxCount) {
        maxCount = hexCounts[hexKey]
      }
    }

    if (maxCount == 0) return

    // Render hexagons
    hexCounts.each { String key, Integer count ->
      if (drop && count == 0) return

      def coords = key.split(',')
      int col = coords[0] as int
      int row = coords[1] as int

      // Calculate hexagon center in data coordinates
      BigDecimal hexX = xMin + col * dx
      BigDecimal hexY = yMin + row * dy

      // Offset every other row
      if (row % 2 == 1) {
        hexX = hexX + dx / 2
      }

      // Transform to pixel coordinates
      BigDecimal centerXPx = xScale?.transform(hexX) as BigDecimal
      BigDecimal centerYPx = yScale?.transform(hexY) as BigDecimal

      if (centerXPx == null || centerYPx == null) return

      // Calculate hexagon size in pixel space
      BigDecimal leftXPx = xScale?.transform(hexX - hexWidth / 2) as BigDecimal
      BigDecimal rightXPx = xScale?.transform(hexX + hexWidth / 2) as BigDecimal
      BigDecimal topYPx = yScale?.transform(hexY - hexHeight / 2) as BigDecimal
      BigDecimal bottomYPx = yScale?.transform(hexY + hexHeight / 2) as BigDecimal

      if (leftXPx == null || rightXPx == null || topYPx == null || bottomYPx == null) return

      BigDecimal pixelWidth = (rightXPx - leftXPx).abs()
      BigDecimal pixelHeight = (bottomYPx - topYPx).abs()
      // Get fill color based on count
      String hexFill
      if (fillScale != null) {
        hexFill = fillScale.transform(count)?.toString() ?: getFillColor(count, maxCount)
      } else {
        hexFill = getFillColor(count, maxCount)
      }
      hexFill = ColorUtil.normalizeColor(hexFill)

      // Draw hexagon using path
      String pathData = createHexagonPath(
          centerXPx,
          centerYPx,
          pixelWidth,
          pixelHeight
      )

      def path = group.addPath()
          .d(pathData)
          .fill(hexFill)

      if (color != null && linewidth > 0) {
        path.stroke(ColorUtil.normalizeColor(color))
        path.addAttribute('stroke-width', linewidth)
      } else {
        path.stroke('none')
      }

      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }
    }
  }

  /**
   * Convert a point to hexagon grid coordinates.
   * Returns [col, row] in the hexagonal grid.
   */
  private static int[] pointToHex(BigDecimal x, BigDecimal y, BigDecimal xMin, BigDecimal yMin,
                                    BigDecimal dx, BigDecimal dy) {
    BigDecimal relX = x - xMin
    BigDecimal relY = y - yMin

    // Approximate column and row
    int row = (relY / dy).round() as int
    int col = (relX / dx).round() as int

    // Adjust column for odd rows
    if (row % 2 == 1) {
      col = ((relX - dx / 2) / dx).round() as int
    }

    return [col, row] as int[]
  }

  /**
   * Create SVG path data for a flat-top hexagon.
   */
  private static String createHexagonPath(Number cx, Number cy, Number width, Number height) {
    // For flat-top hexagon
    BigDecimal w = width / 2
    BigDecimal h = height / 2

    // Six vertices of hexagon (flat-top)
    BigDecimal[][] points = [
        [cx - w / 2, cy - h] as BigDecimal[],      // top left
        [cx + w / 2, cy - h] as BigDecimal[],      // top right
        [cx + w, cy] as BigDecimal[],              // right
        [cx + w / 2, cy + h] as BigDecimal[],      // bottom right
        [cx - w / 2, cy + h] as BigDecimal[],      // bottom left
        [cx - w, cy] as BigDecimal[]               // left
    ]

    StringBuilder path = new StringBuilder("M ${points[0][0]},${points[0][1]}")
    for (int i = 1; i < points.length; i++) {
      path.append(" L ${points[i][0]},${points[i][1]}")
    }
    path.append(" Z")

    return path.toString()
  }

  /**
   * Get fill color based on count value.
   */
  private String getFillColor(int count, int maxCount) {
    if (maxCount == 0 || count == 0) return fillColors[0]

    BigDecimal ratio = count / maxCount
    BigDecimal rawIdx = (ratio * (fillColors.size() - 1))
    BigDecimal colorIdx = 0.max(rawIdx.min(fillColors.size() - 1))
    return fillColors[colorIdx]
  }
}
