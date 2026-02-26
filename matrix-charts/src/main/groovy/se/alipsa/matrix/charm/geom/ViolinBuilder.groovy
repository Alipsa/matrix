package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for violin layers.
 *
 * <p>Produces a {@code VIOLIN / YDENSITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'group'; y = 'value' }
 *   layers {
 *     geomViolin().fill('#cc6677').alpha(0.7)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ViolinBuilder extends LayerBuilder {

  /**
   * Sets violin fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  ViolinBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets violin outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  ViolinBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets violin outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ViolinBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets violin opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  ViolinBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets violin outline line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  ViolinBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets quantile lines to draw inside the violin.
   *
   * @param value list of quantile values (e.g. [0.25, 0.5, 0.75])
   * @return this builder
   */
  ViolinBuilder drawQuantiles(List<Number> value) {
    params['drawQuantiles'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.VIOLIN
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.YDENSITY
  }
}
