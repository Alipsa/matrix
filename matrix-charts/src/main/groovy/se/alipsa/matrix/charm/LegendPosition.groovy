package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed legend position constants.
 *
 * <p>Use these values in place of raw strings for IDE auto-complete and
 * compile-time safety. Absolute positioning via {@code [x, y]} lists is
 * still supported by the {@link Theme#legendPosition} field (typed as
 * {@code Object}).</p>
 */
@CompileStatic
enum LegendPosition {
  RIGHT, LEFT, TOP, BOTTOM, NONE

  private static final Map<String, LegendPosition> LOOKUP = values().collectEntries {
    [(it.name().toLowerCase(Locale.ROOT)): it]
  }

  /**
   * Resolves a string to the matching enum constant (case-insensitive).
   *
   * @param value position string
   * @return matching constant, or {@code null} if unrecognised
   */
  static LegendPosition fromString(String value) {
    String key = value?.trim()?.toLowerCase(Locale.ROOT)
    key ? LOOKUP[key] : null
  }

  /**
   * Normalises an {@code Object} that is either already a {@link LegendPosition},
   * a recognised string, or a {@code List} (absolute position pass-through).
   *
   * @param value raw value
   * @return {@link LegendPosition} enum, the original {@code List}, or the input unchanged
   */
  static Object normalize(Object value) {
    if (value == null || value instanceof LegendPosition || value instanceof List) {
      return value
    }
    if (value instanceof CharSequence) {
      LegendPosition pos = fromString(value.toString())
      if (pos != null) {
        return pos
      }
    }
    value
  }
}
