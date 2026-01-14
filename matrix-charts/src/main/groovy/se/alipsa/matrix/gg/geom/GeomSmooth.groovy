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
import se.alipsa.matrix.gg.scale.Scale

/**
 * Smooth geometry for trend lines (regression, loess, etc.).
 * Renders a fitted line through the data points.
 */
@CompileStatic
class GeomSmooth extends Geom {

  /** Line color */
  String color = '#3366FF'

  /** Line width */
  Number size = 1.5

  /** Line type (solid, dashed, dotted) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Smoothing method: 'lm' (linear), 'loess', 'gam' */
  String method = 'lm'

  /** Number of points to generate for the smoothed line */
  int n = 80

  /** Whether to show standard error band */
  boolean se = true

  /** Fill color for SE band */
  String fill = '#3366FF'

  /** Alpha for SE band */
  Number fillAlpha = 0.2

  GeomSmooth() {
    defaultStat = StatType.SMOOTH
    requiredAes = ['x', 'y']
    defaultAes = [color: '#3366FF', size: 1.5] as Map<String, Object>
  }

  GeomSmooth(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size) this.size = params.size as Number
    if (params.linetype) this.linetype = params.linetype
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.method) this.method = params.method
    if (params.n) this.n = params.n as int
    if (params.containsKey('se')) this.se = params.se
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    this.params = params
  }

  @CompileDynamic
  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    // The data has already been transformed by GgStat.smooth()
    // It contains 'x' and 'y' columns with the fitted values
    Scale xScale = scales['x']
    Scale yScale = scales['y']

    // Determine line color
    String lineColor = this.color
    if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    boolean hasBand = data.columnNames().containsAll(['ymin', 'ymax'])

    // Get the stroke-dasharray for line type
    String dashArray = getDashArray(linetype)

    // Collect all points
    List<List<Number>> points = []
    List<List<Number>> upper = []
    List<List<Number>> lower = []
    data.each { row ->
      def xVal = row['x']
      def yVal = row['y']

      if (xVal == null || yVal == null) return
      if (!(xVal instanceof Number) || !(yVal instanceof Number)) return

      Number xPx = (xScale?.transform(xVal) ?: xVal) as Number
      Number yPx = (yScale?.transform(yVal) ?: yVal) as Number

      points << [xPx, yPx]

      if (hasBand) {
        def yMinVal = row['ymin']
        def yMaxVal = row['ymax']
        if (yMinVal instanceof Number && yMaxVal instanceof Number) {
          Number yMinPx = (yScale?.transform(yMinVal) ?: yMinVal) as Number
          Number yMaxPx = (yScale?.transform(yMaxVal) ?: yMaxVal) as Number
          upper << [xPx, yMaxPx]
          lower << [xPx, yMinPx]
        }
      }
    }

    if (points.size() < 2) return

    if (hasBand && upper.size() > 1 && lower.size() == upper.size()) {
      StringBuilder d = new StringBuilder()
      d << "M ${upper[0][0]} ${upper[0][1]}"
      for (int i = 1; i < upper.size(); i++) {
        d << " L ${upper[i][0]} ${upper[i][1]}"
      }
      for (int i = lower.size() - 1; i >= 0; i--) {
        d << " L ${lower[i][0]} ${lower[i][1]}"
      }
      d << " Z"
      def band = group.addPath().d(d.toString())
          .fill(ColorUtil.normalizeColor(fill) ?: fill)
          .stroke('none')
      band.addAttribute('fill-opacity', fillAlpha)
    }

    // Draw connected line segments
    for (int i = 0; i < points.size() - 1; i++) {
      def p1 = points[i]
      def p2 = points[i + 1]

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

  /**
   * Convert line type to SVG stroke-dasharray.
   */
  private String getDashArray(String type) {
    return switch (type) {
      case 'dashed' -> '8,4'
      case 'dotted' -> '2,2'
      case 'dotdash' -> '2,2,8,2'
      case 'longdash' -> '12,4'
      case 'twodash' -> '4,2,8,2'
      case 'solid' -> null
      default -> null
    }
  }
}
