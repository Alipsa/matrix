package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.PackageScope

/**
 * Shared identifier validation and rendering for formula DSL names.
 */
@PackageScope
final class IdentifierRenderingSupport {
  private static final char DOT_CHAR = '.' as char
  private static final char UNDERSCORE_CHAR = '_' as char
  private static final String DOT_IDENTIFIER = '.'

  private IdentifierRenderingSupport() {
  }

  static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }

  static String renderIdentifier(String name, boolean quoted) {
    if (quoted || !simpleIdentifier(name)) {
      return "`${name.replace('`', '``')}`"
    }
    name
  }

  private static boolean simpleIdentifier(String value) {
    if (value == DOT_IDENTIFIER) {
      return false
    }
    char first = value.charAt(0)
    if (!(Character.isLetter(first) || first == UNDERSCORE_CHAR || first == DOT_CHAR)) {
      return false
    }
    if (first == DOT_CHAR && value.length() > 1 && Character.isDigit(value.charAt(1))) {
      return false
    }
    for (int i = 1; i < value.length(); i++) {
      char current = value.charAt(i)
      if (!(Character.isLetterOrDigit(current) || current == UNDERSCORE_CHAR || current == DOT_CHAR)) {
        return false
      }
    }
    true
  }
}
