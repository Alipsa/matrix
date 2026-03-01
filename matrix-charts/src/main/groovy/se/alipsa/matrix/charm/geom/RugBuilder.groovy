package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for rug layers.
 *
 * <p>Produces a {@code RUG / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'value' }
 *   layers {
 *     geomRug().color('#336699').sides('b')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class RugBuilder extends LayerBuilder {

  /**
   * Sets rug mark colour.
   *
   * @param value colour value
   * @return this builder
   */
  RugBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets rug mark colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  RugBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets rug mark line width.
   *
   * @param value size value
   * @return this builder
   */
  RugBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets rug mark line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  RugBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets which sides of the plot to draw rug marks on.
   *
   * @param value combination of 't', 'r', 'b', 'l' (top, right, bottom, left)
   * @return this builder
   */
  RugBuilder sides(String value) {
    params['sides'] = value
    this
  }

  /**
   * Sets whether rug marks are drawn outside the plot area.
   *
   * @param value true to draw outside
   * @return this builder
   */
  RugBuilder outside(boolean value) {
    params['outside'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.RUG
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
