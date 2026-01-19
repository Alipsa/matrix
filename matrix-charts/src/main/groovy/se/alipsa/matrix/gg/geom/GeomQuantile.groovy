package se.alipsa.matrix.gg.geom

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale

/**
 * Quantile regression geometry.
 * Renders quantile regression lines that estimate conditional quantiles
 * rather than the conditional mean.
 *
 * Example:
 * <pre>
 * // Fit 25th, 50th, and 75th percentile lines
 * chart + geom_quantile()
 *
 * // Fit only the median (50th percentile)
 * chart + geom_quantile(quantiles: [0.5])
 *
 * // Fit 10th, 50th, and 90th percentiles with custom styling
 * chart + geom_quantile(quantiles: [0.1, 0.5, 0.9], color: 'blue', linetype: 'dashed')
 * </pre>
 */
@CompileStatic
class GeomQuantile extends Geom {

  /** Line color */
  String color = '#3366FF'

  /** Line width */
  Number size = 1.0

  /** Line type (solid, dashed, dotted, etc.) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Quantiles to compute [0.25, 0.5, 0.75] */
  List<Number> quantiles = [0.25, 0.5, 0.75]

  /** Number of fitted points */
  int n = 80

  GeomQuantile() {
    defaultStat = StatType.QUANTILE
    requiredAes = ['x', 'y']
    defaultAes = [color: '#3366FF', size: 1.0] as Map<String, Object>
  }

  GeomQuantile(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size) this.size = params.size as Number
    if (params.linetype) this.linetype = params.linetype
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.quantiles) this.quantiles = params.quantiles as List<Number>
    if (params.n) this.n = params.n as int
    this.params = params
  }

  @CompileDynamic
  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    // The data has already been transformed by GgStat.quantile()
    // It contains columns: x, y, quantile, group
    Scale xScale = scales['x']
    Scale yScale = scales['y']

    // Determine line color
    String lineColor = this.color
    if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    // Get stroke-dasharray for line type
    String dashArray = getDashArray(linetype)

    // Group by 'group' column (one group per quantile)
    // Data should come from GgStat.quantile() with columns: x, y, quantile, group
    if (!data.columnNames().containsAll(['x', 'y', 'group'])) {
      // Stat transformation failed or returned unexpected structure - skip rendering
      return
    }

    Map<Integer, List<Map>> groups = [:].withDefault { [] }
    data.each { row ->
      def grpVal = row['group']
      def xVal = row['x']
      def yVal = row['y']
      // Skip rows with missing values
      if (grpVal == null || xVal == null || yVal == null) return

      Integer grp = grpVal as Integer
      groups[grp] << [x: xVal, y: yVal]
    }

    // Render each quantile line
    groups.each { Integer grpIdx, List<Map> points ->
      if (points.size() < 2) return

      // Transform to pixel coordinates
      List<List<Number>> pixelPoints = points.collect { Map pt ->
        double xPx = (xScale?.transform(pt.x) ?: pt.x) as double
        double yPx = (yScale?.transform(pt.y) ?: pt.y) as double
        [xPx, yPx]
      }

      // Draw connected line segments
      for (int i = 0; i < pixelPoints.size() - 1; i++) {
        def p1 = pixelPoints[i]
        def p2 = pixelPoints[i + 1]

        def line = group.addLine(p1[0], p1[1], p2[0], p2[1])
            .stroke(lineColor)
            .strokeWidth(size)

        if (dashArray) {
          line.addAttribute('stroke-dasharray', dashArray)
        }

        if (alpha < 1.0) {
          line.addAttribute('stroke-opacity', alpha)
        }
      }
    }
  }

  @CompileDynamic
  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() < 2) return

    // The data has already been transformed by GgStat.quantile()
    // It contains columns: x, y, quantile, group
    Scale xScale = scales['x']
    Scale yScale = scales['y']

    // Determine line color
    String lineColor = this.color
    if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    // Get stroke-dasharray for line type
    String dashArray = getDashArray(linetype)

    // Group by 'group' column (one group per quantile)
    // Data should come from GgStat.quantile() with columns: x, y, quantile, group
    if (!data.columnNames().containsAll(['x', 'y', 'group'])) {
      // Stat transformation failed or returned unexpected structure - skip rendering
      return
    }

    Map<Integer, List<Map>> groups = [:].withDefault { [] }
    data.each { row ->
      def grpVal = row['group']
      def xVal = row['x']
      def yVal = row['y']
      // Skip rows with missing values
      if (grpVal == null || xVal == null || yVal == null) return

      Integer grp = grpVal as Integer
      groups[grp] << [x: xVal, y: yVal]
    }

    int elementIndex = 0
    // Render each quantile line
    groups.each { Integer grpIdx, List<Map> points ->
      if (points.size() < 2) {
        elementIndex++
        return
      }

      // Transform to pixel coordinates
      List<List<Number>> pixelPoints = points.collect { Map pt ->
        double xPx = (xScale?.transform(pt.x) ?: pt.x) as double
        double yPx = (yScale?.transform(pt.y) ?: pt.y) as double
        [xPx, yPx]
      }

      // Draw connected line segments
      for (int i = 0; i < pixelPoints.size() - 1; i++) {
        def p1 = pixelPoints[i]
        def p2 = pixelPoints[i + 1]

        def line = group.addLine(p1[0], p1[1], p2[0], p2[1])
            .stroke(lineColor)
            .strokeWidth(size)

        if (dashArray) {
          line.addAttribute('stroke-dasharray', dashArray)
        }

        if (alpha < 1.0) {
          line.addAttribute('stroke-opacity', alpha)
        }

        // Apply CSS attributes
        GeomUtils.applyAttributes(line, ctx, 'quantile', 'gg-quantile', elementIndex)
        elementIndex++
      }
    }
  }

  /**
   * Convert line type to SVG stroke-dasharray.
   * Returns null for 'solid' or unknown line types (no dash pattern).
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
}
