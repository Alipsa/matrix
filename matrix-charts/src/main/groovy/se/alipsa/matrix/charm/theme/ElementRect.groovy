package se.alipsa.matrix.charm.theme

import groovy.transform.CompileStatic

/**
 * Rectangle element styling for charm themes.
 *
 * Mirrors the gg ElementRect structure, providing typed access to
 * fill, border color, border size, and linetype properties.
 */
@CompileStatic
class ElementRect {

  /** Fill color (CSS color string) */
  String fill = 'white'

  /** Border color (CSS color string) */
  String color = 'black'

  /** Border width in pixels */
  Number size = 1

  /** Border line type: 'solid', 'dashed', 'dotted' */
  String linetype = 'solid'

  ElementRect() {}

  /**
   * Creates an ElementRect from a map of properties.
   *
   * @param params property map
   */
  ElementRect(Map params) {
    params.each { key, value ->
      String k = key as String
      if (k == 'colour') k = 'color'
      if (this.hasProperty(k)) {
        this.setProperty(k, value)
      }
    }
  }

  /**
   * Creates a deep copy of this element.
   *
   * @return copied element
   */
  ElementRect copy() {
    new ElementRect(
        fill: fill,
        color: color,
        size: size,
        linetype: linetype
    )
  }
}
