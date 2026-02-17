package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Scale as CharmScale

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
    if (params.expand) this.expand = params.expand as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
  }

  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    return ScaleUtils.linearTransformReversed(v, computedDomain[0], computedDomain[1], range[0], range[1])
  }

  @Override
  Object inverse(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    return ScaleUtils.linearInverseReversed(v, computedDomain[0], computedDomain[1], range[0], range[1])
  }

  @Override
  List getComputedBreaks() {
    // Return breaks in reverse order so labels appear correctly
    List b = super.getComputedBreaks()
    return b.reverse()
  }

  /**
   * Converts this gg scale to a charm Scale spec.
   *
   * @return charm Scale with reverse transform and this scale's parameters
   */
  CharmScale toCharmScale() {
    CharmScale s = CharmScale.transform('reverse')
    if (limits) s.params['limits'] = limits
    if (expand) s.params['expand'] = expand
    s
  }
}
