package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete ColorBrewer scale for categorical data.
 */
@CompileStatic
class ScaleColorBrewer extends ScaleDiscrete {

  /** Palette name (e.g. Set1, Blues, Spectral). */
  String palette = 'Set1'

  /** Palette type: 'seq', 'div', or 'qual'. */
  String type = 'qual'

  /** Number of colors to generate (defaults to number of levels). */
  Integer n

  /** Direction: 1 for normal, -1 for reversed. */
  int direction = 1

  /** Color for NA/missing values. */
  String naValue = 'grey50'

  private List<String> computedPalette = []

  /**
   * Create a ColorBrewer scale with defaults.
   */
  ScaleColorBrewer() {
    aesthetic = 'color'
  }

  /**
   * Create a ColorBrewer scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleColorBrewer(Map params) {
    aesthetic = 'color'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.palette != null) this.palette = params.palette.toString()
    if (params.type) this.type = params.type as String
    if (params.n != null) this.n = params.n as Integer
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
    int count = n != null ? n : levels.size()
    String resolvedPalette = resolvePaletteName()
    computedPalette = BrewerPalettes.selectPalette(resolvedPalette, count, direction)
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    int index = levels.indexOf(value)
    if (index < 0) return naValue

    if (computedPalette.isEmpty()) {
      int count = n != null ? n : levels.size()
      String resolvedPalette = resolvePaletteName()
      computedPalette = BrewerPalettes.selectPalette(resolvedPalette, count, direction)
    }
    if (computedPalette.isEmpty()) return naValue
    return computedPalette[index % computedPalette.size()]
  }

  List<String> getColors() {
    if (computedPalette.isEmpty()) {
      int count = n != null ? n : levels.size()
      String resolvedPalette = resolvePaletteName()
      computedPalette = BrewerPalettes.selectPalette(resolvedPalette, count, direction)
    }
    return new ArrayList<>(computedPalette)
  }

  private String resolvePaletteName() {
    if (BrewerPalettes.getPalette(palette) != null) {
      return palette
    }
    switch (type?.toLowerCase()) {
      case 'div':
        return 'Spectral'
      case 'seq':
        return 'Blues'
      case 'qual':
      default:
        return 'Set1'
    }
  }
}
