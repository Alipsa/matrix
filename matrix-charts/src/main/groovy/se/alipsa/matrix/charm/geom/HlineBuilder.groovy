package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for horizontal line layers.
 *
 * <p>Produces a {@code HLINE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomHline().yintercept(5).color('#cc0000').linetype('dashed')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class HlineBuilder extends LayerBuilder {

  /**
   * Sets the y-value where the horizontal line is drawn.
   *
   * @param value y-intercept value
   * @return this builder
   */
  HlineBuilder yintercept(Number value) {
    params['yintercept'] = value
    this
  }

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  HlineBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  HlineBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  HlineBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  HlineBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.HLINE
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
