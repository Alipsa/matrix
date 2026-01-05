package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous alpha scale.
 */
@CompileStatic
class ScaleAlphaContinuous extends ScaleContinuous {

  /** Output range [min, max] for alpha values. */
  List<Number> range = [0.1, 1.0] as List<Number>

  /** Alpha value for NA/missing values. */
  Number naValue = 1.0

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
    if (params.range) this.range = params.range as List<Number>
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
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (dMax == dMin) return (rMin + rMax) / 2.0d

    double normalized = (v - dMin) / (dMax - dMin)
    double mapped = rMin + normalized * (rMax - rMin)
    return Math.max(Math.min(mapped, Math.max(rMin, rMax)), Math.min(rMin, rMax))
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
