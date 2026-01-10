package se.alipsa.matrix.gg

import groovy.transform.CompileStatic

/**
 * Shared constants for annotation implementations.
 */
@CompileStatic
class AnnotationConstants {

  /**
   * Marker value used to represent positive/negative infinity for position bounds.
   * BigDecimal cannot directly represent infinity, so we use this large value as a marker
   * that GeomCustom will recognize and replace with actual panel bounds.
   */
  static final BigDecimal INFINITY_MARKER = 999999999999G

  private AnnotationConstants() {
    // Utility class - prevent instantiation
  }
}
