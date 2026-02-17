package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Parameterized position-adjustment specification carrying the
 * position type plus configuration parameters.
 */
@CompileStatic
class PositionSpec {

  final CharmPositionType type
  final Map<String, Object> params

  /**
   * Creates a new position specification.
   *
   * @param type position type
   * @param params free-form parameters (e.g., width, padding)
   */
  PositionSpec(CharmPositionType type, Map<String, Object> params = [:]) {
    this.type = type ?: CharmPositionType.IDENTITY
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
  }

  /**
   * Factory method for creating a PositionSpec from a type.
   *
   * @param type position type
   * @return new PositionSpec
   */
  static PositionSpec of(CharmPositionType type) {
    new PositionSpec(type)
  }

  /**
   * Factory method for creating a PositionSpec with params.
   *
   * @param type position type
   * @param params position parameters
   * @return new PositionSpec
   */
  static PositionSpec of(CharmPositionType type, Map<String, Object> params) {
    new PositionSpec(type, params)
  }

  /**
   * Creates a copy of this PositionSpec.
   *
   * @return copied PositionSpec
   */
  PositionSpec copy() {
    new PositionSpec(type, new LinkedHashMap<>(params))
  }

  /**
   * Returns the width parameter, if set.
   *
   * @return width value or null
   */
  BigDecimal getWidth() {
    params.width as BigDecimal
  }

  /**
   * Returns the padding parameter, if set.
   *
   * @return padding value or null
   */
  BigDecimal getPadding() {
    params.padding as BigDecimal
  }

  /**
   * Returns the reverse parameter, if set.
   *
   * @return reverse flag or null
   */
  Boolean getReverse() {
    params.reverse as Boolean
  }

  /**
   * Returns the seed parameter, if set.
   *
   * @return seed value or null
   */
  Long getSeed() {
    params.seed as Long
  }

  @Override
  String toString() {
    "PositionSpec(${type})"
  }
}
