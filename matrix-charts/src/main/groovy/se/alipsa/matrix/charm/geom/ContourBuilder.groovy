package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for contour layers.
 *
 * <p>Produces a {@code CONTOUR / CONTOUR} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomContour().color('#336699').bins(10)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ContourBuilder extends LayerBuilder {

  /**
   * Sets contour line colour.
   *
   * @param value colour value
   * @return this builder
   */
  ContourBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets contour line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ContourBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets contour line width.
   *
   * @param value size value
   * @return this builder
   */
  ContourBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets contour line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  ContourBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets the number of contour bins.
   *
   * @param value number of bins
   * @return this builder
   */
  ContourBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  /**
   * Sets the contour bin width.
   *
   * @param value bin width
   * @return this builder
   */
  ContourBuilder binwidth(Number value) {
    params['binwidth'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.CONTOUR
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.CONTOUR
  }
}
