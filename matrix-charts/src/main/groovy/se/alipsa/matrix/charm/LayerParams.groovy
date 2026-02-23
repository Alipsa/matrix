package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Collects free-form layer params via DSL property assignment.
 */
@CompileStatic
class LayerParams {

  private final Map<String, Object> values = [:]

  /**
   * Returns collected values.
   *
   * @return collected values
   */
  Map<String, Object> values() {
    [*:values]
  }

  /**
   * Captures dynamic property assignment.
   *
   * @param name parameter name
   * @param value parameter value
   */
  @CompileDynamic
  void propertyMissing(String name, Object value) {
    values[name] = value
  }

  /**
   * Reads dynamic property values.
   *
   * @param name parameter name
   * @return parameter value
   */
  @CompileDynamic
  Object propertyMissing(String name) {
    values[name]
  }
}
