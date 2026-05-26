package se.alipsa.matrix.charm.sf

/**
 * A single 2D point in a simple feature geometry.
 */
class SfPoint {

  final BigDecimal x
  final BigDecimal y

  SfPoint(BigDecimal x, BigDecimal y) {
    this.x = x
    this.y = y
  }

}
