package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for custom user-defined layers.
 *
 * <p>Produces a {@code CUSTOM / IDENTITY} layer specification. The rendering
 * closure is stored in params and interpreted by the renderer.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomCustom().renderer { svg, layerData -> ... }
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class CustomBuilder extends LayerBuilder {

  /**
   * Sets a custom rendering closure.
   *
   * @param value rendering closure
   * @return this builder
   */
  CustomBuilder renderer(Closure<?> value) {
    params['renderer'] = value
    this
  }

  /**
   * Sets custom layer colour.
   *
   * @param value colour value
   * @return this builder
   */
  CustomBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets custom layer colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  CustomBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets custom layer fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  CustomBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets custom layer size.
   *
   * @param value size value
   * @return this builder
   */
  CustomBuilder size(Number value) {
    params['size'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.CUSTOM
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
