package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Dodge2 position with optional padding.
 */
@CompileStatic
class Dodge2Position {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    BigDecimal padding = NumberCoercionUtil.coerceToBigDecimal(params.padding) ?: 0.1
    List<LayerData> dodged = DodgePosition.compute(layer, data)
    if (padding <= 0) {
      return dodged
    }

    List<LayerData> adjusted = []
    dodged.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(updated.x)
      if (x != null) {
        BigDecimal sign = x < 0 ? -1 : 1
        updated.x = x + sign * padding / 2
      }
      adjusted << updated
    }
    adjusted
  }
}
