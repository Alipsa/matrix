package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned alpha scale for continuous data.
 */
@CompileStatic
class ScaleAlphaBinned extends ScaleContinuous {

  /** Output range [min, max] for alpha values. */
  List<Number> range = [0.2, 1.0] as List<Number>

  /** Number of bins. */
  int bins = 5

  /** Alpha value for NA/missing values. */
  Number naValue = 1.0

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
    if (params.naValue != null) this.naValue = params.naValue as Number
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
    Double numeric = ScaleUtils.coerceToNumber(value)
    if (numeric == null) return naValue

    double v = numeric
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    // When domain is constant, all values map to the midpoint of the range
    if (dMax == dMin) return (range[0] as double + range[1] as double) / 2.0d

    double normalized = (v - dMin) / (dMax - dMin)
    normalized = Math.max(0.0d, Math.min(1.0d, normalized))

    int binsCount = Math.max(1, bins)
    int idx = Math.min(binsCount - 1, (int) Math.floor(normalized * binsCount))
    if (binsCount == 1) return range[0]

    double rMin = range[0] as double
    double rMax = range[1] as double
    double t = idx / (double) (binsCount - 1)
    return rMin + t * (rMax - rMin)
  }
}
