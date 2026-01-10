package se.alipsa.matrix.gg.sf

import groovy.transform.CompileStatic

/**
 * A single 2D point in a simple feature geometry.
 */
@CompileStatic
class SfPoint {
  final BigDecimal x
  final BigDecimal y

  SfPoint(BigDecimal x, BigDecimal y) {
    this.x = x
    this.y = y
  }
}
