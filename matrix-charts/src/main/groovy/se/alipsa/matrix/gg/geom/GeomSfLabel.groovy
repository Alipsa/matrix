package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Label geometry with background for simple feature geometries.
 * Uses stat_sf_coordinates to compute label positions.
 */
@CompileStatic
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
