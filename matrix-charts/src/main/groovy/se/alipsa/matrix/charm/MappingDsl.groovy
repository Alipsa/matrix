package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Closure delegate for `mapping {}` configuration blocks.
 */
@CompileStatic
class MappingDsl {

  String x
  String y
  String color
  String fill
  String size
  String shape
  String group
  String xend
  String yend
  String xmin
  String xmax
  String ymin
  String ymax
  String alpha
  String linetype
  String label
  String weight

  /**
   * Sets the colour aesthetic (alias for color).
   *
   * @param value column name
   */
  void setColour(String value) {
    color = value
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
}
