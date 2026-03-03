package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for line range layers.
 *
 * <p>Produces a {@code LINERANGE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; ymin = 'lo'; ymax = 'hi' }
 *   layers {
 *     geomLinerange().color('#336699').size(1)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class LinerangeBuilder extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  LinerangeBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  LinerangeBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  LinerangeBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type by string name.
   *
   * @param value linetype name
   * @return this builder
   */
  LinerangeBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets line type by {@link se.alipsa.matrix.charm.LinetypeName} enum.
   *
   * @param value linetype enum constant
   * @return this builder
   */
  LinerangeBuilder linetype(se.alipsa.matrix.charm.LinetypeName value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.LINERANGE
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
