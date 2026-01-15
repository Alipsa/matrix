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
import se.alipsa.matrix.gg.geom.Point

/**
 * Polygon geometry for closed filled shapes.
 * Unlike geom_path which draws an open path, geom_polygon automatically
 * closes the path and fills it. Useful for drawing custom shapes,
 * regions, and map polygons.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: fill, color, size/linewidth, linetype, alpha, group
 *
 * Usage:
 * - geom_polygon() - basic filled polygon
 * - geom_polygon(fill: 'blue', color: 'black', linewidth: 2) - styled polygon
 */
@CompileStatic
class GeomPolygon extends Geom {

  /** Fill color */
  String fill = 'gray'

  /** Border color */
  String color = 'black'

  /** Border width */
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Line end style (butt, round, square) */
  String lineend = 'butt'

  /** Line join style (round, mitre, bevel) */
  String linejoin = 'round'

  GeomPolygon() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomPolygon(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
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
    if (data == null || data.rowCount() < 3) return  // Need at least 3 points for a polygon

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.fillColName
    String fillCol = aes.fillColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomPolygon requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale alphaScale = scales['alpha']

    // Group data if a group aesthetic is specified
    Map<Object, List<Map>> groups = data.rows()
        .groupBy { row -> groupCol ? row[groupCol] : '__all__' }
        .collectEntries { groupKey, rows ->
          [(groupKey): rows.collect { it.toMap() }]
        } as Map<Object, List<Map>>

    // Render each group as a separate polygon
    groups.each { groupKey, rows ->
      renderPolygon(group, rows, xCol, yCol, fillCol, colorCol, sizeCol, alphaCol, groupKey,
                   xScale, yScale, fillScale, colorScale, sizeScale, alphaScale, aes)
    }
  }

  private void renderPolygon(G group, List<Map> rows, String xCol, String yCol,
                            String fillCol, String colorCol, String sizeCol, String alphaCol, Object groupKey,
                            Scale xScale, Scale yScale, Scale fillScale, Scale colorScale,
                            Scale sizeScale, Scale alphaScale, Aes aes) {
    // DO NOT sort - preserve data order
    List<Point> points = rows.collect { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return null

      // Transform using scales
      BigDecimal xPx = xScale?.transform(xVal) as BigDecimal
      BigDecimal yPx = yScale?.transform(yVal) as BigDecimal

      if (xPx == null || yPx == null) return null

      new Point(xPx, yPx)
    }.findAll()

    if (points.size() < 3) return  // Need at least 3 points

    // Determine fill color
    String polygonFill = this.fill
    if (fillCol && groupKey != '__all__') {
      if (fillScale) {
        polygonFill = fillScale.transform(groupKey)?.toString() ?: this.fill
      } else {
        polygonFill = GeomUtils.getDefaultColor(groupKey)
      }
    } else if (aes.fill instanceof Identity) {
      polygonFill = (aes.fill as Identity).value.toString()
    }
    polygonFill = ColorUtil.normalizeColor(polygonFill) ?: polygonFill

    // Determine border color
    String borderColor = this.color
    if (colorCol && groupKey != '__all__') {
      if (colorScale) {
        borderColor = colorScale.transform(groupKey)?.toString() ?: this.color
      } else {
        borderColor = GeomUtils.getDefaultColor(groupKey)
      }
    } else if (aes.color instanceof Identity) {
      borderColor = (aes.color as Identity).value.toString()
    }
    borderColor = ColorUtil.normalizeColor(borderColor) ?: borderColor

    BigDecimal lineSize = GeomUtils.extractLineSize(this.size, aes, sizeCol, rows, sizeScale)
    BigDecimal polygonAlpha = GeomUtils.extractLineAlpha(this.alpha, aes, alphaCol, rows, alphaScale)

    // Build SVG path (closed polygon)
    StringBuilder d = new StringBuilder()
    Point first = points[0]
    d << "M ${first.x} ${first.y}"

    for (int i = 1; i < points.size(); i++) {
      Point pt = points[i]
      d << " L ${pt.x} ${pt.y}"
    }
    d << " Z"  // Close the path

    // Create polygon element
    def path = group.addPath()
        .d(d.toString())
        .fill(polygonFill)
        .stroke(borderColor)

    path.addAttribute('stroke-width', lineSize)
    path.addAttribute('stroke-linecap', lineend)
    path.addAttribute('stroke-linejoin', linejoin)

    // Apply line type
    String dashArray = GeomUtils.getDashArray(linetype)
    if (dashArray) {
      path.addAttribute('stroke-dasharray', dashArray)
    }

    // Apply alpha
    if (polygonAlpha < 1.0) {
      path.addAttribute('opacity', polygonAlpha)
    }
  }
}
