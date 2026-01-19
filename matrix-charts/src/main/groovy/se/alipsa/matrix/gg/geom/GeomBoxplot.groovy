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
import se.alipsa.matrix.gg.render.RenderContext
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
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
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
    BigDecimal plotWidth = 640  // default plot width
    // Check if x values are numeric (continuous positioning)
    boolean continuousX = false
    if (data.columnNames().contains('x') && data.rowCount() > 0) {
      def firstX = data['x'].find { it != null }
      continuousX = firstX instanceof Number
    }
    boolean useVarwidth = varwidth == true
    BigDecimal maxRelVarwidth = 1.0
    if (useVarwidth && data.columnNames().contains('relvarwidth')) {
      List<Number> relValues = (data['relvarwidth'] as List).findAll { it instanceof Number } as List<Number>
      if (!relValues.isEmpty()) {
        maxRelVarwidth = relValues.max() as BigDecimal
      }
    }

    int elementIndex = 0
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
      BigDecimal xCenter
      BigDecimal halfWidth

      boolean discreteX = xScale instanceof ScaleDiscrete

      BigDecimal xminData = row['xmin'] instanceof Number ? (row['xmin'] as BigDecimal) : null
      BigDecimal xmaxData = row['xmax'] instanceof Number ? (row['xmax'] as BigDecimal) : null
      BigDecimal boundsWidth = (xminData != null && xmaxData != null) ? (xmaxData - xminData) : null
      BigDecimal centerData = (xminData != null && xmaxData != null) ? (xminData + xmaxData) / 2.0d : null

      BigDecimal widthData = resolveWidthData(row, (width as BigDecimal), useVarwidth, maxRelVarwidth, boundsWidth)
      boolean usedBounds = false
      // When xmin/xmax bounds are present (e.g., from position_dodge2), preserve their center
      // and resolve a single width value so varwidth scaling matches the drawn width.
      if (centerData != null && xScale != null) {
        xCenter = xScale.transform(centerData) as BigDecimal
        if (widthData > 0d) {
          BigDecimal leftPx = xScale.transform(centerData - widthData / 2.0d) as BigDecimal
          BigDecimal rightPx = xScale.transform(centerData + widthData / 2.0d) as BigDecimal
          halfWidth = (rightPx - leftPx).abs() / 2.0d
        } else {
          BigDecimal leftPx = xScale.transform(xminData) as BigDecimal
          BigDecimal rightPx = xScale.transform(xmaxData) as BigDecimal
          halfWidth = (rightPx - leftPx).abs() / 2.0d
        }
        usedBounds = true
      }

      if (!usedBounds) {
        if (continuousX && xScale != null && xVal instanceof Number) {
          xCenter = xScale.transform(xVal as Number) as BigDecimal
          if (widthData > 0) {
            BigDecimal leftPx = xScale.transform((xVal as BigDecimal) - widthData / 2) as BigDecimal
            BigDecimal rightPx = xScale.transform((xVal as BigDecimal) + widthData / 2) as BigDecimal
            halfWidth = (rightPx - leftPx).abs() / 2
          } else {
            halfWidth = plotWidth / (numBoxes * 2).max(1)
          }
        } else if (xScale != null && discreteX) {
          def transformed = xScale.transform(xVal)
          if (transformed instanceof Number) {
            xCenter = transformed as BigDecimal
          } else {
            xCenter = xScale.transform(idx) as BigDecimal
          }
          BigDecimal bandWidth = ((ScaleDiscrete) xScale).getBandwidth()
          halfWidth = bandWidth * widthData / 2
        } else if (xScale != null) {
          // Categorical x without discrete scale support
          def transformed = xScale.transform(xVal)
          if (transformed instanceof Number) {
            xCenter = transformed as BigDecimal
          } else {
            xCenter = xScale.transform(idx) as BigDecimal
          }
          halfWidth = (plotWidth / numBoxes.max(1)) * widthData / 2
        } else {
          // Fallback positioning when no scale is available
          BigDecimal slotWidth = plotWidth / numBoxes.max(1)
          xCenter = (idx + 0.5) * slotWidth
          halfWidth = (slotWidth * widthData) / 2.0
        }
      }

      // Transform y coordinates
      BigDecimal yminPx = yScale?.transform(ymin) as BigDecimal
      BigDecimal lowerPx = yScale?.transform(lower) as BigDecimal
      BigDecimal middlePx = yScale?.transform(middle) as BigDecimal
      BigDecimal upperPx = yScale?.transform(upper) as BigDecimal
      BigDecimal ymaxPx = yScale?.transform(ymax) as BigDecimal

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
            .x1(xCenter)
            .y1(yminPx)
            .x2(xCenter)
            .y2(lowerPx)
            .stroke(boxColor)
            .addAttribute('stroke-width', linewidth)
      }

      // Draw upper whisker line (from upper to ymax)
      if (ymax != null && ymaxPx != upperPx) {
        boxGroup.addLine()
            .x1(xCenter)
            .y1(upperPx)
            .x2(xCenter)
            .y2(ymaxPx)
            .stroke(boxColor)
            .addAttribute('stroke-width', linewidth)
      }

      // Draw whisker caps (horizontal lines at ymin and ymax)
      BigDecimal capHalfWidth = halfWidth * (stapleWidth as BigDecimal)
      if (stapleWidth > 0d) {
        if (ymin != null) {
          boxGroup.addLine()
              .x1(xCenter - capHalfWidth)
              .y1(yminPx)
              .x2(xCenter + capHalfWidth)
              .y2(yminPx)
              .stroke(boxColor)
              .addAttribute('stroke-width', linewidth)
        }

        if (ymax != null) {
          boxGroup.addLine()
              .x1(xCenter - capHalfWidth)
              .y1(ymaxPx)
              .x2(xCenter + capHalfWidth)
              .y2(ymaxPx)
              .stroke(boxColor)
              .addAttribute('stroke-width', linewidth)
        }
      }

      // Draw box (from lower/Q1 to upper/Q3)
      BigDecimal boxTop = lowerPx.min(upperPx)
      BigDecimal boxHeight = (upperPx - lowerPx).abs()

      def rect = boxGroup.addRect(halfWidth * 2, boxHeight)
          .x(xCenter - halfWidth)
          .y(boxTop)
          .fill(boxFill)
          .stroke(boxColor)
      rect.addAttribute('stroke-width', linewidth)

      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Apply CSS attributes
      GeomUtils.applyAttributes(rect, ctx, 'boxplot', 'gg-boxplot', elementIndex)

      // Draw median line
      boxGroup.addLine()
          .x1(xCenter - halfWidth)
          .y1(middlePx)
          .x2(xCenter + halfWidth)
          .y2(middlePx)
          .stroke(boxColor)
          .addAttribute('stroke-width', linewidth * 1.5)

      // Draw outliers
      if (outliers && outlierList != null) {
        String outlierCol = outlierColor ?: boxColor
        outlierCol = ColorUtil.normalizeColor(outlierCol)
        List<?> outlierValues = outlierList instanceof List ? outlierList as List : []

        for (def outlier : outlierValues) {
          if (outlier instanceof Number) {
            BigDecimal outlierPx = yScale?.transform(outlier as Number) as BigDecimal
            BigDecimal radius = (outlierSize as BigDecimal) * 2

            if (outlierShape == 'circle') {
              // ggplot2 renders outliers as filled circles
              def circle = boxGroup.addCircle()
                  .cx(xCenter)
                  .cy(outlierPx)
                  .r(radius)
                  .fill(outlierCol)
                  .stroke(outlierCol)
              circle.addAttribute('stroke-width', linewidth)
            } else {
              // Square or other shapes - also filled
              boxGroup.addRect(radius * 2, radius * 2)
                  .x(xCenter - radius)
                  .y(outlierPx - radius)
                  .fill(outlierCol)
                  .stroke(outlierCol)
                  .addAttribute('stroke-width', linewidth)
            }
          }
        }
      }

      elementIndex++
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
  static BigDecimal resolveWidthData(Row row, Number defaultWidth, boolean useVarwidth, Number maxRelVarwidth, Number boundsWidth = null) {
    BigDecimal widthData
    if (boundsWidth != null) {
      widthData = boundsWidth as BigDecimal
    } else if (row['width'] instanceof Number) {
      widthData = (row['width'] as BigDecimal)
    } else if (row['xresolution'] instanceof Number) {
      widthData = (row['xresolution'] as BigDecimal) * defaultWidth
    } else {
      widthData = defaultWidth
    }
    if (useVarwidth && row['relvarwidth'] instanceof Number && maxRelVarwidth > 0d) {
      BigDecimal rel = (row['relvarwidth'] as BigDecimal) / maxRelVarwidth
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

    int index = value.hashCode().abs() % palette.size()
    return palette[index]
  }
}
