package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for boxplot layers.
 *
 * <p>Produces a {@code BOXPLOT / BOXPLOT} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'group'; y = 'value' }
 *   layers {
 *     geomBoxplot().fill('#eeeeee').notch(true)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class BoxplotBuilder extends LayerBuilder {

  /**
   * Sets box width.
   *
   * @param value width value
   * @return this builder
   */
  BoxplotBuilder width(Number value) {
    params['width'] = value
    params['boxWidth'] = value
    this
  }

  /**
   * Sets box fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  BoxplotBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets box outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  BoxplotBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets box outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  BoxplotBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets box opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  BoxplotBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets outlier shape.
   *
   * @param value shape name or integer code
   * @return this builder
   */
  BoxplotBuilder outlierShape(Object value) {
    params['outlierShape'] = value
    this
  }

  /**
   * Sets whether to draw notches.
   *
   * @param value true for notched boxplot
   * @return this builder
   */
  BoxplotBuilder notch(boolean value) {
    params['notch'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.BOXPLOT
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.BOXPLOT
  }
}
