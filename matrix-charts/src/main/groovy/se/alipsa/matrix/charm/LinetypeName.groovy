package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed line-type name constants.
 *
 * <p>Use these in {@code geomLine().linetype(LinetypeName.DASHED)} or inside
 * the {@code layers {}} DSL where the constants are directly available.</p>
 */
@CompileStatic
enum LinetypeName {
  SOLID, DASHED, DOTTED, DOTDASH, LONGDASH, TWODASH

  private static final Map<String, LinetypeName> LOOKUP = values().collectEntries {
    [(it.name().toLowerCase(Locale.ROOT)): it]
  }

  /**
   * Resolves a string to the matching enum constant (case-insensitive).
   *
   * @param value linetype string
   * @return matching constant, or {@code null} if unrecognised
   */
  static LinetypeName fromString(String value) {
    LOOKUP[value?.trim()?.toLowerCase(Locale.ROOT)]
  }

  /**
   * Normalises an {@code Object} to a {@link LinetypeName} when possible.
   *
   * @param value raw value (enum, string, or data-mapped value)
   * @return resolved enum, or the input unchanged if unrecognised
   */
  static Object normalize(Object value) {
    if (value == null || value instanceof LinetypeName) {
      return value
    }
    if (value instanceof CharSequence) {
      LinetypeName lt = fromString(value.toString())
      if (lt != null) {
        return lt
      }
    }
    value
  }
}
