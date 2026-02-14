package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Coordinate system specification.
 */
@CompileStatic
class Coord {

  private CoordType type = CoordType.CARTESIAN
  private Map<String, Object> params = [:]

  /**
   * Returns coordinate system type.
   *
   * @return coordinate system type
   */
  CoordType getType() {
    type
  }

  /**
   * Sets coordinate system type.
   *
   * @param value CoordType or string value
   */
  void setType(Object value) {
    if (value == null) {
      type = CoordType.CARTESIAN
      return
    }
    if (value instanceof CoordType) {
      type = value as CoordType
      return
    }
    if (value instanceof CharSequence) {
      switch (value.toString().trim().toLowerCase(Locale.ROOT)) {
        case 'cartesian' -> type = CoordType.CARTESIAN
        case 'polar' -> type = CoordType.POLAR
        default -> throw new CharmValidationException("Unsupported coord type '${value}'")
      }
      return
    }
    throw new CharmValidationException("Unsupported coord type input '${value.getClass().name}'")
  }

  /**
   * Returns coordinate parameters.
   *
   * @return parameter map
   */
  Map<String, Object> getParams() {
    params
  }

  /**
   * Sets coordinate parameters.
   *
   * @param params parameters
   */
  void setParams(Map<String, Object> params) {
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
  }

  /**
   * Handles arbitrary property assignments in `coord {}` blocks.
   *
   * @param name property name
   * @param value property value
   */
  @CompileDynamic
  void propertyMissing(String name, Object value) {
    params[name] = value
  }

  /**
   * Looks up arbitrary properties from coord params.
   *
   * @param name property name
   * @return property value
   */
  @CompileDynamic
  Object propertyMissing(String name) {
    params[name]
  }

  /**
   * Copies this coord spec.
   *
   * @return copied coord
   */
  Coord copy() {
    new Coord(type: type, params: new LinkedHashMap<>(params))
  }
}
