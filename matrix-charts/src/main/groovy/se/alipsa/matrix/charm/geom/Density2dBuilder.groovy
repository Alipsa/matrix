package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for 2D density contour layers.
 *
 * <p>Produces a {@code DENSITY_2D / DENSITY_2D} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomDensity2d().color('#336699').bins(10)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class Density2dBuilder extends LayerBuilder {

  /**
   * Sets contour line colour.
   *
   * @param value colour value
   * @return this builder
   */
  Density2dBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets contour line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  Density2dBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets contour line width.
   *
   * @param value size value
   * @return this builder
   */
  Density2dBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets contour line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  Density2dBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets contour opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  Density2dBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets the number of contour bins.
   *
   * @param value number of bins
   * @return this builder
   */
  Density2dBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.DENSITY_2D
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.DENSITY_2D
  }
}
