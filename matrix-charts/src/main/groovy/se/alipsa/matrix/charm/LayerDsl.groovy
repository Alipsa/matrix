package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Layer DSL delegate for geom configuration blocks.
 *
 * <p>Common aesthetic properties ({@code size}, {@code alpha}, {@code color},
 * {@code fill}, {@code shape}, {@code linetype}) are declared as explicit typed
 * fields so that IDEs can autocomplete them. Geom-specific properties (e.g.
 * {@code method}, {@code se}, {@code bins}) still fall through to
 * {@link LayerParams#propertyMissing(String, Object) propertyMissing}.</p>
 */
@CompileStatic
class LayerDsl extends LayerParams {

  /** Point/line size. */
  Number size

  /** Opacity (0–1). */
  Number alpha

  /** Stroke/outline colour. */
  String color

  /** Fill colour. */
  String fill

  /** Shape (string name or integer code). */
  Object shape

  /** Line-type (string name or integer code). */
  Object linetype

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
   * Returns merged values: explicit typed fields plus free-form params.
   * Explicit fields take precedence over values stored via {@code propertyMissing}.
   *
   * @return merged parameter map
   */
  @Override
  Map<String, Object> values() {
    Map<String, Object> merged = super.values()
    if (size != null) merged['size'] = size
    if (alpha != null) merged['alpha'] = alpha
    if (color != null) merged['color'] = color
    if (fill != null) merged['fill'] = fill
    if (shape != null) merged['shape'] = shape
    if (linetype != null) merged['linetype'] = linetype
    merged
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
