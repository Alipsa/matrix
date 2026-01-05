package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.PositionType

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
