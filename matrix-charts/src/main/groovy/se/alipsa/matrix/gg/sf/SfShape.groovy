package se.alipsa.matrix.gg.sf

import groovy.transform.CompileStatic

/**
 * A shape is a collection of rings that together define a geometry part.
 */
@CompileStatic
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
