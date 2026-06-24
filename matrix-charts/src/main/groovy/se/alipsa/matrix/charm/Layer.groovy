package se.alipsa.matrix.charm

import groovy.transform.PackageScope

/**
 * Canonical layer specification in Charm.
 */
@SuppressWarnings('ParameterCount')
class Layer {

  private final GeomSpec geomSpec
  private final StatSpec statSpec
  private final Mapping mapping
  private final boolean inheritMapping
  private final PositionSpec positionSpec
  private final Map<String, Object> params
  private final Closure styleCallback
  private final Map<String, Scale> scales

  /**
   * Creates a new layer.
   *
   * @param geomSpec geometry specification
   * @param statSpec statistical transformation specification
   * @param mapping layer-specific mappings
   * @param inheritMapping true when plot-level mappings are inherited
   * @param positionSpec position adjustment specification
   * @param params free-form layer parameters
   * @param styleCallback per-datum style override callback
   * @param scales per-layer scale overrides keyed by aesthetic name
   */
  Layer(
      GeomSpec geomSpec,
      StatSpec statSpec = StatSpec.of(CharmStatType.IDENTITY),
      Mapping mapping = null,
      boolean inheritMapping = true,
      PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY),
      Map<String, Object> params = [:],
      Closure styleCallback = null,
      Map<String, Scale> scales = [:]
  ) {
    this.geomSpec = geomSpec ?: GeomSpec.of(CharmGeomType.POINT)
    this.statSpec = statSpec ?: StatSpec.of(CharmStatType.IDENTITY)
    this.mapping = mapping
    this.inheritMapping = inheritMapping
    this.positionSpec = positionSpec ?: PositionSpec.of(CharmPositionType.IDENTITY)
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
    this.styleCallback = styleCallback
    this.scales = scales == null ? [:] : new LinkedHashMap<>(scales)
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
   * @return defensive copy of the geometry spec
   */
  GeomSpec getGeomSpec() {
    geomSpec.copy()
  }

  /**
   * Returns the canonical geometry specification without copying.
   * Internal use only — callers in this package that already own a
   * defensive copy of the enclosing {@code Layer} may read this directly.
   *
   * @return the stored geometry spec
   */
  @PackageScope
  GeomSpec rawGeomSpec() {
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
   * @return defensive copy of the stat spec
   */
  StatSpec getStatSpec() {
    statSpec.copy()
  }

  /**
   * Returns the canonical stat specification without copying.
   * Internal use only — see {@link #rawGeomSpec()}.
   *
   * @return the stored stat spec
   */
  @PackageScope
  StatSpec rawStatSpec() {
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
   * @return defensive copy of the position spec
   */
  PositionSpec getPositionSpec() {
    positionSpec.copy()
  }

  /**
   * Returns the canonical position specification without copying.
   * Internal use only — see {@link #rawGeomSpec()}.
   *
   * @return the stored position spec
   */
  @PackageScope
  PositionSpec rawPositionSpec() {
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
   * Returns the per-datum style override callback, or null.
   *
   * @return style callback closure
   */
  Closure getStyleCallback() {
    styleCallback
  }

  /**
   * Returns per-layer scale overrides keyed by aesthetic name (e.g. 'color', 'x').
   *
   * @return unmodifiable map of defensive copies of non-null scale overrides, or empty map if none
   */
  Map<String, Scale> getScales() {
    Collections.unmodifiableMap(SpecCopyUtil.copyScales(scales))
  }

  /**
   * Returns the canonical per-layer scale overrides map without copying.
   * Internal use only — see {@link #rawGeomSpec()}.
   *
   * @return the stored scale overrides map
   */
  @PackageScope
  Map<String, Scale> rawScales() {
    scales
  }

  /**
   * Creates a copy of this layer.
   *
   * @return copied layer
   */
  Layer copy() {
    Map<String, Scale> copiedScales = SpecCopyUtil.copyScales(scales)
    Map<String, Object> copiedParams = SpecCopyUtil.deepCopyParams(params)
    new Layer(geomSpec.copy(), statSpec.copy(), mapping?.copy(), inheritMapping, positionSpec.copy(), copiedParams, styleCallback, copiedScales)
  }

}
