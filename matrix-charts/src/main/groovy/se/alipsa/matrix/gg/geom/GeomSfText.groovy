package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Text labels for simple feature geometries.
 * Uses stat_sf_coordinates to compute label positions.
 */
@CompileStatic
class GeomSfText extends GeomText {

  GeomSfText() {
    super()
    defaultStat = StatType.SF_COORDINATES
    requiredAes = []
  }

  GeomSfText(Map params) {
    super(params)
    defaultStat = StatType.SF_COORDINATES
    requiredAes = []
  }

  /**
   * Render text labels with x/y defaults from stat_sf_coordinates.
   *
   * @param group SVG group for rendering
   * @param data stat-expanded label coordinates
   * @param aes aesthetic mappings
   * @param scales scale map for rendering
   * @param coord coordinate system
   */
  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    Aes resolvedAes = resolveAes(aes)
    super.render(group, data, resolvedAes, scales, coord)
  }

  private static Aes resolveAes(Aes aes) {
    Aes base = new Aes([x: 'x', y: 'y'])
    return aes != null ? aes.merge(base) : base
  }
}
