package se.alipsa.matrix.arff

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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

  /** Resolve a charset instance or name from an SPI option; null uses UTF-8. */
  static Charset charsetValue(Object value) {
    if (value == null) {
      return StandardCharsets.UTF_8
    }
    if (value instanceof Charset) {
      return value
    }
    if (value instanceof CharSequence) {
      return Charset.forName(value.toString())
    }
    throw new IllegalArgumentException('charset must be a Charset or name')
  }

  /** Resolve the supported INTEGER types without loading arbitrary classes. */
  static Class<? extends Number> integerTypeValue(Object value) {
    if (value == null) {
      return Integer
    }
    for (Class<? extends Number> type : [Integer, Long, BigDecimal]) {
      if (value == type || (value instanceof CharSequence &&
          (value.toString().equalsIgnoreCase(type.simpleName) || value.toString().equalsIgnoreCase(type.name)))) {
        return type
      }
    }
    throw new IllegalArgumentException('integerType must be Integer, Long or BigDecimal')
  }

}
