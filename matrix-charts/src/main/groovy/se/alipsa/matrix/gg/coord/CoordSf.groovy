package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

/**
 * Coordinate system for simple feature data.
 * Defaults to a fixed aspect ratio for equal x/y units.
 */
@CompileStatic
class CoordSf extends CoordFixed {

  CoordSf() {
    super()
  }

  CoordSf(Map params) {
    super(params)
  }
}
