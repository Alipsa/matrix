package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for error bar layers.
 *
 * <p>Produces a {@code ERRORBAR / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; ymin = 'lo'; ymax = 'hi' }
 *   layers {
 *     geomErrorbar().width(0.2).color('#333333')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ErrorbarBuilder extends LayerBuilder {

  /**
   * Sets error bar cap width.
   *
   * @param value width value
   * @return this builder
   */
  ErrorbarBuilder width(Number value) {
    params['width'] = value
    this
  }

  /**
   * Sets error bar colour.
   *
   * @param value colour value
   * @return this builder
   */
  ErrorbarBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets error bar colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ErrorbarBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets error bar line width.
   *
   * @param value size value
   * @return this builder
   */
  ErrorbarBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type by string name.
   *
   * @param value linetype name
   * @return this builder
   */
  ErrorbarBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets line type by {@link se.alipsa.matrix.charm.LinetypeName} enum.
   *
   * @param value linetype enum constant
   * @return this builder
   */
  ErrorbarBuilder linetype(se.alipsa.matrix.charm.LinetypeName value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.ERRORBAR
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
