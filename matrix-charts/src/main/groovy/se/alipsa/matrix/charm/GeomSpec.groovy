package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Parameterized geometry specification carrying the geom type plus
 * metadata needed for rendering dispatch.
 */
@CompileStatic
class GeomSpec {

  final CharmGeomType type
  final Map<String, Object> params
  final List<String> requiredAes
  final Map<String, Object> defaultAes
  final CharmStatType defaultStat
  final CharmPositionType defaultPosition

  /**
   * Creates a new geometry specification.
   *
   * @param type geometry type
   * @param params free-form parameters
   * @param requiredAes list of required aesthetic names
   * @param defaultAes default aesthetic values
   * @param defaultStat default stat type
   * @param defaultPosition default position type
   */
  GeomSpec(
      CharmGeomType type,
      Map<String, Object> params = [:],
      List<String> requiredAes = [],
      Map<String, Object> defaultAes = [:],
      CharmStatType defaultStat = CharmStatType.IDENTITY,
      CharmPositionType defaultPosition = CharmPositionType.IDENTITY
  ) {
    this.type = type ?: CharmGeomType.POINT
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
    this.requiredAes = requiredAes == null ? [] : new ArrayList<>(requiredAes)
    this.defaultAes = defaultAes == null ? [:] : new LinkedHashMap<>(defaultAes)
    this.defaultStat = defaultStat ?: CharmStatType.IDENTITY
    this.defaultPosition = defaultPosition ?: CharmPositionType.IDENTITY
  }

  /**
   * Factory method for creating a GeomSpec from a type.
   *
   * @param type geometry type
   * @return new GeomSpec
   */
  static GeomSpec of(CharmGeomType type) {
    new GeomSpec(type)
  }

  /**
   * Factory method for creating a GeomSpec with params.
   *
   * @param type geometry type
   * @param params free-form parameters
   * @return new GeomSpec
   */
  static GeomSpec of(CharmGeomType type, Map<String, Object> params) {
    new GeomSpec(type, params)
  }

  /**
   * Creates a copy of this GeomSpec.
   *
   * @return copied GeomSpec
   */
  GeomSpec copy() {
    new GeomSpec(
        type,
        new LinkedHashMap<>(params),
        new ArrayList<>(requiredAes),
        new LinkedHashMap<>(defaultAes),
        defaultStat,
        defaultPosition
    )
  }

  @Override
  String toString() {
    "GeomSpec(${type})"
  }
}
