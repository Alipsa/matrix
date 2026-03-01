package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for hex bin layers.
 *
 * <p>Produces a {@code HEX / BIN_HEX} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomHex().fill('#336699').bins(20)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class HexBuilder extends LayerBuilder {

  /**
   * Sets hex fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  HexBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets hex outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  HexBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets hex outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  HexBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets hex opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  HexBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets the number of bins.
   *
   * @param value bin count
   * @return this builder
   */
  HexBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.HEX
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.BIN_HEX
  }
}
