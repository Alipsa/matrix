package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.render.SpokeSupport
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
    String angleCol = SpokeSupport.resolveAngleColumn(params)
    BigDecimal angleDefault = SpokeSupport.resolveAngleDefault(params)
    String radiusCol = SpokeSupport.resolveRadiusColumn(params)
    BigDecimal radiusDefault = SpokeSupport.resolveRadiusDefault(params)

    List<LayerData> result = []
    data.each { LayerData datum ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      if (x == null || y == null) {
        return
      }

      Map<String, Object> row = SfStatSupport.rowMap(datum)
      BigDecimal angle = angleCol == null ? null : NumberCoercionUtil.coerceToBigDecimal(row[angleCol])
      if (angle == null) {
        angle = angleDefault
      }
      BigDecimal radius = radiusCol == null ? null : NumberCoercionUtil.coerceToBigDecimal(row[radiusCol])
      if (radius == null) {
        radius = radiusDefault
      }

      LayerData updated = LayerDataUtil.copyDatum(datum)
      updated.xend = x + radius * angle.cos()
      updated.yend = y + radius * angle.sin()
      result << updated
    }

    result
  }
}
