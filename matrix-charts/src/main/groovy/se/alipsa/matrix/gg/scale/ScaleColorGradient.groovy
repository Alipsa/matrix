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
    expand = [0, 0] as List<Number>  // No expansion for color scales
  }

  ScaleColorGradient(Map params) {
    aesthetic = 'color'
    expand = [0, 0] as List<Number>
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

    double v = value as double
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double

    // Handle edge case
    if (dMax == dMin) return mid ?: interpolateColor(low, high, 0.5)

    // Normalize to 0-1
    double normalized = (v - dMin) / (dMax - dMin)
    normalized = Math.max(0, Math.min(1, normalized))  // Clamp

    // Use diverging scale if mid is set
    if (mid && midpoint != null) {
      double midNorm = (midpoint as double - dMin) / (dMax - dMin)
      if (normalized < midNorm) {
        // Interpolate between low and mid
        double t = normalized / midNorm
        return interpolateColor(low, mid, t)
      } else {
        // Interpolate between mid and high
        double t = (normalized - midNorm) / (1 - midNorm)
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
  private String interpolateColor(String color1, String color2, double t) {
    int[] rgb1 = parseColor(color1)
    int[] rgb2 = parseColor(color2)

    int r = (int) Math.round(rgb1[0] + t * (rgb2[0] - rgb1[0]))
    int g = (int) Math.round(rgb1[1] + t * (rgb2[1] - rgb1[1]))
    int b = (int) Math.round(rgb1[2] + t * (rgb2[2] - rgb1[2]))

    // Clamp values
    r = Math.max(0, Math.min(255, r))
    g = Math.max(0, Math.min(255, g))
    b = Math.max(0, Math.min(255, b))

    return String.format('#%02X%02X%02X', r, g, b)
  }

  /**
   * Parse a color string to RGB values.
   * Supports hex format (#RGB, #RRGGBB) and some named colors.
   */
  private int[] parseColor(String color) {
    if (color == null) return [128, 128, 128] as int[]

    // Handle hex colors
    if (color.startsWith('#')) {
      String hex = color.substring(1)
      if (hex.length() == 3) {
        // Short form #RGB -> #RRGGBB
        hex = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
      }
      if (hex.length() == 6) {
        int r = Integer.parseInt(hex.substring(0, 2), 16)
        int g = Integer.parseInt(hex.substring(2, 4), 16)
        int b = Integer.parseInt(hex.substring(4, 6), 16)
        return [r, g, b] as int[]
      }
    }

    // Handle named colors (common ones)
    switch (color.toLowerCase()) {
      case 'white': return [255, 255, 255] as int[]
      case 'black': return [0, 0, 0] as int[]
      case 'red': return [255, 0, 0] as int[]
      case 'green': return [0, 128, 0] as int[]
      case 'blue': return [0, 0, 255] as int[]
      case 'yellow': return [255, 255, 0] as int[]
      case 'cyan': return [0, 255, 255] as int[]
      case 'magenta': return [255, 0, 255] as int[]
      case 'orange': return [255, 165, 0] as int[]
      case 'purple': return [128, 0, 128] as int[]
      case 'pink': return [255, 192, 203] as int[]
      case 'grey': case 'gray': return [128, 128, 128] as int[]
      case 'grey50': case 'gray50': return [128, 128, 128] as int[]
      case 'darkblue': return [0, 0, 139] as int[]
      case 'lightblue': return [173, 216, 230] as int[]
      case 'darkgreen': return [0, 100, 0] as int[]
      case 'lightgreen': return [144, 238, 144] as int[]
      case 'darkred': return [139, 0, 0] as int[]
      case 'steelblue': return [70, 130, 180] as int[]
      case 'navy': return [0, 0, 128] as int[]
      case 'maroon': return [128, 0, 0] as int[]
      case 'olive': return [128, 128, 0] as int[]
      case 'teal': return [0, 128, 128] as int[]
      default: return [128, 128, 128] as int[]  // Default gray
    }
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
    this.midpoint = midpoint
    return this
  }

  /**
   * Convenience method to set limits.
   */
  ScaleColorGradient limits(Number min, Number max) {
    this.limits = [min, max]
    return this
  }
}
