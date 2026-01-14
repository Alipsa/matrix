package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.coord.CoordRadial
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Bar geometry for bar charts (counts/frequencies).
 * Uses stat_count by default to count observations in each category.
 * For pre-computed heights, use GeomCol instead.
 */
@CompileStatic
class GeomBar extends Geom {

  /** Bar fill color */
  String fill = '#595959'

  /** Bar outline color */
  String color = null

  /** Bar width as fraction of bandwidth (0-1), null = auto */
  Number width = null

  /** Alpha transparency */
  Number alpha = 1.0

  /** Outline width */
  Number linewidth = 0.5

  GeomBar() {
    defaultStat = StatType.COUNT
    defaultPosition = PositionType.STACK
    requiredAes = ['x']
    defaultAes = [fill: '#595959', alpha: 1.0] as Map<String, Object>
  }

  GeomBar(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.width) this.width = params.width as Number
    if (params.alpha) this.alpha = params.alpha as Number
    if (params.linewidth) this.linewidth = params.linewidth as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    if (coord instanceof CoordRadial) {
      renderRadial(group, data, aes, scales, coord as CoordRadial)
      return
    }

    if (coord instanceof CoordPolar) {
      renderPolar(group, data, aes, scales, coord as CoordPolar)
      return
    }

    String xCol = aes.xColName
    // After stat_count, y values are in 'count' column
    // For identity stat (GeomCol), use the aes y column
    String yCol = data.columnNames().contains('count') ? 'count' : aes.yColName
    String fillCol = aes.fillColName

    if (xCol == null) {
      throw new IllegalArgumentException("GeomBar requires x aesthetic")
    }
    if (yCol == null) {
      throw new IllegalArgumentException("GeomBar requires y aesthetic or stat_count")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Calculate bar width
    BigDecimal barWidth = calculateBarWidth(xScale)

    // Render each bar
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform coordinates
      BigDecimal xCenter = xScale?.transform(xVal) as BigDecimal
      BigDecimal yTop
      BigDecimal yBottom
      if (data.columnNames().contains('ymin') && data.columnNames().contains('ymax')) {
        Number yMinVal = row['ymin'] as Number
        Number yMaxVal = row['ymax'] as Number
        yTop = yScale?.transform(yMaxVal) as BigDecimal
        yBottom = yScale?.transform(yMinVal) as BigDecimal
      } else {
        yTop = yScale?.transform(yVal) as BigDecimal
        yBottom = yScale?.transform(0) as BigDecimal
      }

      if (xCenter == null || yTop == null || yBottom == null) return

      BigDecimal xPx = xCenter - barWidth / 2
      BigDecimal yPx = yTop.min(yBottom)
      BigDecimal heightPx = (yBottom - yTop).abs()

      // Determine fill color
      String barFill = this.fill
      if (fillCol && row[fillCol] != null) {
        if (fillScale) {
          barFill = fillScale.transform(row[fillCol])?.toString() ?: this.fill
        } else {
          barFill = getDefaultColor(row[fillCol])
        }
      } else if (aes.fill instanceof Identity) {
        barFill = (aes.fill as Identity).value.toString()
      }
      barFill = ColorUtil.normalizeColor(barFill) ?: barFill

      // Draw bar
      def rect = group.addRect(barWidth, heightPx)
          .x(xPx)
          .y(yPx)
          .fill(barFill)

      // Add stroke if specified
      if (color != null) {
        String strokeColor = ColorUtil.normalizeColor(color) ?: color
        rect.stroke(strokeColor)
        rect.addAttribute('stroke-width', linewidth)
      }

      // Add transparency
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
    }
  }

  /**
   * Calculate bar width based on scale bandwidth.
   */
  protected BigDecimal calculateBarWidth(Scale xScale) {
    if (width != null) {
      // User specified width as fraction
      if (xScale instanceof ScaleDiscrete) {
        return (xScale as ScaleDiscrete).getBandwidth() * (width as BigDecimal)
      }
      return 20 * (width as BigDecimal)  // Fallback for continuous
    }

    // Default: 90% of bandwidth for discrete, 20px for continuous
    if (xScale instanceof ScaleDiscrete) {
      return (xScale as ScaleDiscrete).getBandwidth() * 0.9
    }
    return 20
  }

  /**
   * Get a default color from a discrete palette based on a value.
   */
  protected String getDefaultColor(Object value) {
    List<String> palette = [
        '#F8766D', '#C49A00', '#53B400',
        '#00C094', '#00B6EB', '#A58AFF',
        '#FB61D7'
    ]

    int index = value.hashCode().abs() % palette.size()
    return palette[index]
  }

  /**
   * Render bars using polar coordinates (pie/donut charts).
   * This path rendering is separate from the cartesian bar logic because it
   * converts stacked bar segments into arc slices.
   *
   * <p><b>Algorithm for converting stacked bars to pie slices:</b></p>
   * <ol>
   *   <li>Groups data rows by x aesthetic (each x value becomes a concentric ring)</li>
   *   <li>Calculates ring dimensions: outer rings for first x groups, inner for later ones</li>
   *   <li>For each ring, converts y values to proportional arc angles (0 to 2π radians)</li>
   *   <li>Creates SVG path arcs for each segment using polar coordinate transformations</li>
   *   <li>Applies fill colors from the fill scale or default palette</li>
   * </ol>
   *
   * <p><b>Ordering behavior:</b></p>
   * <ul>
   *   <li>Rows within each x group are sorted by fill scale levels (if fill is discrete)</li>
   *   <li>When coord.theta == 'y', rows are reversed to match ggplot2's counterclockwise
   *       legend order (legend top-to-bottom maps to chart counterclockwise)</li>
   *   <li>Missing fill levels sort last (Integer.MAX_VALUE position)</li>
   *   <li>Arc angles are calculated cumulatively, starting at 0 radians</li>
   * </ul>
   *
   * @param group SVG group to render into
   * @param data data matrix (after stat/position transforms)
   * @param aes aesthetic mappings
   * @param scales computed scales
   * @param coord polar coordinate system
   */
  private void renderRadial(G group, Matrix data, Aes aes, Map<String, Scale> scales, CoordRadial coord) {
    String xCol = aes.xColName
    String yCol = data.columnNames().contains('count') ? 'count' : aes.yColName
    String fillCol = aes.fillColName

    Scale fillScale = scales['fill'] ?: scales['color']

    Map<Object, List<Row>> groups = [:].withDefault { [] }
    data.each { row ->
      Object key = xCol != null ? row[xCol] : null
      groups[key] << row
    }

    double outerRadius = coord.getMaxRadius() as double
    double innerOffset = coord.getInnerRadiusPx() as double
    innerOffset = innerOffset.min(outerRadius).max(0.0d)
    double availableRadius = (outerRadius - innerOffset).max(0.0d)
    int groupCount = groups.size()

    double span = coord.getAngularSpan() as double

    int idx = 0
    for (Map.Entry<Object, List<Row>> entry : groups.entrySet()) {
      List<Row> rows = new ArrayList<>(entry.value)
      if (fillCol && fillScale instanceof ScaleDiscrete) {
        List<Object> levels = (fillScale as ScaleDiscrete).levels
        if (!levels.isEmpty()) {
          Map<Object, Integer> levelIndex = [:]
          levels.eachWithIndex { level, int pos ->
            levelIndex[level] = pos
          }
          rows.sort { Row row ->
            int pos = levelIndex.getOrDefault(row[fillCol], Integer.MAX_VALUE)
            return pos < 0 ? Integer.MAX_VALUE : pos
          }
        }
      }
      if (coord.theta == 'y') {
        rows = rows.reverse()
      }

      double ringSize = groupCount > 0 ? (availableRadius / (double) groupCount) : availableRadius
      double groupOuter = outerRadius - (idx * ringSize)
      double groupInner = (groupOuter - ringSize).max(innerOffset)
      idx++

      List<Double> values = rows.collect { row ->
        if (row['ymin'] != null && row['ymax'] != null) {
          return ((row['ymax'] as double) - (row['ymin'] as double))
        }
        if (yCol != null && row[yCol] instanceof Number) {
          return row[yCol] as double
        }
        return 0.0d
      }
      double total = values.sum(0.0d) as double
      if (total <= 0.0d) {
        continue
      }

      double current = 0.0d
      rows.eachWithIndex { row, int rowIdx ->
        double value = values[rowIdx]
        if (value <= 0.0d) {
          return
        }
        double startAngle = (current / total) * span
        double endAngle = ((current + value) / total) * span
        current += value

        String sliceFill = this.fill
        if (fillCol && row[fillCol] != null) {
          if (fillScale) {
            sliceFill = fillScale.transform(row[fillCol])?.toString() ?: this.fill
          } else {
            sliceFill = getDefaultColor(row[fillCol])
          }
        } else if (aes.fill instanceof Identity) {
          sliceFill = (aes.fill as Identity).value.toString()
        }
        sliceFill = ColorUtil.normalizeColor(sliceFill) ?: sliceFill

        def path = group.addPath()
            .d(coord.createArcPath(startAngle, endAngle, groupInner, groupOuter))
            .fill(sliceFill)
        if (color != null) {
          String strokeColor = ColorUtil.normalizeColor(color) ?: color
          path.stroke(strokeColor)
          path.addAttribute('stroke-width', linewidth)
        } else {
          path.stroke('none')
        }
        if (alpha < 1.0) {
          path.addAttribute('fill-opacity', alpha)
        }
      }
    }
  }

  private void renderPolar(G group, Matrix data, Aes aes, Map<String, Scale> scales, CoordPolar coord) {
    String xCol = aes.xColName
    String yCol = data.columnNames().contains('count') ? 'count' : aes.yColName
    String fillCol = aes.fillColName

    Scale fillScale = scales['fill'] ?: scales['color']

    Map<Object, List<Row>> groups = [:].withDefault { [] }
    data.each { row ->
      Object key = xCol != null ? row[xCol] : null
      groups[key] << row
    }

    double outerRadius = coord.getMaxRadius() * 0.9
    int groupCount = groups.size()

    int idx = 0
    for (Map.Entry<Object, List<Row>> entry : groups.entrySet()) {
      List<Row> rows = new ArrayList<>(entry.value)
      if (fillCol && fillScale instanceof ScaleDiscrete) {
        List<Object> levels = (fillScale as ScaleDiscrete).levels
        if (!levels.isEmpty()) {
          Map<Object, Integer> levelIndex = [:]
          levels.eachWithIndex { level, int pos ->
            levelIndex[level] = pos
          }
          rows.sort { Row row ->
            int pos = levelIndex.getOrDefault(row[fillCol], Integer.MAX_VALUE)
            return pos < 0 ? Integer.MAX_VALUE : pos
          }
        }
      }
      if (coord.theta == 'y') {
        // Match ggplot2 slice order: legend order appears counterclockwise
        rows = rows.reverse()
      }
      double ringSize = groupCount > 0 ? (outerRadius / (double) groupCount) : outerRadius
      double groupOuter = outerRadius - (idx * ringSize)
      double groupInner = (groupOuter - ringSize).max(0.0d)
      idx++

      List<Double> values = rows.collect { row ->
        if (row['ymin'] != null && row['ymax'] != null) {
          return ((row['ymax'] as double) - (row['ymin'] as double))
        }
        if (yCol != null && row[yCol] instanceof Number) {
          return row[yCol] as double
        }
        return 0.0d
      }
      double total = values.sum(0.0d) as double
      if (total <= 0.0d) {
        continue
      }

      double current = 0.0d
      rows.eachWithIndex { row, int rowIdx ->
        double value = values[rowIdx]
        if (value <= 0.0d) {
          return
        }
        double startAngle = (current / total) * 2 * PI
        double endAngle = ((current + value) / total) * 2 * PI
        current += value

        String sliceFill = this.fill
        if (fillCol && row[fillCol] != null) {
          if (fillScale) {
            sliceFill = fillScale.transform(row[fillCol])?.toString() ?: this.fill
          } else {
            sliceFill = getDefaultColor(row[fillCol])
          }
        } else if (aes.fill instanceof Identity) {
          sliceFill = (aes.fill as Identity).value.toString()
        }
        sliceFill = ColorUtil.normalizeColor(sliceFill) ?: sliceFill

        def path = group.addPath()
            .d(coord.createArcPath(startAngle, endAngle, groupInner, groupOuter))
            .fill(sliceFill)
        if (color != null) {
          String strokeColor = ColorUtil.normalizeColor(color) ?: color
          path.stroke(strokeColor)
          path.addAttribute('stroke-width', linewidth)
        } else {
          path.stroke('none')
        }
        if (alpha < 1.0) {
          path.addAttribute('fill-opacity', alpha)
        }
      }
    }
  }
}
