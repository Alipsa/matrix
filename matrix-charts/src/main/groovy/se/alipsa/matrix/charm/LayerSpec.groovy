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
   * @param mapping layer mapping
   * @param inheritMapping inherit flag
   * @param positionSpec position specification
   * @param params layer params
   */
  LayerSpec(
      GeomSpec geomSpec,
      StatSpec statSpec = StatSpec.of(CharmStatType.IDENTITY),
      Mapping mapping = null,
      boolean inheritMapping = true,
      PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY),
      Map<String, Object> params = [:]
  ) {
    super(geomSpec, statSpec, mapping, inheritMapping, positionSpec, params)
  }

  /**
   * Copies this layer spec.
   *
   * @return copied layer spec
   */
  @Override
  LayerSpec copy() {
    Mapping layerMapping = super.getMapping()
    new LayerSpec(geomSpec.copy(), statSpec.copy(), layerMapping, inheritMapping, positionSpec.copy(), params)
  }
}
