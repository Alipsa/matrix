package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

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

  /** Output range [min, max] for alpha values. */
  List<Number> range = [0.2, 1.0] as List<Number>

  /** Number of bins. */
  int bins = 5

  /** Alpha value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 1.0G

  /**
   * Create a binned alpha scale with defaults.
   */
  ScaleAlphaBinned() {
    aesthetic = 'alpha'
    expand = [0, 0] as List<Number>
  }

  /**
   * Create a binned alpha scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleAlphaBinned(Map params) {
    aesthetic = 'alpha'
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

    BigDecimal dMin = computedDomain[0] as BigDecimal
    BigDecimal dMax = computedDomain[1] as BigDecimal
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal
    // When domain is constant, all values map to the midpoint of the range
    if (dMax.compareTo(dMin) == 0) {
      return (rMin + rMax).divide(BigDecimal.valueOf(2), ScaleUtils.MATH_CONTEXT)
    }

    BigDecimal normalized = (v - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    normalized = normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE)

    int binsCount = Math.max(1, bins)
    if (binsCount == 1) return rMin

    BigDecimal scaled = normalized * BigDecimal.valueOf(binsCount)
    int idx = Math.min(binsCount - 1, scaled.intValue())
    BigDecimal t = BigDecimal.valueOf(idx).divide(BigDecimal.valueOf(binsCount - 1), ScaleUtils.MATH_CONTEXT)
    return rMin + t * (rMax - rMin)
  }
}
