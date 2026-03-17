package se.alipsa.matrix.arff

import groovy.transform.CompileStatic

import java.util.Locale

@CompileStatic
final class ArffOptionValues {

  private ArffOptionValues() {
  }

  static boolean booleanValue(Object value, String name) {
    if (value instanceof Boolean) {
      return (Boolean) value
    }
    if (value instanceof CharSequence) {
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
