package se.alipsa.matrix.charm.theme

import groovy.transform.CompileStatic

/**
 * Blank element marker for charm themes.
 *
 * Presence of this class indicates the element should not be drawn.
 * Used when a theme explicitly removes an element via element_blank().
 */
@CompileStatic
class ElementBlank {
  // Marker class - presence indicates element should not be drawn
}
