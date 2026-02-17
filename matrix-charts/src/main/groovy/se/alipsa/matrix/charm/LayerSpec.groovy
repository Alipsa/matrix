package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed layer specification for Charm core.
 */
@CompileStatic
class LayerSpec extends Layer {

  /**
   * Creates a new layer spec.
   *
   * @param geomSpec geometry specification
   * @param statSpec stat specification
   * @param aes layer aes
   * @param inheritAes inherit flag
   * @param positionSpec position specification
   * @param params layer params
   */
  LayerSpec(
      GeomSpec geomSpec,
      StatSpec statSpec = StatSpec.of(CharmStatType.IDENTITY),
      Aes aes = null,
      boolean inheritAes = true,
      PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY),
      Map<String, Object> params = [:]
  ) {
    super(geomSpec, statSpec, aes, inheritAes, positionSpec, params)
  }

  /**
   * Copies this layer spec.
   *
   * @return copied layer spec
   */
  @Override
  LayerSpec copy() {
    Aes layerAes = super.getAes()
    new LayerSpec(geomSpec.copy(), statSpec.copy(), layerAes, inheritAes, positionSpec.copy(), params)
  }
}
