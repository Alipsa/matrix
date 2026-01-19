package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.stats.kde.Kernel
import se.alipsa.matrix.stats.kde.KernelDensity

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

  /** Fill color (null for no fill) */
  String fill

  /** Line color */
  String color = 'black'

  /** Line width */
  BigDecimal linewidth = 1

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Line type: 'solid', 'dashed', 'dotted', etc. */
  String linetype = 'solid'

  /** Bandwidth adjustment factor (multiplier for default bandwidth) */
  BigDecimal adjust = 1.0

  /** Kernel type: 'gaussian', 'epanechnikov', 'uniform', or 'triangular' */
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
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.linewidth = (params.linewidth ?: params.size) as BigDecimal ?: this.linewidth
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.linetype = params.linetype as String ?: this.linetype
    this.adjust = params.adjust as BigDecimal ?: this.adjust
    this.kernel = params.kernel as String ?: this.kernel
    this.n = params.n as Integer ?: this.n
    if (params.trim != null) this.trim = params.trim as boolean
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
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
    Map<Object, List<Number>> groups = data.rows()
        .findAll { row -> row[xCol] != null && row[xCol] instanceof Number }
        .groupBy { row -> groupCol ? row[groupCol] : '__all__' }
        .collectEntries { groupKey, rows ->
          [(groupKey): rows.collect { row -> row[xCol] as Number }]
        } as Map<Object, List<Number>>

    if (groups.isEmpty()) return

    int elementIndex = 0
    // Compute density for each group
    groups.each { groupKey, List<Number> xValues ->
      if (xValues.size() < 3) return

      List<DensityPoint> density = computeKernelDensity(xValues)
      if (density.isEmpty()) return

      // Find max density for y-scale training if not already set
      BigDecimal maxDensity = density.max { it.density }.density

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
      lineColor = ColorUtil.normalizeColor(lineColor) ?: lineColor
      areaFill = areaFill != null ? (ColorUtil.normalizeColor(areaFill) ?: areaFill) : null

      // Build path
      List<DensityPoint> transformedPoints = []
      density.each { DensityPoint point ->
        BigDecimal xPx = xScale?.transform(point.position) as BigDecimal
        // Scale y to pixel space - density values are typically small
        // Use yScale if available, otherwise scale to plot height
        BigDecimal yPx
        if (yScale != null) {
          yPx = yScale.transform(point.density) as BigDecimal
        } else {
          // Default: scale to 480px plot height with max density at ~80% height
          yPx = 480 - (point.density / maxDensity) * 480 * 0.8
        }

        if (xPx != null && yPx != null) {
          transformedPoints << new DensityPoint(xPx, yPx)
        }
      }

      if (transformedPoints.size() < 2) return

      // Draw filled area if fill is specified
      if (areaFill != null) {
        StringBuilder areaPath = new StringBuilder()
        DensityPoint first = transformedPoints[0]
        DensityPoint last = transformedPoints[transformedPoints.size() - 1]

        // Get baseline (bottom of plot or y=0)
        BigDecimal baseline = 480  // default

        areaPath << "M ${first.position} ${baseline}"
        transformedPoints.each { DensityPoint pt ->
          areaPath << " L ${pt.position} ${pt.density}"
        }
        areaPath << " L ${last.position} ${baseline}"
        areaPath << " Z"

        def area = group.addPath().d(areaPath.toString())
            .fill(areaFill)
            .stroke('none')

        if (alpha < 1.0) {
          area.addAttribute('fill-opacity', alpha)
        }

        // Apply CSS attributes to filled area
        GeomUtils.applyAttributes(area, ctx, 'density', 'gg-density', elementIndex)
      }

      // Draw line
      for (int i = 0; i < transformedPoints.size() - 1; i++) {
        DensityPoint p1 = transformedPoints[i]
        DensityPoint p2 = transformedPoints[i + 1]

        def line = group.addLine()
            .x1(p1.position)
            .y1(p1.density)
            .x2(p2.position)
            .y2(p2.density)
            .stroke(lineColor)

        line.addAttribute('stroke-width', linewidth)

        String dashArray = getDashArray(linetype)
        if (dashArray) {
          line.addAttribute('stroke-dasharray', dashArray)
        }

        if (alpha < 1.0 && areaFill == null) {
          line.addAttribute('stroke-opacity', alpha)
        }

        // Apply CSS attributes to line segments
        GeomUtils.applyAttributes(line, ctx, 'density', 'gg-density', elementIndex)
      }

      elementIndex++
    }
  }

  /**
   * Compute kernel density estimate using the configured kernel.
   * Delegates to the KernelDensity class from matrix-stats.
   */
  private List<DensityPoint> computeKernelDensity(List<Number> values) {
    if (values.size() < 2) return []

    // Handle edge case where all values are identical
    Set<Number> unique = new HashSet<>(values)
    if (unique.size() == 1) {
      BigDecimal val = values[0] as BigDecimal
      return [new DensityPoint(val, 1.0)]
    }

    int numPoints = [n, 256].min()

    KernelDensity kde = new KernelDensity(values, [
        kernel: kernel,
        adjust: adjust,
        n: numPoints,
        trim: trim
    ])

    // Convert from double[] arrays to DensityPoint objects
    return kde.toPointList().collect { double[] point ->
      new DensityPoint(point[0] as BigDecimal, point[1] as BigDecimal)
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
}
