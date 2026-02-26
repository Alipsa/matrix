package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for smooth (regression/loess) layers.
 *
 * <p>Produces a {@code SMOOTH / SMOOTH} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomSmooth().method('lm').se(false)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class SmoothBuilder extends LayerBuilder {

  /**
   * Sets smoothing method ('lm', 'loess', 'gam').
   *
   * @param value method name
   * @return this builder
   */
  SmoothBuilder method(String value) {
    params['method'] = value
    this
  }

  /**
   * Sets whether to display a confidence interval.
   *
   * @param value true to show, false to hide
   * @return this builder
   */
  SmoothBuilder se(boolean value) {
    params['se'] = value
    this
  }

  /**
   * Sets the confidence level for the interval.
   *
   * @param value confidence level (e.g. 0.95)
   * @return this builder
   */
  SmoothBuilder level(Number value) {
    params['level'] = value
    this
  }

  /**
   * Sets the span parameter for loess smoothing.
   *
   * @param value span value (0–1)
   * @return this builder
   */
  SmoothBuilder span(Number value) {
    params['span'] = value
    this
  }

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  SmoothBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  SmoothBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets confidence interval fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  SmoothBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  SmoothBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SMOOTH
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.SMOOTH
  }
}
