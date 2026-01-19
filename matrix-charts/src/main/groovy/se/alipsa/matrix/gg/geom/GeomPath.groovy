package se.alipsa.matrix.gg.geom

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
import se.alipsa.matrix.gg.geom.Point

/**
 * Path geometry for connecting observations in data order.
 * Unlike geom_line which connects points sorted by x value,
 * geom_path connects points in the order they appear in the data.
 * This is useful for trajectories, polygons, and other ordered paths.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: color, size/linewidth, linetype, alpha, group
 *
 * Usage:
 * - geom_path() - basic path in data order
 * - geom_path(color: 'red', linewidth: 2) - styled path
 */
@CompileStatic
class GeomPath extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Line end style (butt, round, square) */
  String lineend = 'butt'

  /** Line join style (round, mitre, bevel) */
  String linejoin = 'round'

  GeomPath() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomPath(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.size = params.linewidth as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.lineend = params.lineend as String ?: this.lineend
    this.linejoin = params.linejoin as String ?: this.linejoin
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomPath requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale alphaScale = scales['alpha']

    // Group data if a group aesthetic is specified
    Map<Object, List<Map>> groups = data.rows()
        .groupBy { row -> groupCol ? row[groupCol] : '__all__' }
        .collectEntries { groupKey, rows ->
          [(groupKey): rows.collect { it.toMap() }]
        } as Map<Object, List<Map>>

    // Render each group as a separate path
    int elementIndex = 0
    groups.each { groupKey, rows ->
      renderPath(group, rows, xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
                 xScale, yScale, colorScale, sizeScale, alphaScale, aes, ctx, elementIndex)
      elementIndex++
    }
  }

  private void renderPath(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, String sizeCol, String alphaCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale,
                          Scale sizeScale, Scale alphaScale, Aes aes, RenderContext ctx, int elementIndex) {
    // DO NOT sort - preserve data order (this is the key difference from geom_line)
    List<Point> points = rows.collect { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return null

      // Transform using scales
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      if (xTransformed == null || yTransformed == null) return null

      BigDecimal xPx = xTransformed as BigDecimal
      BigDecimal yPx = yTransformed as BigDecimal

      new Point(xPx, yPx)
    }.findAll()

    if (points.size() < 2) return

    // Determine line color
    String lineColor = this.color
    if (colorCol && groupKey != '__all__') {
      if (colorScale) {
        lineColor = colorScale.transform(groupKey)?.toString() ?: this.color
      } else {
        lineColor = GeomUtils.getDefaultColor(groupKey)
      }
    } else if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    BigDecimal lineSize = GeomUtils.extractLineSize(this.size, aes, sizeCol, rows, sizeScale)
    BigDecimal lineAlpha = GeomUtils.extractLineAlpha(this.alpha, aes, alphaCol, rows, alphaScale)

    // Build SVG path
    StringBuilder d = new StringBuilder()
    Point first = points[0]
    d << "M ${first.x} ${first.y}"

    for (int i = 1; i < points.size(); i++) {
      Point pt = points[i]
      d << " L ${pt.x} ${pt.y}"
    }

    // Create path element
    def path = group.addPath()
        .d(d.toString())
        .fill('none')
        .stroke(lineColor)

    path.addAttribute('stroke-width', lineSize)
    path.addAttribute('stroke-linecap', lineend)
    path.addAttribute('stroke-linejoin', linejoin)

    // Apply line type
    String dashArray = GeomUtils.getDashArray(linetype)
    if (dashArray) {
      path.addAttribute('stroke-dasharray', dashArray)
    }

    // Apply alpha
    if (lineAlpha < 1.0) {
      path.addAttribute('stroke-opacity', lineAlpha)
    }

    // Apply CSS attributes
    GeomUtils.applyAttributes(path, ctx, 'path', 'gg-path', elementIndex)
  }
}
