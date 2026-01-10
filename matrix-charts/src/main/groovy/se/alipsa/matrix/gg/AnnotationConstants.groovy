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
   *
   * Uses 1e15 (1 quadrillion) to avoid conflicts with typical data values while
   * remaining safely below Double.MAX_VALUE (approximately 1.8e308).
   */
  static final BigDecimal INFINITY_MARKER = 1e15 as BigDecimal

  private AnnotationConstants() {
    // Utility class - prevent instantiation
  }
}
