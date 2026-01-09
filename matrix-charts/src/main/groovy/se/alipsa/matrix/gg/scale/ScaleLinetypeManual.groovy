package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Manual discrete linetype scale.
 * Maps categorical values to specific linetypes provided by the user.
 *
 * Usage:
 * scale_linetype_manual(values: ['solid', 'dashed', 'dotted'])  // Map by order
 * scale_linetype_manual(values: [cat1: 'solid', cat2: 'dashed'])  // Map by name
 *
 * Available linetypes: solid, dashed, dotted, dotdash, longdash, twodash
 */
@CompileStatic
class ScaleLinetypeManual extends ScaleDiscrete {

  /** List of linetypes or map of value -> linetype */
  List<String> values = []

  /** Named mapping of data values to linetypes */
  Map<Object, String> namedValues = [:]

  /** Linetype for NA/missing values */
  String naValue = 'solid'

  /** Default linetypes to use if no values provided */
  private static final List<String> DEFAULT_LINETYPES = [
      'solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash'
  ]

  ScaleLinetypeManual() {
    aesthetic = 'linetype'
  }

  ScaleLinetypeManual(Map params) {
    aesthetic = 'linetype'
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

    // Get linetype from values list or default linetypes
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }

    // Use default linetypes if no custom values provided
    return DEFAULT_LINETYPES[index % DEFAULT_LINETYPES.size()]
  }

  /**
   * Find the first level that maps to the given linetype.
   * When linetypes cycle (more levels than linetypes), this returns the first level
   * that transforms to the given linetype value.
   *
   * @param value the linetype name to find
   * @return the first level that maps to this linetype, or null if not found
   */
  @Override
  Object inverse(Object value) {
    // Find the level that maps to this linetype
    if (value == null) return null

    // Check named values
    def namedEntry = namedValues.find { k, v -> v == value }
    if (namedEntry) return namedEntry.key

    // Iterate through all levels to find the first one that transforms to this linetype
    // This correctly handles cycled linetypes
    for (Object level : levels) {
      if (transform(level) == value) {
        return level
      }
    }

    return null
  }

  /**
   * Get the linetype for a specific level index.
   */
  String getLinetypeForIndex(int index) {
    if (index < 0) return naValue
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }
    return DEFAULT_LINETYPES[index % DEFAULT_LINETYPES.size()]
  }

  /**
   * Get all linetypes in order of levels.
   */
  List<String> getLinetypes() {
    return levels.collect { transform(it) as String }
  }

  /**
   * Convenience method to set values.
   */
  ScaleLinetypeManual values(List<String> linetypes) {
    this.values = linetypes
    return this
  }

  /**
   * Convenience method to set named values.
   */
  ScaleLinetypeManual values(Map<Object, String> mapping) {
    this.namedValues = mapping
    return this
  }
}
