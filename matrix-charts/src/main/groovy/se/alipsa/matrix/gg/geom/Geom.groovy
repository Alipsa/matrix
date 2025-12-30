package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Base class for geometric objects (geoms).
 * Geoms determine how data is visually represented (points, lines, bars, etc.).
 */
@CompileStatic
class Geom {

  /** Default statistical transformation for this geom */
  StatType defaultStat = StatType.IDENTITY

  /** Default position adjustment for this geom */
  PositionType defaultPosition = PositionType.IDENTITY

  /** Fixed aesthetic parameters (not mapped to data) */
  Map params = [:]

  /** Required aesthetic mappings for this geom */
  List<String> requiredAes = []

  /** Optional aesthetic mappings with defaults */
  Map<String, Object> defaultAes = [:]

  /**
   * Render this geom to an SVG group.
   * Subclasses should override this method to provide actual rendering.
   *
   * @param group The SVG group to render into
   * @param data The transformed data (after stat and position adjustments)
   * @param aes The aesthetic mappings
   * @param scales Map of aesthetic name to Scale
   * @param coord The coordinate system
   */
  void render(G group, Matrix data, Aes aes,
              Map<String, Scale> scales, Coord coord) {
    throw new UnsupportedOperationException(
        "render() not yet implemented for ${this.class.simpleName}")
  }
}
