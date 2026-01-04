package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Box plot geometry for visualizing distributions through quartiles.
 * Uses stat_boxplot by default to compute quartiles and whiskers.
 *
 * After stat_boxplot transformation, data has columns:
 * - x: group key (category)
 * - ymin: lower whisker
 * - lower: Q1 (25th percentile, bottom of box)
 * - middle: median (50th percentile, line inside box)
 * - upper: Q3 (75th percentile, top of box)
 * - ymax: upper whisker
 * - outliers: list of outlier values beyond whiskers
 * - width: box width in data units
 * - xmin/xmax: box width extents in data units
 * - relvarwidth: relative width scaling factor (sqrt(n))
 * - xresolution: minimum non-zero x resolution for width calculation
 */
@CompileStatic
class GeomBoxplot extends Geom {

  /** Box fill color */
  String fill = 'white'

  /** Box and whisker outline color */
  String color = 'black'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Line width for box outline and whiskers */
  Number linewidth = 0.5

  /** Width of boxes relative to spacing (0-1) */
  Number width = 0.75

  /** Whether to scale box widths based on sample size (null/false = disabled, true = enabled) */
  Boolean varwidth = null

  /** Whether to show outlier points */
  boolean outliers = true

  /** Size of outlier points */
  Number outlierSize = 1.5

  /** Shape of outlier points (circle, square, etc.) */
  String outlierShape = 'circle'

  /** Color of outlier points (null = use color) */
  String outlierColor = null

  /** Width of whisker caps relative to box width */
  Number stapleWidth = 0.0

  GeomBoxplot() {
    defaultStat = StatType.BOXPLOT
    defaultPosition = PositionType.DODGE2
    requiredAes = ['y']  // x is optional for single boxplot
    defaultAes = [fill: 'white', color: 'black', alpha: 1.0] as Map<String, Object>
  }

  GeomBoxplot(Aes aes) {
    this()
    if (aes != null) {
      this.params = [mapping: aes]
    }
  }

  GeomBoxplot(Map params) {
    this()
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.width != null) this.width = params.width as Number
    if (params.varwidth != null) this.varwidth = params.varwidth as Boolean
    if (params.var_width != null) this.varwidth = params.var_width as Boolean
    if (params.outliers != null) this.outliers = params.outliers as boolean
    if (params.outlierSize != null) this.outlierSize = params.outlierSize as Number
    if (params.outlier_size != null) this.outlierSize = params.outlier_size as Number
    if (params.outlierShape) this.outlierShape = params.outlierShape as String
    if (params.outlier_shape) this.outlierShape = params.outlier_shape as String
    if (params.outlierColor) this.outlierColor = params.outlierColor as String
    if (params.outlier_color) this.outlierColor = params.outlier_color as String
    if (params.stapleWidth != null) this.stapleWidth = params.stapleWidth as Number
    if (params.staple_width != null) this.stapleWidth = params.staple_width as Number
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    if (this.outlierColor != null) {
      this.outlierColor = ColorUtil.normalizeColor(this.outlierColor)
    }
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    // After stat_boxplot, data has: x, ymin, lower, middle, upper, ymax, outliers, width, n
    List<String> required = ['ymin', 'lower', 'middle', 'upper', 'ymax']
    for (String col : required) {
      if (!data.columnNames().contains(col)) {
        throw new IllegalArgumentException("GeomBoxplot requires stat_boxplot transformation (${col} column missing)")
      }
    }

    String fillCol = aes.fillColName
    String colorCol = aes.colorColName

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill']
    Scale colorScale = scales['color']

    int numBoxes = data.rowCount()
    double plotWidth = 640  // default plot width
    // Check if x values are numeric (continuous positioning)
    boolean continuousX = false
    if (data.columnNames().contains('x') && data.rowCount() > 0) {
      def firstX = data['x'].find { it != null }
      continuousX = firstX instanceof Number
    }
    boolean useVarwidth = varwidth == true
    double maxRelVarwidth = 1.0d
    if (useVarwidth && data.columnNames().contains('relvarwidth')) {
      List<Number> relValues = (data['relvarwidth'] as List).findAll { it instanceof Number } as List<Number>
      if (!relValues.isEmpty()) {
        maxRelVarwidth = relValues.max() as double
      }
    }

    // Render each boxplot
    data.eachWithIndex { Row row, int idx ->
      def xVal = row['x']
      def ymin = row['ymin'] as Number
      def lower = row['lower'] as Number
      def middle = row['middle'] as Number
      def upper = row['upper'] as Number
      def ymax = row['ymax'] as Number
      def outlierList = row['outliers']

      if (lower == null || upper == null || middle == null) return

      // Calculate x position and width
      double xCenter
      double halfWidth

      boolean discreteX = xScale instanceof ScaleDiscrete

      Double xminData = row['xmin'] instanceof Number ? (row['xmin'] as Number).doubleValue() : null
      Double xmaxData = row['xmax'] instanceof Number ? (row['xmax'] as Number).doubleValue() : null
      Double boundsWidth = (xminData != null && xmaxData != null) ? (xmaxData - xminData) : null
      Double centerData = (xminData != null && xmaxData != null) ? (xminData + xmaxData) / 2.0d : null

      double widthData = resolveWidthData(row, (width as Number).doubleValue(), useVarwidth, maxRelVarwidth, boundsWidth)
      boolean usedBounds = false
      // When xmin/xmax bounds are present (e.g., from position_dodge2), preserve their center
      // and resolve a single width value so varwidth scaling matches the drawn width.
      if (centerData != null && xScale != null) {
        xCenter = xScale.transform(centerData) as double
        if (widthData > 0d) {
          double leftPx = xScale.transform(centerData - widthData / 2.0d) as double
          double rightPx = xScale.transform(centerData + widthData / 2.0d) as double
          halfWidth = Math.abs(rightPx - leftPx) / 2.0d
        } else {
          double leftPx = xScale.transform(xminData) as double
          double rightPx = xScale.transform(xmaxData) as double
          halfWidth = Math.abs(rightPx - leftPx) / 2.0d
        }
        usedBounds = true
      }

      if (!usedBounds) {
        if (continuousX && xScale != null && xVal instanceof Number) {
          xCenter = xScale.transform(xVal as Number) as double
          if (widthData > 0) {
            double leftPx = xScale.transform((xVal as Number).doubleValue() - widthData / 2) as double
            double rightPx = xScale.transform((xVal as Number).doubleValue() + widthData / 2) as double
            halfWidth = Math.abs(rightPx - leftPx) / 2
          } else {
            halfWidth = plotWidth / Math.max(numBoxes * 2, 1)
          }
        } else if (xScale != null && discreteX) {
          def transformed = xScale.transform(xVal)
          if (transformed instanceof Number) {
            xCenter = transformed as double
          } else {
            xCenter = xScale.transform(idx) as double
          }
          double bandWidth = ((ScaleDiscrete) xScale).getBandwidth()
          halfWidth = bandWidth * widthData / 2
        } else if (xScale != null) {
          // Categorical x without discrete scale support
          def transformed = xScale.transform(xVal)
          if (transformed instanceof Number) {
            xCenter = transformed as double
          } else {
            xCenter = xScale.transform(idx) as double
          }
          halfWidth = (plotWidth / Math.max(numBoxes, 1)) * widthData / 2
        } else {
          // Fallback positioning when no scale is available
          double slotWidth = plotWidth / Math.max(numBoxes, 1)
          xCenter = (idx + 0.5d) * slotWidth
          halfWidth = (slotWidth * widthData) / 2.0d
        }
      }

      // Transform y coordinates
      double yminPx = yScale?.transform(ymin) as double
      double lowerPx = yScale?.transform(lower) as double
      double middlePx = yScale?.transform(middle) as double
      double upperPx = yScale?.transform(upper) as double
      double ymaxPx = yScale?.transform(ymax) as double

      // Determine colors
      String boxFill = this.fill
      String boxColor = this.color

      if (fillCol && row[fillCol] != null) {
        if (fillScale) {
          boxFill = fillScale.transform(row[fillCol])?.toString() ?: this.fill
        } else {
          boxFill = getDefaultColor(row[fillCol])
        }
      } else if (aes.fill instanceof Identity) {
        boxFill = (aes.fill as Identity).value.toString()
      }

      if (colorCol && row[colorCol] != null) {
        if (colorScale) {
          boxColor = colorScale.transform(row[colorCol])?.toString() ?: this.color
        } else {
          boxColor = getDefaultColor(row[colorCol])
        }
      } else if (aes.color instanceof Identity) {
        boxColor = (aes.color as Identity).value.toString()
      }

      boxFill = ColorUtil.normalizeColor(boxFill)
      boxColor = ColorUtil.normalizeColor(boxColor)

      // Create a group for this boxplot
      G boxGroup = group.addG()
      boxGroup.styleClass('geom-boxplot')

      // Draw lower whisker line (from ymin to lower)
      if (ymin != null && yminPx != lowerPx) {
        boxGroup.addLine()
            .x1(xCenter as int)
            .y1(yminPx as int)
            .x2(xCenter as int)
            .y2(lowerPx as int)
            .stroke(boxColor)
            .addAttribute('stroke-width', linewidth)
      }

      // Draw upper whisker line (from upper to ymax)
      if (ymax != null && ymaxPx != upperPx) {
        boxGroup.addLine()
            .x1(xCenter as int)
            .y1(upperPx as int)
            .x2(xCenter as int)
            .y2(ymaxPx as int)
            .stroke(boxColor)
            .addAttribute('stroke-width', linewidth)
      }

      // Draw whisker caps (horizontal lines at ymin and ymax)
      double capHalfWidth = halfWidth * (stapleWidth as double)
      if ((stapleWidth as double) > 0d) {
        if (ymin != null) {
          boxGroup.addLine()
              .x1((xCenter - capHalfWidth) as int)
              .y1(yminPx as int)
              .x2((xCenter + capHalfWidth) as int)
              .y2(yminPx as int)
              .stroke(boxColor)
              .addAttribute('stroke-width', linewidth)
        }

        if (ymax != null) {
          boxGroup.addLine()
              .x1((xCenter - capHalfWidth) as int)
              .y1(ymaxPx as int)
              .x2((xCenter + capHalfWidth) as int)
              .y2(ymaxPx as int)
              .stroke(boxColor)
              .addAttribute('stroke-width', linewidth)
        }
      }

      // Draw box (from lower/Q1 to upper/Q3)
      double boxTop = Math.min(lowerPx, upperPx)
      double boxHeight = Math.abs(upperPx - lowerPx)

      def rect = boxGroup.addRect(halfWidth * 2, boxHeight)
          .x((xCenter - halfWidth) as int)
          .y(boxTop as int)
          .fill(boxFill)
          .stroke(boxColor)
      rect.addAttribute('stroke-width', linewidth)

      if ((alpha as double) < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Draw median line
      boxGroup.addLine()
          .x1((xCenter - halfWidth) as int)
          .y1(middlePx as int)
          .x2((xCenter + halfWidth) as int)
          .y2(middlePx as int)
          .stroke(boxColor)
          .addAttribute('stroke-width', linewidth * 1.5)

      // Draw outliers
      if (outliers && outlierList != null) {
        String outlierCol = outlierColor ?: boxColor
        outlierCol = ColorUtil.normalizeColor(outlierCol)
        List<?> outlierValues = outlierList instanceof List ? outlierList as List : []

        for (def outlier : outlierValues) {
          if (outlier instanceof Number) {
            double outlierPx = yScale?.transform(outlier as Number) as double
            double radius = (outlierSize as double) * 2

            if (outlierShape == 'circle') {
              // ggplot2 renders outliers as filled circles
              def circle = boxGroup.addCircle()
                  .cx(xCenter as int)
                  .cy(outlierPx as int)
                  .r(radius)
                  .fill(outlierCol)
                  .stroke(outlierCol)
              circle.addAttribute('stroke-width', linewidth)
            } else {
              // Square or other shapes - also filled
              boxGroup.addRect(radius * 2, radius * 2)
                  .x((xCenter - radius) as int)
                  .y((outlierPx - radius) as int)
                  .fill(outlierCol)
                  .stroke(outlierCol)
                  .addAttribute('stroke-width', linewidth)
            }
          }
        }
      }
    }
  }

  /**
   * Resolve a boxplot width in data units, honoring stat-derived widths and optional varwidth scaling.
   *
   * @param row the row data providing optional width, xresolution, and relvarwidth columns
   * @param defaultWidth fallback width in data units
   * @param useVarwidth whether varwidth scaling should be applied
   * @param maxRelVarwidth maximum relvarwidth value across rows (for normalization)
   * @param boundsWidth optional width derived from xmin/xmax bounds (e.g., position-adjusted)
   * @return resolved width in data units
   */
  static double resolveWidthData(Row row, double defaultWidth, boolean useVarwidth, double maxRelVarwidth, Double boundsWidth = null) {
    double widthData
    if (boundsWidth != null) {
      widthData = boundsWidth
    } else if (row['width'] instanceof Number) {
      widthData = (row['width'] as Number).doubleValue()
    } else if (row['xresolution'] instanceof Number) {
      widthData = (row['xresolution'] as Number).doubleValue() * defaultWidth
    } else {
      widthData = defaultWidth
    }
    if (useVarwidth && row['relvarwidth'] instanceof Number && maxRelVarwidth > 0d) {
      double rel = (row['relvarwidth'] as Number).doubleValue() / maxRelVarwidth
      widthData = widthData * rel
    }
    return widthData
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
