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
 * Frequency polygon geometry.
 * Uses stat_bin to compute counts per bin and draws a line through the bin centers.
 */
@CompileStatic
class GeomFreqpoly extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number size = 1

  /** Line type (solid, dashed, dotted, dotdash, longdash, twodash) */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /**
   * Create a frequency polygon geom with default settings.
   */
  GeomFreqpoly() {
    defaultStat = StatType.BIN
    requiredAes = ['x']
    defaultAes = [color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  /**
   * Create a frequency polygon geom with parameters.
   *
   * @param params geom parameters
   */
  GeomFreqpoly(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.size != null) this.size = params.size as Number
    if (params.linewidth != null) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 2) return

    String xCol = data.columnNames().contains('x') ? 'x' : aes.xColName
    String yCol = resolveYColumn(data, aes)
    String groupCol = aes.groupColName ?: aes.colorColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomFreqpoly requires stat_bin output with x and count/density columns")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale alphaScale = scales['alpha']

    Map<Object, List<Map>> groups = [:]
    data.each { row ->
      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << row.toMap()
    }

    groups.each { groupKey, rows ->
      renderLine(group, rows, xCol, yCol, colorCol, sizeCol, alphaCol, groupKey,
          xScale, yScale, colorScale, sizeScale, alphaScale, aes)
    }
  }

  private String resolveYColumn(Matrix data, Aes aes) {
    if (aes.isAfterStat('y')) {
      String statCol = aes.getAfterStatName('y')
      if (statCol && data.columnNames().contains(statCol)) {
        return statCol
      }
    }
    String yCol = aes.yColName
    if (yCol && data.columnNames().contains(yCol)) {
      return yCol
    }
    if (data.columnNames().contains('count')) {
      return 'count'
    }
    if (data.columnNames().contains('density')) {
      return 'density'
    }
    if (data.columnNames().contains('y')) {
      return 'y'
    }
    return null
  }

  private void renderLine(G group, List<Map> rows, String xCol, String yCol,
                          String colorCol, String sizeCol, String alphaCol, Object groupKey,
                          Scale xScale, Scale yScale, Scale colorScale,
                          Scale sizeScale, Scale alphaScale, Aes aes) {
    List<Map> sortedRows = rows.sort { a, b ->
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

    List<double[]> points = []
    sortedRows.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal == null || yVal == null) return

      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)
      if (xTransformed == null || yTransformed == null) return

      points << ([xTransformed as double, yTransformed as double] as double[])
    }

    if (points.size() < 2) return

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

    String dashArray = getDashArray(linetype)

    for (int i = 0; i < points.size() - 1; i++) {
      double[] p1 = points[i]
      double[] p2 = points[i + 1]

      def line = group.addLine(p1[0], p1[1], p2[0], p2[1])
          .stroke(lineColor)
          .strokeWidth(lineSize)

      if (dashArray) {
        line.addAttribute('stroke-dasharray', dashArray)
      }
      if ((lineAlpha as double) < 1.0) {
        line.addAttribute('stroke-opacity', lineAlpha)
      }
    }
  }

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
