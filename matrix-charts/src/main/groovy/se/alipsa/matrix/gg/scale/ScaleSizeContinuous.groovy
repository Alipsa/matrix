package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous size scale.
 */
@CompileStatic
class ScaleSizeContinuous extends ScaleContinuous {

  /** Output range [min, max] for size values. */
  List<Number> range = [1.0, 6.0] as List<Number>

  /** Size value for NA/missing values. */
  Number naValue = 3.0

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
    return rMin + normalized * (rMax - rMin)
  }

  protected static Double coerceToNumber(Object value) {
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
