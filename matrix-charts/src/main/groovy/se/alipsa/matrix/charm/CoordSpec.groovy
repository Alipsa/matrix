package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed coordinate specification for Charm core.
 */
@CompileStatic
class CoordSpec extends Coord {

  /**
   * Builder-style coordinate type setter.
   *
   * @param value coord type value
   * @return this spec
   */
  CoordSpec type(Object value) {
    setType(value)
    this
  }

  /**
   * Copies this coord spec.
   *
   * @return copied coord spec
   */
  @Override
  CoordSpec copy() {
    new CoordSpec(type: type, params: new LinkedHashMap<>(params))
  }
}
