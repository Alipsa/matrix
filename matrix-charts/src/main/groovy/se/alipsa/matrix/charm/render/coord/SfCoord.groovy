package se.alipsa.matrix.charm.render.coord

import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Coordinate behavior for simple-feature layers.
 */
class SfCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    FixedCoord.compute(coordSpec, data)
  }

}
