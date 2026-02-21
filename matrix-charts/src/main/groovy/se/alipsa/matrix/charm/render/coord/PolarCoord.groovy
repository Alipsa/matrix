package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Polar coordinate transform at data level.
 */
@CompileStatic
class PolarCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    String theta = coordSpec?.params?.theta?.toString()?.toLowerCase() ?: 'x'
    BigDecimal start = NumberCoercionUtil.coerceToBigDecimal(coordSpec?.params?.start) ?: 0
    BigDecimal direction = NumberCoercionUtil.coerceToBigDecimal(coordSpec?.params?.direction) ?: 1

    List<LayerData> result = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal t = NumberCoercionUtil.coerceToBigDecimal(theta == 'y' ? datum.y : datum.x)
      BigDecimal r = NumberCoercionUtil.coerceToBigDecimal(theta == 'y' ? datum.x : datum.y)
      if (t != null && r != null) {
        BigDecimal angle = start + direction * t * 2 * PI
        updated.x = r * angle.cos()
        updated.y = r * angle.sin()
      }
      result << updated
    }
    CartesianCoord.compute(coordSpec, result)
  }
}
