package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic

/**
 * Wrapper for referencing scaled aesthetics in mappings.
 */
@CompileStatic
class AfterScale {

  /** The name of the scaled aesthetic to reference. */
  final String aesthetic

  AfterScale(String aesthetic) {
    if (aesthetic == null || aesthetic.trim().isEmpty()) {
      throw new IllegalArgumentException("aesthetic name cannot be null, empty, or whitespace-only")
    }
    this.aesthetic = aesthetic.trim()
  }

  /**
   * Get the aesthetic name.
   */
  String getAesthetic() {
    return aesthetic
  }

  @Override
  String toString() {
    return "after_scale(${aesthetic})"
  }

  @Override
  boolean equals(Object obj) {
    if (obj instanceof AfterScale) {
      return aesthetic == ((AfterScale) obj).aesthetic
    }
    return false
  }

  @Override
  int hashCode() {
    return aesthetic.hashCode()
  }
}
