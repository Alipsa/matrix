package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Density geometry for showing kernel density estimates.
 * Displays a smooth curve showing the distribution of data.
 *
 * Usage:
 * - geom_density() - basic density plot
 * - geom_density(fill: 'lightblue', alpha: 0.5) - filled density
 * - geom_density(adjust: 0.5) - narrower bandwidth for more detail
 */
@CompileStatic
class GeomDensity extends Geom {

  /** Fill color (null for no fill) */
  String fill

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 1

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Line type: 'solid', 'dashed', 'dotted', etc. */
  String linetype = 'solid'

  /** Bandwidth adjustment factor (multiplier for default bandwidth) */
  Number adjust = 1.0

  /** Kernel type: 'gaussian' (only gaussian supported currently) */
  String kernel = 'gaussian'

  /** Number of points for density estimation */
  int n = 512

  /** Whether to trim to data range */
  boolean trim = false

  GeomDensity() {
    defaultStat = StatType.IDENTITY  // We compute density internally
    requiredAes = ['x']
    defaultAes = [color: 'black', linewidth: 1] as Map<String, Object>
  }

  GeomDensity(Map params) {
    this()
    if (params.fill) this.fill = params.fill as String
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.adjust != null) this.adjust = params.adjust as Number
    if (params.kernel) this.kernel = params.kernel as String
    if (params.n != null) this.n = params.n as int
    if (params.trim != null) this.trim = params.trim as boolean
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() < 3) return

    String xCol = aes.xColName
    String colorCol = aes.colorColName
    String fillCol = aes.fillColName
    String groupCol = aes.groupColName ?: colorCol ?: fillCol

    if (xCol == null) {
      throw new IllegalArgumentException("GeomDensity requires x aesthetic")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale fillScale = scales['fill']

    // Group data if needed
    Map<Object, List<Number>> groups = [:]
    data.each { row ->
      def xVal = row[xCol]
      if (xVal == null || !(xVal instanceof Number)) return

      def groupKey = groupCol ? row[groupCol] : '__all__'
      if (!groups.containsKey(groupKey)) {
        groups[groupKey] = []
      }
      groups[groupKey] << (xVal as Number)
    }

    if (groups.isEmpty()) return

    // Compute density for each group
    groups.each { groupKey, xValues ->
      if (xValues.size() < 3) return

      List<double[]> density = computeKernelDensity(xValues)
      if (density.isEmpty()) return

      // Find max density for y-scale training if not already set
      double maxDensity = density.max { it[1] }[1]

      // Determine colors
      String lineColor = this.color
      String areaFill = this.fill

      if (colorCol && groupKey != '__all__') {
        if (colorScale) {
          lineColor = colorScale.transform(groupKey)?.toString() ?: this.color
        }
      }

      if (fillCol && groupKey != '__all__') {
        if (fillScale) {
          areaFill = fillScale.transform(groupKey)?.toString() ?: this.fill
        }
      }

      // Build path
      List<double[]> transformedPoints = []
      density.each { double[] point ->
        def xPx = xScale?.transform(point[0])
        // Scale y to pixel space - density values are typically small
        // Use yScale if available, otherwise scale to plot height
        double yPx
        if (yScale != null) {
          def yTransformed = yScale.transform(point[1])
          yPx = yTransformed != null ? yTransformed as double : 0
        } else {
          // Default: scale to 480px plot height with max density at ~80% height
          yPx = 480 - (point[1] / maxDensity) * 480 * 0.8
        }

        if (xPx != null) {
          transformedPoints << ([xPx as double, yPx] as double[])
        }
      }

      if (transformedPoints.size() < 2) return

      // Draw filled area if fill is specified
      if (areaFill != null) {
        StringBuilder areaPath = new StringBuilder()
        double[] first = transformedPoints[0]
        double[] last = transformedPoints[transformedPoints.size() - 1]

        // Get baseline (bottom of plot or y=0)
        double baseline = 480  // default

        areaPath << "M ${first[0]} ${baseline}"
        transformedPoints.each { double[] pt ->
          areaPath << " L ${pt[0]} ${pt[1]}"
        }
        areaPath << " L ${last[0]} ${baseline}"
        areaPath << " Z"

        def area = group.addPath().d(areaPath.toString())
            .fill(areaFill)
            .stroke('none')

        if ((alpha as double) < 1.0) {
          area.addAttribute('fill-opacity', alpha)
        }
      }

      // Draw line
      for (int i = 0; i < transformedPoints.size() - 1; i++) {
        double[] p1 = transformedPoints[i]
        double[] p2 = transformedPoints[i + 1]

        def line = group.addLine()
            .x1(p1[0] as int)
            .y1(p1[1] as int)
            .x2(p2[0] as int)
            .y2(p2[1] as int)
            .stroke(lineColor)

        line.addAttribute('stroke-width', linewidth)

        String dashArray = getDashArray(linetype)
        if (dashArray) {
          line.addAttribute('stroke-dasharray', dashArray)
        }

        if ((alpha as double) < 1.0 && areaFill == null) {
          line.addAttribute('stroke-opacity', alpha)
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
      return [[min, 1.0] as double[]]
    }

    // Silverman's rule of thumb for bandwidth
    double std = computeStd(data)
    double iqr = computeIQR(data)
    double bandwidth = 0.9 * Math.min(std, iqr / 1.34) * Math.pow(data.length, -0.2) * (adjust as double)

    if (bandwidth <= 0) bandwidth = range / 10

    // Extend range
    double extension = trim ? 0.0 : range * 0.1
    double evalMin = min - extension
    double evalMax = max + extension

    // Compute density
    List<double[]> result = []
    int numPoints = Math.min(n, 256)

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
}
