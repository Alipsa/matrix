package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for quantile regression layers.
 *
 * <p>Produces a {@code QUANTILE / QUANTILE} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomQuantile().quantiles([0.25, 0.5, 0.75]).color('#336699')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class QuantileBuilder extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  QuantileBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  QuantileBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  QuantileBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  QuantileBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets the quantiles to compute.
   *
   * @param value list of quantile values (e.g. [0.25, 0.5, 0.75])
   * @return this builder
   */
  QuantileBuilder quantiles(List<Number> value) {
    params['quantiles'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.QUANTILE
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.QUANTILE
  }
}
