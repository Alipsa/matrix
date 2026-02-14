package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Supported geometry families for Charm v1.
 */
@CompileStatic
enum Geom {
  POINT,
  LINE,
  BAR,
  COL,
  HISTOGRAM,
  BOXPLOT,
  SMOOTH
}
