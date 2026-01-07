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
 * Contour geometry for drawing contour lines from 2D density/height data.
 * Uses marching squares algorithm to trace isolines.
 *
 * Usage:
 * - geom_contour() - contour lines with automatic levels
 * - geom_contour(bins: 10) - specify number of contour levels
 * - geom_contour(binwidth: 0.5) - specify spacing between levels
 *
 * Data format:
 * - Grid data with x, y, z columns
 * - Or use stat_contour to compute from density
 */
@CompileStatic
class GeomContour extends Geom {

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

  /** Spacing between contour levels (overrides bins if set) */
  Number binwidth

  GeomContour() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'z']
    defaultAes = [color: 'black', linewidth: 0.5] as Map<String, Object>
  }

  GeomContour(Map params) {
    this()
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.bins != null) this.bins = params.bins as int
    if (params.binwidth != null) this.binwidth = params.binwidth as Number
    this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 4) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String zCol = aes.label instanceof String ? aes.label as String : 'z'

    // Try to find z column
    List<String> colNames = data.columnNames()
    if (!colNames.contains(zCol)) {
      zCol = colNames.find { it.toLowerCase() == 'z' || it.toLowerCase() == 'value' || it.toLowerCase() == 'height' }
    }

    if (xCol == null || yCol == null || zCol == null) {
      throw new IllegalArgumentException("GeomContour requires x, y, and z aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    // Build grid from data
    GridData grid = buildGrid(data, xCol, yCol, zCol)
    if (grid == null || grid.values.length < 2) return

    // Determine contour levels
    List<BigDecimal> levels = computeLevels(grid.zMin, grid.zMax)

    // Generate and render contours for each level
    levels.eachWithIndex { BigDecimal level, int idx ->
      List<List<BigDecimal[]>> contours = marchingSquares(grid, level)

      // Determine color (could be based on level)
      String lineColor = this.color
      if (colorScale != null) {
        lineColor = colorScale.transform(level)?.toString() ?: this.color
      }
      lineColor = ColorUtil.normalizeColor(lineColor)

      // Draw each contour line
      contours.each { List<BigDecimal[]> contour ->
        if (contour.size() < 2) return

        for (int i = 0; i < contour.size() - 1; i++) {
          BigDecimal[] p1 = contour[i]
          BigDecimal[] p2 = contour[i + 1]

          // Transform to pixel coordinates
          def x1Px = xScale?.transform(p1[0])
          def y1Px = yScale?.transform(p1[1])
          def x2Px = xScale?.transform(p2[0])
          def y2Px = yScale?.transform(p2[1])

          if (x1Px == null || y1Px == null || x2Px == null || y2Px == null) continue

          def line = group.addLine()
              .x1(x1Px as int)
              .y1(y1Px as int)
              .x2(x2Px as int)
              .y2(y2Px as int)
              .stroke(lineColor)

          line.addAttribute('stroke-width', linewidth)

          String dashArray = getDashArray(linetype)
          if (dashArray) {
            line.addAttribute('stroke-dasharray', dashArray)
          }

          if (alpha < 1.0) {
            line.addAttribute('stroke-opacity', alpha)
          }
        }
      }
    }
  }

  /**
   * Build a grid from scattered data points.
   */
  protected GridData buildGrid(Matrix data, String xCol, String yCol, String zCol) {
    // Collect unique x and y values
    Set<BigDecimal> xSet = new TreeSet<>()
    Set<BigDecimal> ySet = new TreeSet<>()
    Map<String, BigDecimal> zMap = [:]

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def zVal = row[zCol]

      if (xVal instanceof Number && yVal instanceof Number && zVal instanceof Number) {
        BigDecimal x = xVal as BigDecimal
        BigDecimal y = yVal as BigDecimal
        BigDecimal z = zVal as BigDecimal

        xSet.add(x)
        ySet.add(y)
        zMap["${x},${y}".toString()] = z as BigDecimal
      }
    }

    if (xSet.size() < 2 || ySet.size() < 2) return null

    List<BigDecimal> xValues = xSet.toList()
    List<BigDecimal> yValues = ySet.toList()

    int nx = xValues.size()
    int ny = yValues.size()

    BigDecimal[][] values = new BigDecimal[ny][nx]
    BigDecimal zMin = Double.MAX_VALUE
    BigDecimal zMax = -Double.MAX_VALUE

    for (int j = 0; j < ny; j++) {
      for (int i = 0; i < nx; i++) {
        String key = "${xValues[i]},${yValues[j]}"
        BigDecimal z = zMap[key]
        if (z != null) {
          values[j][i] = z
          if (z < zMin) zMin = z
          if (z > zMax) zMax = z
        } else {
          // Interpolate or use NaN
          values[j][i] = null
        }
      }
    }

    return new GridData(
        xValues: xValues as BigDecimal[],
        yValues: yValues as BigDecimal[],
        values: values,
        zMin: zMin,
        zMax: zMax
    )
  }

  /**
   * Compute contour levels.
   */
  private List<BigDecimal> computeLevels(BigDecimal zMin, BigDecimal zMax) {
    List<BigDecimal> levels = []
    BigDecimal range = zMax - zMin

    if (range == 0) {
      return [zMin]
    }

    BigDecimal step
    if (binwidth != null) {
      step = binwidth as BigDecimal
    } else {
      step = range / bins
    }

    BigDecimal level = zMin + step
    while (level < zMax) {
      levels << level
      level += step
    }

    return levels
  }

  /**
   * Marching squares algorithm for contour generation.
   */
  protected List<List<BigDecimal[]>> marchingSquares(GridData grid, BigDecimal level) {
    List<List<BigDecimal[]>> contours = []
    int nx = grid.xValues.length
    int ny = grid.yValues.length

    // Process each cell
    for (int j = 0; j < ny - 1; j++) {
      for (int i = 0; i < nx - 1; i++) {
        BigDecimal[] cell = [
            grid.values[j][i],
            grid.values[j][i + 1],
            grid.values[j + 1][i + 1],
            grid.values[j + 1][i]
        ] as BigDecimal[]

        // Skip if any value is NaN
        if (cell.any { it == null }) continue

        // Determine case (4-bit code based on which corners are above level)
        int code = 0
        if (cell[0] >= level) code |= 1
        if (cell[1] >= level) code |= 2
        if (cell[2] >= level) code |= 4
        if (cell[3] >= level) code |= 8

        // Skip if all above or all below
        if (code == 0 || code == 15) continue

        // Get cell coordinates
        BigDecimal x0 = grid.xValues[i]
        BigDecimal x1 = grid.xValues[i + 1]
        BigDecimal y0 = grid.yValues[j]
        BigDecimal y1 = grid.yValues[j + 1]

        // Generate line segments for this cell
        List<BigDecimal[]> segments = getContourSegments(code, cell, level, x0, x1, y0, y1)
        if (!segments.isEmpty()) {
          contours << segments
        }
      }
    }

    return contours
  }

  /**
   * Get contour line segments for a cell based on marching squares case.
   */
  private List<BigDecimal[]> getContourSegments(int code, BigDecimal[] cell, BigDecimal level,
                                            BigDecimal x0, BigDecimal x1, BigDecimal y0, BigDecimal y1) {
    List<BigDecimal[]> points = []

    // Interpolation helper
    Closure<BigDecimal> lerp = { BigDecimal v0, BigDecimal v1, BigDecimal t0, BigDecimal t1 ->
      if (v1 == v0) return t0
      return t0 + (t1 - t0) * (level - v0) / (v1 - v0)
    }

    // Edge midpoints (interpolated)
    BigDecimal xTop = lerp(cell[0], cell[1], x0, x1)
    BigDecimal xBottom = lerp(cell[3], cell[2], x0, x1)
    BigDecimal yLeft = lerp(cell[0], cell[3], y0, y1)
    BigDecimal yRight = lerp(cell[1], cell[2], y0, y1)

    // Cases based on 4-bit code
    switch (code) {
      case 1: case 14:
        points << ([xTop, y0] as BigDecimal[])
        points << ([x0, yLeft] as BigDecimal[])
        break
      case 2: case 13:
        points << ([xTop, y0] as BigDecimal[])
        points << ([x1, yRight] as BigDecimal[])
        break
      case 3: case 12:
        points << ([x0, yLeft] as BigDecimal[])
        points << ([x1, yRight] as BigDecimal[])
        break
      case 4: case 11:
        points << ([x1, yRight] as BigDecimal[])
        points << ([xBottom, y1] as BigDecimal[])
        break
      case 5:
        // Saddle point - two separate lines
        points << ([xTop, y0] as BigDecimal[])
        points << ([x1, yRight] as BigDecimal[])
        // Second line handled separately
        break
      case 6: case 9:
        points << ([xTop, y0] as BigDecimal[])
        points << ([xBottom, y1] as BigDecimal[])
        break
      case 7: case 8:
        points << ([x0, yLeft] as BigDecimal[])
        points << ([xBottom, y1] as BigDecimal[])
        break
      case 10:
        // Saddle point - two separate lines
        points << ([xTop, y0] as BigDecimal[])
        points << ([x0, yLeft] as BigDecimal[])
        // Second line handled separately
        break
    }

    return points
  }

  private String getDashArray(String type) {
    switch (type?.toLowerCase()) {
      case 'dashed': return '8,4'
      case 'dotted': return '2,2'
      case 'longdash': return '12,4'
      case 'twodash': return '4,2,8,2'
      case 'solid':
      default: return null
    }
  }

  /**
   * Helper class to hold grid data.
   */
  @CompileStatic
  protected static class GridData {
    BigDecimal[] xValues
    BigDecimal[] yValues
    BigDecimal[][] values
    BigDecimal zMin
    BigDecimal zMax
  }
}
