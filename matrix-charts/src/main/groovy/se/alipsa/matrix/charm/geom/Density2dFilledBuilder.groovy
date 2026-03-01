package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for filled 2D density layers.
 *
 * <p>Produces a {@code DENSITY_2D_FILLED / DENSITY_2D} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomDensity2dFilled().alpha(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class Density2dFilledBuilder extends LayerBuilder {

  /**
   * Sets fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  Density2dFilledBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  Density2dFilledBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  Density2dFilledBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  Density2dFilledBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets the number of contour bins.
   *
   * @param value number of bins
   * @return this builder
   */
  Density2dFilledBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.DENSITY_2D_FILLED
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.DENSITY_2D
  }
}
