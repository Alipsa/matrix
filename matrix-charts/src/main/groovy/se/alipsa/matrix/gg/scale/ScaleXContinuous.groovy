package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous scale for the x-axis position aesthetic.
 * Provides x-axis specific defaults and configuration.
 */
@CompileStatic
class ScaleXContinuous extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  /** Secondary axis configuration (optional) */
  ScaleXContinuous secAxis

  ScaleXContinuous() {
    aesthetic = 'x'
  }

  ScaleXContinuous(Map params) {
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

  /**
   * Convenience method to set limits.
   */
  ScaleXContinuous limits(Number min, Number max) {
    this.limits = [min, max]
    return this
  }

  /**
   * Convenience method to set breaks.
   */
  ScaleXContinuous breaks(List<Number> breaks) {
    this.breaks = breaks
    return this
  }

  /**
   * Convenience method to set labels.
   */
  ScaleXContinuous labels(List<String> labels) {
    this.labels = labels
    return this
  }

  /**
   * Convenience method to set expansion.
   */
  ScaleXContinuous expand(Number mult, Number add = 0) {
    this.expand = [mult, add]
    return this
  }
}
