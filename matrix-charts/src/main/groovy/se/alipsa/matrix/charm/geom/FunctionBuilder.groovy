package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for function layers.
 *
 * <p>Produces a {@code FUNCTION / FUNCTION} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x' }
 *   layers {
 *     geomFunction().color('#336699').n(200)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class FunctionBuilder extends LayerBuilder {

  /**
   * Sets function line colour.
   *
   * @param value colour value
   * @return this builder
   */
  FunctionBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets function line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  FunctionBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets function line width.
   *
   * @param value size value
   * @return this builder
   */
  FunctionBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets function line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  FunctionBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets the number of evaluation points.
   *
   * @param value number of points
   * @return this builder
   */
  FunctionBuilder n(Integer value) {
    params['n'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.FUNCTION
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.FUNCTION
  }
}
