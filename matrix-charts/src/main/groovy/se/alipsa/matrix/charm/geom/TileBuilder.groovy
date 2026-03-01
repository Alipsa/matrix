package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for tile (heatmap) layers.
 *
 * <p>Produces a {@code TILE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'col'; y = 'row' }
 *   layers {
 *     geomTile().fill('#336699').alpha(0.8)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class TileBuilder extends LayerBuilder {

  /**
   * Sets tile fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  TileBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets tile outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  TileBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets tile outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  TileBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets tile width.
   *
   * @param value width value
   * @return this builder
   */
  TileBuilder width(Number value) {
    params['width'] = value
    this
  }

  /**
   * Sets tile height.
   *
   * @param value height value
   * @return this builder
   */
  TileBuilder height(Number value) {
    params['height'] = value
    this
  }

  /**
   * Sets tile opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  TileBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.TILE
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
