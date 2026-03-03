package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for point (scatter) layers.
 *
 * <p>Produces a {@code POINT / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'cty'; y = 'hwy' }
 *   layers {
 *     geomPoint().size(3).alpha(0.7).color('#336699')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class PointBuilder extends LayerBuilder {

  /**
   * Sets point size.
   *
   * @param value size value
   * @return this builder
   */
  PointBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets point opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  PointBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets point stroke colour.
   *
   * @param value colour value
   * @return this builder
   */
  PointBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets point stroke colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  PointBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets point fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  PointBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets point shape by string name.
   *
   * @param value shape name
   * @return this builder
   */
  PointBuilder shape(String value) {
    params['shape'] = value
    this
  }

  /**
   * Sets point shape by {@link se.alipsa.matrix.charm.ShapeName} enum.
   *
   * @param value shape enum constant
   * @return this builder
   */
  PointBuilder shape(se.alipsa.matrix.charm.ShapeName value) {
    params['shape'] = value
    this
  }

  /**
   * Sets point line type by string name.
   *
   * @param value linetype name
   * @return this builder
   */
  PointBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets point line type by {@link se.alipsa.matrix.charm.LinetypeName} enum.
   *
   * @param value linetype enum constant
   * @return this builder
   */
  PointBuilder linetype(se.alipsa.matrix.charm.LinetypeName value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.POINT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
