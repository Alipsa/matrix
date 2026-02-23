package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete greyscale for categorical data.
 */
@CompileStatic
class ScaleColorGrey extends ScaleDiscrete {

  /** Start grey value (0-1). */
  Number start = 0.2

  /** End grey value (0-1). */
  Number end = 0.8

  /** Direction: 1 for normal, -1 for reversed. */
  int direction = 1

  /** Color for NA/missing values. */
  String naValue = 'grey50'

  /** Generated color palette (Map for O(1) lookup) */
  private Map<String, String> palette = [:]

  /**
   * Create a greyscale with defaults.
   */
  ScaleColorGrey() {
    aesthetic = 'color'
  }

  /**
   * Create a greyscale with parameters.
   *
   * @param params scale parameters
   */
  ScaleColorGrey(Map params) {
    aesthetic = 'color'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.start != null) this.start = params.start as Number
    if (params.end != null) this.end = params.end as Number
    if (params.direction != null) this.direction = params.direction as int
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  @Override
  void train(List data) {
    super.train(data)
    generatePalette()
  }

  /**
   * Generate color palette based on current levels.
   */
  private void generatePalette() {
    if (domain == null || domain.isEmpty()) {
      palette = [:]
      return
    }

    int n = domain.size()
    List<String> colors = buildGreyPalette(n)
    palette = buildPaletteMap(colors)
  }

  @Override
  Object transform(Object value) {
    return lookupColor(palette, value, naValue)
  }

  List<String> getColors() {
    if (palette.isEmpty()) {
      generatePalette()
    }
    return getColorsFromPalette(palette, naValue)
  }

  private List<String> buildGreyPalette(int n) {
    if (n <= 0) return []
    BigDecimal startVal = start as BigDecimal
    BigDecimal endVal = end as BigDecimal
    if (direction < 0) {
      BigDecimal tmp = startVal
      startVal = endVal
      endVal = tmp
    }
    List<String> palette = []
    if (n == 1) {
      palette << greyHex((startVal + endVal) / 2)
      return palette
    }
    for (int i = 0; i < n; i++) {
      BigDecimal t = i / (n - 1)
      BigDecimal val = startVal + t * (endVal - startVal)
      palette << greyHex(val)
    }
    return palette
  }

  private static String greyHex(BigDecimal value) {
    BigDecimal clamped = 0.max(value.min(1))
    int v = (clamped * 255).round().intValue()
    return String.format('#%02X%02X%02X', v, v, v)
  }
}
