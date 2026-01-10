package se.alipsa.matrix.gg.sf

import groovy.transform.CompileStatic

/**
 * A ring is an ordered list of points, optionally marked as a hole.
 */
@CompileStatic
class SfRing {
  final List<SfPoint> points
  final boolean hole

  SfRing(List<SfPoint> points, boolean hole = false) {
    this.points = points ?: []
    this.hole = hole
  }
}
