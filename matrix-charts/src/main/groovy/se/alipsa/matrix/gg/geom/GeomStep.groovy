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
 * Step geometry for step plots (staircase pattern).
 * Creates a step function plot where horizontal segments connect to vertical segments.
 * Useful for visualizing piecewise constant functions or cumulative distributions.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: color, size/linewidth, linetype, alpha, group
 *
 * The 'direction' parameter controls where the step occurs:
 * - 'hv' (default): horizontal then vertical (step happens at the end)
 * - 'vh': vertical then horizontal (step happens at the start)
 * - 'mid': step happens at the midpoint between x values
 *
 * Usage:
 * - geom_step() - default step plot (hv direction)
 * - geom_step(direction: 'vh') - vertical first
 * - geom_step(color: 'blue', linewidth: 2) - styled step
 */
@CompileStatic
class GeomStep extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Step direction: 'hv' (horizontal-vertical), 'vh' (vertical-horizontal), 'mid' (midpoint) */
  String direction = 'hv'

  GeomStep() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomStep(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size != null) this.size = params.size as Number
    if (params.linewidth != null) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.direction) this.direction = params.direction as String
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomStep requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    // Group data if a group aesthetic is specified
    Map<Object, List<Map>> groups = [:]
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

    // Render each group as a separate step line
    groups.each { groupKey, rows ->
      renderStep(group, rows, xCol, yCol, colorCol, groupKey,
                 xScale, yScale, colorScale, aes)
    }
  }

  private void renderStep(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale, Aes aes) {
    // Sort rows by x value
    List<Map> sortedRows = sortRowsByX(rows, xCol)

    // Collect transformed points
    List<double[]> points = []
    sortedRows.each { row ->
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

    // Build step path based on direction
    StringBuilder d = new StringBuilder()
    double[] first = points[0]
    d << "M ${first[0]} ${first[1]}"

    for (int i = 1; i < points.size(); i++) {
      double[] prev = points[i - 1]
      double[] curr = points[i]

      switch (direction.toLowerCase()) {
        case 'vh':
          // Vertical first, then horizontal
          d << " L ${prev[0]} ${curr[1]}"
          d << " L ${curr[0]} ${curr[1]}"
          break
        case 'mid':
          // Step at midpoint
          double midX = (prev[0] + curr[0]) / 2
          d << " L ${midX} ${prev[1]}"
          d << " L ${midX} ${curr[1]}"
          d << " L ${curr[0]} ${curr[1]}"
          break
        case 'hv':
        default:
          // Horizontal first, then vertical (default)
          d << " L ${curr[0]} ${prev[1]}"
          d << " L ${curr[0]} ${curr[1]}"
          break
      }
    }

    // Create path element
    def path = group.addPath()
        .d(d.toString())
        .fill('none')
        .stroke(lineColor)

    path.addAttribute('stroke-width', size)

    // Apply line type
    String dashArray = getDashArray(linetype)
    if (dashArray) {
      path.addAttribute('stroke-dasharray', dashArray)
    }

    // Apply alpha
    if ((alpha as double) < 1.0) {
      path.addAttribute('stroke-opacity', alpha)
    }
  }

  /**
   * Sort rows by x value for proper step rendering.
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
