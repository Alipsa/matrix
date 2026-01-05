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

  @Override
  Object transform(Object value) {
    Double numeric = coerceToNumber(value)
    if (numeric == null) return naValue

    double v = numeric
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
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

  private static Double coerceToNumber(Object value) {
    if (value == null) return null
    if (value instanceof Number) {
      double v = (value as Number).doubleValue()
      return Double.isNaN(v) ? null : v
    }
    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty() || s.equalsIgnoreCase('NA') || s.equalsIgnoreCase('NaN') || s.equalsIgnoreCase('null')) {
        return null
      }
      try {
        return Double.parseDouble(s)
      } catch (NumberFormatException ignored) {
        return null
      }
    }
    return null
  }
}
