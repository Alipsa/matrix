package se.alipsa.matrix.gg.geom

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
 * Q-Q plot points.
 * Uses stat_qq to compute theoretical quantiles for comparison.
 */
@CompileStatic
class GeomQq extends Geom {

  /** Default point color */
  String color = 'black'

  /** Default point fill */
  String fill = 'black'

  /** Default point size (radius) */
  Number size = 3

  /** Default point shape */
  String shape = 'circle'

  /** Default alpha (transparency) */
  Number alpha = 1.0

  /**
   * Create a Q-Q point geom with default settings.
   */
  GeomQq() {
    defaultStat = StatType.QQ
    requiredAes = ['x']
    defaultAes = [color: 'black', size: 3, alpha: 1.0] as Map<String, Object>
  }

  /**
   * Create a Q-Q point geom with parameters.
   *
   * @param params geom parameters
   */
  GeomQq(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.size) this.size = params.size as Number
    if (params.shape) this.shape = params.shape
    if (params.alpha) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = data.columnNames().contains('x') ? 'x' : aes.xColName
    String yCol = data.columnNames().contains('y') ? 'y' : aes.yColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String shapeCol = aes.shape instanceof String ? aes.shape as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomQq requires stat_qq output with x and y columns")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale shapeScale = scales['shape']
    Scale alphaScale = scales['alpha']

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)
      if (xTransformed == null || yTransformed == null) return

      double xPx = xTransformed as double
      double yPx = yTransformed as double

      String pointColor = this.color
      if (colorCol && row[colorCol] != null) {
        if (colorScale) {
          pointColor = colorScale.transform(row[colorCol])?.toString() ?: this.color
        } else {
          pointColor = GeomUtils.getDefaultColor(row[colorCol])
        }
      } else if (aes.color instanceof Identity) {
        pointColor = (aes.color as Identity).value.toString()
      }
      pointColor = ColorUtil.normalizeColor(pointColor) ?: pointColor

      Number pointSize = GeomUtils.extractPointSize(this.size, aes, sizeCol, row.toMap(), sizeScale)

      String pointShape = this.shape
      if (shapeCol && row[shapeCol] != null) {
        pointShape = shapeScale?.transform(row[shapeCol])?.toString() ?: row[shapeCol].toString()
      } else if (aes.shape instanceof Identity) {
        pointShape = (aes.shape as Identity).value.toString()
      }

      Number pointAlpha = GeomUtils.extractPointAlpha(this.alpha, aes, alphaCol, row.toMap(), alphaScale)

      GeomUtils.drawPoint(group, xPx, yPx, (pointSize as Number).doubleValue(), pointColor, pointShape,
          (pointAlpha as Number).doubleValue())
    }
  }
}
