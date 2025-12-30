package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.scale.Scale

/**
 * Filled contour geometry for drawing filled regions between contour levels.
 * Uses marching squares to create filled polygons between isolines.
 *
 * Usage:
 * - geom_contour_filled() - filled contours with automatic levels
 * - geom_contour_filled(bins: 10) - specify number of contour levels
 * - geom_contour_filled(binwidth: 0.5) - specify spacing between levels
 *
 * Data format:
 * - Grid data with x, y, z columns
 */
@CompileStatic
class GeomContourFilled extends GeomContour {

  /** Fill alpha transparency (0-1) */
  Number fillAlpha = 0.8

  /** Default color palette for fills */
  List<String> fillColors = [
      '#440154', '#482878', '#3E4A89', '#31688E', '#26838E',
      '#1F9E89', '#35B779', '#6DCD59', '#B4DE2C', '#FDE725'
  ]

  GeomContourFilled() {
    super()
    defaultAes = [fill: 'blue', alpha: 0.8] as Map<String, Object>
  }

  GeomContourFilled(Map params) {
    super(params)
    if (params.fillAlpha != null) this.fillAlpha = params.fillAlpha as Number
    if (params.fill_colors) this.fillColors = params.fill_colors as List<String>
    if (params.fillColors) this.fillColors = params.fillColors as List<String>
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
      throw new IllegalArgumentException("GeomContourFilled requires x, y, and z aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']

    // Build grid from data
    GridData grid = buildGrid(data, xCol, yCol, zCol)
    if (grid == null || grid.values.length < 2) return

    // Determine contour levels (including endpoints for filling)
    List<Double> levels = computeFilledLevels(grid.zMin, grid.zMax)
    if (levels.size() < 2) return

    int nx = grid.xValues.length
    int ny = grid.yValues.length

    // For each level band, fill the cells
    for (int levelIdx = 0; levelIdx < levels.size() - 1; levelIdx++) {
      double lowLevel = levels[levelIdx]
      double highLevel = levels[levelIdx + 1]

      // Get fill color for this band
      String bandFill = getFillColor(levelIdx, levels.size() - 1, fillScale, lowLevel, highLevel)

      // Process each cell and fill based on level range
      for (int j = 0; j < ny - 1; j++) {
        for (int i = 0; i < nx - 1; i++) {
          double[] cell = [
              grid.values[j][i],
              grid.values[j][i + 1],
              grid.values[j + 1][i + 1],
              grid.values[j + 1][i]
          ] as double[]

          // Skip if any value is NaN
          if (cell.any { Double.isNaN(it) }) continue

          // Skip if cell doesn't intersect this level band
          double cellMin = cell.min()
          double cellMax = cell.max()
          if (cellMax < lowLevel || cellMin >= highLevel) continue

          // Get cell coordinates
          double x0 = grid.xValues[i]
          double x1 = grid.xValues[i + 1]
          double y0 = grid.yValues[j]
          double y1 = grid.yValues[j + 1]

          // Generate filled polygon for this cell and level band
          List<double[]> polygon = getFilledPolygon(cell, lowLevel, highLevel, x0, x1, y0, y1)
          if (polygon.size() < 3) continue

          // Transform to pixel coordinates
          StringBuilder pathD = new StringBuilder()
          boolean first = true

          for (double[] point : polygon) {
            def xPx = xScale?.transform(point[0])
            def yPx = yScale?.transform(point[1])
            if (xPx == null || yPx == null) continue

            if (first) {
              pathD << "M ${xPx as int} ${yPx as int}"
              first = false
            } else {
              pathD << " L ${xPx as int} ${yPx as int}"
            }
          }
          pathD << " Z"

          if (!first) {
            def path = group.addPath().d(pathD.toString())
                .fill(bandFill)
                .stroke('none')

            if ((fillAlpha as double) < 1.0) {
              path.addAttribute('fill-opacity', fillAlpha)
            }
          }
        }
      }
    }

    // Optionally draw contour lines on top
    if ((linewidth as double) > 0) {
      // Draw outlines at each level
      for (int levelIdx = 1; levelIdx < levels.size() - 1; levelIdx++) {
        double level = levels[levelIdx]
        List<List<double[]>> contours = marchingSquares(grid, level)

        contours.each { List<double[]> contour ->
          if (contour.size() < 2) return

          for (int i = 0; i < contour.size() - 1; i++) {
            double[] p1 = contour[i]
            double[] p2 = contour[i + 1]

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
                .stroke(color)

            line.addAttribute('stroke-width', linewidth * 0.5)

            if ((alpha as double) < 1.0) {
              line.addAttribute('stroke-opacity', alpha)
            }
          }
        }
      }
    }
  }

  /**
   * Compute levels for filled contours (includes min and max).
   */
  private List<Double> computeFilledLevels(double zMin, double zMax) {
    List<Double> levels = [zMin]
    double range = zMax - zMin

    if (range == 0) {
      return [zMin, zMin + 1]
    }

    double step
    if (binwidth != null) {
      step = binwidth as double
    } else {
      step = range / bins
    }

    double level = zMin + step
    while (level < zMax) {
      levels << level
      level += step
    }
    levels << zMax

    return levels
  }

  /**
   * Get fill color for a level band.
   */
  private String getFillColor(int levelIdx, int numBands, Scale fillScale, double lowLevel, double highLevel) {
    if (fillScale != null) {
      double midLevel = (lowLevel + highLevel) / 2
      def transformed = fillScale.transform(midLevel)
      if (transformed != null) return transformed.toString()
    }

    // Use default color palette
    int colorIdx = (int) (levelIdx * (fillColors.size() - 1) / Math.max(1, numBands - 1))
    colorIdx = Math.max(0, Math.min(colorIdx, fillColors.size() - 1))
    return fillColors[colorIdx]
  }

  /**
   * Generate filled polygon for a cell within a level band.
   * Uses modified marching squares to create polygons instead of lines.
   */
  private List<double[]> getFilledPolygon(double[] cell, double lowLevel, double highLevel,
                                           double x0, double x1, double y0, double y1) {
    List<double[]> points = []

    // Clamp cell values to level band
    double[] clamped = new double[4]
    for (int i = 0; i < 4; i++) {
      clamped[i] = Math.max(lowLevel, Math.min(highLevel, cell[i]))
    }

    // Determine which corners are within the band
    boolean[] inBand = new boolean[4]
    for (int i = 0; i < 4; i++) {
      inBand[i] = cell[i] >= lowLevel && cell[i] < highLevel
    }

    // Build polygon by walking around the cell
    // Corners: 0=bottom-left, 1=bottom-right, 2=top-right, 3=top-left
    // Edges: 0=bottom, 1=right, 2=top, 3=left

    List<double[]> vertices = [
        [x0, y0] as double[],  // corner 0
        [x1, y0] as double[],  // corner 1
        [x1, y1] as double[],  // corner 2
        [x0, y1] as double[]   // corner 3
    ]

    // Interpolation helper
    Closure<double[]> interpolateEdge = { int corner1, int corner2, double level ->
      double v1 = cell[corner1]
      double v2 = cell[corner2]
      if (v1 == v2) return vertices[corner1]

      double t = (level - v1) / (v2 - v1)
      t = Math.max(0, Math.min(1, t))
      double[] p1 = vertices[corner1]
      double[] p2 = vertices[corner2]
      return [p1[0] + t * (p2[0] - p1[0]), p1[1] + t * (p2[1] - p1[1])] as double[]
    }

    // Walk around edges and collect boundary points
    int[][] edges = [[0, 1], [1, 2], [2, 3], [3, 0]] as int[][]

    for (int e = 0; e < 4; e++) {
      int c1 = edges[e][0]
      int c2 = edges[e][1]
      double v1 = cell[c1]
      double v2 = cell[c2]

      // Add corner if it's at or above lowLevel
      if (v1 >= lowLevel && v1 < highLevel) {
        points << vertices[c1]
      } else if (v1 >= highLevel) {
        // Corner above high level - add intersection with high level
        if (v2 < highLevel) {
          points << interpolateEdge(c1, c2, highLevel)
        }
      } else {
        // Corner below low level - add intersection with low level
        if (v2 >= lowLevel) {
          points << interpolateEdge(c1, c2, lowLevel)
        }
      }

      // Check for level crossings along edge
      if ((v1 < lowLevel && v2 >= highLevel) || (v1 >= highLevel && v2 < lowLevel)) {
        // Edge crosses both levels
        double[] pLow = interpolateEdge(c1, c2, lowLevel)
        double[] pHigh = interpolateEdge(c1, c2, highLevel)

        if (v1 < v2) {
          // Going up: add low then high
          if (!containsPoint(points, pLow)) points << pLow
          if (!containsPoint(points, pHigh)) points << pHigh
        } else {
          // Going down: add high then low
          if (!containsPoint(points, pHigh)) points << pHigh
          if (!containsPoint(points, pLow)) points << pLow
        }
      } else if (v1 < lowLevel && v2 >= lowLevel && v2 < highLevel) {
        // Edge crosses low level going in
        double[] p = interpolateEdge(c1, c2, lowLevel)
        if (!containsPoint(points, p)) points << p
      } else if (v1 >= lowLevel && v1 < highLevel && v2 < lowLevel) {
        // Edge crosses low level going out
        double[] p = interpolateEdge(c1, c2, lowLevel)
        if (!containsPoint(points, p)) points << p
      } else if (v1 < highLevel && v2 >= highLevel) {
        // Edge crosses high level going out
        double[] p = interpolateEdge(c1, c2, highLevel)
        if (!containsPoint(points, p)) points << p
      } else if (v1 >= highLevel && v2 < highLevel && v2 >= lowLevel) {
        // Edge crosses high level going in
        double[] p = interpolateEdge(c1, c2, highLevel)
        if (!containsPoint(points, p)) points << p
      }
    }

    // Remove duplicates and order properly
    return removeDuplicatePoints(points)
  }

  private boolean containsPoint(List<double[]> points, double[] p) {
    for (double[] existing : points) {
      if (Math.abs(existing[0] - p[0]) < 1e-10 && Math.abs(existing[1] - p[1]) < 1e-10) {
        return true
      }
    }
    return false
  }

  private List<double[]> removeDuplicatePoints(List<double[]> points) {
    if (points.size() <= 1) return points

    List<double[]> result = []
    for (double[] p : points) {
      if (!containsPoint(result, p)) {
        result << p
      }
    }
    return result
  }
}
