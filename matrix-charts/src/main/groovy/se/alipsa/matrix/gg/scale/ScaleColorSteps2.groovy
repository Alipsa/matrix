package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned diverging color scale - splits at a midpoint with separate color bins on each side.
 *
 * Diverging scales are useful for visualizing data where both extremes are important
 * and there's a meaningful center point (e.g., temperature anomalies, profit/loss).
 *
 * Example:
 * <pre>
 * // 9 bins diverging from blue through white to red
 * scale_color_steps2(bins: 9, low: 'blue', mid: 'white', high: 'red')
 *
 * // Set specific midpoint value
 * scale_color_steps2(bins: 7, midpoint: 50)
 * </pre>
 */
@CompileStatic
class ScaleColorSteps2 extends ScaleContinuous {
  String aesthetic = 'color'
  int bins = 5
  List<String> colors
  String low = '#D73027'     // Default: red
  String mid = '#FFFFBF'     // Default: pale yellow
  String high = '#1A9850'    // Default: green
  BigDecimal midpoint        // Data value at center (null = auto-calculate)
  String naValue = 'grey50'
  String guideType = 'bins'

  ScaleColorSteps2(Map params = [:]) {
    applyParams(params)

    // Generate color palette if not provided (must be odd count for diverging)
    if (!colors) {
      int n = (bins % 2 == 0) ? bins + 1 : bins  // Ensure odd count
      colors = generateDivergingPalette(low, mid, high, n)
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
      return colors[colors.size() / 2]  // Return middle color
    }

    // Determine midpoint (auto-calculate if not set)
    BigDecimal mid = midpoint != null ? midpoint : (dMin + dMax) / 2

    // Normalize based on which side of midpoint
    BigDecimal normalized
    if (v <= mid) {
      // Map [dMin, mid] → [0, 0.5]
      BigDecimal range = mid - dMin
      normalized = range != 0 ? ((v - dMin) / range) * 0.5 : 0.25
    } else {
      // Map (mid, dMax] → (0.5, 1]
      BigDecimal range = dMax - mid
      normalized = range != 0 ? 0.5 + ((v - mid) / range) * 0.5 : 0.75
    }

    normalized = 0.max(normalized.min(1))

    // Calculate bin index
    int binsCount = Math.max(1, colors.size())
    if (binsCount == 1) return colors[0]

    BigDecimal scaled = normalized * binsCount
    int binIndex = scaled.floor() as int
    binIndex = Math.min(binIndex, binsCount - 1)
    binIndex = Math.max(binIndex, 0)

    return colors[binIndex]
  }

  private List<String> generateDivergingPalette(String lowColor, String midColor, String highColor, int n) {
    List<String> palette = []
    int halfN = n.intdiv(2)

    // Low to mid
    for (int i = 0; i <= halfN; i++) {
      BigDecimal t = halfN > 0 ? (i / halfN) : 0.5
      palette << ColorScaleUtil.interpolateColor(lowColor, midColor, t)
    }

    // Mid to high (skip duplicate mid)
    for (int i = 1; i <= halfN; i++) {
      BigDecimal t = halfN > 0 ? (i / halfN) : 0.5
      palette << ColorScaleUtil.interpolateColor(midColor, highColor, t)
    }

    return palette
  }

  private void applyParams(Map params) {
    if (params.aesthetic) this.aesthetic = params.aesthetic as String
    if (params.bins != null) this.bins = (params.bins as Number).intValue()
    if (params.containsKey('colors')) this.colors = params.colors as List<String>
    if (params.low) this.low = params.low as String
    if (params.mid) this.mid = params.mid as String
    if (params.high) this.high = params.high as String
    if (params.midpoint != null) this.midpoint = params.midpoint as BigDecimal
    if (params.naValue) this.naValue = params.naValue as String
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
  }

  ScaleColorSteps2 midpoint(BigDecimal value) {
    this.midpoint = value
    return this
  }
}
