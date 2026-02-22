package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Filled 2D density contours geometry.
 * Computes 2D kernel density estimation and renders filled contour regions.
 *
 * Usage:
 * - geom_density_2d_filled() - default filled density contours
 * - geom_density_2d_filled(bins: 10) - specify number of contour levels
 * - geom_density_2d_filled(h: [0.5, 0.5]) - specify bandwidth for KDE
 * - geom_density_2d_filled(n: 100) - number of grid points
 */
@CompileStatic
class GeomDensity2dFilled extends Geom {

  /** Fill colors (will be interpolated based on levels) */
  List<String> fillColors = [
      '#f7fbff', '#deebf7', '#c6dbef', '#9ecae1', '#6baed6',
      '#4292c6', '#2171b5', '#08519c', '#08306b'
  ]

  /** Stroke color for contour borders */
  String color = null

  /** Line width for borders */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Number of contour bins/levels */
  int bins = 10

  /** Grid size for density estimation */
  int n = 100

  /** Bandwidth for KDE [h_x, h_y]. If null, auto-calculated */
  List<Number> h = null

  GeomDensity2dFilled() {
    defaultStat = StatType.DENSITY_2D
    requiredAes = ['x', 'y']
    defaultAes = [alpha: 1.0] as Map<String, Object>
  }

  GeomDensity2dFilled(Map params) {
    this()
    if (params.fillColors) this.fillColors = params.fillColors as List<String>
    if (params.fill_colors) this.fillColors = params.fill_colors as List<String>
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.bins != null) this.bins = params.bins as int
    if (params.n != null) this.n = params.n as int
    if (params.h) this.h = params.h as List<Number>
    this.fillColors = this.fillColors.collect { ColorUtil.normalizeColor(it) }
    if (this.color) this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

}
