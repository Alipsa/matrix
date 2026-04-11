package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.PackageScope

/**
 * Shared identifier validation and rendering for formula DSL names.
 */
@PackageScope
final class IdentifierRenderingSupport {

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
    if (value == '.') {
      return false
    }
    char first = value.charAt(0)
    if (!(Character.isLetter(first) || first == '_' || first == '.')) {
      return false
    }
    if (first == '.' && value.length() > 1 && Character.isDigit(value.charAt(1))) {
      return false
    }
    for (int i = 1; i < value.length(); i++) {
      char current = value.charAt(i)
      if (!(Character.isLetterOrDigit(current) || current == '_' || current == '.')) {
        return false
      }
    }
    true
  }
}
