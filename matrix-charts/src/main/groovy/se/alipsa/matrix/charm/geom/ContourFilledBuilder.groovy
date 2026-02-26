package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for filled contour layers.
 *
 * <p>Produces a {@code CONTOUR_FILLED / CONTOUR} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomContourFilled().alpha(0.5).bins(10)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ContourFilledBuilder extends LayerBuilder {

  /**
   * Sets fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  ContourFilledBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  ContourFilledBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ContourFilledBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  ContourFilledBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets the number of contour bins.
   *
   * @param value number of bins
   * @return this builder
   */
  ContourFilledBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  /**
   * Sets contour bin width.
   *
   * @param value bin width
   * @return this builder
   */
  ContourFilledBuilder binwidth(Number value) {
    params['binwidth'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.CONTOUR_FILLED
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.CONTOUR
  }
}
