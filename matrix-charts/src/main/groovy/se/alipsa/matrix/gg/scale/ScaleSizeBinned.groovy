package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.math.RoundingMode

/**
 * Binned size scale for continuous data.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 */
@CompileStatic
class ScaleSizeBinned extends ScaleContinuous {

  /** Output range [min, max] for size values. */
  List<Number> range = [1.0, 6.0] as List<Number>

  /** Number of bins. */
  int bins = 5

  /** Size value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 3.0G

  /**
   * Create a binned size scale with defaults.
   */
  ScaleSizeBinned() {
    aesthetic = 'size'
    expand = [0, 0] as List<Number>
  }

  /**
   * Create a binned size scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeBinned(Map params) {
    aesthetic = 'size'
    expand = [0, 0] as List<Number>
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.range) this.range = params.range as List<Number>
    if (params.bins != null) this.bins = (params.bins as Number).intValue()
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
      return (rMin + rMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)
    }

    BigDecimal normalized = (v - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    normalized = normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE)

    int binsCount = Math.max(1, bins)
    if (binsCount == 1) return rMin

    BigDecimal scaled = normalized * BigDecimal.valueOf(binsCount)
    int idx = Math.min(binsCount - 1, scaled.setScale(0, RoundingMode.DOWN).intValue())
    BigDecimal t = BigDecimal.valueOf(idx).divide(BigDecimal.valueOf(binsCount - 1), ScaleUtils.MATH_CONTEXT)
    return rMin + t * (rMax - rMin)
  }
}
