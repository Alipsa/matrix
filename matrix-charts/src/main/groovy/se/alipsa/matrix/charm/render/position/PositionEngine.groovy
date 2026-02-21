package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.util.Logger

/**
 * Central dispatch hub for position adjustments.
 * Routes position computation to the appropriate position class based on the layer's position type.
 */
@CompileStatic
class PositionEngine {

  private static final Logger log = Logger.getLogger(PositionEngine)

  /**
   * Merges layer params with position spec params into a single effective params map.
   * Position spec params take priority over layer params.
   *
   * @param layer layer specification
   * @return merged params map
   */
  static Map<String, Object> effectiveParams(LayerSpec layer) {
    Map<String, Object> merged = new LinkedHashMap<>(layer.params ?: [:])
    if (layer.positionSpec.params) {
      merged.putAll(layer.positionSpec.params)
    }
    merged
  }

  /**
   * Applies the position adjustment specified by the layer's position type.
   *
   * @param layer layer specification containing position type and params
   * @param data layer data from stat transformation
   * @return position-adjusted layer data
   */
  static List<LayerData> apply(LayerSpec layer, List<LayerData> data) {
    CharmPositionType positionType = layer.positionType
    switch (positionType) {
      case CharmPositionType.IDENTITY -> IdentityPosition.compute(layer, data)
      case CharmPositionType.DODGE -> DodgePosition.compute(layer, data)
      case CharmPositionType.DODGE2 -> Dodge2Position.compute(layer, data)
      case CharmPositionType.STACK -> StackPosition.compute(layer, data)
      case CharmPositionType.FILL -> FillPosition.compute(layer, data)
      case CharmPositionType.JITTER -> JitterPosition.compute(layer, data)
      case CharmPositionType.NUDGE -> NudgePosition.compute(layer, data)
      default -> {
        log.debug("Unimplemented position type ${positionType}, falling back to identity")
        data
      }
    }
  }
}
