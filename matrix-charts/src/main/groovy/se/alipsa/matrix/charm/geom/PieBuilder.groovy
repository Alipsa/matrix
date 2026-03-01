package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for pie layers.
 *
 * <p>Produces a {@code PIE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'category'; y = 'value' }
 *   layers {
 *     geomPie().fill('#336699').alpha(0.9)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class PieBuilder extends LayerBuilder {

  /**
   * Sets pie fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  PieBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets pie outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  PieBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets pie outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  PieBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets pie opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  PieBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.PIE
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
