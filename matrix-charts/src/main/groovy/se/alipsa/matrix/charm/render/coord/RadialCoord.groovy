package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Radial coordinate transform (polar with non-negative radius).
 */
@CompileStatic
class RadialCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    List<LayerData> clamped = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(updated.y)
      if (y != null && y < 0) {
        updated.y = 0
      }
      clamped << updated
    }
    PolarCoord.compute(coordSpec, clamped)
  }
}
