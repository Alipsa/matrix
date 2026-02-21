package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Jitter position adds random noise to x/y values.
 */
@CompileStatic
class JitterPosition {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    BigDecimal width = NumberCoercionUtil.coerceToBigDecimal(params.width) ?: 0.4
    BigDecimal height = NumberCoercionUtil.coerceToBigDecimal(params.height) ?: width
    Long seed = NumberCoercionUtil.coerceToBigDecimal(params.seed)?.longValue()
    Random random = seed != null ? new Random(seed) : new Random()

    List<LayerData> result = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      if (x != null) {
        updated.x = x + ((random.nextDouble() - 0.5d) as BigDecimal) * width
      }
      if (y != null) {
        updated.y = y + ((random.nextDouble() - 0.5d) as BigDecimal) * height
      }
      result << updated
    }
    result
  }
}
