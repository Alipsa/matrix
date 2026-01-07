package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous size scale.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 */
@CompileStatic
class ScaleSizeContinuous extends ScaleContinuous {

  /** Output range [min, max] for size values as BigDecimal. */
  List<BigDecimal> range = [new BigDecimal('1.0'), new BigDecimal('6.0')]

  /** Size value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 3.0G

  /**
   * Create a continuous size scale with defaults.
   */
  ScaleSizeContinuous() {
    aesthetic = 'size'
    expand = NO_EXPAND
  }

  /**
   * Create a continuous size scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeContinuous(Map params) {
    aesthetic = 'size'
    expand = ScaleContinuous.NO_EXPAND
    applyParams(params)
  }

  protected void applyParams(Map params) {
    if (params.range) this.range = (params.range as List).collect { it as BigDecimal } as List<BigDecimal>
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue != null) this.naValue = ScaleUtils.coerceToNumber(params.naValue)
  }

  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return naValue

    BigDecimal result = ScaleUtils.linearTransform(v, computedDomain[0], computedDomain[1], range[0], range[1])
    return result != null ? result : naValue
  }
}
