package se.alipsa.matrix.charm.render.coord

import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

/**
 * Radial coordinate transform (polar with non-negative radius).
 */
class RadialCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    List<LayerData> clamped = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal y = ValueConverter.asBigDecimal(updated.y)
      if (y != null && y < 0) {
        updated.y = 0
      }
      clamped << updated
    }
    PolarCoord.compute(coordSpec, clamped)
  }

}
