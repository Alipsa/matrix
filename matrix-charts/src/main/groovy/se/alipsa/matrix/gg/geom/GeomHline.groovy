package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Horizontal line geometry for drawing reference lines across the plot.
 *
 * Usage:
 * - geom_hline(yintercept: 5) - single horizontal line at y=5
 * - geom_hline(yintercept: [2, 4, 6]) - multiple horizontal lines
 * - With data mapping: aes(yintercept: 'threshold_column')
 */
@CompileStatic
class GeomHline extends Geom {

  /** Y-intercept value(s) for the horizontal line(s) */
  def yintercept

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  GeomHline() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // yintercept can be specified as parameter
    defaultAes = [color: 'black', linewidth: 1, alpha: 1.0] as Map<String, Object>
  }

  GeomHline(Map params) {
    this()
    if (params.yintercept != null) this.yintercept = params.yintercept
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
    Scale yScale = scales['y']
    if (yScale == null) return

    // Get plot width from the group's parent context (use default if not available)
    int plotWidth = 640  // default

    // Collect y-intercept values
    List<Number> yValues = []

    // From parameter
    if (yintercept != null) {
      if (yintercept instanceof List) {
        yValues.addAll((yintercept as List).findAll { it instanceof Number } as List<Number>)
      } else if (yintercept instanceof Number) {
        yValues << (yintercept as Number)
      }
    }

    // From data mapping (if aes has yintercept)
    if (data != null && aes?.y != null) {
      String yCol = aes.yColName
      if (yCol && data.columnNames().contains(yCol)) {
        data[yCol].each { val ->
          if (val instanceof Number) {
            yValues << (val as Number)
          }
        }
      }
    }

    if (yValues.isEmpty()) return

    // Draw horizontal lines
    yValues.unique().each { Number yVal ->
      def yPx = yScale.transform(yVal)
      if (yPx == null) return

      double y = yPx as double

      def line = group.addLine()
          .x1(0)
          .y1(y as int)
          .x2(plotWidth)
          .y2(y as int)
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
