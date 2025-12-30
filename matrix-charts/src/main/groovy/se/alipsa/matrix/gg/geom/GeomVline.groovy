package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Vertical line geometry for drawing reference lines across the plot.
 *
 * Usage:
 * - geom_vline(xintercept: 5) - single vertical line at x=5
 * - geom_vline(xintercept: [2, 4, 6]) - multiple vertical lines
 * - With data mapping: aes(xintercept: 'threshold_column')
 */
@CompileStatic
class GeomVline extends Geom {

  /** X-intercept value(s) for the vertical line(s) */
  def xintercept

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomVline() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // xintercept can be specified as parameter
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomVline(Map params) {
    this()
    if (params.xintercept != null) this.xintercept = params.xintercept
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    Scale xScale = scales['x']
    if (xScale == null) return

    // Get plot height from the group's parent context (use default if not available)
    int plotHeight = 480  // default

    // Collect x-intercept values
    List<Number> xValues = []

    // From parameter
    if (xintercept != null) {
      if (xintercept instanceof List) {
        xValues.addAll((xintercept as List).findAll { it instanceof Number } as List<Number>)
      } else if (xintercept instanceof Number) {
        xValues << (xintercept as Number)
      }
    }

    // From data mapping (if aes has xintercept)
    if (data != null && aes?.x != null) {
      String xCol = aes.xColName
      if (xCol && data.columnNames().contains(xCol)) {
        data[xCol].each { val ->
          if (val instanceof Number) {
            xValues << (val as Number)
          }
        }
      }
    }

    if (xValues.isEmpty()) return

    // Draw vertical lines
    xValues.unique().each { Number xVal ->
      def xPx = xScale.transform(xVal)
      if (xPx == null) return

      double x = xPx as double

      def line = group.addLine()
          .x1(x as int)
          .y1(0)
          .x2(x as int)
          .y2(plotHeight)
          .stroke(color)

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
