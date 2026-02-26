package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for QQ plot layers.
 *
 * <p>Produces a {@code QQ / QQ} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'sample' }
 *   layers {
 *     geomQq().color('#336699').size(2)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class QqBuilder extends LayerBuilder {

  /**
   * Sets point colour.
   *
   * @param value colour value
   * @return this builder
   */
  QqBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets point colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  QqBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets point size.
   *
   * @param value size value
   * @return this builder
   */
  QqBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets point shape.
   *
   * @param value shape name or integer code
   * @return this builder
   */
  QqBuilder shape(Object value) {
    params['shape'] = value
    this
  }

  /**
   * Sets point opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  QqBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.QQ
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.QQ
  }
}
