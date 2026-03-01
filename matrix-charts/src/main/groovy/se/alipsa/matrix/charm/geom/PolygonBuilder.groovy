package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for polygon layers.
 *
 * <p>Produces a {@code POLYGON / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'long'; y = 'lat'; group = 'region' }
 *   layers {
 *     geomPolygon().fill('#336699').color('#000000')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class PolygonBuilder extends LayerBuilder {

  /**
   * Sets polygon fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  PolygonBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets polygon outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  PolygonBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets polygon outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  PolygonBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets polygon opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  PolygonBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets polygon outline line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  PolygonBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets polygon outline line width.
   *
   * @param value size value
   * @return this builder
   */
  PolygonBuilder size(Number value) {
    params['size'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.POLYGON
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
