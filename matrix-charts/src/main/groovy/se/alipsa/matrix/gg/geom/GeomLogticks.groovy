package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleXLog10
import se.alipsa.matrix.gg.scale.ScaleYLog10

/**
 * Annotation layer for logarithmic tick marks.
 * Automatically generates tick marks at appropriate positions for log-scaled axes.
 *
 * Usage:
 * - annotation_logticks() - default bottom and left sides
 * - annotation_logticks(sides: 't') - top side only
 * - annotation_logticks(sides: 'trbl') - all four sides
 * - annotation_logticks(base: 2) - base-2 logarithmic ticks
 *
 * Tick types (for base >= 4):
 * - Major: powers of base (e.g., 1, 10, 100, ... for base 10)
 * - Intermediate: multiples at 2 and 5 (e.g., 2, 5, 20, 50, ... for base 10)
 *   Note: For base 10, both 2 and 5 are intermediate. For other bases >= 4,
 *   only mult==2 is intermediate, and mult==5 is minor (if base > 5).
 * - Minor: other integer multiples (e.g., 3, 4, 6, 7, 8, 9, ... for base 10)
 *
 * For bases 2 and 3, only major ticks are generated (no intermediate/minor ticks).
 */
@CompileStatic
class GeomLogticks extends Geom {

  /** Logarithmic base (default: 10) */
  int base = 10

  /** Which sides to draw ticks: 't' (top), 'r' (right), 'b' (bottom), 'l' (left) (default: 'bl') */
  String sides = 'bl'

  /** Whether ticks extend outside plot area (default: false) */
  boolean outside = false

  /** Whether data is already log-transformed (default: true) */
  boolean scaled = true

  /** Length of minor tick marks in pixels (default: 1.5) */
  BigDecimal shortLength = 1.5

  /** Length of intermediate tick marks in pixels (default: 2.25) */
  BigDecimal midLength = 2.25

  /** Length of major tick marks in pixels (default: 4.5) */
  BigDecimal longLength = 4.5

  /** Tick color */
  String colour = 'black'

  /** Tick line width */
  BigDecimal linewidth = 0.5

  /** Tick line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  GeomLogticks() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [:] as Map<String, Object>
  }

  GeomLogticks(Map params) {
    this()
    if (params.base != null) this.base = params.base as int
    if (params.sides) this.sides = params.sides as String
    if (params.outside != null) this.outside = params.outside as boolean
    if (params.scaled != null) this.scaled = params.scaled as boolean
    if (params.short != null) this.shortLength = params.short as BigDecimal
    if (params.mid != null) this.midLength = params.mid as BigDecimal
    if (params.long != null) this.longLength = params.long as BigDecimal
    if (params.colour) this.colour = ColorUtil.normalizeColor(params.colour as String)
    if (params.color) this.colour = ColorUtil.normalizeColor(params.color as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    Scale xScale = scales['x']
    Scale yScale = scales['y']

    if (xScale == null || yScale == null) return

    // Detect log scales and draw ticks accordingly
    boolean isXLog = xScale instanceof ScaleXLog10
    boolean isYLog = yScale instanceof ScaleYLog10

    if (!isXLog && !isYLog) return // No log scales, nothing to draw

    String sidesLower = sides.toLowerCase()

    // Draw x-axis ticks (bottom and/or top)
    if (isXLog && (sidesLower.contains('b') || sidesLower.contains('t'))) {
      List<BigDecimal> domain = (xScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getComputedDomain()
      List<Map> tickData = generateLogTickPositions(domain[0], domain[1], base)

      if (sidesLower.contains('b')) {
        drawXTicks(group, tickData, 'bottom', xScale, yScale)
      }
      if (sidesLower.contains('t')) {
        drawXTicks(group, tickData, 'top', xScale, yScale)
      }
    }

    // Draw y-axis ticks (left and/or right)
    if (isYLog && (sidesLower.contains('l') || sidesLower.contains('r'))) {
      List<BigDecimal> domain = (yScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getComputedDomain()
      List<Map> tickData = generateLogTickPositions(domain[0], domain[1], base)

      if (sidesLower.contains('l')) {
        drawYTicks(group, tickData, 'left', xScale, yScale)
      }
      if (sidesLower.contains('r')) {
        drawYTicks(group, tickData, 'right', xScale, yScale)
      }
    }
  }

  /**
   * Generate tick positions for logarithmic scale.
   * Returns list of maps with 'value' (in data space) and 'type' (major/intermediate/minor).
   *
   * For base 2 and other small bases (< 4), only major ticks are generated because
   * there are no intermediate integer multiples between powers.
   */
  private List<Map> generateLogTickPositions(BigDecimal logMin, BigDecimal logMax, int base) {
    List<Map> ticks = []

    int minPow = logMin.floor().intValue()
    int maxPow = logMax.ceil().intValue()

    for (int pow = minPow; pow <= maxPow; pow++) {
      // Major tick at base^pow
      BigDecimal majorValue = (base ** pow) as BigDecimal
      BigDecimal logMajor = pow as BigDecimal
      if (logMajor >= logMin && logMajor <= logMax) {
        ticks << [value: majorValue, type: 'major']
      }

      // Intermediate and minor ticks (multiples of base^pow)
      // Only generate for bases >= 4 where intermediate multiples exist
      // For base 2 or 3, there are no meaningful intermediate/minor ticks
      if (base >= 4) {
        for (int mult = 2; mult < base; mult++) {
          BigDecimal value = mult * (base ** pow)
          BigDecimal logValue = value.log(base)
          if (logValue >= logMin && logValue <= logMax) {
            // For base 10, both 2 and 5 are intermediate ticks (visually significant)
            // For other bases, only 2 is intermediate (5 is minor or may not exist)
            String type
            if (base == 10) {
              type = (mult == 2 || mult == 5) ? 'intermediate' : 'minor'
            } else {
              type = (mult == 2) ? 'intermediate' : 'minor'
            }
            ticks << [value: value, type: type]
          }
        }
      }
    }

    return ticks
  }

  /**
   * Draw tick marks for x-axis (bottom or top).
   */
  private void drawXTicks(G group, List<Map> tickData, String side, Scale xScale, Scale yScale) {
    List<BigDecimal> yRange = (yScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getRange()
    BigDecimal yPos = (side == 'bottom') ? yRange[1] : yRange[0]

    tickData.each { Map tick ->
      BigDecimal tickValue = tick.value as BigDecimal
      String tickType = tick.type as String

      // Transform value to pixel position
      BigDecimal xPx = xScale.transform(tickValue) as BigDecimal
      if (xPx == null) return

      // Determine tick length based on type
      BigDecimal tickLen = getTickLength(tickType)

      // Calculate tick endpoints
      BigDecimal y1, y2
      if (outside) {
        y1 = yPos
        y2 = (side == 'bottom') ? (yPos + tickLen) : (yPos - tickLen)
      } else {
        y1 = yPos
        y2 = (side == 'bottom') ? (yPos - tickLen) : (yPos + tickLen)
      }

      drawTickLine(group, xPx, y1, xPx, y2)
    }
  }

  /**
   * Draw tick marks for y-axis (left or right).
   */
  private void drawYTicks(G group, List<Map> tickData, String side, Scale xScale, Scale yScale) {
    List<BigDecimal> xRange = (xScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getRange()
    BigDecimal xPos = (side == 'left') ? xRange[0] : xRange[1]

    tickData.each { Map tick ->
      BigDecimal tickValue = tick.value as BigDecimal
      String tickType = tick.type as String

      // Transform value to pixel position
      BigDecimal yPx = yScale.transform(tickValue) as BigDecimal
      if (yPx == null) return

      // Determine tick length based on type
      BigDecimal tickLen = getTickLength(tickType)

      // Calculate tick endpoints
      BigDecimal x1, x2
      if (outside) {
        x1 = xPos
        x2 = (side == 'left') ? (xPos - tickLen) : (xPos + tickLen)
      } else {
        x1 = xPos
        x2 = (side == 'left') ? (xPos + tickLen) : (xPos - tickLen)
      }

      drawTickLine(group, x1, yPx, x2, yPx)
    }
  }

  /**
   * Get tick length based on tick type.
   */
  private BigDecimal getTickLength(String tickType) {
    switch (tickType) {
      case 'major':
        return longLength
      case 'intermediate':
        return midLength
      case 'minor':
        return shortLength
      default:
        return shortLength
    }
  }

  /**
   * Draw a single tick line.
   */
  private void drawTickLine(G group, BigDecimal x1, BigDecimal y1, BigDecimal x2, BigDecimal y2) {
    String lineColor = ColorUtil.normalizeColor(colour) ?: colour
    def line = group.addLine()
        .x1(x1 as int)
        .y1(y1 as int)
        .x2(x2 as int)
        .y2(y2 as int)
        .stroke(lineColor)

    line.addAttribute('stroke-width', linewidth)

    // Apply line type
    String dashArray = getLineDashArray(linetype)
    if (dashArray) {
      line.addAttribute('stroke-dasharray', dashArray)
    }

    // Apply alpha
    if (alpha < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }
  }

  /**
   * Convert line type name to SVG stroke-dasharray value.
   */
  private static String getLineDashArray(String type) {
    final Map<String, String> dashArrays = [
        dashed: '5,5',
        dotted: '2,2',
        longdash: '10,5',
        twodash: '10,5,2,5'
    ]
    dashArrays[type?.toLowerCase()]
  }
}
