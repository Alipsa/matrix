package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous scale for the y-axis position aesthetic.
 * Provides y-axis specific defaults and configuration.
 * Note: Y-axis range is typically inverted in SVG coordinates (0 at top).
 */
@CompileStatic
class ScaleYContinuous extends ScaleContinuous {

  /** Position of the y-axis: 'left' (default) or 'right' */
  String position = 'left'

  /** Secondary axis configuration (optional) */
  ScaleYContinuous secAxis

  ScaleYContinuous() {
    aesthetic = 'y'
  }

  ScaleYContinuous(Map params) {
    aesthetic = 'y'
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

  /**
   * Convenience method to set limits.
   */
  ScaleYContinuous limits(BigDecimal min, BigDecimal max) {
    this.limits = [min, max]
    return this
  }

  /**
   * Convenience method to set breaks.
   */
  ScaleYContinuous breaks(List<BigDecimal> breaks) {
    this.breaks = breaks
    return this
  }

  /**
   * Convenience method to set labels.
   */
  ScaleYContinuous labels(List<String> labels) {
    this.labels = labels
    return this
  }

  /**
   * Convenience method to set expansion.
   */
  ScaleYContinuous expand(BigDecimal mult, BigDecimal add = 0) {
    this.expand = [mult, add]
    return this
  }
}
