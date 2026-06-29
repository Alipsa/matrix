package se.alipsa.matrix.gg.coord


/**
 * Coordinate system for simple feature data.
 * Defaults to a fixed aspect ratio for equal x/y units.
 */
class CoordSf extends CoordFixed {

  CoordSf() {
    super()
  }

  CoordSf(Map params) {
    super(params)
  }
}
