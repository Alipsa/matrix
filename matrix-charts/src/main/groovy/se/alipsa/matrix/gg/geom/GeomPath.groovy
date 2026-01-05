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
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

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
    Map<Object, List<Map>> groups = new LinkedHashMap<>()  // Preserve insertion order
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

    // Render each group as a separate path
    groups.each { groupKey, rows ->
      renderPath(group, rows, xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
                 xScale, yScale, colorScale, sizeScale, alphaScale, aes)
    }
  }

  private void renderPath(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, String sizeCol, String alphaCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale,
                          Scale sizeScale, Scale alphaScale, Aes aes) {
    // DO NOT sort - preserve data order (this is the key difference from geom_line)
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

    if (points.size() < 2) return

    // Determine line color
    String lineColor = this.color
    if (colorCol && groupKey != '__all__') {
      if (colorScale) {
        lineColor = colorScale.transform(groupKey)?.toString() ?: this.color
      } else {
        lineColor = getDefaultColor(groupKey)
      }
    } else if (aes.color instanceof Identity) {
      lineColor = (aes.color as Identity).value.toString()
    }
    lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor

    Number lineSize = this.size
    if (aes.size instanceof Identity) {
      lineSize = (aes.size as Identity).value as Number
    } else if (sizeCol && !rows.isEmpty() && rows[0][sizeCol] != null) {
      def rawSize = rows[0][sizeCol]
      if (sizeScale) {
        def scaled = sizeScale.transform(rawSize)
        if (scaled instanceof Number) {
          lineSize = scaled as Number
        }
      } else if (rawSize instanceof Number) {
        lineSize = rawSize as Number
      }
    }

    Number lineAlpha = this.alpha
    if (aes.alpha instanceof Identity) {
      lineAlpha = (aes.alpha as Identity).value as Number
    } else if (alphaCol && !rows.isEmpty() && rows[0][alphaCol] != null) {
      def rawAlpha = rows[0][alphaCol]
      if (alphaScale) {
        def scaled = alphaScale.transform(rawAlpha)
        if (scaled instanceof Number) {
          lineAlpha = scaled as Number
        }
      } else if (rawAlpha instanceof Number) {
        lineAlpha = rawAlpha as Number
      }
    }

    // Build SVG path
    StringBuilder d = new StringBuilder()
    double[] first = points[0]
    d << "M ${first[0]} ${first[1]}"

    for (int i = 1; i < points.size(); i++) {
      double[] pt = points[i]
      d << " L ${pt[0]} ${pt[1]}"
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
    String dashArray = getDashArray(linetype)
    if (dashArray) {
      path.addAttribute('stroke-dasharray', dashArray)
    }

    // Apply alpha
    if ((lineAlpha as double) < 1.0) {
      path.addAttribute('stroke-opacity', lineAlpha)
    }
  }

  /**
   * Convert line type to SVG stroke-dasharray.
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

  /**
   * Get a default color from a discrete palette based on a value.
   */
  private String getDefaultColor(Object value) {
    List<String> palette = [
        '#F8766D', '#C49A00', '#53B400',
        '#00C094', '#00B6EB', '#A58AFF',
        '#FB61D7'
    ]

    int index = Math.abs(value.hashCode()) % palette.size()
    return palette[index]
  }
}
