package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for jitter layers.
 *
 * <p>Produces a {@code JITTER / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'class'; y = 'hwy' }
 *   layers {
 *     geomJitter().width(0.2).height(0).alpha(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class JitterBuilder extends LayerBuilder {

  /**
   * Sets jitter width.
   *
   * @param value jitter width
   * @return this builder
   */
  JitterBuilder width(Number value) {
    params['width'] = value
    this
  }

  /**
   * Sets jitter height.
   *
   * @param value jitter height
   * @return this builder
   */
  JitterBuilder height(Number value) {
    params['height'] = value
    this
  }

  /**
   * Sets point colour.
   *
   * @param value colour value
   * @return this builder
   */
  JitterBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets point colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  JitterBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets point size.
   *
   * @param value size value
   * @return this builder
   */
  JitterBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets point shape.
   *
   * @param value shape name or integer code
   * @return this builder
   */
  JitterBuilder shape(Object value) {
    params['shape'] = value
    this
  }

  /**
   * Sets point opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  JitterBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.JITTER
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
