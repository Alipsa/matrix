package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for histogram layers.
 *
 * <p>Produces a {@code HISTOGRAM / BIN} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'value' }
 *   layers {
 *     geomHistogram().bins(20).fill('#336699')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class HistogramBuilder extends LayerBuilder {

  /**
   * Sets the number of bins.
   *
   * @param value bin count
   * @return this builder
   */
  HistogramBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  /**
   * Sets the bin width (alternative to bins).
   *
   * @param value bin width
   * @return this builder
   */
  HistogramBuilder binwidth(Number value) {
    params['binwidth'] = value
    this
  }

  /**
   * Sets histogram fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  HistogramBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets histogram outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  HistogramBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets histogram outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  HistogramBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets histogram opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  HistogramBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.HISTOGRAM
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.BIN
  }
}
