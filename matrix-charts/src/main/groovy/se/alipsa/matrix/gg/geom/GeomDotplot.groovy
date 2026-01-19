package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Dot plot geometry for displaying distributions.
 * Stacks dots for each observation to show the distribution of data.
 *
 * Usage:
 * - geom_dotplot() - vertical dotplot
 * - geom_dotplot(binaxis: 'y') - horizontal dotplot
 * - geom_dotplot(binwidth: 0.5) - specify bin width
 * - geom_dotplot(method: 'histodot') - histodot stacking method
 */
@CompileStatic
class GeomDotplot extends Geom {

  /** Axis to bin along: 'x' (vertical) or 'y' (horizontal) */
  String binaxis = 'x'

  /** Method for stacking: 'dotdensity' or 'histodot' */
  String method = 'dotdensity'

  /** Bin width (auto-calculated if null) */
  Number binwidth = null

  /** Number of bins (used if binwidth not specified) */
  int bins = 30

  /** Stacking direction: 'up', 'down', 'center', 'centerwhole' */
  String stackdir = 'up'

  /** Ratio of dot height to bin width */
  Number dotsize = 1.0

  /** Dot fill color */
  String fill = 'black'

  /** Dot stroke color */
  String color = 'black'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Width of stack relative to binwidth */
  Number stackratio = 1.0

  GeomDotplot() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x']
    defaultAes = [fill: 'black', color: 'black', alpha: 1.0] as Map<String, Object>
  }

  GeomDotplot(Map params) {
    this()
    if (params.binaxis) this.binaxis = params.binaxis as String
    if (params.method) this.method = params.method as String
    if (params.binwidth != null) this.binwidth = params.binwidth as Number
    if (params.bins != null) this.bins = params.bins as int
    if (params.stackdir) this.stackdir = params.stackdir as String
    if (params.dotsize != null) this.dotsize = params.dotsize as Number
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.stackratio != null) this.stackratio = params.stackratio as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    boolean isVertical = binaxis == 'x'
    String binCol = isVertical ? aes.xColName : aes.yColName
    String stackCol = isVertical ? aes.yColName : aes.xColName
    String fillCol = aes.fillColName

    if (binCol == null) {
      throw new IllegalArgumentException("GeomDotplot requires ${binaxis} aesthetic")
    }

    Scale binScale = isVertical ? scales['x'] : scales['y']
    Scale stackScale = isVertical ? scales['y'] : scales['x']
    Scale fillScale = scales['fill']

    // Collect values for binning
    List<BigDecimal> binValues = []
    data.each { row ->
      def val = row[binCol]
      if (val instanceof Number) {
        binValues << (val as BigDecimal)
      }
    }

    if (binValues.isEmpty()) return

    // Calculate bin width if not specified
    BigDecimal bw
    if (binwidth != null) {
      bw = binwidth as BigDecimal
    } else {
      BigDecimal range = binValues.max() - binValues.min()
      bw = range > 0 ? range / bins : 1.0
    }

    // Group values into bins
    Map<Integer, List<Map>> binnedData = [:].withDefault { [] }
    data.each { row ->
      def binVal = row[binCol]
      if (!(binVal instanceof Number)) return

      BigDecimal val = binVal as BigDecimal
      BigDecimal minVal = binValues.min()
      int binIdx = ((val - minVal) / bw) as int

      binnedData[binIdx] << [
          binValue: val,
          stackValue: stackCol ? row[stackCol] : 0,
          fillValue: fillCol ? row[fillCol] : null,
          row: row
      ]
    }

    // Calculate dot radius based on bin width
    BigDecimal sampleBinCenter = binValues.min() + bw / 2
    BigDecimal p1 = binScale?.transform(sampleBinCenter) as BigDecimal
    BigDecimal p2 = binScale?.transform(sampleBinCenter + bw) as BigDecimal

    if (p1 == null || p2 == null) return

    BigDecimal binWidthPx = (p2 - p1).abs()
    BigDecimal dotRadius = (binWidthPx / 2) * dotsize * 0.9

    // Render dots for each bin
    binnedData.each { int binIdx, List<Map> points ->
      if (points.isEmpty()) return

      // Calculate bin center
      BigDecimal minVal = binValues.min()
      BigDecimal binCenter = minVal + binIdx * bw + bw / 2

      // Transform bin center to pixels
      def binCenterPx = binScale?.transform(binCenter)
      if (binCenterPx == null) return

      // Sort points by stack value (or just use order for dotdensity)
      if (method == 'histodot') {
        points = points.sort { it.stackValue }
      }

      // Stack dots
      int stackCount = points.size()
      for (int i = 0; i < stackCount; i++) {
        Map point = points[i]

        // Calculate stack position
        BigDecimal stackOffset = calculateStackOffset(i, stackCount, dotRadius)

        // Get base position (start of stack)
        BigDecimal basePx
        if (stackScale != null) {
          def baseVal = stackScale.transform(stackCol ? 0 : (point.stackValue ?: 0))
          basePx = baseVal != null ? (baseVal as BigDecimal) : 0
        } else {
          // Default to 0 for base position
          basePx = 0
        }

        // Calculate final positions
        BigDecimal cx, cy
        if (isVertical) {
          cx = binCenterPx as BigDecimal
          cy = basePx - stackOffset  // Stack upward (negative y)
        } else {
          cx = basePx + stackOffset  // Stack rightward (positive x)
          cy = binCenterPx as BigDecimal
        }

        // Determine fill color
        String dotFill = this.fill
        if (fillCol && point.fillValue != null) {
          if (fillScale) {
            dotFill = fillScale.transform(point.fillValue)?.toString() ?: this.fill
          } else {
            dotFill = GeomUtils.getDefaultColor(point.fillValue)
          }
        } else if (aes.fill instanceof Identity) {
          dotFill = (aes.fill as Identity).value.toString()
        }
        dotFill = ColorUtil.normalizeColor(dotFill) ?: dotFill

        // Draw dot
        def circle = group.addCircle()
            .cx(cx as int)
            .cy(cy as int)
            .r(dotRadius as int)
            .fill(dotFill)

        if (color != null) {
          circle.stroke(ColorUtil.normalizeColor(color))
          circle.addAttribute('stroke-width', 0.5)
        }

        if (alpha < 1.0) {
          circle.addAttribute('fill-opacity', alpha)
          if (color != null) {
            circle.addAttribute('stroke-opacity', alpha)
          }
        }
      }
    }
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() == 0) return

    boolean isVertical = binaxis == 'x'
    String binCol = isVertical ? aes.xColName : aes.yColName
    String stackCol = isVertical ? aes.yColName : aes.xColName
    String fillCol = aes.fillColName

    if (binCol == null) {
      throw new IllegalArgumentException("GeomDotplot requires ${binaxis} aesthetic")
    }

    Scale binScale = isVertical ? scales['x'] : scales['y']
    Scale stackScale = isVertical ? scales['y'] : scales['x']
    Scale fillScale = scales['fill']

    // Collect values for binning
    List<BigDecimal> binValues = []
    data.each { row ->
      def val = row[binCol]
      if (val instanceof Number) {
        binValues << (val as BigDecimal)
      }
    }

    if (binValues.isEmpty()) return

    // Calculate bin width if not specified
    BigDecimal bw
    if (binwidth != null) {
      bw = binwidth as BigDecimal
    } else {
      BigDecimal range = binValues.max() - binValues.min()
      bw = range > 0 ? range / bins : 1.0
    }

    // Group values into bins
    Map<Integer, List<Map>> binnedData = [:].withDefault { [] }
    data.each { row ->
      def binVal = row[binCol]
      if (!(binVal instanceof Number)) return

      BigDecimal val = binVal as BigDecimal
      BigDecimal minVal = binValues.min()
      int binIdx = ((val - minVal) / bw) as int

      binnedData[binIdx] << [
          binValue: val,
          stackValue: stackCol ? row[stackCol] : 0,
          fillValue: fillCol ? row[fillCol] : null,
          row: row
      ]
    }

    // Calculate dot radius based on bin width
    BigDecimal sampleBinCenter = binValues.min() + bw / 2
    BigDecimal p1 = binScale?.transform(sampleBinCenter) as BigDecimal
    BigDecimal p2 = binScale?.transform(sampleBinCenter + bw) as BigDecimal

    if (p1 == null || p2 == null) return

    BigDecimal binWidthPx = (p2 - p1).abs()
    BigDecimal dotRadius = (binWidthPx / 2) * dotsize * 0.9

    int elementIndex = 0
    // Render dots for each bin
    binnedData.each { int binIdx, List<Map> points ->
      if (points.isEmpty()) return

      // Calculate bin center
      BigDecimal minVal = binValues.min()
      BigDecimal binCenter = minVal + binIdx * bw + bw / 2

      // Transform bin center to pixels
      def binCenterPx = binScale?.transform(binCenter)
      if (binCenterPx == null) return

      // Sort points by stack value (or just use order for dotdensity)
      if (method == 'histodot') {
        points = points.sort { it.stackValue }
      }

      // Stack dots
      int stackCount = points.size()
      for (int i = 0; i < stackCount; i++) {
        Map point = points[i]

        // Calculate stack position
        BigDecimal stackOffset = calculateStackOffset(i, stackCount, dotRadius)

        // Get base position (start of stack)
        BigDecimal basePx
        if (stackScale != null) {
          def baseVal = stackScale.transform(stackCol ? 0 : (point.stackValue ?: 0))
          basePx = baseVal != null ? (baseVal as BigDecimal) : 0
        } else {
          // Default to 0 for base position
          basePx = 0
        }

        // Calculate final positions
        BigDecimal cx, cy
        if (isVertical) {
          cx = binCenterPx as BigDecimal
          cy = basePx - stackOffset  // Stack upward (negative y)
        } else {
          cx = basePx + stackOffset  // Stack rightward (positive x)
          cy = binCenterPx as BigDecimal
        }

        // Determine fill color
        String dotFill = this.fill
        if (fillCol && point.fillValue != null) {
          if (fillScale) {
            dotFill = fillScale.transform(point.fillValue)?.toString() ?: this.fill
          } else {
            dotFill = GeomUtils.getDefaultColor(point.fillValue)
          }
        } else if (aes.fill instanceof Identity) {
          dotFill = (aes.fill as Identity).value.toString()
        }
        dotFill = ColorUtil.normalizeColor(dotFill) ?: dotFill

        // Draw dot
        def circle = group.addCircle()
            .cx(cx as int)
            .cy(cy as int)
            .r(dotRadius as int)
            .fill(dotFill)

        if (color != null) {
          circle.stroke(ColorUtil.normalizeColor(color))
          circle.addAttribute('stroke-width', 0.5)
        }

        if (alpha < 1.0) {
          circle.addAttribute('fill-opacity', alpha)
          if (color != null) {
            circle.addAttribute('stroke-opacity', alpha)
          }
        }

        // Apply CSS attributes
        GeomUtils.applyAttributes(circle, ctx, 'dotplot', 'gg-dotplot', elementIndex)
        elementIndex++
      }
    }
  }

  /**
   * Calculate the stack offset for a dot at position i in a stack of count dots.
   */
  private BigDecimal calculateStackOffset(int i, int count, BigDecimal dotRadius) {
    BigDecimal dotDiameter = dotRadius * 2

    switch (stackdir) {
      case 'up':
        return i * dotDiameter * stackratio
      case 'down':
        return -(i * dotDiameter * stackratio)
      case 'center':
        BigDecimal totalHeight = count * dotDiameter * stackratio
        return i * dotDiameter * stackratio - totalHeight / 2
      case 'centerwhole':
        BigDecimal totalHeight = count * dotDiameter * stackratio
        return i * dotDiameter * stackratio - totalHeight / 2 + dotRadius
      default:
        return i * dotDiameter * stackratio
    }
  }
}
