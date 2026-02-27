package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Jittered point geometry.
 * Convenience wrapper for points with position_jitter applied.
 */
@CompileStatic
class GeomJitter extends GeomPoint {

  /**
   * Create a jittered point geom with default settings.
   */
  GeomJitter() {
    super()
    defaultPosition = PositionType.JITTER
  }

  /**
   * Create a jittered point geom with parameters.
   *
   * @param params geom parameters
   */
  GeomJitter(Map params) {
    super(params)
    defaultPosition = PositionType.JITTER
  }

}
