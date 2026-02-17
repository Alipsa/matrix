package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Expanded coordinate system types covering the full gg surface.
 *
 * Each value corresponds to a ggplot2 coord family.
 */
@CompileStatic
enum CharmCoordType {
  CARTESIAN,
  POLAR,
  FLIP,
  FIXED,
  TRANS,
  RADIAL,
  MAP,
  QUICKMAP,
  SF
}
