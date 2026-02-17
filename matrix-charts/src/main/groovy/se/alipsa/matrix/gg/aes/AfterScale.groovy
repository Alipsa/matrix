package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmExpression

/**
 * Wrapper for referencing scaled aesthetics in mappings.
 */
@CompileStatic
class AfterScale implements CharmExpression {

  /** The name of the scaled aesthetic to reference. */
  final String aesthetic

  AfterScale(String aesthetic) {
    if (aesthetic == null) {
      throw new IllegalArgumentException("aesthetic name cannot be null")
    }
    if (aesthetic.isEmpty()) {
      throw new IllegalArgumentException("aesthetic name cannot be empty")
    }
    String trimmed = aesthetic.trim()
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("aesthetic name cannot be whitespace-only")
    }
    this.aesthetic = trimmed
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

  @Override
  String describe() {
    "after_scale(${aesthetic})"
  }
}
