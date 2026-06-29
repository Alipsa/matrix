package se.alipsa.matrix.gg.coord


/**
 * Quick map coordinate system with approximate aspect ratio for lon/lat data.
 * Ratio is computed from the data's mean latitude in the renderer.
 */
class CoordQuickmap extends CoordFixed {

  CoordQuickmap() {
    super()
  }

  CoordQuickmap(Map params) {
    super(params)
  }
}
