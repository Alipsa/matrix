package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import java.nio.charset.Charset
import java.util.Locale

/**
 * Shared conversion helpers for CSV option handling.
 */
@CompileStatic
class CsvOptionUtil {

  /**
   * Resolves a charset option supplied as either a {@link Charset} or charset name.
   *
   * @param value the option value to resolve
   * @return the resolved charset
   * @throws IllegalArgumentException if the value is neither a Charset nor a String
   */
  static Charset resolveCharset(Charset value) {
    value
  }

  /**
   * Resolves a charset option supplied as a charset name.
   *
   * @param value the charset name to resolve
   * @return the resolved charset
   */
  static Charset resolveCharset(String value) {
    Charset.forName(value)
  }

  /**
   * Resolves a charset option supplied as any {@link CharSequence}.
   *
   * @param value the charset name to resolve
   * @return the resolved charset
   */
  static Charset resolveCharset(CharSequence value) {
    Charset.forName(String.valueOf(value))
  }

  /**
   * Resolves a boolean option supplied as either a {@link Boolean} or
   * a string containing {@code true} or {@code false}.
   *
   * @param value the option value to resolve
   * @param optionName the option name for error messages
   * @return the resolved boolean value
   */
  static boolean booleanValue(Object value, String optionName) {
    if (value instanceof Boolean) {
      return value as Boolean
    }
    if (value instanceof CharSequence) {
      String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT)
      if (normalized == 'true') {
        return true
      }
      if (normalized == 'false') {
        return false
      }
    }
    throw new IllegalArgumentException("${optionName} must be a Boolean but was ${value?.class}")
  }

  /**
   * Resolves a required character option supplied as a {@link Character} or
   * a single-character string.
   *
   * @param value the option value to resolve
   * @param optionName the option name for error messages
   * @return the resolved character value
   */
  static Character requiredCharacterValue(Object value, String optionName) {
    Character resolved = optionalCharacterValue(value, optionName)
    if (resolved == null) {
      throw new IllegalArgumentException("${optionName} must be a Character or single-character string")
    }
    resolved
  }

  /**
   * Resolves an optional character option supplied as a {@link Character},
   * {@link CharSequence}, or {@code null}.
   *
   * <p>Empty strings are treated as {@code null}.</p>
   *
   * @param value the option value to resolve
   * @param optionName the option name for error messages
   * @return the resolved character value, or {@code null}
   */
  static Character optionalCharacterValue(Object value, String optionName) {
    if (value == null) {
      return null
    }
    if (value instanceof Character) {
      return value as Character
    }
    if (value instanceof CharSequence) {
      String text = String.valueOf(value)
      if (text.isEmpty()) {
        return null
      }
      if (text.length() == 1) {
        return text.charAt(0)
      }
    }
    throw new IllegalArgumentException("${optionName} must be a Character or single-character string but was ${value?.class}")
  }

  /**
   * Returns {@code true} when the supplied file name uses the TSV/TAB extensions.
   *
   * @param fileName the file name or path to inspect
   * @return whether the name indicates a TSV-style file
   */
  static boolean isTsvFileName(String fileName) {
    if (fileName == null) {
      return false
    }
    String lower = String.valueOf(fileName).toLowerCase(Locale.ROOT)
    lower.endsWith('.tsv') || lower.endsWith('.tab')
  }
}
