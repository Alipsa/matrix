package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for horizontal error bar layers.
 *
 * <p>Produces a {@code ERRORBARH / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { y = 'y'; xmin = 'lo'; xmax = 'hi' }
 *   layers {
 *     geomErrorbarh().height(0.2).color('#333333')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ErrorbarhBuilder extends LayerBuilder {

  /**
   * Sets error bar cap height.
   *
   * @param value height value
   * @return this builder
   */
  ErrorbarhBuilder height(Number value) {
    params['height'] = value
    params['width'] = value
    this
  }

  /**
   * Sets error bar colour.
   *
   * @param value colour value
   * @return this builder
   */
  ErrorbarhBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets error bar colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ErrorbarhBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets error bar line width.
   *
   * @param value size value
   * @return this builder
   */
  ErrorbarhBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets error bar line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  ErrorbarhBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.ERRORBARH
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
