package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for curve layers.
 *
 * <p>Produces a {@code CURVE / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; xend = 'xend'; yend = 'yend' }
 *   layers {
 *     geomCurve().curvature(0.3).color('#336699')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class CurveBuilder extends LayerBuilder {

  /**
   * Sets curve colour.
   *
   * @param value colour value
   * @return this builder
   */
  CurveBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets curve colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  CurveBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets curve line width.
   *
   * @param value size value
   * @return this builder
   */
  CurveBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets curve line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  CurveBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets the amount of curvature. Negative values produce left-hand curves,
   * positive values produce right-hand curves, and zero produces a straight line.
   *
   * @param value curvature amount
   * @return this builder
   */
  CurveBuilder curvature(Number value) {
    params['curvature'] = value
    this
  }

  /**
   * Sets the angle of the curve in degrees.
   *
   * @param value angle in degrees
   * @return this builder
   */
  CurveBuilder angle(Number value) {
    params['angle'] = value
    this
  }

  /**
   * Sets the number of control points for the curve.
   *
   * @param value number of control points
   * @return this builder
   */
  CurveBuilder ncp(Integer value) {
    params['ncp'] = value
    this
  }

  /**
   * Sets arrow specification for curve endpoints.
   *
   * @param value arrow specification
   * @return this builder
   */
  CurveBuilder arrow(Object value) {
    params['arrow'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.CURVE
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
