package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Radius-based size scale.
 */
@CompileStatic
class ScaleRadius extends ScaleSizeContinuous {

  /**
   * Create a radius scale with defaults.
   */
  ScaleRadius() {
    super()
  }

  /**
   * Create a radius scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleRadius(Map params) {
    super(params)
  }
}
