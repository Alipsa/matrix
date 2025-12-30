package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Rug geometry for showing marginal distributions as tick marks along axes.
 *
 * Usage:
 * - geom_rug() - rug marks on both x and y axes
 * - geom_rug(sides: 'b') - only bottom (x-axis)
 * - geom_rug(sides: 'l') - only left (y-axis)
 * - geom_rug(sides: 'bl') - both bottom and left
 */
@CompileStatic
class GeomRug extends Geom {

  /** Which sides to draw: 't'=top, 'b'=bottom, 'l'=left, 'r'=right */
  String sides = 'bl'

  /** Rug mark color */
  String color = 'black'

  /** Rug mark length (in pixels) */
  Number length = 10

  /** Line width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 0.5

  /** Position outside plot area */
  boolean outside = false

  GeomRug() {
    defaultStat = StatType.IDENTITY
    requiredAes = []  // x and/or y depending on sides
    defaultAes = [color: 'black', alpha: 0.5] as Map<String, Object>
  }

  GeomRug(Map params) {
    this()
    if (params.sides) this.sides = params.sides as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.length != null) this.length = params.length as Number
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.outside != null) this.outside = params.outside as boolean
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String colorCol = aes.colorColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    // Default plot dimensions
    int plotWidth = 640
    int plotHeight = 480

    double rugLength = length as double

    // Collect x values for bottom/top rugs
    if ((sides.contains('b') || sides.contains('t')) && xCol != null && xScale != null) {
      data.each { row ->
        def xVal = row[xCol]
        if (xVal == null) return

        def xTransformed = xScale.transform(xVal)
        if (xTransformed == null) return

        double xPx = xTransformed as double

        // Determine color
        String rugColor = getRugColor(row, colorCol, colorScale)

        // Bottom rug
        if (sides.contains('b')) {
          double y1 = outside ? (plotHeight as double) : (plotHeight - rugLength) as double
          double y2 = plotHeight as double
          drawRugMark(group, xPx, y1, xPx, y2, rugColor)
        }

        // Top rug
        if (sides.contains('t')) {
          double y1 = 0
          double y2 = outside ? 0 : rugLength
          drawRugMark(group, xPx, y1, xPx, y2, rugColor)
        }
      }
    }

    // Collect y values for left/right rugs
    if ((sides.contains('l') || sides.contains('r')) && yCol != null && yScale != null) {
      data.each { row ->
        def yVal = row[yCol]
        if (yVal == null) return

        def yTransformed = yScale.transform(yVal)
        if (yTransformed == null) return

        double yPx = yTransformed as double

        // Determine color
        String rugColor = getRugColor(row, colorCol, colorScale)

        // Left rug
        if (sides.contains('l')) {
          double x1 = outside ? 0 : 0
          double x2 = rugLength
          drawRugMark(group, x1, yPx, x2, yPx, rugColor)
        }

        // Right rug
        if (sides.contains('r')) {
          double x1 = plotWidth - rugLength
          double x2 = plotWidth
          drawRugMark(group, x1, yPx, x2, yPx, rugColor)
        }
      }
    }
  }

  private String getRugColor(def row, String colorCol, Scale colorScale) {
    if (colorCol && row[colorCol] != null) {
      if (colorScale) {
        return colorScale.transform(row[colorCol])?.toString() ?: this.color
      }
    }
    return this.color
  }

  private void drawRugMark(G group, double x1, double y1, double x2, double y2, String rugColor) {
    def line = group.addLine()
        .x1(x1 as int)
        .y1(y1 as int)
        .x2(x2 as int)
        .y2(y2 as int)
        .stroke(rugColor)

    line.addAttribute('stroke-width', linewidth)

    if ((alpha as double) < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }
  }
}
