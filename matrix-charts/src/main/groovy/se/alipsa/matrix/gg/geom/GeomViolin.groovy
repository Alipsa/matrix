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
    double maxDensity = 0
    Map<Object, List<double[]>> densityData = [:]

    groups.each { xVal, yValues ->
      if (yValues.size() < 3) return

      List<double[]> density = computeKernelDensity(yValues)
      densityData[xVal] = density

      density.each { double[] point ->
        if (point[1] > maxDensity) maxDensity = point[1]
      }
    }

    if (maxDensity == 0) return

    // Get violin width in pixels
    double violinWidth = 50  // Default pixel width
    if (xScale != null) {
      // Try to get width from scale spacing
      def testX1 = xScale.transform(0)
      def testX2 = xScale.transform(1)
      if (testX1 != null && testX2 != null) {
        violinWidth = Math.abs((testX2 as double) - (testX1 as double)) * 0.4 * (width as double)
      }
    }

    // Render each violin
    densityData.each { xVal, density ->
      if (density.isEmpty()) return

      // Get center x position
      def xCenter = xScale?.transform(xVal)
      if (xCenter == null) return

      double centerX = xCenter as double

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
        double[] point = density[i]
        double yData = point[0]
        double densityVal = point[1]

        def yPx = yScale?.transform(yData)
        if (yPx == null) continue

        double y = yPx as double
        double halfWidth = (densityVal / maxDensity) * violinWidth

        if (first) {
          pathD << "M ${centerX + halfWidth} ${y}"
          first = false
        } else {
          pathD << " L ${centerX + halfWidth} ${y}"
        }
      }

      // Left half (bottom to top, mirrored)
      for (int i = density.size() - 1; i >= 0; i--) {
        double[] point = density[i]
        double yData = point[0]
        double densityVal = point[1]

        def yPx = yScale?.transform(yData)
        if (yPx == null) continue

        double y = yPx as double
        double halfWidth = (densityVal / maxDensity) * violinWidth

        pathD << " L ${centerX - halfWidth} ${y}"
      }

      pathD << " Z"

      // Draw violin
      def path = group.addPath().d(pathD.toString())
          .fill(violinFill)
          .stroke(color)

      path.addAttribute('stroke-width', linewidth)

      if ((alpha as double) < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }

      // Draw quantile lines if requested
      if (!draw_quantiles.isEmpty()) {
        List<Number> yValues = groups[xVal]
        List<Number> sorted = yValues.sort()

        draw_quantiles.each { Number q ->
          double quantileY = getQuantile(sorted, q as double)
          def qYPx = yScale?.transform(quantileY)
          if (qYPx != null) {
            double yPos = qYPx as double
            // Find density at this y position
            double densityAtQ = interpolateDensity(density, quantileY) / maxDensity * violinWidth

            def qLine = group.addLine()
                .x1((centerX - densityAtQ) as int)
                .y1(yPos as int)
                .x2((centerX + densityAtQ) as int)
                .y2(yPos as int)
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
  private List<double[]> computeKernelDensity(List<Number> values) {
    if (values.size() < 2) return []

    double[] data = values.collect { it as double } as double[]
    Arrays.sort(data)

    double min = data[0]
    double max = data[data.length - 1]
    double range = max - min

    if (range == 0) {
      // All values are the same
      return [[min, 1.0] as double[]]
    }

    // Silverman's rule of thumb for bandwidth
    double std = computeStd(data)
    double iqr = computeIQR(data)
    double bandwidth = 0.9 * Math.min(std, iqr / 1.34) * Math.pow(data.length, -0.2) * (adjust as double)

    if (bandwidth <= 0) bandwidth = range / 10

    // Extend range slightly
    double extension = trim ? 0.0 : range * 0.1
    double evalMin = min - extension
    double evalMax = max + extension

    // Compute density at n points
    List<double[]> result = []
    int numPoints = Math.min(n, 256)  // Limit for performance

    for (int i = 0; i < numPoints; i++) {
      double x = evalMin + (evalMax - evalMin) * i / (numPoints - 1)
      double density = 0

      for (double d : data) {
        double z = (x - d) / bandwidth
        density += Math.exp(-0.5 * z * z) / (Math.sqrt(2 * Math.PI) * bandwidth)
      }
      density /= data.length

      result << ([x, density] as double[])
    }

    return result
  }

  private double computeStd(double[] data) {
    double mean = 0
    for (double d : data) mean += d
    mean /= data.length

    double variance = 0
    for (double d : data) variance += (d - mean) * (d - mean)
    variance /= (data.length - 1)

    return Math.sqrt(variance)
  }

  private double computeIQR(double[] data) {
    int n = data.length
    double q1 = data[(int)(n * 0.25)]
    double q3 = data[(int)(n * 0.75)]
    return q3 - q1
  }

  private double getQuantile(List<Number> sorted, double q) {
    int n = sorted.size()
    double index = q * (n - 1)
    int lower = (int) index
    int upper = Math.min(lower + 1, n - 1)
    double frac = index - lower
    return (sorted[lower] as double) * (1 - frac) + (sorted[upper] as double) * frac
  }

  private double interpolateDensity(List<double[]> density, double y) {
    if (density.isEmpty()) return 0

    // Find surrounding points
    for (int i = 0; i < density.size() - 1; i++) {
      if (y >= density[i][0] && y <= density[i + 1][0]) {
        double frac = (y - density[i][0]) / (density[i + 1][0] - density[i][0])
        return density[i][1] * (1 - frac) + density[i + 1][1] * frac
      }
    }

    // Return closest endpoint
    if (y < density[0][0]) return density[0][1]
    return density[density.size() - 1][1]
  }
}
