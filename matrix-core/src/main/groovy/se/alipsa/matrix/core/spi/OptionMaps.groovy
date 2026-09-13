package se.alipsa.matrix.core.spi

/**
 * Shared helpers for normalizing SPI option maps.
 */
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

  /**
   * Parses an optional Boolean SPI value without Groovy truthiness semantics.
   *
   * @param value a Boolean, a case-insensitive {@code true}/{@code false} string, or null
   * @param optionName option name used in validation messages
   * @return the parsed value, or null when the supplied value is null
   */
  static Boolean booleanValueOrNull(Object value, String optionName) {
    if (value == null || Boolean.isInstance(value)) {
      return (Boolean) value
    }
    if (CharSequence.isInstance(value)) {
      String normalized = String.valueOf(value).trim()
      if (normalized.equalsIgnoreCase('true')) {
        return true
      }
      if (normalized.equalsIgnoreCase('false')) {
        return false
      }
    }
    throw new IllegalArgumentException("$optionName must be a Boolean or 'true'/'false' string but was ${value.class}")
  }

}
