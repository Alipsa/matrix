package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Parallel coordinates plot geometry for multivariate data visualization.
 * Creates a vertical axis for each variable and draws lines connecting values across axes.
 *
 * Each observation becomes a polyline, allowing visualization of patterns and clusters
 * in high-dimensional data.
 *
 * Required aesthetics: none (uses all numeric columns by default)
 * Optional aesthetics: color, group, alpha
 *
 * Parameters:
 * - vars: List of column names to include (default: all numeric columns)
 * - alpha: Line transparency (0-1, default: 0.5)
 * - color: Line color (default: 'steelblue')
 * - linewidth: Line width (default: 0.5)
 * - scale: Scaling method - 'uniminmax' (0-1 per variable), 'globalminmax' (0-1 global),
 *          'center' (mean-centered), 'std' (standardized), 'none' (default: 'uniminmax')
 *
 * Usage:
 * <pre>
 * // All numeric columns
 * ggplot(iris) + geom_parallel()
 *
 * // Specific columns
 * ggplot(iris) + geom_parallel(vars: ['Sepal.Length', 'Sepal.Width', 'Petal.Length'])
 *
 * // Colored by species
 * ggplot(iris, aes(color: 'Species')) + geom_parallel()
 *
 * // Custom styling
 * ggplot(mtcars) + geom_parallel(alpha: 0.3, linewidth: 1)
 * </pre>
 */
@CompileStatic
class GeomParallel extends Geom {

  /** Line color */
  String color = 'steelblue'

  /** Line width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 0.5

  /** Variables to include (null = all numeric columns) */
  List<String> vars = null

  /** Scaling method: 'uniminmax', 'globalminmax', 'center', 'std', 'none' */
  String scale = 'uniminmax'

  GeomParallel() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [color: 'steelblue', alpha: 0.5] as Map<String, Object>
  }

  GeomParallel(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.vars) this.vars = params.vars as List<String>
    if (params.scale) this.scale = params.scale as String
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    // Determine which columns to use
    List<String> columns = determineColumns(data)
    if (columns.isEmpty()) return

    int nVars = columns.size()
    int nObs = data.rowCount()

    // Get plot dimensions from scales
    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    // Compute axis positions (evenly spaced along x)
    List<BigDecimal> axisPositions = []
    for (int i = 0; i < nVars; i++) {
      BigDecimal xPos = xScale.transform(i) as BigDecimal
      if (xPos != null) {
        axisPositions << xPos
      }
    }

    if (axisPositions.size() != nVars) return

    // Normalize data for each variable
    Map<String, Map<String, BigDecimal>> stats = computeStats(data, columns)
    Map<String, List<BigDecimal>> normalizedData = normalizeData(data, columns, stats)

    // Get color column if specified
    String colorCol = aes?.colorColName
    Scale colorScale = scales['color'] ?: scales['fill']

    // Get group column if specified
    String groupCol = aes?.groupColName

    // Render axes first
    renderAxes(group, columns, axisPositions, yScale, stats)

    // Render polylines for each observation
    data.eachWithIndex { row, int idx ->
      List<BigDecimal> values = []
      for (String col : columns) {
        List<BigDecimal> colData = normalizedData[col]
        if (colData && idx < colData.size()) {
          values << colData[idx]
        } else {
          values << null
        }
      }

      // Skip if any values are null
      if (values.any { it == null }) return

      // Determine line color
      String lineColor = this.color
      if (colorCol && colorScale) {
        def colorValue = row[colorCol]
        if (colorValue != null) {
          def transformedColor = colorScale.transform(colorValue)
          if (transformedColor != null) {
            lineColor = ColorUtil.normalizeColor(transformedColor.toString())
          }
        }
      }

      // Build polyline path
      StringBuilder pathD = new StringBuilder()
      for (int i = 0; i < nVars; i++) {
        BigDecimal norm = values[i]
        BigDecimal xPos = axisPositions[i]

        // Transform normalized value (0-1) through y scale
        // Map 0 to yScale.min, 1 to yScale.max
        Number yPx = yScale.transform(norm) as Number
        if (yPx == null) continue

        if (i == 0) {
          pathD << "M ${xPos} ${yPx}"
        } else {
          pathD << " L ${xPos} ${yPx}"
        }
      }

      // Draw polyline
      def path = group.addPath()
          .d(pathD.toString())
          .stroke(lineColor)
          .fill('none')

      path.addAttribute('stroke-width', linewidth)

      if (alpha < 1.0) {
        path.addAttribute('stroke-opacity', alpha)
      }
    }
  }

  /**
   * Determine which columns to use for parallel coordinates.
   */
  private List<String> determineColumns(Matrix data) {
    if (vars != null && !vars.isEmpty()) {
      // Use specified columns
      return vars.findAll { data.columnNames().contains(it) }
    }

    // Use all numeric columns (check first non-null value)
    List<String> numericCols = []
    for (String col : data.columnNames()) {
      def firstVal = data.column(col).find { it != null }
      if (firstVal instanceof Number) {
        numericCols << col
      }
    }
    return numericCols
  }

  /**
   * Compute statistics for each variable.
   */
  private Map<String, Map<String, BigDecimal>> computeStats(Matrix data, List<String> columns) {
    Map<String, Map<String, BigDecimal>> stats = [:]

    for (String col : columns) {
      List<BigDecimal> values = data.column(col).findAll { it instanceof Number }.collect { it as BigDecimal }
      if (values.isEmpty()) {
        stats[col] = [min: 0.0 as BigDecimal, max: 1.0 as BigDecimal, mean: 0.5 as BigDecimal, std: 0.5 as BigDecimal]
        continue
      }

      BigDecimal min = values.min()
      BigDecimal max = values.max()
      BigDecimal sum = values.sum() as BigDecimal
      BigDecimal mean = sum / values.size()

      // Compute standard deviation
      BigDecimal variance = 0
      if (values.size() > 1) {
        BigDecimal sumSq = values.collect { (it - mean) ** 2 }.sum() as BigDecimal
        variance = sumSq / values.size()
      }
      BigDecimal std = variance.sqrt()

      stats[col] = [min: min, max: max, mean: mean, std: std]
    }

    return stats
  }

  /**
   * Normalize data based on scaling method.
   */
  private Map<String, List<BigDecimal>> normalizeData(Matrix data, List<String> columns,
                                                       Map<String, Map<String, BigDecimal>> stats) {
    Map<String, List<BigDecimal>> normalized = [:]

    if (scale == 'globalminmax') {
      // Find global min/max across all variables
      BigDecimal globalMin = stats.values().collect { it.min }.min()
      BigDecimal globalMax = stats.values().collect { it.max }.max()
      BigDecimal range = globalMax - globalMin
      if (range == 0) range = 1

      for (String col : columns) {
        normalized[col] = data.column(col).collect { val ->
          if (!(val instanceof Number)) return null
          ((val as BigDecimal) - globalMin) / range
        }
      }
    } else if (scale == 'center') {
      // Mean-centered (subtract mean)
      for (String col : columns) {
        BigDecimal mean = stats[col].mean
        normalized[col] = data.column(col).collect { val ->
          if (!(val instanceof Number)) return null
          (val as BigDecimal) - mean
        }
      }
    } else if (scale == 'std') {
      // Standardized (z-scores)
      for (String col : columns) {
        BigDecimal mean = stats[col].mean
        BigDecimal std = stats[col].std
        if (std == 0) std = 1

        normalized[col] = data.column(col).collect { val ->
          if (!(val instanceof Number)) return null
          ((val as BigDecimal) - mean) / std
        }
      }
    } else if (scale == 'none') {
      // No scaling, use raw values
      for (String col : columns) {
        normalized[col] = data.column(col).collect { val ->
          if (!(val instanceof Number)) return null
          val as BigDecimal
        }
      }
    } else {
      // 'uniminmax' - normalize each variable to [0, 1]
      for (String col : columns) {
        BigDecimal min = stats[col].min
        BigDecimal max = stats[col].max
        BigDecimal range = max - min
        if (range == 0) range = 1

        normalized[col] = data.column(col).collect { val ->
          if (!(val instanceof Number)) return null
          ((val as BigDecimal) - min) / range
        }
      }
    }

    return normalized
  }

  /**
   * Render vertical axes for each variable.
   */
  private void renderAxes(G group, List<String> columns, List<BigDecimal> axisPositions,
                          Scale yScale, Map<String, Map<String, BigDecimal>> stats) {
    // Get y range from scale
    Number yMin = yScale.transform(0) as Number
    Number yMax = yScale.transform(1) as Number
    if (yMin == null || yMax == null) return

    for (int i = 0; i < columns.size(); i++) {
      String col = columns[i]
      BigDecimal xPos = axisPositions[i]

      // Draw vertical axis line
      group.addLine(xPos, yMin, xPos, yMax)
           .stroke('#CCCCCC')
           .strokeWidth(1)

      // Add variable label at top
      group.addText(col)
           .x(xPos)
           .y((yMax as BigDecimal) - 10)
           .textAnchor('middle')
           .fontSize(10)
           .fill('#333333')

      // Add min/max labels
      Map<String, BigDecimal> varStats = stats[col]
      if (varStats) {
        // Min label at bottom
        group.addText(String.format('%.2f', varStats.min))
             .x(xPos)
             .y((yMin as BigDecimal) + 12)
             .textAnchor('middle')
             .fontSize(8)
             .fill('#666666')

        // Max label at top
        group.addText(String.format('%.2f', varStats.max))
             .x(xPos)
             .y((yMax as BigDecimal) - 25)
             .textAnchor('middle')
             .fontSize(8)
             .fill('#666666')
      }
    }
  }
}
