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
    if (params.expand) this.expand = params.expand as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int

    // Handle secondary axis specification
    if (params.sec_axis || params.secAxis || params['sec.axis']) {
      SecondaryAxis spec = (params.sec_axis ?: params.secAxis ?: params['sec.axis']) as SecondaryAxis
      this.secAxis = createSecondaryScale(spec)
    }
  }

  /**
   * Create a secondary scale from a SecondaryAxis.
   * The secondary scale is configured to derive its breaks and labels from the primary scale.
   * Note: This is called during param processing, but the breaks are computed lazily during rendering.
   */
  private ScaleXContinuous createSecondaryScale(SecondaryAxis spec) {
    ScaleXContinuous secondary = new ScaleXContinuous() {
      // Store spec as field to avoid GC/serialization issues
      private final SecondaryAxis secondarySpec = spec

      @Override
      List getComputedBreaks() {
        // If spec has explicit breaks in secondary units, inverse-transform them to primary units
        if (secondarySpec.breaks) {
          List<BigDecimal> primaryDomain = ScaleXContinuous.this.getComputedDomain()
          return secondarySpec.breaks.collect { secondaryBreak ->
            // Inverse-transform from secondary to primary units
            secondarySpec.inverseTransform(secondaryBreak, primaryDomain[0], primaryDomain[1])
          }.findAll { it != null }  // Filter out any nulls from failed inversions
        }
        // Use the same breaks as the primary scale (in primary scale units)
        // This ensures the tick marks appear at the same positions
        return ScaleXContinuous.this.getComputedBreaks()
      }

      @Override
      List<String> getComputedLabels() {
        // If spec has explicit labels, use them
        if (secondarySpec.labels) {
          return secondarySpec.labels
        }
        // Transform the primary breaks to secondary scale and format
        List primaryBreaks = getComputedBreaks()
        return primaryBreaks.collect { breakVal ->
          BigDecimal transformed = secondarySpec.applyTransform(breakVal as Number)
          ScaleXContinuous.this.formatNumber(transformed)
        }
      }
    }

    secondary.position = (this.position == 'bottom') ? 'top' : 'bottom'
    secondary.name = spec.name
    secondary.aesthetic = 'x'
    // Note: range and computedDomain will be set later during scale training

    return secondary
  }

  /**
   * Update the secondary scale's range and domain to match the primary scale.
   * This should be called after the primary scale is trained.
   */
  void updateSecondaryScale() {
    if (secAxis != null) {
      secAxis.range = this.range
      secAxis.computedDomain = this.computedDomain
      secAxis.trained = this.trained
    }
  }

  /**
   * Convenience method to set limits.
   */
  ScaleXContinuous limits(BigDecimal min, BigDecimal max) {
    this.limits = [min, max]
    return this
  }

  ScaleXContinuous limits(Number min, Number max) {
    this.limits = [min as BigDecimal, max as BigDecimal]
    return this
  }

  ScaleXContinuous limits(List<? extends Number> minMax) {
    this.limits = [minMax.first as BigDecimal, minMax.last as BigDecimal]
    return this
  }

  /**
   * Convenience method to set breaks.
   */
  ScaleXContinuous breaks(List<? extends Number> breaks) {
    this.breaks = breaks.collect { it as BigDecimal }
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
  ScaleXContinuous expand(BigDecimal mult, BigDecimal add = 0) {
    this.expand = [mult, add]
    return this
  }

  ScaleXContinuous expand(Number mult, Number add = 0) {
    this.expand = [mult as BigDecimal, add as BigDecimal]
    return this
  }
}
