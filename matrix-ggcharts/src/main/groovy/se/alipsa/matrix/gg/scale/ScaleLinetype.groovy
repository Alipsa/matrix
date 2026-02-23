package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete linetype scale for line geometries.
 * Maps categorical levels to a repeating set of linetype names.
 *
 * Available linetypes: solid, dashed, dotted, dotdash, longdash, twodash
 */
@CompileStatic
class ScaleLinetype extends ScaleDiscrete {

  List<String> linetypes = [
      'solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash'
  ]

  /**
   * Create a linetype scale with default linetype mappings.
   */
  ScaleLinetype() {
    aesthetic = 'linetype'
  }

  /**
   * Create a linetype scale with optional parameters.
   *
   * Supported params: name, limits, breaks, labels, linetypes
   *
   * @param params configuration map
   */
  ScaleLinetype(Map params) {
    aesthetic = 'linetype'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.linetypes) this.linetypes = params.linetypes as List<String>
  }

  /**
   * Map a data value to a linetype name.
   *
   * @param value a discrete level
   * @return the linetype name for the level, or null if not mapped
   */
  @Override
  Object transform(Object value) {
    if (value == null || levels.isEmpty()) return null
    int index = levels.indexOf(value)
    if (index < 0) return null
    return linetypes[index % linetypes.size()]
  }
}
