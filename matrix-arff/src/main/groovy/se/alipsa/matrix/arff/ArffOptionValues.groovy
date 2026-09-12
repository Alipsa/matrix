package se.alipsa.matrix.arff

/**
 * Shared coercion helpers for ARFF SPI option values.
 */
final class ArffOptionValues {

  private ArffOptionValues() {
  }

  static boolean booleanValue(Object value, String name) {
    if (Boolean.isInstance(value)) {
      return (Boolean) value
    }
    if (CharSequence.isInstance(value)) {
      String normalized = value.toString().trim().toLowerCase(Locale.ROOT)
      if (normalized == 'true') {
        return true
      }
      if (normalized == 'false') {
        return false
      }
    }
    throw new IllegalArgumentException("$name must be a boolean but was ${value?.class}")
  }

  /**
   * Coerce an option value to an enum constant: the constant itself, or its name as a string (case-insensitive).
   */
  static <E extends Enum<E>> E enumValue(Object value, Class<E> type, String name) {
    if (type.isInstance(value)) {
      return type.cast(value)
    }
    if (CharSequence.isInstance(value)) {
      try {
        return Enum.valueOf(type, value.toString().trim().toUpperCase(Locale.ROOT))
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("$name must be one of ${type.enumConstants.toList()} but was $value", e)
      }
    }
    throw new IllegalArgumentException("$name must be a ${type.simpleName} or String but was ${value?.class}")
  }

}
