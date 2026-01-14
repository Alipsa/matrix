package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Violin geometry for showing kernel density estimates.
 * Like a mirrored density plot, useful for comparing distributions.
 *
 * Usage:
 * - geom_violin() - basic violin plot
 * - geom_violin(fill: 'lightblue', alpha: 0.7)
 * - geom_violin(draw_quantiles: [0.25, 0.5, 0.75]) - with quantile lines
 */
@CompileStatic
class GeomViolin extends Geom {

  /**
   * Data class to hold kernel density estimation points.
   */
  @CompileStatic
  static class DensityPoint {
    final BigDecimal position
    final BigDecimal density

    DensityPoint(BigDecimal position, BigDecimal density) {
      this.position = position
      this.density = density
    }
  }

  /** Fill color */
  String fill = 'gray'

  /** Outline color */
  String color = 'black'

  /** Line width for outline */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 0.7

  /** Width of violin (relative, 0-1) */
  Number width = 0.9

  /** Whether to trim violin to data range */
  boolean trim = true

  /** Whether to scale all violins to same max width ('area', 'count', 'width') */
  String scale = 'area'

  /** Quantiles to draw as lines */
  List<Number> draw_quantiles = []

  /** Number of points for density estimation */
  int n = 512

  /** Bandwidth adjustment factor (multiplier for default bandwidth) */
  Number adjust = 1.0

  GeomViolin() {
    defaultStat = StatType.IDENTITY  // We compute density internally
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomViolin(Aes aes) {
    this()
  }

  GeomViolin(Map params) {
    this()
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.width != null) this.width = params.width as Number
    if (params.trim != null) this.trim = params.trim as boolean
    if (params.scale) this.scale = params.scale as String
    if (params.draw_quantiles) this.draw_quantiles = params.draw_quantiles as List<Number>
    if (params.n != null) this.n = params.n as int
    if (params.adjust != null) this.adjust = params.adjust as Number
    this.fill = ColorUtil.normalizeColor(this.fill)
    this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 3) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomViolin requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Group data by x value
    Map<Object, List<Number>> groups = [:]
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal == null || yVal == null || !(yVal instanceof Number)) return

      if (!groups.containsKey(xVal)) {
        groups[xVal] = []
      }
      groups[xVal] << (yVal as Number)
    }

    if (groups.isEmpty()) return

    // Calculate max density for scaling
    BigDecimal maxDensity = 0
    Map<Object, List<DensityPoint>> densityData = [:]

    groups.each { xVal, yValues ->
      if (yValues.size() < 3) return

      List<DensityPoint> density = computeKernelDensity(yValues)
      densityData[xVal] = density

      density.each { DensityPoint point ->
        if (point.density > maxDensity) maxDensity = point.density
      }
    }

    if (maxDensity == 0) return

    // Get violin width in pixels
    BigDecimal violinWidth = 50  // Default pixel width
    if (xScale != null) {
      // Try to get width from scale spacing
      Number testX1 = xScale.transform(0) as Number
      Number testX2 = xScale.transform(1) as Number
      if (testX1 != null && testX2 != null) {
        BigDecimal test1 = testX1 as BigDecimal
        BigDecimal test2 = testX2 as BigDecimal
        violinWidth = (test2 - test1).abs() * 0.4 * (width as BigDecimal)
      }
    }

    // Render each violin
    densityData.each { xVal, density ->
      if (density.isEmpty()) return

      // Get center x position
      Number xCenter = xScale?.transform(xVal) as Number
      if (xCenter == null) return

      BigDecimal centerX = xCenter as BigDecimal

      // Determine fill color
      String violinFill = this.fill
      if (fillCol && fillScale) {
        violinFill = fillScale.transform(xVal)?.toString() ?: this.fill
      }
      violinFill = ColorUtil.normalizeColor(violinFill)

      // Build violin path (mirrored density)
      StringBuilder pathD = new StringBuilder()

      // Right half (top to bottom)
      boolean first = true
      for (int i = 0; i < density.size(); i++) {
        DensityPoint point = density[i]
        BigDecimal yData = point.position
        BigDecimal densityVal = point.density

        Number yPx = yScale?.transform(yData) as Number
        if (yPx == null) continue

        BigDecimal y = yPx as BigDecimal
        BigDecimal halfWidth = (densityVal / maxDensity) * violinWidth

        if (first) {
          pathD << "M ${centerX + halfWidth} ${y}"
          first = false
        } else {
          pathD << " L ${centerX + halfWidth} ${y}"
        }
      }

      // Left half (bottom to top, mirrored)
      for (int i = density.size() - 1; i >= 0; i--) {
        DensityPoint point = density[i]
        BigDecimal yData = point.position
        BigDecimal densityVal = point.density

        Number yPx = yScale?.transform(yData) as Number
        if (yPx == null) continue

        BigDecimal y = yPx as BigDecimal
        BigDecimal halfWidth = (densityVal / maxDensity) * violinWidth

        pathD << " L ${centerX - halfWidth} ${y}"
      }

      pathD << " Z"

      // Draw violin
      def path = group.addPath().d(pathD.toString())
          .fill(violinFill)
          .stroke(color)

      path.addAttribute('stroke-width', linewidth)

      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }

      // Draw quantile lines if requested
      if (!draw_quantiles.isEmpty()) {
        List<Number> yValues = groups[xVal]
        List<Number> sorted = yValues.sort()

        draw_quantiles.each { Number q ->
          BigDecimal quantileY = getQuantile(sorted, q as BigDecimal)
          Number qYPx = yScale?.transform(quantileY) as Number
          if (qYPx != null) {
            BigDecimal yPos = qYPx as BigDecimal
            // Find density at this y position
            BigDecimal densityAtQ = interpolateDensity(density, quantileY) / maxDensity * violinWidth

            def qLine = group.addLine()
                .x1(centerX - densityAtQ)
                .y1(yPos)
                .x2(centerX + densityAtQ)
                .y2(yPos)
                .stroke(color)
            qLine.addAttribute('stroke-width', linewidth)
          }
        }
      }
    }
  }

  /**
   * Compute kernel density estimate using Gaussian kernel.
   */
  private List<DensityPoint> computeKernelDensity(List<Number> values) {
    if (values.size() < 2) return []

    List<BigDecimal> data = values.collect { it as BigDecimal }.sort()

    BigDecimal min = data[0]
    BigDecimal max = data[data.size() - 1]
    BigDecimal range = max - min

    if (range == 0) {
      // All values are the same
      return [new DensityPoint(min, 1.0)]
    }

    // Silverman's rule of thumb for bandwidth
    BigDecimal std = computeStd(data)
    BigDecimal iqr = computeIQR(data)
    // Use Math.pow for fractional power (BigDecimal.pow only accepts int)
    BigDecimal sizePower = Math.pow(data.size() as double, -0.2 as double) as BigDecimal
    BigDecimal bandwidth = (0.9 as BigDecimal) * std.min(iqr / 1.34) * sizePower * (adjust as BigDecimal)

    if (bandwidth <= 0) bandwidth = range / 10

    // Extend range slightly
    BigDecimal extension = trim ? 0 : range * 0.1
    BigDecimal evalMin = min - extension
    BigDecimal evalMax = max + extension

    // Compute density at n points
    List<DensityPoint> result = []
    int numPoints = ([n, 256].min()) as int  // Limit for performance

    for (int i = 0; i < numPoints; i++) {
      BigDecimal x = evalMin + (evalMax - evalMin) * i / (numPoints - 1)
      BigDecimal density = 0

      for (BigDecimal d : data) {
        BigDecimal z = (x - d) / bandwidth
        BigDecimal sqrtTwoPi = (2 * PI).sqrt()
        density += (-0.5 * z * z).exp() / (sqrtTwoPi * bandwidth)
      }
      density /= data.size()

      result << new DensityPoint(x, density)
    }

    return result
  }

  private BigDecimal computeStd(List<BigDecimal> data) {
    BigDecimal mean = 0
    for (BigDecimal d : data) mean += d
    mean /= data.size()

    BigDecimal variance = 0
    for (BigDecimal d : data) variance += (d - mean) * (d - mean)
    variance /= (data.size() - 1)

    return variance.sqrt()
  }

  private BigDecimal computeIQR(List<BigDecimal> data) {
    int n = data.size()
    BigDecimal q1 = data[(n * 0.25) as int]
    BigDecimal q3 = data[(n * 0.75) as int]
    return q3 - q1
  }

  private BigDecimal getQuantile(List<Number> sorted, BigDecimal q) {
    int n = sorted.size()
    BigDecimal index = q * (n - 1)
    int lower = index as int
    int upper = ([lower + 1, n - 1].min()) as int
    BigDecimal frac = index - lower
    return (sorted[lower] as BigDecimal) * (1 - frac) + (sorted[upper] as BigDecimal) * frac
  }

  private BigDecimal interpolateDensity(List<DensityPoint> density, BigDecimal y) {
    if (density.isEmpty()) return 0

    // Find surrounding points
    for (int i = 0; i < density.size() - 1; i++) {
      DensityPoint curr = density[i]
      DensityPoint next = density[i + 1]
      if (y >= curr.position && y <= next.position) {
        BigDecimal frac = (y - curr.position) / (next.position - curr.position)
        return curr.density * (1 - frac) + next.density * frac
      }
    }

    // Return closest endpoint
    if (y < density[0].position) return density[0].density
    return density[density.size() - 1].density
  }
}
