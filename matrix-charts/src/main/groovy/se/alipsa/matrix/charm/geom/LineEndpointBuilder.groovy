package se.alipsa.matrix.charm.geom

import se.alipsa.matrix.charm.ArrowSpec
import se.alipsa.matrix.charm.LinetypeName

/**
 * Shared fluent API for line-like geoms with start and end points.
 *
 * @param <T> concrete builder type
 */
abstract class LineEndpointBuilder<T extends LineEndpointBuilder<T>> extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  T color(String value) {
    params['color'] = value
    self()
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  T colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  T size(Number value) {
    params['size'] = value
    self()
  }

  /**
   * Sets line type.
   *
   * @param value linetype name
   * @return this builder
   */
  T linetype(LinetypeName value) {
    params['linetype'] = value
    self()
  }

  /**
   * Sets arrow specification for line endpoints.
   *
   * @param value arrow specification
   * @return this builder
   */
  T arrow(ArrowSpec value) {
    params['arrow'] = value
    self()
  }

  protected T self() {
    this as T
  }

}
