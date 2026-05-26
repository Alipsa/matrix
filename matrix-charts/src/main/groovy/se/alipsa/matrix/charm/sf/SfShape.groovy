package se.alipsa.matrix.charm.sf

/**
 * A shape is a collection of rings that together define a geometry part.
 */
class SfShape {

  final SfType type
  final List<SfRing> rings

  SfShape(SfType type, List<SfRing> rings) {
    this.type = type
    this.rings = rings ?: []
  }

  SfShape(List<SfRing> rings) {
    this(null, rings)
  }

}
