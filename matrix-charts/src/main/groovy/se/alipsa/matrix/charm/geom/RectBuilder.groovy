package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for rect layers.
 *
 * <p>Produces a {@code RECT / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { xmin = 'x1'; xmax = 'x2'; ymin = 'y1'; ymax = 'y2' }
 *   layers {
 *     geomRect().fill('#336699').alpha(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class RectBuilder extends LayerBuilder {

  /**
   * Sets rectangle minimum x value.
   *
   * @param value xmin value
   * @return this builder
   */
  RectBuilder xmin(Number value) {
    params['xmin'] = value
    this
  }

  /**
   * Sets rectangle maximum x value.
   *
   * @param value xmax value
   * @return this builder
   */
  RectBuilder xmax(Number value) {
    params['xmax'] = value
    this
  }

  /**
   * Sets rectangle minimum y value.
   *
   * @param value ymin value
   * @return this builder
   */
  RectBuilder ymin(Number value) {
    params['ymin'] = value
    this
  }

  /**
   * Sets rectangle maximum y value.
   *
   * @param value ymax value
   * @return this builder
   */
  RectBuilder ymax(Number value) {
    params['ymax'] = value
    this
  }

  /**
   * Sets rectangle fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  RectBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets rectangle outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  RectBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets rectangle outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  RectBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets rectangle opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  RectBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.RECT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
