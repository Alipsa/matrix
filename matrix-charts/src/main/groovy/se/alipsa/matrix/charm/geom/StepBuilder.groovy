package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for step layers.
 *
 * <p>Produces a {@code STEP / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomStep().color('#336699').direction('hv')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class StepBuilder extends LayerBuilder {

  /**
   * Sets step line colour.
   *
   * @param value colour value
   * @return this builder
   */
  StepBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets step line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  StepBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets step line width.
   *
   * @param value size value
   * @return this builder
   */
  StepBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets step line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  StepBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets step direction.
   *
   * @param value direction (e.g. 'hv' or 'vh')
   * @return this builder
   */
  StepBuilder direction(String value) {
    params['direction'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.STEP
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
