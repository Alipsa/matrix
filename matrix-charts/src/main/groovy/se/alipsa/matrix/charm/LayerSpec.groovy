package se.alipsa.matrix.charm

/**
 * Typed layer specification for Charm core.
 */
@SuppressWarnings('ParameterCount')
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
   * @param styleCallback per-datum style override callback
   * @param scales per-layer scale overrides
   */
  LayerSpec(
      GeomSpec geomSpec,
      StatSpec statSpec = StatSpec.of(CharmStatType.IDENTITY),
      Mapping mapping = null,
      boolean inheritMapping = true,
      PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY),
      Map<String, Object> params = [:],
      Closure styleCallback = null,
      Map<String, Scale> scales = [:]
  ) {
    super(geomSpec, statSpec, mapping, inheritMapping, positionSpec, params, styleCallback, scales)
  }

  /**
   * Copies this layer spec.
   *
   * @return copied layer spec
   */
  @Override
  LayerSpec copy() {
    Mapping layerMapping = super.getMapping()
    // Read canonical values via the package-scope raw accessors rather than the
    // public getters (which now return defensive copies) to avoid copying twice.
    Map<String, Scale> rawScalesMap = rawScales()
    Map<String, Scale> copiedScales = rawScalesMap ?
        rawScalesMap.findAll { String k, Scale v -> v != null }
            .collectEntries { String k, Scale v -> [(k): v.copy()] } as Map<String, Scale> :
        [:]
    Map<String, Object> copiedParams = [:]
    params.each { String key, Object value ->
      copiedParams[key] = SpecCopyUtil.deepCopyValue(value)
    }
    new LayerSpec(rawGeomSpec().copy(), rawStatSpec().copy(), layerMapping, inheritMapping, rawPositionSpec().copy(), copiedParams, styleCallback, copiedScales)
  }

}
