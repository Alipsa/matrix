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
import se.alipsa.matrix.gg.render.RenderContext

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
  BigDecimal size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Step direction: 'hv' (horizontal-vertical), 'vh' (vertical-horizontal), 'mid' (midpoint) */
  String direction = 'hv'

  GeomStep() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomStep(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    if (params.linewidth != null) this.size = params.linewidth as BigDecimal
    this.linetype = params.linetype as String ?: this.linetype
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.direction = params.direction as String ?: this.direction
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
      throw new IllegalArgumentException("GeomStep requires x and y aesthetics")
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

    // Render each group as a separate step line
    int elementIndex = 0
    groups.each { groupKey, rows ->
      renderStep(group, rows, xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
                 xScale, yScale, colorScale, sizeScale, alphaScale, aes, ctx, elementIndex)
      elementIndex++
    }
  }

  private void renderStep(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, String sizeCol, String alphaCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale,
                          Scale sizeScale, Scale alphaScale, Aes aes,
                          RenderContext ctx, int elementIndex) {
    // Sort rows by x value
    List<Map> sortedRows = sortRowsByX(rows, xCol)

    // Collect transformed points
    List<Point> points = []
    sortedRows.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform using scales
      BigDecimal xPx = xScale?.transform(xVal) as BigDecimal
      BigDecimal yPx = yScale?.transform(yVal) as BigDecimal

      if (xPx == null || yPx == null) return

      points << new Point(xPx, yPx)
    }

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

    BigDecimal lineSize = GeomUtils.extractLineSize(this.size, aes, sizeCol, sortedRows, sizeScale)
    BigDecimal lineAlpha = GeomUtils.extractLineAlpha(this.alpha, aes, alphaCol, sortedRows, alphaScale)

    // Build step path based on direction
    StringBuilder d = new StringBuilder()
    Point first = points[0]
    d << "M ${first.x} ${first.y}"

    for (int i = 1; i < points.size(); i++) {
      Point prev = points[i - 1]
      Point curr = points[i]

      switch (direction.toLowerCase()) {
        case 'vh':
          // Vertical first, then horizontal
          d << " L ${prev.x} ${curr.y}"
          d << " L ${curr.x} ${curr.y}"
          break
        case 'mid':
          // Step at midpoint
          BigDecimal midX = (prev.x as BigDecimal + curr.x as BigDecimal) / 2
          d << " L ${midX} ${prev.y}"
          d << " L ${midX} ${curr.y}"
          d << " L ${curr.x} ${curr.y}"
          break
        case 'hv':
        default:
          // Horizontal first, then vertical (default)
          d << " L ${curr.x} ${prev.y}"
          d << " L ${curr.x} ${curr.y}"
          break
      }
    }

    // Create path element
    def path = group.addPath()
        .d(d.toString())
        .fill('none')
        .stroke(lineColor)

    path.addAttribute('stroke-width', lineSize)

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
    GeomUtils.applyAttributes(path, ctx, 'step', 'gg-step', elementIndex)
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
}
