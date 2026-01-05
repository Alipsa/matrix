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
 * Ribbon geometry for displaying confidence bands or ranges.
 * Draws a filled area between ymin and ymax values.
 *
 * Required aesthetics: x, ymin, ymax
 * Optional aesthetics: fill, color, alpha, linetype, linewidth
 *
 * Usage:
 * - geom_ribbon(aes(ymin: 'lower', ymax: 'upper')) - basic ribbon
 * - geom_ribbon(fill: 'blue', alpha: 0.3) - semi-transparent blue ribbon
 */
@CompileStatic
class GeomRibbon extends Geom {

  /** Fill color for the ribbon */
  String fill = 'gray'

  /** Stroke color for the outline */
  String color = null

  /** Line width for outline (0 for no outline) */
  Number linewidth = 0

  /** Alpha transparency (0-1) */
  Number alpha = 0.5

  /** Line type for outline */
  String linetype = 'solid'

  GeomRibbon() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'ymin', 'ymax']
    defaultAes = [fill: 'gray', alpha: 0.5] as Map<String, Object>
  }

  GeomRibbon(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yminCol = params?.get('ymin')?.toString() ?: 'ymin'
    String ymaxCol = params?.get('ymax')?.toString() ?: 'ymax'
    String groupCol = aes.groupColName ?: aes.fillColName
    String fillCol = aes.fillColName

    if (xCol == null) {
      throw new IllegalArgumentException("GeomRibbon requires x aesthetic")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Group data if a group aesthetic is specified
    Map<Object, List<Map>> groups = [:]
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

    // Render each group as a separate ribbon
    groups.each { groupKey, rows ->
      renderRibbon(group, rows, xCol, yminCol, ymaxCol, fillCol, groupKey,
                   xScale, yScale, fillScale, aes)
    }
  }

  private void renderRibbon(G group, List<Map> rows, String xCol, String yminCol, String ymaxCol,
                            String fillCol, Object groupKey,
                            Scale xScale, Scale yScale, Scale fillScale, Aes aes) {
    // Sort rows by x value
    List<Map> sortedRows = sortRowsByX(rows, xCol)

    // Collect transformed points for upper and lower bounds
    List<double[]> upperPoints = []
    List<double[]> lowerPoints = []

    sortedRows.each { row ->
      def xVal = row[xCol]
      def yminVal = row[yminCol]
      def ymaxVal = row[ymaxCol]

      if (xVal == null || yminVal == null || ymaxVal == null) return

      // Transform using scales
      def xTransformed = xScale?.transform(xVal)
      def yminTransformed = yScale?.transform(yminVal)
      def ymaxTransformed = yScale?.transform(ymaxVal)

      if (xTransformed == null || yminTransformed == null || ymaxTransformed == null) return

      double xPx = xTransformed as double
      double yminPx = yminTransformed as double
      double ymaxPx = ymaxTransformed as double

      upperPoints << ([xPx, ymaxPx] as double[])
      lowerPoints << ([xPx, yminPx] as double[])
    }

    if (upperPoints.size() < 2) return

    // Determine fill color
    String ribbonFill = this.fill
    if (fillCol && groupKey != '__all__') {
      if (fillScale) {
        ribbonFill = fillScale.transform(groupKey)?.toString() ?: this.fill
      } else {
        ribbonFill = getDefaultColor(groupKey)
      }
    } else if (aes.fill instanceof Identity) {
      ribbonFill = (aes.fill as Identity).value.toString()
    }
    ribbonFill = ColorUtil.normalizeColor(ribbonFill) ?: ribbonFill

    // Build SVG path: go along upper bound, then back along lower bound
    StringBuilder d = new StringBuilder()

    // Start at first upper point
    double[] firstUpper = upperPoints[0]
    d << "M ${firstUpper[0]} ${firstUpper[1]}"

    // Line to each subsequent upper point
    for (int i = 1; i < upperPoints.size(); i++) {
      double[] pt = upperPoints[i]
      d << " L ${pt[0]} ${pt[1]}"
    }

    // Line to last lower point and back along lower bound
    for (int i = lowerPoints.size() - 1; i >= 0; i--) {
      double[] pt = lowerPoints[i]
      d << " L ${pt[0]} ${pt[1]}"
    }

    // Close path
    d << " Z"

    // Create filled ribbon
    def path = group.addPath().d(d.toString())
        .fill(ribbonFill)

    // Apply alpha
    if ((alpha as double) < 1.0) {
      path.addAttribute('fill-opacity', alpha)
    }

    // Apply stroke if color is specified and linewidth > 0
    if (color != null && (linewidth as double) > 0) {
      String strokeColor = ColorUtil.normalizeColor(color) ?: color
      path.stroke(strokeColor)
      path.addAttribute('stroke-width', linewidth)

      String dashArray = getDashArray(linetype)
      if (dashArray) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
    } else {
      path.stroke('none')
    }
  }

  /**
   * Sort rows by x value for proper ribbon rendering.
   */
  private List<Map> sortRowsByX(List<Map> rows, String xCol) {
    return rows.sort { a, b ->
      def xA = a[xCol]
      def xB = b[xCol]

      if (xA instanceof Number && xB instanceof Number) {
        return (xA as Number) <=> (xB as Number)
      }

      if (xA instanceof Comparable && xB instanceof Comparable) {
        return (xA as Comparable) <=> (xB as Comparable)
      }

      return xA?.toString() <=> xB?.toString()
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
   * Get a default color from a discrete palette.
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
