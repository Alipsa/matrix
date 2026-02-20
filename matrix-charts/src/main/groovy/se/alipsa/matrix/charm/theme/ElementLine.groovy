package se.alipsa.matrix.charm.theme

import groovy.transform.CompileStatic

/**
 * Line element styling for charm themes.
 *
 * Mirrors the gg ElementLine structure, providing typed access to
 * color, size (line width), linetype, and lineend properties.
 */
@CompileStatic
class ElementLine {

  /** Line color (CSS color string) */
  String color = 'black'

  /** Line width in pixels */
  Number size = 1

  /** Line type: 'solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash' */
  String linetype = 'solid'

  /** Line end cap: 'butt', 'round', 'square' */
  String lineend = 'butt'

  ElementLine() {}

  /**
   * Creates an ElementLine from a map of properties.
   *
   * @param params property map
   */
  ElementLine(Map params) {
    params.each { key, value ->
      String k = key as String
      if (k == 'colour') k = 'color'
      else if (k == 'linewidth') k = 'size'
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
  ElementLine copy() {
    new ElementLine(
        color: color,
        size: size,
        linetype: linetype,
        lineend: lineend
    )
  }
}
