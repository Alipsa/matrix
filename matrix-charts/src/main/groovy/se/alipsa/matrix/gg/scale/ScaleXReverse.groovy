package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Reversed continuous scale for the x-axis.
 * Maps values in reverse order (high values on left, low values on right).
 *
 * Usage:
 * - scale_x_reverse() - reversed x-axis
 * - scale_x_reverse(limits: [0, 100]) - with explicit limits
 */
@CompileStatic
class ScaleXReverse extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  ScaleXReverse() {
    aesthetic = 'x'
  }

  ScaleXReverse(Map params) {
    aesthetic = 'x'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.expand) this.expand = params.expand as List<Number>
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
  }

  @Override
  Object transform(Object value) {
    BigDecimal numeric = ScaleUtils.coerceToNumber(value)
    if (numeric == null) return null

    double v = numeric.doubleValue()
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (dMax == dMin) return (rMin + rMax) / 2

    // REVERSED: Linear interpolation but inverted
    double normalized = (v - dMin) / (dMax - dMin)
    // Instead of rMin + normalized * (rMax - rMin), we do rMax - normalized * (rMax - rMin)
    return rMax - normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    BigDecimal numeric = ScaleUtils.coerceToNumber(value)
    if (numeric == null) return null

    double v = numeric.doubleValue()
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (rMax == rMin) return (dMin + dMax) / 2

    // REVERSED: Inverse linear interpolation
    double normalized = (rMax - v) / (rMax - rMin)
    return dMin + normalized * (dMax - dMin)
  }

  @Override
  List getComputedBreaks() {
    // Return breaks in reverse order so labels appear correctly
    List b = super.getComputedBreaks()
    return b.reverse()
  }

}
