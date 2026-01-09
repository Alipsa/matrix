package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Manual discrete shape scale.
 * Maps categorical values to specific shapes provided by the user.
 *
 * Usage:
 * scale_shape_manual(values: ['circle', 'square', 'triangle'])  // Map by order
 * scale_shape_manual(values: [cat1: 'circle', cat2: 'square'])  // Map by name
 */
@CompileStatic
class ScaleShapeManual extends ScaleDiscrete {

  /** List of shapes or map of value -> shape */
  List<String> values = []

  /** Named mapping of data values to shapes */
  Map<Object, String> namedValues = [:]

  /** Shape for NA/missing values */
  String naValue = 'circle'

  /** Default shapes to use if no values provided */
  private static final List<String> DEFAULT_SHAPES = [
      'circle', 'triangle', 'square', 'diamond', 'plus', 'x', 'cross'
  ]

  ScaleShapeManual() {
    aesthetic = 'shape'
  }

  ScaleShapeManual(Map params) {
    aesthetic = 'shape'
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

    // Get shape from values list or default shapes
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }

    // Use default shapes if no custom values provided
    return DEFAULT_SHAPES[index % DEFAULT_SHAPES.size()]
  }

  @Override
  Object inverse(Object value) {
    // Find the level that maps to this shape
    if (value == null) return null

    // Check named values
    def namedEntry = namedValues.find { k, v -> v == value }
    if (namedEntry) return namedEntry.key

    // Check positional mapping
    if (!values.isEmpty()) {
      int shapeIndex = values.indexOf(value as String)
      if (shapeIndex >= 0 && shapeIndex < levels.size()) {
        return levels[shapeIndex]
      }
    } else {
      // Check default shapes
      int shapeIndex = DEFAULT_SHAPES.indexOf(value as String)
      if (shapeIndex >= 0 && shapeIndex < levels.size()) {
        return levels[shapeIndex]
      }
    }

    return null
  }

  /**
   * Get the shape for a specific level index.
   */
  String getShapeForIndex(int index) {
    if (index < 0) return naValue
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }
    return DEFAULT_SHAPES[index % DEFAULT_SHAPES.size()]
  }

  /**
   * Get all shapes in order of levels.
   */
  List<String> getShapes() {
    return levels.collect { transform(it) as String }
  }

  /**
   * Convenience method to set values.
   */
  ScaleShapeManual values(List<String> shapes) {
    this.values = shapes
    return this
  }

  /**
   * Convenience method to set named values.
   */
  ScaleShapeManual values(Map<Object, String> mapping) {
    this.namedValues = mapping
    return this
  }
}
