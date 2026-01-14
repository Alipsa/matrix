package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Manual discrete color scale.
 * Maps categorical values to specific colors provided by the user.
 *
 * Usage:
 * scale_color_manual(values: ['red', 'green', 'blue'])  // Map by order
 * scale_color_manual(values: [cat1: 'red', cat2: 'blue'])  // Map by name
 */
@CompileStatic
class ScaleColorManual extends ScaleDiscrete {

  /** List of colors or map of value -> color */
  List<String> values = []

  /** Named mapping of data values to colors */
  Map<Object, String> namedValues = [:]

  /** Color for NA/missing values */
  String naValue = 'grey50'

  private List<String> computedPalette = []

  ScaleColorManual() {
    aesthetic = 'color'
  }

  ScaleColorManual(Map params) {
    aesthetic = 'color'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.values) {
      def v = params.values
      if (v instanceof List) {
        this.values = v.collect { it?.toString() }
        this.computedPalette = []
      } else if (v instanceof Map) {
        this.namedValues = (v as Map).collectEntries { k, val ->
          [k, val?.toString()]
        } as Map<Object, String>
        this.computedPalette = []
      }
    }
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
    // Support 'colour' British spelling
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  @Override
  void train(List data) {
    super.train(data)
    if (values.isEmpty() && namedValues.isEmpty()) {
      computedPalette = generateHuePalette(levels.size())
    } else {
      computedPalette = []
    }
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    // Check named mapping first
    if (namedValues.containsKey(value)) {
      return namedValues[value]
    }

    // Fall back to positional mapping
    int index = levels.indexOf(value)
    if (index < 0) return naValue

    // Get color from values list or computed palette
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }
    if (computedPalette.isEmpty()) {
      computedPalette = generateHuePalette(levels.size())
    }
    // Guard against empty or stale palettes even if levels are populated.
    if (index >= computedPalette.size()) return naValue
    return computedPalette[index]
  }

  @Override
  Object inverse(Object value) {
    // Find the level that maps to this color
    if (value == null) return null

    // Check named values
    def namedEntry = namedValues.find { k, v -> v == value }
    if (namedEntry) return namedEntry.key

    // Check positional mapping
    if (!values.isEmpty()) {
      int colorIndex = values.indexOf(value as String)
      if (colorIndex >= 0 && colorIndex < levels.size()) {
        return levels[colorIndex]
      }
    } else {
      if (computedPalette.isEmpty()) {
        computedPalette = generateHuePalette(levels.size())
      }
      int colorIndex = computedPalette.indexOf(value as String)
      if (colorIndex >= 0 && colorIndex < levels.size()) {
        return levels[colorIndex]
      }
    }

    return null
  }

  /**
   * Get the color for a specific level index.
   */
  String getColorForIndex(int index) {
    if (index < 0) return naValue
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }
    if (computedPalette.isEmpty()) {
      computedPalette = generateHuePalette(levels.size())
    }
    if (index >= computedPalette.size()) return naValue
    return computedPalette[index]
  }

  /**
   * Get all colors in order of levels.
   */
  List<String> getColors() {
    if (values.isEmpty() && namedValues.isEmpty()) {
      if (computedPalette.isEmpty()) {
        computedPalette = generateHuePalette(levels.size())
      }
      return new ArrayList<>(computedPalette)
    }
    return levels.collect { transform(it) as String }
  }

  /**
   * Convenience method to set values.
   */
  ScaleColorManual values(List<String> colors) {
    this.values = colors
    this.computedPalette = []
    return this
  }

  /**
   * Convenience method to set named values.
   */
  ScaleColorManual values(Map<Object, String> mapping) {
    this.namedValues = mapping
    this.computedPalette = []
    return this
  }

  /**
   * Generate a ggplot2-like hue palette in HCL space.
   *
   * @param n number of colors to generate
   * @return list of hex RGB colors
   */
  private static List<String> generateHuePalette(int n) {
    if (n <= 0) return []
    // ggplot2 hue palette defaults: h = [15, 375], c = 100, l = 65.
    double start = 15.0
    double end = 375.0
    double step = (end - start) / n
    List<String> colors = new ArrayList<>(n)
    for (int i = 0; i < n; i++) {
      double hue = start + step * i
      hue = hue % 360.0
      colors << ColorSpaceUtil.hclToHex(hue, 100, 65)
    }
    return colors
  }

}
