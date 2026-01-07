package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous color scale for numeric data.
 * Maps numeric values to colors along a gradient between two or more colors.
 */
@CompileStatic
class ScaleColorGradient extends ScaleContinuous {

  /** Low color (for minimum values) */
  String low = '#132B43'  // Dark blue

  /** High color (for maximum values) */
  String high = '#56B1F7'  // Light blue

  /** Midpoint color (optional, for diverging scales) */
  String mid

  /** Midpoint value (used when mid color is set) */
  Number midpoint

  /** Color for NA values */
  String naValue = 'grey50'

  /** Guide type: 'colorbar' or 'legend' */
  String guideType = 'colorbar'

  ScaleColorGradient() {
    aesthetic = 'color'
    expand = ScaleContinuous.NO_EXPAND  // No expansion for color scales
  }

  ScaleColorGradient(Map params) {
    aesthetic = 'color'
    expand = ScaleContinuous.NO_EXPAND
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.low) this.low = params.low as String
    if (params.high) this.high = params.high as String
    if (params.mid) this.mid = params.mid as String
    if (params.midpoint) this.midpoint = params.midpoint as Number
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
    if (params.guide) this.guideType = params.guide as String
    // Support 'colour' British spelling
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (!(value instanceof Number)) return naValue

    BigDecimal v = value as BigDecimal
    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]

    // Handle edge case
    if (dMax == dMin) return mid ?: interpolateColor(low, high, 0.5)

    // Normalize to 0-1
    BigDecimal normalized = (v - dMin) / (dMax - dMin)

    normalized = normalized.min(1.0G).max(0.0G)  // Clamp

    // Use diverging scale if mid is set
    if (mid && midpoint != null) {
      BigDecimal midNorm = (midpoint - dMin) / (dMax - dMin)
      if (normalized < midNorm) {
        // Interpolate between low and mid
        BigDecimal t = normalized / midNorm
        return interpolateColor(low, mid, t)
      } else {
        // Interpolate between mid and high
        BigDecimal t = (normalized - midNorm) / (1 - midNorm)
        return interpolateColor(mid, high, t)
      }
    }

    // Simple two-color gradient
    return interpolateColor(low, high, normalized)
  }

  /**
   * Interpolate between two colors.
   * @param color1 Start color (hex string)
   * @param color2 End color (hex string)
   * @param t Interpolation factor (0-1)
   * @return Interpolated color as hex string
   */
  private static String interpolateColor(String color1, String color2, BigDecimal t) {
    return ColorScaleUtil.interpolateColor(color1, color2, t)
  }

  /**
   * Parse a color string to RGB values.
   * Supports hex format (#RGB, #RRGGBB) and some named colors.
   */
  private static int[] parseColor(String color) {
    return ColorScaleUtil.parseColor(color)
  }

  /**
   * Convenience method to set low color.
   */
  ScaleColorGradient low(String color) {
    this.low = color
    return this
  }

  /**
   * Convenience method to set high color.
   */
  ScaleColorGradient high(String color) {
    this.high = color
    return this
  }

  /**
   * Convenience method to set mid color and midpoint.
   */
  ScaleColorGradient mid(String color, Number midpoint) {
    this.mid = color
    this.midpoint = midpoint as BigDecimal
    return this
  }

  /**
   * Convenience method to set limits.
   */
  ScaleColorGradient limits(Number min, Number max) {
    this.limits = [min as BigDecimal, max as BigDecimal]
    return this
  }
}
