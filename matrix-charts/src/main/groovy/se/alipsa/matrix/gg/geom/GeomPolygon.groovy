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
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

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
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size != null) this.size = params.size as Number
    if (params.linewidth != null) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.lineend) this.lineend = params.lineend as String
    if (params.linejoin) this.linejoin = params.linejoin as String
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
    Map<Object, List<Map>> groups = new LinkedHashMap<>()  // Preserve insertion order
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

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
    List<double[]> points = []
    rows.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform using scales
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      if (xTransformed == null || yTransformed == null) return

      double xPx = xTransformed as double
      double yPx = yTransformed as double

      points << ([xPx, yPx] as double[])
    }

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

    Number lineSize = GeomUtils.extractLineSize(this.size, aes, sizeCol, rows, sizeScale)
    Number polygonAlpha = GeomUtils.extractLineAlpha(this.alpha, aes, alphaCol, rows, alphaScale)

    // Build SVG path (closed polygon)
    StringBuilder d = new StringBuilder()
    double[] first = points[0]
    d << "M ${first[0]} ${first[1]}"

    for (int i = 1; i < points.size(); i++) {
      double[] pt = points[i]
      d << " L ${pt[0]} ${pt[1]}"
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
    if ((polygonAlpha as double) < 1.0) {
      path.addAttribute('opacity', polygonAlpha)
    }
  }
}
