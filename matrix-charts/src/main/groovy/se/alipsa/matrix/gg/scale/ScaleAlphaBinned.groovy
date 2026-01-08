package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.math.RoundingMode

/**
 * Binned alpha scale for continuous data.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 * <p>
 * This scale divides the continuous input data into discrete bins and maps
 * each bin to an alpha transparency value within the specified range.
 * <p>
 * <b>Important edge case:</b> When the data domain is constant (all values are identical),
 * the scale cannot compute a meaningful normalization. In this case, <em>any</em> input value
 * (including values that differ from the domain value) will map to the midpoint of the
 * output range. This behavior may be unexpected if you expect out-of-domain values to
 * be rejected or treated differently. If stricter validation is required, consider
 * pre-filtering the data or using a custom scale implementation.
 */
@CompileStatic
class ScaleAlphaBinned extends ScaleContinuous {

  /** Output range [min, max] for alpha values as BigDecimal. */
  List<BigDecimal> range = [0.2G, BigDecimal.ONE]

  /** Number of bins. */
  int bins = 5

  /** Alpha value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 1.0

  /**
   * Create a binned alpha scale with defaults.
   */
  ScaleAlphaBinned() {
    aesthetic = 'alpha'
    expand = NO_EXPAND
  }

  /**
   * Create a binned alpha scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleAlphaBinned(Map params) {
    aesthetic = 'alpha'
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

  /**
   * Transform a data value to an alpha value within the configured range.
   * <p>
   * Edge case: When all data values are identical (dMax == dMin), any input value
   * maps to the midpoint of the range. This includes values that differ from the
   * domain value, which may produce unexpected results if input validation is needed.
   *
   * @param value the data value to transform
   * @return the alpha value, or naValue if the input is null/invalid
   */
  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return naValue

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    // When domain is constant, all values map to the midpoint of the range
    if (dMax == dMin) {
      return (rMin + rMax) / 2
    }

    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    normalized = normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE)

    int binsCount = Math.max(1, bins)
    if (binsCount == 1) return rMin

    BigDecimal scaled = normalized * binsCount
    BigDecimal idx = (binsCount - 1).min(scaled.floor())
    BigDecimal t = idx / (binsCount - 1)
    return rMin + t * (rMax - rMin)
  }
}
