package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.math.RoundingMode

/**
 * Binned size scale for continuous data.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 */
@CompileStatic
class ScaleSizeBinned extends ScaleContinuous {

  /** Output range [min, max] for size values as BigDecimal. */
  List<BigDecimal> range = [1.0, 6.0]

  /** Number of bins. */
  int bins = 5

  /** Size value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 3.0

  /**
   * Create a binned size scale with defaults.
   */
  ScaleSizeBinned() {
    aesthetic = 'size'
    expand = NO_EXPAND
  }

  /**
   * Create a binned size scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeBinned(Map params) {
    aesthetic = 'size'
    expand = ScaleContinuous.NO_EXPAND
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.range) this.range = (params.range as List).collect { it as BigDecimal } as List<BigDecimal>
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

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) {
      return (rMin + rMax) / 2
    }

    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    normalized = normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE)

    int binsCount = 1.max(bins) as int
    if (binsCount == 1) return rMin

    BigDecimal scaled = normalized * binsCount
    BigDecimal idx = (binsCount - 1).min(scaled.floor())
    BigDecimal t = idx / (binsCount - 1)
    return rMin + t * (rMax - rMin)
  }
}
