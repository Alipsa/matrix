package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for map layers.
 *
 * <p>Produces a {@code MAP / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'long'; y = 'lat'; group = 'group' }
 *   layers {
 *     geomMap().fill('#336699').color('#000000')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class MapBuilder extends LayerBuilder {

  /**
   * Sets map region fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  MapBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets map region outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  MapBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets map region outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  MapBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets map region opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  MapBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets map region outline line width.
   *
   * @param value size value
   * @return this builder
   */
  MapBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets map region outline line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  MapBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.MAP
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
