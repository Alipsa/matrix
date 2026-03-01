package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for log tick annotation layers.
 *
 * <p>Produces a {@code LOGTICKS / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomPoint().size(2)
 *     geomLogticks().sides('bl').size(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class LogticksBuilder extends LayerBuilder {

  /**
   * Sets which sides to draw log ticks on.
   *
   * @param value combination of 't', 'r', 'b', 'l'
   * @return this builder
   */
  LogticksBuilder sides(String value) {
    params['sides'] = value
    this
  }

  /**
   * Sets log tick line colour.
   *
   * @param value colour value
   * @return this builder
   */
  LogticksBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets log tick line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  LogticksBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets log tick line width.
   *
   * @param value size value
   * @return this builder
   */
  LogticksBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets log tick line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  LogticksBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.LOGTICKS
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
