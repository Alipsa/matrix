package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous size scale.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 */
@CompileStatic
class ScaleSizeContinuous extends ScaleContinuous {

  /** Output range [min, max] for size values. */
  List<Number> range = [1.0, 6.0] as List<Number>

  /** Size value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 3.0G

  /**
   * Create a continuous size scale with defaults.
   */
  ScaleSizeContinuous() {
    aesthetic = 'size'
    expand = [0, 0] as List<Number>
  }

  /**
   * Create a continuous size scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeContinuous(Map params) {
    aesthetic = 'size'
    expand = [0, 0] as List<Number>
    applyParams(params)
  }

  protected void applyParams(Map params) {
    if (params.range) this.range = params.range as List<Number>
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

    BigDecimal dMin = computedDomain[0] as BigDecimal
    BigDecimal dMax = computedDomain[1] as BigDecimal
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    if (dMax.compareTo(dMin) == 0) {
      return (rMin + rMax).divide(BigDecimal.valueOf(2), ScaleUtils.MATH_CONTEXT)
    }

    BigDecimal normalized = (v - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    return rMin + normalized * (rMax - rMin)
  }
}
