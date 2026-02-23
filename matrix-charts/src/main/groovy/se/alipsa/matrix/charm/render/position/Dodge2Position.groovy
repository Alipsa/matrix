package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

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
    BigDecimal padding = ValueConverter.asBigDecimal(params.padding) ?: 0.1
    List<LayerData> dodged = DodgePosition.compute(layer, data)
    if (padding <= 0) {
      return dodged
    }

    Map<Integer, List<BigDecimal>> sourceXByRowIndex = [:].withDefault { [] }
    data.each { LayerData datum ->
      BigDecimal sourceX = ValueConverter.asBigDecimal(datum.x)
      if (sourceX != null) {
        sourceXByRowIndex[datum.rowIndex] << sourceX
      }
    }

    List<LayerData> adjusted = []
    dodged.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal dodgedX = ValueConverter.asBigDecimal(updated.x)
      BigDecimal sourceX = nearestSourceX(dodgedX, sourceXByRowIndex[datum.rowIndex])
      if (dodgedX != null && sourceX != null) {
        BigDecimal offsetFromCenter = dodgedX - sourceX
        updated.x = sourceX + (offsetFromCenter * (1 + padding))
      }
      adjusted << updated
    }
    adjusted
  }

  private static BigDecimal nearestSourceX(BigDecimal target, List<BigDecimal> candidates) {
    if (target == null || candidates == null || candidates.isEmpty()) {
      return null
    }
    BigDecimal best = candidates[0]
    BigDecimal bestDistance = (target - best).abs()
    for (int i = 1; i < candidates.size(); i++) {
      BigDecimal candidate = candidates[i]
      BigDecimal candidateDistance = (target - candidate).abs()
      if (candidateDistance < bestDistance) {
        best = candidate
        bestDistance = candidateDistance
      }
    }
    best
  }
}
