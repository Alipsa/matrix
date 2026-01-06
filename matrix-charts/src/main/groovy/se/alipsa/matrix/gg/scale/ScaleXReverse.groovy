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
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return null

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) return (rMin + rMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)

    // REVERSED: Linear interpolation but inverted
    BigDecimal normalized = (v - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    // Instead of rMin + normalized * (rMax - rMin), we do rMax - normalized * (rMax - rMin)
    return rMax - normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return null

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (rMax == rMin) return (dMin + dMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)

    // REVERSED: Inverse linear interpolation
    BigDecimal normalized = (rMax - v).divide((rMax - rMin), ScaleUtils.MATH_CONTEXT)
    return dMin + normalized * (dMax - dMin)
  }

  @Override
  List getComputedBreaks() {
    // Return breaks in reverse order so labels appear correctly
    List b = super.getComputedBreaks()
    return b.reverse()
  }

}
