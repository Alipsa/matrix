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
 * Area geometry for area charts.
 * Like geom_line but fills the area under the line.
 *
 * Usage:
 * - geom_area() - basic area chart
 * - geom_area(fill: 'blue', alpha: 0.5) - semi-transparent blue area
 * - With grouping: aes(fill: 'category') for stacked areas
 */
@CompileStatic
class GeomArea extends Geom {

  /** Fill color for the area */
  String fill = 'gray'

  /** Stroke color for the outline */
  String color = 'black'

  /** Line width for outline (0 for no outline) */
  BigDecimal linewidth = 0.5

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 0.7

  /** Line type for outline */
  String linetype = 'solid'

  /** Position adjustment: 'identity', 'stack', 'fill' */
  String position = 'stack'

  GeomArea() {
    defaultStat = StatType.ALIGN
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomArea(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.linewidth = (params.linewidth ?: params.size) as BigDecimal ?: this.linewidth
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.linetype = params.linetype as String ?: this.linetype
    this.position = params.position as String ?: this.position
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.fillColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomArea requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Get baseline y position (typically y=0 or bottom of plot)
    BigDecimal baselineY = getBaselineY(yScale)

    // Group data if a group aesthetic is specified
    def groups = data.rows().groupBy { row ->
      groupCol ? row[groupCol] : '__all__'
    }

    // Render each group as a separate area
    int elementIndex = 0
    groups.each { groupKey, rows ->
      renderArea(group, rows*.toMap(), xCol, yCol, fillCol, groupKey,
                 xScale, yScale, fillScale, baselineY, aes, null, elementIndex)
      elementIndex++
    }
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() < 2) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.fillColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomArea requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Get baseline y position (typically y=0 or bottom of plot)
    BigDecimal baselineY = getBaselineY(yScale)

    // Group data if a group aesthetic is specified
    def groups = data.rows().groupBy { row ->
      groupCol ? row[groupCol] : '__all__'
    }

    // Render each group as a separate area
    int elementIndex = 0
    groups.each { groupKey, rows ->
      renderArea(group, rows*.toMap(), xCol, yCol, fillCol, groupKey,
                 xScale, yScale, fillScale, baselineY, aes, ctx, elementIndex)
      elementIndex++
    }
  }

  private void renderArea(G group, List<Map> rows, String xCol, String yCol,
                          String fillCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale fillScale,
                          BigDecimal baselineY, Aes aes, RenderContext ctx, int elementIndex) {
    // Sort rows by x value
    List<Map> sortedRows = sortRowsByX(rows, xCol)

    // Collect transformed points
    List<Point> points = sortedRows.collect { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return null

      // Transform using scales
      Number xTransformed = xScale?.transform(xVal) as Number
      Number yTransformed = yScale?.transform(yVal) as Number

      if (xTransformed == null || yTransformed == null) return null

      new Point(xTransformed, yTransformed)
    }.findAll()  // Remove nulls

    if (points.size() < 2) return

    // Determine fill color
    String areaFill = this.fill
    if (fillCol && groupKey != '__all__') {
      if (fillScale) {
        areaFill = fillScale.transform(groupKey)?.toString() ?: this.fill
      } else {
        areaFill = getDefaultColor(groupKey)
      }
    } else if (aes.fill instanceof Identity) {
      areaFill = (aes.fill as Identity).value.toString()
    }
    areaFill = ColorUtil.normalizeColor(areaFill) ?: areaFill

    // Build SVG path
    StringBuilder d = new StringBuilder()

    // Start at first point
    Point firstPoint = points[0]
    d << "M ${firstPoint.x} ${firstPoint.y}"

    // Line to each subsequent point
    for (int i = 1; i < points.size(); i++) {
      Point pt = points[i]
      d << " L ${pt.x} ${pt.y}"
    }

    // Line down to baseline at last x
    Point lastPoint = points[points.size()-1]
    d << " L ${lastPoint.x} ${baselineY}"

    // Line along baseline back to first x
    d << " L ${firstPoint.x} ${baselineY}"

    // Close path
    d << " Z"

    // Create filled area
    def path = group.addPath().d(d.toString())
        .fill(areaFill)

    // Apply alpha
    if (alpha < 1.0) {
      path.addAttribute('fill-opacity', alpha)
    }

    // Apply stroke if linewidth > 0
    if (linewidth > 0) {
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

    // Apply CSS attributes
    if (ctx != null) {
      GeomUtils.applyAttributes(path, ctx, 'area', 'gg-area', elementIndex)
    }
  }

  /**
   * Get the baseline y coordinate (bottom of plot area or y=0).
   */
  private BigDecimal getBaselineY(Scale yScale) {
    // Try to transform y=0 to get the baseline
    if (yScale != null) {
      def baseline = yScale.transform(0)
      if (baseline != null) {
        return baseline as BigDecimal
      }
    }
    // Default: assume 480px plot height (bottom of plot)
    return 480.0
  }

  /**
   * Sort rows by x value for proper area rendering.
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

    int index = value.hashCode().abs() % palette.size()
    return palette[index]
  }
}
