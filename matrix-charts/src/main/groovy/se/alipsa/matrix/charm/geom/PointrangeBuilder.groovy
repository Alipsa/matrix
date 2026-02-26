package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for point range layers.
 *
 * <p>Produces a {@code POINTRANGE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'mid'; ymin = 'lo'; ymax = 'hi' }
 *   layers {
 *     geomPointrange().color('#336699').size(1)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class PointrangeBuilder extends LayerBuilder {

  /**
   * Sets point and line colour.
   *
   * @param value colour value
   * @return this builder
   */
  PointrangeBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets point and line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  PointrangeBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width and point size.
   *
   * @param value size value
   * @return this builder
   */
  PointrangeBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets point shape.
   *
   * @param value shape name or integer code
   * @return this builder
   */
  PointrangeBuilder shape(Object value) {
    params['shape'] = value
    this
  }

  /**
   * Sets point fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  PointrangeBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets the multiplicative factor for the point size.
   *
   * @param value fatten factor
   * @return this builder
   */
  PointrangeBuilder fatten(Number value) {
    params['fatten'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.POINTRANGE
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
