package se.alipsa.matrix.core.spi

import groovy.transform.CompileStatic

import java.util.Locale

/**
 * Shared helpers for normalizing SPI option maps.
 */
@CompileStatic
class OptionMaps {

  private OptionMaps() {
    // Utility class
  }

  /**
   * Returns a new map with all keys lower-cased for case-insensitive lookups.
   *
   * @param options the original options map
   * @return a mutable map keyed by lower-case strings
   */
  static Map<String, Object> normalizeKeys(Map<String, ?> options) {
    Map<String, Object> normalized = [:]
    if (options == null) {
      return normalized
    }
    options.each { k, v ->
      normalized.put(String.valueOf(k).toLowerCase(Locale.ROOT), v)
    }
    normalized
  }

  /**
   * Converts a possibly-null option value to a String while preserving null.
   *
   * @param value the option value to stringify
   * @return null when the value is null, otherwise {@link String#valueOf(Object)}
   */
  static String stringValueOrNull(Object value) {
    value == null ? null : String.valueOf(value)
  }
}
