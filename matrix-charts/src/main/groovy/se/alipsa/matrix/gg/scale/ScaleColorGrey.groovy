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

  private List<String> computedPalette = []

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
    if (params.direction != null) this.direction = (params.direction as Number).intValue()
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
    computedPalette = buildPalette(levels.size())
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    int index = levels.indexOf(value)
    if (index < 0) return naValue

    if (computedPalette.isEmpty()) {
      computedPalette = buildPalette(levels.size())
    }
    if (computedPalette.isEmpty()) return naValue
    return computedPalette[index % computedPalette.size()]
  }

  List<String> getColors() {
    if (computedPalette.isEmpty()) {
      computedPalette = buildPalette(levels.size())
    }
    return new ArrayList<>(computedPalette)
  }

  private List<String> buildPalette(int n) {
    if (n <= 0) return []
    double startVal = start as double
    double endVal = end as double
    if (direction < 0) {
      double tmp = startVal
      startVal = endVal
      endVal = tmp
    }
    List<String> palette = []
    if (n == 1) {
      palette << greyHex((startVal + endVal) / 2.0d)
      return palette
    }
    for (int i = 0; i < n; i++) {
      double t = i / (double) (n - 1)
      double val = startVal + t * (endVal - startVal)
      palette << greyHex(val)
    }
    return palette
  }

  private static String greyHex(double value) {
    double clamped = Math.max(0.0d, Math.min(1.0d, value))
    int v = (int) Math.round(clamped * 255.0d)
    return String.format('#%02X%02X%02X', v, v, v)
  }
}
