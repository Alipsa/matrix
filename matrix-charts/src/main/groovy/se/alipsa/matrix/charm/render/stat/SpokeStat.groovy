package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Computes xend/yend from x/y plus angle/radius for spoke geoms.
 */
@CompileStatic
class SpokeStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    String angleCol = params.angle?.toString() ?: 'angle'
    String radiusCol = params.radius?.toString() ?: 'radius'
    BigDecimal radiusDefault = NumberCoercionUtil.coerceToBigDecimal(params.radiusDefault ?: params.radius) ?: 1

    List<LayerData> result = []
    data.each { LayerData datum ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      if (x == null || y == null) {
        return
      }

      Map<String, Object> row = SfStatSupport.rowMap(datum)
      BigDecimal angle = NumberCoercionUtil.coerceToBigDecimal(row[angleCol]) ?: 0
      BigDecimal radius = NumberCoercionUtil.coerceToBigDecimal(row[radiusCol]) ?: radiusDefault

      LayerData updated = LayerDataUtil.copyDatum(datum)
      updated.xend = x + radius * angle.cos()
      updated.yend = y + radius * angle.sin()
      result << updated
    }

    result
  }
}
