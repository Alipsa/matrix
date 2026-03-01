package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for magnification (mag) layers.
 *
 * <p>Produces a {@code MAG / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomMag().color('#336699').size(1)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class MagBuilder extends LayerBuilder {

  /**
   * Sets mag colour.
   *
   * @param value colour value
   * @return this builder
   */
  MagBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets mag colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  MagBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets mag line width.
   *
   * @param value size value
   * @return this builder
   */
  MagBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets mag opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  MagBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.MAG
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
