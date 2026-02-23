package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

/**
 * Nudge position shifts points by fixed x/y offsets.
 */
@CompileStatic
class NudgePosition {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    BigDecimal nudgeX = ValueConverter.asBigDecimal(params.x) ?: 0
    BigDecimal nudgeY = ValueConverter.asBigDecimal(params.y) ?: 0
    if (nudgeX == 0 && nudgeY == 0) {
      return data
    }

    List<LayerData> result = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal x = ValueConverter.asBigDecimal(updated.x)
      BigDecimal y = ValueConverter.asBigDecimal(updated.y)
      if (x != null) {
        updated.x = x + nudgeX
      }
      if (y != null) {
        updated.y = y + nudgeY
      }
      result << updated
    }
    result
  }
}
