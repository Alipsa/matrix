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

}
