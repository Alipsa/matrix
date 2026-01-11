package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned n-color scale - bins continuous values and maps to a custom color palette.
 *
 * Most flexible binned scale, accepting any number of colors with optional
 * custom positioning. The colors define a gradient, and bins are sampled from
 * that gradient with interpolation. The number of bins and colors can differ.
 *
 * Useful for custom color schemes and brand-specific palettes.
 *
 * Example:
 * <pre>
 * // 5 bins sampled from 4-color gradient (colors are interpolated)
 * scale_color_stepsn(bins: 5, colors: ['red', 'yellow', 'green', 'blue'])
 *
 * // To get exactly these 4 colors without interpolation, use bins: 4
 * scale_color_stepsn(bins: 4, colors: ['red', 'yellow', 'green', 'blue'])
 *
 * // 10 bins sampled from a 2-color gradient (red → blue)
 * scale_color_stepsn(bins: 10, colors: ['red', 'blue'])
 *
 * // Custom color positions for non-uniform gradient
 * scale_color_stepsn(bins: 10,
 *                    colors: ['#440154', '#31688e', '#35b779', '#fde724'],
 *                    values: [0.0, 0.33, 0.67, 1.0])
 * </pre>
 *
 * @param bins Number of discrete color bins to create
 * @param colors Color palette defining the gradient (can be any size)
 * @param values Optional positions for each color in [0,1] range
 */
@CompileStatic
class ScaleColorStepsN extends ScaleContinuous {
  String aesthetic = 'color'
  int bins = 5
  List<String> colors = ['#132B43', '#56B1F7']  // Default: 2 colors
  List<BigDecimal> values  // Optional: custom positions for colors (0-1)
  String naValue = 'grey50'
  String guideType = 'bins'

  ScaleColorStepsN(Map params = [:]) {
    aesthetic = 'color'
    expand = ScaleContinuous.NO_EXPAND  // No expansion for color scales
    applyParams(params)

    // If values not specified, auto-generate equally spaced
    if (!values && colors) {
      values = generateEquallySpacedValues(colors.size())
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

    // Calculate bin index using bins parameter (user-configurable bin count)
    int binsCount = Math.max(1, bins)
    if (binsCount == 1) return colors[0]

    BigDecimal scaled = normalized * binsCount
    int binIndex = scaled.floor() as int
    binIndex = Math.min(binIndex, binsCount - 1)
    binIndex = Math.max(binIndex, 0)

    // Map bin to color palette position
    BigDecimal binPosition = binsCount > 1 ? (binIndex / (binsCount - 1)) : 0.5

    // Find surrounding colors in palette
    return findColorAtPosition(binPosition)
  }

  private String findColorAtPosition(BigDecimal position) {
    // Find the two colors surrounding this position
    int i = 0
    while (i < values.size() - 1 && values[i + 1] <= position) {
      i++
    }

    // If exactly at a color stop, return it
    if (values[i] == position || i == values.size() - 1) {
      return colors[i]
    }

    // Interpolate between colors[i] and colors[i+1]
    BigDecimal v1 = values[i]
    BigDecimal v2 = values[i + 1]
    BigDecimal t = (position - v1) / (v2 - v1)

    return ColorScaleUtil.interpolateColor(colors[i], colors[i + 1], t)
  }

  private void applyParams(Map params) {
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
    if (params.bins != null) this.bins = (params.bins as Number).intValue()
    if (params.containsKey('colors')) this.colors = params.colors as List<String>
    if (params.containsKey('colours')) this.colors = params.colours as List<String>
    if (params.containsKey('values')) this.values = params.values as List<BigDecimal>
    if (params.naValue) this.naValue = params.naValue as String
    if (params.guide) this.guideType = params.guide as String
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
  }

  ScaleColorStepsN colors(List<String> colors) {
    this.colors = colors
    // Regenerate values to match new color count
    if (colors) {
      values = generateEquallySpacedValues(colors.size())
    }
    return this
  }

  private List<BigDecimal> generateEquallySpacedValues(int n) {
    List<BigDecimal> result = []
    for (int i = 0; i < n; i++) {
      BigDecimal position
      if (n > 1) {
        position = new BigDecimal(i).divide(new BigDecimal(n - 1), java.math.MathContext.DECIMAL64)
      } else {
        position = 0.5
      }
      result << position
    }
    return result
  }

  ScaleColorStepsN values(List<BigDecimal> values) {
    this.values = values
    return this
  }
}
