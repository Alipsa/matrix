package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous alpha scale.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 */
@CompileStatic
class ScaleAlphaContinuous extends ScaleContinuous {

  /** Output range [min, max] for alpha values as BigDecimal. */
  List<BigDecimal> range = [new BigDecimal('0.1'), BigDecimal.ONE]

  /** Alpha value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 1.0G

  /**
   * Create a continuous alpha scale with defaults.
   */
  ScaleAlphaContinuous() {
    aesthetic = 'alpha'
    expand = [0, 0] as List<Number>
  }

  /**
   * Create a continuous alpha scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleAlphaContinuous(Map params) {
    aesthetic = 'alpha'
    expand = [0, 0] as List<Number>
    applyParams(params)
  }

  private void applyParams(Map params) {
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

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) {
      return (rMin + rMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)
    }

    BigDecimal normalized = (v - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    BigDecimal mapped = rMin + normalized * (rMax - rMin)
    BigDecimal low = rMin.min(rMax)
    BigDecimal high = rMin.max(rMax)
    return mapped.max(low).min(high)
  }
}
