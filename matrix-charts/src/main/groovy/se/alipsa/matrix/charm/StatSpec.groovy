package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Parameterized statistical transformation specification carrying the
 * stat type plus configuration parameters.
 */
@CompileStatic
class StatSpec {

  final CharmStatType type
  final Map<String, Object> params

  /**
   * Creates a new stat specification.
   *
   * @param type stat type
   * @param params free-form parameters (e.g., bins, method)
   */
  StatSpec(CharmStatType type, Map<String, Object> params = [:]) {
    this.type = type ?: CharmStatType.IDENTITY
    this.params = params == null ? [:] : [*:params]
  }

  /**
   * Factory method for creating a StatSpec from a type.
   *
   * @param type stat type
   * @return new StatSpec
   */
  static StatSpec of(CharmStatType type) {
    new StatSpec(type)
  }

  /**
   * Factory method for creating a StatSpec with params.
   *
   * @param type stat type
   * @param params stat parameters
   * @return new StatSpec
   */
  static StatSpec of(CharmStatType type, Map<String, Object> params) {
    new StatSpec(type, params)
  }

  /**
   * Creates a copy of this StatSpec.
   *
   * @return copied StatSpec
   */
  StatSpec copy() {
    new StatSpec(type, [*:params])
  }

  @Override
  String toString() {
    "StatSpec(${type})"
  }
}
