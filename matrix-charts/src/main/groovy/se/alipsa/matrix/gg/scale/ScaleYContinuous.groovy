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
  private ScaleYContinuous createSecondaryScale(SecondaryAxis spec) {
    ScaleYContinuous secondary = new ScaleYContinuous() {
      @Override
      List getComputedBreaks() {
        // If spec has explicit breaks, inverse-transform them to primary scale units
        // so they appear at the correct pixel positions
        if (spec.breaks) {
          // Explicit breaks are in secondary scale units, need to find corresponding primary values
          // This is complex - for now, just use them as-is (user must provide in primary units)
          return spec.breaks
        }
        // Use the same breaks as the primary scale (in primary scale units)
        // This ensures the tick marks appear at the same positions
        return ScaleYContinuous.this.getComputedBreaks()
      }

      @Override
      List<String> getComputedLabels() {
        // If spec has explicit labels, use them
        if (spec.labels) {
          return spec.labels
        }
        // Transform the primary breaks to secondary scale and format
        List primaryBreaks = getComputedBreaks()
        return primaryBreaks.collect { breakVal ->
          BigDecimal transformed = spec.applyTransform(breakVal as Number)
          formatNumber(transformed)
        }
      }

      private String formatNumber(Number n) {
        if (n == null) return ''
        BigDecimal bd = n instanceof BigDecimal ? n as BigDecimal : new BigDecimal(n.toString())
        if (bd.stripTrailingZeros().scale() <= 0) {
          return bd.toBigInteger().toString()
        }
        return String.format('%.2g', bd as double)
      }
    }

    secondary.position = (this.position == 'left') ? 'right' : 'left'
    secondary.name = spec.name
    secondary.aesthetic = 'y'
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
