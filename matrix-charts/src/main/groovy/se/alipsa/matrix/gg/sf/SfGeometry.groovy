package se.alipsa.matrix.gg.sf

import groovy.transform.CompileStatic

/**
 * Parsed simple feature geometry with normalized shape/ring/point structure.
 */
@CompileStatic
class SfGeometry {
  final SfType type
  final List<SfShape> shapes
  final Integer srid
  final boolean empty

  SfGeometry(SfType type, List<SfShape> shapes = [], Integer srid = null, boolean empty = false) {
    this.type = type
    this.shapes = shapes ?: []
    this.srid = srid
    this.empty = empty
  }
}
