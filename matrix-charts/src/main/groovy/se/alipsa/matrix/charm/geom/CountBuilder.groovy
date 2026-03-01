package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for count layers.
 *
 * <p>Produces a {@code COUNT / COUNT} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'cut'; y = 'color' }
 *   layers {
 *     geomCount().color('#336699').size(3)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class CountBuilder extends LayerBuilder {

  /**
   * Sets point outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  CountBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets point outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  CountBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets point fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  CountBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets point size.
   *
   * @param value size value
   * @return this builder
   */
  CountBuilder size(Number value) {
    params['size'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.COUNT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.COUNT
  }
}
