package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete shape scale for point geometries.
 * Maps categorical levels to a repeating set of shape names.
 */
@CompileStatic
class ScaleShape extends ScaleDiscrete {

  List<String> shapes = [
      'circle', 'triangle', 'square', 'diamond', 'plus', 'x', 'cross'
  ]

  /**
   * Create a shape scale with default shape mappings.
   */
  ScaleShape() {
    aesthetic = 'shape'
  }

  /**
   * Create a shape scale with optional parameters.
   *
   * Supported params: name, limits, breaks, labels, shapes
   *
   * @param params configuration map
   */
  ScaleShape(Map params) {
    aesthetic = 'shape'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.shapes) this.shapes = params.shapes as List<String>
  }

  /**
   * Map a data value to a shape name.
   *
   * @param value a discrete level
   * @return the shape name for the level, or null if not mapped
   */
  @Override
  Object transform(Object value) {
    if (value == null || levels.isEmpty()) return null
    int index = levels.indexOf(value)
    if (index < 0) return null
    return shapes[index % shapes.size()]
  }
}
