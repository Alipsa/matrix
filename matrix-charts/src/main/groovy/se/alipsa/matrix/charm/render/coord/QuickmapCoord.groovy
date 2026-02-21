package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Quickmap coord with approximate latitude-based aspect correction.
 */
@CompileStatic
class QuickmapCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    List<BigDecimal> latitudes = data.collect { LayerData datum ->
      NumberCoercionUtil.coerceToBigDecimal(datum.y)
    }.findAll { it != null } as List<BigDecimal>
    if (latitudes.isEmpty()) {
      return FixedCoord.compute(coordSpec, data)
    }

    BigDecimal meanLat = (latitudes.sum() as BigDecimal) / latitudes.size()
    BigDecimal ratio = meanLat.toRadians().cos().abs()
    if (ratio <= 0) {
      ratio = 1
    }

    List<LayerData> adjusted = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(updated.y)
      if (y != null) {
        updated.y = y * ratio
      }
      adjusted << updated
    }

    FixedCoord.compute(coordSpec, adjusted)
  }
}
