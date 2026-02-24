package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Canonical layer specification in Charm.
 */
@CompileStatic
class Layer {

  private final GeomSpec geomSpec
  private final StatSpec statSpec
  private final Mapping mapping
  private final boolean inheritMapping
  private final PositionSpec positionSpec
  private final Map<String, Object> params

  /**
   * Creates a new layer.
   *
   * @param geomSpec geometry specification
   * @param statSpec statistical transformation specification
   * @param mapping layer-specific mappings
   * @param inheritMapping true when plot-level mappings are inherited
   * @param positionSpec position adjustment specification
   * @param params free-form layer parameters
   */
  Layer(
      GeomSpec geomSpec,
      StatSpec statSpec = StatSpec.of(CharmStatType.IDENTITY),
      Mapping mapping = null,
      boolean inheritMapping = true,
      PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY),
      Map<String, Object> params = [:]
  ) {
    this.geomSpec = geomSpec ?: GeomSpec.of(CharmGeomType.POINT)
    this.statSpec = statSpec ?: StatSpec.of(CharmStatType.IDENTITY)
    this.mapping = mapping
    this.inheritMapping = inheritMapping
    this.positionSpec = positionSpec ?: PositionSpec.of(CharmPositionType.IDENTITY)
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
  }

  /**
   * Returns the geometry type.
   *
   * @return geometry type
   */
  CharmGeomType getGeomType() {
    geomSpec.type
  }

  /**
   * Returns the geometry specification.
   *
   * @return geometry spec
   */
  GeomSpec getGeomSpec() {
    geomSpec
  }

  /**
   * Returns the statistical transformation type.
   *
   * @return stat type
   */
  CharmStatType getStatType() {
    statSpec.type
  }

  /**
   * Returns the stat specification.
   *
   * @return stat spec
   */
  StatSpec getStatSpec() {
    statSpec
  }

  /**
   * Returns layer-specific mappings.
   *
   * @return layer mapping or null
   */
  Mapping getMapping() {
    mapping?.copy()
  }

  /**
   * Returns whether layer inherits plot-level mappings.
   *
   * @return true if inheriting mappings
   */
  boolean isInheritMapping() {
    inheritMapping
  }

  /**
   * Returns position-adjustment type.
   *
   * @return position type
   */
  CharmPositionType getPositionType() {
    positionSpec.type
  }

  /**
   * Returns the position specification.
   *
   * @return position spec
   */
  PositionSpec getPositionSpec() {
    positionSpec
  }

  /**
   * Returns immutable layer params.
   *
   * @return parameter map
   */
  Map<String, Object> getParams() {
    Collections.unmodifiableMap(params)
  }

  /**
   * Creates a copy of this layer.
   *
   * @return copied layer
   */
  Layer copy() {
    new Layer(geomSpec.copy(), statSpec.copy(), mapping?.copy(), inheritMapping, positionSpec.copy(), params)
  }
}
