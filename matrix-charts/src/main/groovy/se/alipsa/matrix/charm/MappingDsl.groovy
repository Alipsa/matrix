package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Closure delegate for `mapping {}` configuration blocks.
 */
@CompileStatic
class MappingDsl {

  private final Cols col = new Cols()
  Object x
  Object y
  Object color
  Object fill
  Object size
  Object shape
  Object group
  Object xend
  Object yend
  Object xmin
  Object xmax
  Object ymin
  Object ymax
  Object alpha
  Object linetype
  Object label
  Object weight

  /**
   * Returns the column namespace proxy.
   *
   * @return `col` proxy
   */
  Cols getCol() {
    col
  }

  /**
   * Collects non-null aesthetic values as mapping input.
   *
   * @return mapping map
   */
  Map<String, Object> toMapping() {
    Map<String, Object> mapping = [:]
    if (x != null) mapping.x = x
    if (y != null) mapping.y = y
    if (color != null) mapping.color = color
    if (fill != null) mapping.fill = fill
    if (size != null) mapping.size = size
    if (shape != null) mapping.shape = shape
    if (group != null) mapping.group = group
    if (xend != null) mapping.xend = xend
    if (yend != null) mapping.yend = yend
    if (xmin != null) mapping.xmin = xmin
    if (xmax != null) mapping.xmax = xmax
    if (ymin != null) mapping.ymin = ymin
    if (ymax != null) mapping.ymax = ymax
    if (alpha != null) mapping.alpha = alpha
    if (linetype != null) mapping.linetype = linetype
    if (label != null) mapping.label = label
    if (weight != null) mapping.weight = weight
    mapping
  }

  /**
   * Supports `colour` alias.
   *
   * @param name property name
   * @param value property value
   */
  @CompileDynamic
  void propertyMissing(String name, Object value) {
    if (name == 'colour') {
      color = value
      return
    }
    throw new CharmMappingException("Unsupported aesthetic '${name}'")
  }
}
