package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for abline (y = intercept + slope * x) layers.
 *
 * <p>Produces a {@code ABLINE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomAbline().intercept(0).slope(1).color('#cc0000')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class AblineBuilder extends LayerBuilder {

  /**
   * Sets the y-intercept of the line.
   *
   * @param value intercept value
   * @return this builder
   */
  AblineBuilder intercept(Number value) {
    params['intercept'] = value
    this
  }

  /**
   * Sets the slope of the line.
   *
   * @param value slope value
   * @return this builder
   */
  AblineBuilder slope(Number value) {
    params['slope'] = value
    this
  }

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  AblineBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  AblineBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  AblineBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  AblineBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.ABLINE
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
