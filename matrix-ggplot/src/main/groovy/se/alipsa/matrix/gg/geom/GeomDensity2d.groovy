package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.pictura.util.ColorUtil

/**
 * 2D density contour lines geometry.
 * Computes 2D kernel density estimation and renders contour lines.
 *
 * Usage:
 * - geom_density_2d() - default density contours
 * - geom_density_2d(bins: 10) - specify number of contour levels
 * - geom_density_2d(h: [0.5, 0.5]) - specify bandwidth for KDE
 * - geom_density_2d(n: 100) - number of grid points
 */
@CompileStatic
class GeomDensity2d extends Geom {

  /** Line color */
  String color = 'black'

  /** Line width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Line type */
  String linetype = 'solid'

  /** Number of contour bins/levels */
  int bins = 10

  /** Grid size for density estimation */
  int n = 100

  /** Bandwidth for KDE [h_x, h_y]. If null, auto-calculated */
  List<Number> h = null

  GeomDensity2d() {
    defaultStat = StatType.DENSITY_2D
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', linewidth: 0.5] as Map<String, Object>
  }

  GeomDensity2d(Map params) {
    this()
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.bins != null) this.bins = params.bins as int
    if (params.n != null) this.n = params.n as int
    if (params.h) this.h = params.h as List<Number>
    this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

}
