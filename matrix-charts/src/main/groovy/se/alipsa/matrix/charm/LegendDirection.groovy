package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed legend direction constants.
 */
@CompileStatic
enum LegendDirection {
  VERTICAL, HORIZONTAL

  private static final Map<String, LegendDirection> LOOKUP = values().collectEntries {
    [(it.name().toLowerCase(Locale.ROOT)): it]
  }

  /**
   * Resolves a string to the matching enum constant (case-insensitive).
   *
   * @param value direction string
   * @return matching constant, or {@code null} if unrecognised
   */
  static LegendDirection fromString(String value) {
    LOOKUP[value?.trim()?.toLowerCase(Locale.ROOT)]
  }

  /**
   * Normalises an {@code Object} to a {@link LegendDirection}.
   *
   * @param value raw value (enum or string)
   * @return resolved enum, or the input unchanged
   */
  static Object normalize(Object value) {
    if (value == null || value instanceof LegendDirection) {
      return value
    }
    if (value instanceof CharSequence) {
      LegendDirection dir = fromString(value.toString())
      if (dir != null) {
        return dir
      }
    }
    value
  }
}
