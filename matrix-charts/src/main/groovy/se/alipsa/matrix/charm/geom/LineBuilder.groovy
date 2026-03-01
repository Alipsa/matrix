package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for line layers.
 *
 * <p>Produces a {@code LINE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomLine().color('#336699').linetype('dashed')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class LineBuilder extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  LineBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  LineBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  LineBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  LineBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  LineBuilder size(Number value) {
    params['size'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.LINE
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
