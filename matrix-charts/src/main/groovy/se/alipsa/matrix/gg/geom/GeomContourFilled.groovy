package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Filled contour geometry for drawing filled regions between contour levels.
 * Uses marching squares to create filled polygons between isolines.
 *
 * Usage:
 * - geom_contour_filled() - filled contours with automatic levels
 * - geom_contour_filled(bins: 10) - specify number of contour levels
 * - geom_contour_filled(binwidth: 0.5) - specify spacing between levels
 *
 * Data format:
 * - Grid data with x, y, z columns
 */
@CompileStatic
class GeomContourFilled extends GeomContour {

  /** Fill alpha transparency (0-1) */
  Number fillAlpha = 0.8

  /** Default color palette for fills */
  List<String> fillColors = [
      '#440154', '#482878', '#3E4A89', '#31688E', '#26838E',
      '#1F9E89', '#35B779', '#6DCD59', '#B4DE2C', '#FDE725'
  ]

  GeomContourFilled() {
    super()
    defaultAes = [fill: 'blue', alpha: 0.8] as Map<String, Object>
  }

  GeomContourFilled(Map params) {
    super(params)
    if (params.fillAlpha != null) this.fillAlpha = params.fillAlpha as Number
    if (params.fill_colors) this.fillColors = params.fill_colors as List<String>
    if (params.fillColors) this.fillColors = params.fillColors as List<String>
    this.fillColors = this.fillColors.collect { ColorUtil.normalizeColor(it) }
  }

}
