package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Layer DSL delegate for geom configuration blocks.
 */
@CompileStatic
class LayerDsl extends LayerParams {

  private Aes layerAes
  boolean inheritAes = true
  private PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY)

  /**
   * Configures layer-level aesthetics.
   *
   * @param configure aes closure
   */
  void aes(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AesDsl) Closure<?> configure) {
    AesDsl dsl = new AesDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    Aes mapped = new Aes()
    mapped.apply(dsl.toMapping())
    this.layerAes = mapped
  }

  /**
   * Configures layer-level aesthetics using named arguments.
   *
   * @param mapping aes mapping
   */
  void aes(Map<String, ?> mapping) {
    Aes mapped = new Aes()
    mapped.apply(mapping)
    this.layerAes = mapped
  }

  /**
   * Returns copied layer-level mappings, if provided.
   *
   * @return layer aes or null
   */
  Aes layerAes() {
    layerAes?.copy()
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
