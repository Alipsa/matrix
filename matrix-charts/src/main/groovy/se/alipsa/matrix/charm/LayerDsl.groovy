package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Layer DSL delegate for geom configuration blocks.
 */
@CompileStatic
class LayerDsl extends LayerParams {

  private Mapping layerMapping
  boolean inheritMapping = true
  private PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY)

  /**
   * Configures layer-level mappings.
   *
   * @param configure mapping closure
   */
  void mapping(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MappingDsl) Closure<?> configure) {
    MappingDsl dsl = new MappingDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    Mapping mapped = new Mapping()
    mapped.apply(dsl.toMapping())
    this.layerMapping = mapped
  }

  /**
   * Configures layer-level mappings using named arguments.
   *
   * @param mappingEntries mapping entries
   */
  void mapping(Map<String, ?> mappingEntries) {
    Mapping mapped = new Mapping()
    mapped.apply(mappingEntries)
    this.layerMapping = mapped
  }

  /**
   * Returns copied layer-level mappings, if provided.
   *
   * @return layer mapping or null
   */
  Mapping layerMapping() {
    layerMapping?.copy()
  }

  /**
   * Returns normalized layer position spec.
   *
   * @return layer position spec
   */
  PositionSpec getPositionSpec() {
    positionSpec
  }

  /**
   * Sets layer position from CharmPositionType, PositionSpec, or string value.
   *
   * @param value position value
   */
  void setPosition(Object value) {
    this.positionSpec = parsePosition(value)
  }

  /**
   * Builder-style position setter.
   *
   * @param value position value
   * @return this layer dsl
   */
  LayerDsl position(Object value) {
    setPosition(value)
    this
  }

  /**
   * Parses layer position values.
   *
   * @param value position value
   * @return normalized PositionSpec
   */
  static PositionSpec parsePosition(Object value) {
    if (value == null) {
      return PositionSpec.of(CharmPositionType.IDENTITY)
    }
    if (value instanceof PositionSpec) {
      return (value as PositionSpec).copy()
    }
    if (value instanceof CharmPositionType) {
      return PositionSpec.of(value as CharmPositionType)
    }
    if (value instanceof CharSequence) {
      try {
        return PositionSpec.of(CharmPositionType.valueOf(value.toString().trim().toUpperCase(Locale.ROOT)))
      } catch (IllegalArgumentException e) {
        throw new CharmValidationException("Unsupported layer position '${value}'")
      }
    }
    throw new CharmValidationException("Unsupported layer position type '${value.getClass().name}'")
  }

  /**
   * Forwards unknown properties to free-form layer params.
   *
   * @param name parameter name
   * @param value parameter value
   */
  @CompileDynamic
  @Override
  void propertyMissing(String name, Object value) {
    super.propertyMissing(name, value)
  }

  /**
   * Reads unknown properties from free-form layer params.
   *
   * @param name parameter name
   * @return parameter value
   */
  @CompileDynamic
  @Override
  Object propertyMissing(String name) {
    super.propertyMissing(name)
  }
}
