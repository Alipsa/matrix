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
 * Contour geometry for drawing contour lines from 2D density/height data.
 * Uses marching squares algorithm to trace isolines.
 *
 * Usage:
 * - geom_contour() - contour lines with automatic levels
 * - geom_contour(bins: 10) - specify number of contour levels
 * - geom_contour(binwidth: 0.5) - specify spacing between levels
 *
 * Data format:
 * - Grid data with x, y, z columns
 * - Or use stat_contour to compute from density
 */
@CompileStatic
class GeomContour extends Geom {

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

  /** Spacing between contour levels (overrides bins if set) */
  Number binwidth

  GeomContour() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y', 'z']
    defaultAes = [color: 'black', linewidth: 0.5] as Map<String, Object>
  }

  GeomContour(Map params) {
    this()
    if (params.color) this.color = params.color as String
    if (params.colour) this.color = params.colour as String
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.bins != null) this.bins = params.bins as int
    if (params.binwidth != null) this.binwidth = params.binwidth as Number
    this.color = ColorUtil.normalizeColor(this.color)
    this.params = params
  }

}
