package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
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

}
