package se.alipsa.matrix.charm.theme

import groovy.transform.CompileStatic

/**
 * Text element styling for charm themes.
 *
 * Mirrors the gg ElementText structure, providing typed access to
 * font family, face, size, color, justification, angle, line height,
 * and margin properties.
 */
@CompileStatic
class ElementText {

  /** Font family (e.g., 'sans-serif', 'serif', 'monospace') */
  String family

  /** Font face: 'plain', 'italic', 'bold', 'bold.italic' */
  String face

  /** Font size in points */
  Number size

  /** Text color (CSS color string) */
  String color

  /** Horizontal justification (0=left, 0.5=center, 1=right) */
  Number hjust = 0.5

  /** Vertical justification */
  Number vjust = 0.5

  /** Rotation angle in degrees */
  Number angle = 0

  /** Line height multiplier */
  Number lineheight

  /** Margin [top, right, bottom, left] in pixels */
  List<Number> margin = [0, 0, 0, 0]

  ElementText() {}

  /**
   * Creates an ElementText from a map of properties.
   *
   * @param params property map
   */
  ElementText(Map params) {
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
  ElementText copy() {
    new ElementText(
        family: family,
        face: face,
        size: size,
        color: color,
        hjust: hjust,
        vjust: vjust,
        angle: angle,
        lineheight: lineheight,
        margin: margin != null ? new ArrayList<>(margin) : [0, 0, 0, 0]
    )
  }
}
