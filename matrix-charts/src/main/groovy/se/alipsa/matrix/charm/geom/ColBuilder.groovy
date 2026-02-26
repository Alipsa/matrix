package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for column (identity bar) layers.
 *
 * <p>Produces a {@code COL / IDENTITY} layer specification. Unlike
 * {@link BarBuilder}, the y values are taken directly from the data
 * rather than counted.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'category'; y = 'value' }
 *   layers {
 *     geomCol().fill('#cc6677')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ColBuilder extends LayerBuilder {

  /**
   * Sets bar width.
   *
   * @param value width value
   * @return this builder
   */
  ColBuilder width(Number value) {
    params['width'] = value
    this
  }

  /**
   * Sets bar fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  ColBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets bar outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  ColBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets bar outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ColBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets bar opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  ColBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.COL
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
