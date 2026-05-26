package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic

/**
 * Coordinate system specification.
 */
class Coord {

  private CharmCoordType type = CharmCoordType.CARTESIAN
  private Map<String, Object> params = [:]

  /**
   * Returns coordinate system type.
   *
   * @return coordinate system type
   */
  CharmCoordType getType() {
    type
  }

  /**
   * Sets coordinate system type.
   *
   * @param value coord type value
   */
  void setType(CharmCoordType value) {
    if (value == null) {
      type = CharmCoordType.CARTESIAN
      return
    }
    type = value
  }

  /**
   * Sets coordinate system type.
   *
   * @param value coord type name
   */
  void setType(String value) {
    if (value == null) {
      type = CharmCoordType.CARTESIAN
      return
    }
    switch (value.trim().toLowerCase(Locale.ROOT)) {
      case 'cartesian' -> type = CharmCoordType.CARTESIAN
      case 'polar' -> type = CharmCoordType.POLAR
      case 'flip' -> type = CharmCoordType.FLIP
      case 'fixed' -> type = CharmCoordType.FIXED
      case 'trans' -> type = CharmCoordType.TRANS
      case 'radial' -> type = CharmCoordType.RADIAL
      case 'map' -> type = CharmCoordType.MAP
      case 'quickmap' -> type = CharmCoordType.QUICKMAP
      case 'sf' -> type = CharmCoordType.SF
      default -> throw new CharmValidationException("Unsupported coord type '${value}'")
    }
  }

  /**
   * Resets coordinate system type to cartesian.
   *
   * @param value null coord type value
   */
  @SuppressWarnings('UnusedMethodParameter')
  void setType(Void value) {
    type = CharmCoordType.CARTESIAN
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
