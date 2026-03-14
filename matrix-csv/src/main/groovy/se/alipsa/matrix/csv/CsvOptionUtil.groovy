package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import java.nio.charset.Charset

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
}
