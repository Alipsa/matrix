package se.alipsa.matrix.gg.geom


import se.alipsa.groovy.svg.G
import se.alipsa.matrix.gg.layer.StatType

/**
 * Label geometry with background for simple feature geometries.
 * Uses stat_sf_coordinates to compute label positions.
 */
class GeomSfLabel extends GeomLabel {

  GeomSfLabel() {
    super()
    defaultStat = StatType.SF_COORDINATES
    requiredAes = []
  }

  GeomSfLabel(Map params) {
    super(params)
    defaultStat = StatType.SF_COORDINATES
    requiredAes = []
  }

  /**
   * Render label boxes with x/y defaults from stat_sf_coordinates.
   *
   * @param group SVG group for rendering
   * @param data stat-expanded label coordinates
   * @param aes aesthetic mappings
   * @param scales scale map for rendering
   * @param coord coordinate system
   */

}
