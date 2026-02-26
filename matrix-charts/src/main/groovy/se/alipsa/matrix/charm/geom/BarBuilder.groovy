package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for bar (count) layers.
 *
 * <p>Produces a {@code BAR / COUNT} layer specification. The COUNT stat
 * automatically counts observations per x category.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'category' }
 *   layers {
 *     geomBar().fill('#336699').position('dodge')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class BarBuilder extends LayerBuilder {

  /**
   * Sets bar width.
   *
   * @param value width value
   * @return this builder
   */
  BarBuilder width(Number value) {
    params['width'] = value
    this
  }

  /**
   * Sets bar fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  BarBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets bar outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  BarBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets bar outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  BarBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets bar opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  BarBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.BAR
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.COUNT
  }
}
