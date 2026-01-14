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

  /** Generated color mapping (Map for O(1) lookup) */
  private Map<Object, String> paletteMap = [:]

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
    generatePalette()
  }

  /**
   * Generate color palette based on current levels.
   */
  private void generatePalette() {
    if (domain == null || domain.isEmpty()) {
      paletteMap = [:]
      return
    }

    int count = n != null ? n : domain.size()
    String resolvedPalette = resolvePaletteName()
    List<String> colors = BrewerPalettes.selectPalette(resolvedPalette, count, direction)

    paletteMap = [:]
    domain.eachWithIndex { value, idx ->
      // Use toString() to ensure consistent String keys (handles GString vs String)
      paletteMap[value.toString()] = colors[idx % colors.size()]
    }
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    // Use toString() to match storage format
    String key = value.toString()
    if (paletteMap.containsKey(key)) {
      return paletteMap[key]
    }
    return naValue
  }

  List<String> getColors() {
    if (levels.isEmpty()) return []
    if (paletteMap.isEmpty()) {
      generatePalette()
    }
    List<String> result = []
    for (Object level : levels) {
      // Use toString() to match storage format
      String color = paletteMap.get(level.toString())
      result.add(color != null ? color : naValue)
    }
    return result
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
