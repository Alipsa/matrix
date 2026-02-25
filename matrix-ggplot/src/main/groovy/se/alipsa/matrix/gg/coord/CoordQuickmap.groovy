package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

/**
 * Quick map coordinate system with approximate aspect ratio for lon/lat data.
 * Ratio is computed from the data's mean latitude in the renderer.
 */
@CompileStatic
class CoordQuickmap extends CoordFixed {

  CoordQuickmap() {
    super()
  }

  CoordQuickmap(Map params) {
    super(params)
  }
}
