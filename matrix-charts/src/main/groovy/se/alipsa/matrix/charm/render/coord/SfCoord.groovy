package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Coordinate behavior for simple-feature layers.
 */
@CompileStatic
class SfCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    FixedCoord.compute(coordSpec, data)
  }
}
