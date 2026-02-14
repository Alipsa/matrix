package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Generic map delegate used by nested theme DSL blocks.
 */
@CompileStatic
class MapDsl {

  private final Map<String, Object> values = [:]

  /**
   * Returns collected map values.
   *
   * @return collected values
   */
  Map<String, Object> values() {
    new LinkedHashMap<>(values)
  }

  /**
   * Captures dynamic property assignment.
   *
   * @param name property name
   * @param value property value
   */
  @CompileDynamic
  void propertyMissing(String name, Object value) {
    values[name] = value
  }
}
