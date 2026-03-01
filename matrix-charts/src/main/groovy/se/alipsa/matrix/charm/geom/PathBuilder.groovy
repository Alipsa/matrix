package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for path layers.
 *
 * <p>Produces a {@code PATH / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomPath().color('#336699').size(1)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class PathBuilder extends LayerBuilder {

  /**
   * Sets path colour.
   *
   * @param value colour value
   * @return this builder
   */
  PathBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets path colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  PathBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets path line width.
   *
   * @param value size value
   * @return this builder
   */
  PathBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets path line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  PathBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets path opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  PathBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets the line end style.
   *
   * @param value line end style (e.g. 'butt', 'round', 'square')
   * @return this builder
   */
  PathBuilder lineend(String value) {
    params['lineend'] = value
    this
  }

  /**
   * Sets the line join style.
   *
   * @param value line join style (e.g. 'round', 'mitre', 'bevel')
   * @return this builder
   */
  PathBuilder linejoin(String value) {
    params['linejoin'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.PATH
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
