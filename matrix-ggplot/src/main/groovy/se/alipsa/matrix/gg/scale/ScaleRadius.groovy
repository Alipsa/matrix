package se.alipsa.matrix.gg.scale


/**
 * Radius-based size scale.
 * Missing or invalid values map to naValue (BigDecimal, nullable) inherited from ScaleSizeContinuous.
 */
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
