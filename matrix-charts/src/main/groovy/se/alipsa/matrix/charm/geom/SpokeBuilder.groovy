package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for spoke layers.
 *
 * <p>Produces a {@code SPOKE / SPOKE} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomSpoke().color('#336699').size(1)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class SpokeBuilder extends LayerBuilder {

  /**
   * Sets spoke line colour.
   *
   * @param value colour value
   * @return this builder
   */
  SpokeBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets spoke line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  SpokeBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets spoke line width.
   *
   * @param value size value
   * @return this builder
   */
  SpokeBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets spoke opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  SpokeBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets spoke line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  SpokeBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SPOKE
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.SPOKE
  }
}
