package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Canonical layer specification in Charm.
 */
@CompileStatic
class Layer {

  private final Geom geom
  private final Stat stat
  private final Aes aes
  private final boolean inheritAes
  private final Position position
  private final Map<String, Object> params

  /**
   * Creates a new layer.
   *
   * @param geom geometry type
   * @param stat statistical transformation
   * @param aes layer-specific mappings
   * @param inheritAes true when plot-level mappings are inherited
   * @param position position adjustment
   * @param params free-form layer parameters
   */
  Layer(
      Geom geom,
      Stat stat = Stat.IDENTITY,
      Aes aes = null,
      boolean inheritAes = true,
      Position position = Position.IDENTITY,
      Map<String, Object> params = [:]
  ) {
    this.geom = geom ?: Geom.POINT
    this.stat = stat ?: Stat.IDENTITY
    this.aes = aes
    this.inheritAes = inheritAes
    this.position = position ?: Position.IDENTITY
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
  }

  /**
   * Returns the geometry type.
   *
   * @return geometry type
   */
  Geom getGeom() {
    geom
  }

  /**
   * Returns the statistical transformation type.
   *
   * @return stat type
   */
  Stat getStat() {
    stat
  }

  /**
   * Returns layer-specific aesthetics.
   *
   * @return layer aes or null
   */
  Aes getAes() {
    aes?.copy()
  }

  /**
   * Returns whether layer inherits plot-level aesthetics.
   *
   * @return true if inheriting aesthetics
   */
  boolean isInheritAes() {
    inheritAes
  }

  /**
   * Returns position-adjustment mode.
   *
   * @return position mode
   */
  Position getPosition() {
    position
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
    new Layer(geom, stat, aes?.copy(), inheritAes, position, params)
  }
}
