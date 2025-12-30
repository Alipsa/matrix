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

  /** Default color palette (used when values not specified) */
  private static final List<String> DEFAULT_PALETTE = [
    '#F8766D', '#00BA38', '#619CFF', '#F564E3', '#00BFC4',
    '#B79F00', '#DE8C00', '#7CAE00', '#00B4F0', '#C77CFF'
  ]

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
      } else if (v instanceof Map) {
        this.namedValues = (v as Map).collectEntries { k, val ->
          [k, val?.toString()]
        } as Map<Object, String>
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

    // Get color from values list or default palette
    List<String> colorList = values.isEmpty() ? DEFAULT_PALETTE : values
    return colorList[index % colorList.size()]
  }

  @Override
  Object inverse(Object value) {
    // Find the level that maps to this color
    if (value == null) return null

    // Check named values
    def namedEntry = namedValues.find { k, v -> v == value }
    if (namedEntry) return namedEntry.key

    // Check positional mapping
    List<String> colorList = values.isEmpty() ? DEFAULT_PALETTE : values
    int colorIndex = colorList.indexOf(value as String)
    if (colorIndex >= 0 && colorIndex < levels.size()) {
      return levels[colorIndex]
    }

    return null
  }

  /**
   * Get the color for a specific level index.
   */
  String getColorForIndex(int index) {
    if (index < 0) return naValue
    List<String> colorList = values.isEmpty() ? DEFAULT_PALETTE : values
    return colorList[index % colorList.size()]
  }

  /**
   * Get all colors in order of levels.
   */
  List<String> getColors() {
    return levels.collect { transform(it) as String }
  }

  /**
   * Convenience method to set values.
   */
  ScaleColorManual values(List<String> colors) {
    this.values = colors
    return this
  }

  /**
   * Convenience method to set named values.
   */
  ScaleColorManual values(Map<Object, String> mapping) {
    this.namedValues = mapping
    return this
  }
}
