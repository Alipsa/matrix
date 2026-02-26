package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for vertical line layers.
 *
 * <p>Produces a {@code VLINE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomVline().xintercept(3).color('#cc0000').linetype('dashed')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class VlineBuilder extends LayerBuilder {

  /**
   * Sets the x-value where the vertical line is drawn.
   *
   * @param value x-intercept value
   * @return this builder
   */
  VlineBuilder xintercept(Number value) {
    params['xintercept'] = value
    this
  }

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  VlineBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  VlineBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  VlineBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  VlineBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.VLINE
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
