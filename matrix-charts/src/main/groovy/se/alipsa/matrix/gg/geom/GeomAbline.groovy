package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Arbitrary line geometry defined by slope and intercept (y = slope * x + intercept).
 *
 * Usage:
 * - geom_abline(slope: 1, intercept: 0) - diagonal line through origin
 * - geom_abline(slope: 2, intercept: 5) - line with slope 2, y-intercept 5
 * - With data mapping: aes(slope: 'slope_col', intercept: 'intercept_col')
 */
@CompileStatic
class GeomAbline extends Geom {

  /** Slope of the line */
  Number slope = 1

  /** Y-intercept of the line */
  Number intercept = 0

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomAbline() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // slope/intercept can be specified as parameters
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomAbline(Map params) {
    this()
    if (params.slope != null) this.slope = params.slope as Number
    if (params.intercept != null) this.intercept = params.intercept as Number
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    // Get plot dimensions (use defaults if not available)
    int plotWidth = 640
    int plotHeight = 480

    // Collect slope/intercept pairs
    List<Map<String, Number>> lines = []

    // From parameters
    if (slope != null && intercept != null) {
      lines << [slope: slope, intercept: intercept]
    }

    // From data mapping (if data contains slope/intercept columns)
    if (data != null && data.rowCount() > 0) {
      List<String> colNames = data.columnNames()
      if (colNames.contains('slope') && colNames.contains('intercept')) {
        for (int i = 0; i < data.rowCount(); i++) {
          def s = data['slope'][i]
          def ic = data['intercept'][i]
          if (s instanceof Number && ic instanceof Number) {
            lines << [slope: s as Number, intercept: ic as Number]
          }
        }
      }
    }

    if (lines.isEmpty()) return

    // Get the x-domain to determine line extent
    List<Number> xDomain = getScaleDomain(xScale)
    if (xDomain == null || xDomain.size() < 2) return

    double xMin = xDomain[0] as double
    double xMax = xDomain[1] as double

    // Draw lines
    lines.unique().each { Map<String, Number> lineParams ->
      double s = lineParams.slope as double
      double ic = lineParams.intercept as double

      // Calculate y values at x domain boundaries
      double y1 = s * xMin + ic
      double y2 = s * xMax + ic

      // Transform to pixel coordinates
      def x1Px = xScale.transform(xMin)
      def x2Px = xScale.transform(xMax)
      def y1Px = yScale.transform(y1)
      def y2Px = yScale.transform(y2)

      if (x1Px == null || x2Px == null || y1Px == null || y2Px == null) return

      String lineColor = ColorUtil.normalizeColor(color) ?: color
      def line = group.addLine()
          .x1(x1Px as int)
          .y1(y1Px as int)
          .x2(x2Px as int)
          .y2(y2Px as int)
          .stroke(lineColor)

      line.addAttribute('stroke-width', linewidth)

      // Apply line type
      String dashArray = getLineDashArray(linetype)
      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }

      // Apply alpha
      if ((alpha as double) < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
    }
  }

  /**
   * Get the domain from a scale.
   */
  private List<Number> getScaleDomain(Scale scale) {
    // Try to get domain from scale
    if (scale.domain != null && !scale.domain.isEmpty()) {
      List<Number> result = []
      scale.domain.each { val ->
        if (val instanceof Number) {
          result << (val as Number)
        }
      }
      if (result.size() >= 2) {
        return [result.min(), result.max()]
      }
    }
    return null
  }

  /**
   * Convert line type name to SVG stroke-dasharray value.
   */
  private String getLineDashArray(String type) {
    switch (type?.toLowerCase()) {
      case 'dashed': return '5,5'
      case 'dotted': return '2,2'
      case 'longdash': return '10,5'
      case 'twodash': return '10,5,2,5'
      case 'solid':
      default: return null
    }
  }
}
