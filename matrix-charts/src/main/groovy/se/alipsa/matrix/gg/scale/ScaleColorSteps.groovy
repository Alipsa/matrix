package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned sequential color scale - divides continuous range into discrete color bins.
 *
 * Unlike gradient scales which create smooth color transitions, binned scales
 * map continuous values to a fixed number of discrete color bins/steps.
 * Each bin maps directly to one color from the palette (no interpolation).
 *
 * The number of bins always equals the number of colors in the palette.
 * Useful for choropleth maps and categorical data visualization.
 *
 * Example:
 * <pre>
 * // Generate 7 colors from white to dark blue (creates 7 bins)
 * scale_color_steps(bins: 7, low: 'white', high: 'darkblue')
 *
 * // Custom 4-color palette (creates 4 bins, one per color)
 * scale_color_steps(colors: ['#ffffcc', '#a1dab4', '#41b6c4', '#225ea8'])
 *
 * // For flexible bin counts with interpolation, use scale_color_stepsn()
 * </pre>
 *
 * @param bins Number of colors to generate from low→high gradient
 * @param low Starting color for auto-generated palette
 * @param high Ending color for auto-generated palette
 * @param colors Custom color palette (overrides low/high; bin count = color count)
 */
@CompileStatic
class ScaleColorSteps extends ScaleContinuous {
  String aesthetic = 'color'
  int bins = 5
  List<String> colors
  String low = '#132B43'   // Default: dark blue
  String high = '#56B1F7'  // Default: light blue
  String naValue = 'grey50'
  String guideType = 'bins'  // Use binned legend
  private boolean customColors = false  // Track if colors were user-provided

  ScaleColorSteps(Map params = [:]) {
    aesthetic = 'color'
    expand = ScaleContinuous.NO_EXPAND  // No expansion for color scales
    applyParams(params)

    // Generate color palette if not provided
    if (!colors) {
      colors = generateSequentialPalette(low, high, bins)
    }
  }

  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return naValue

    if (!colors || colors.isEmpty()) return naValue
    if (colors.size() == 1) return colors[0]

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]

    // Edge case: constant domain
    if (dMax == dMin) {
      return colors[colors.size() / 2]
    }

    // Normalize to [0, 1]
    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    normalized = 0.max(normalized.min(1))

    // Calculate bin index using color count for consistency
    int binsCount = Math.max(1, colors.size())
    if (binsCount == 1) return colors[0]

    BigDecimal scaled = normalized * binsCount
    int binIndex = scaled.floor() as int
    binIndex = Math.min(binIndex, binsCount - 1)
    binIndex = Math.max(binIndex, 0)

    // Get color from palette
    return colors[binIndex]
  }

  private List<String> generateSequentialPalette(String lowColor, String highColor, int n) {
    List<String> palette = []
    for (int i = 0; i < n; i++) {
      BigDecimal t = n > 1 ? (i / (n - 1)) : 0.5
      palette << ColorScaleUtil.interpolateColor(lowColor, highColor, t)
    }
    return palette
  }

  private void applyParams(Map params) {
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
    if (params.bins != null) this.bins = (params.bins as Number).intValue()
    if (params.containsKey('colors')) {
      this.colors = params.colors as List<String>
      this.customColors = true
    }
    if (params.containsKey('colours')) {
      this.colors = params.colours as List<String>
      this.customColors = true
    }
    if (params.low) this.low = params.low as String
    if (params.high) this.high = params.high as String
    if (params.naValue) this.naValue = params.naValue as String
    if (params.guide) this.guideType = params.guide as String
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
  }

  // Fluent API
  ScaleColorSteps colors(List<String> colors) {
    this.colors = colors
    this.customColors = true
    return this
  }

  ScaleColorSteps bins(int n) {
    this.bins = n
    // Only regenerate palette if colors weren't custom-set
    if (!customColors) {
      this.colors = generateSequentialPalette(low, high, n)
    }
    return this
  }
}
