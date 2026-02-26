package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LayerDsl
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.Mapping
import se.alipsa.matrix.charm.MappingDsl
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.StatSpec

/**
 * Abstract base class for fluent layer builders.
 *
 * <p>Subclasses specify the geometry type, stat type, and expose
 * geom-specific fluent setters. Common concerns (mapping, position,
 * inherit-mapping) are handled here.</p>
 */
@CompileStatic
abstract class LayerBuilder {

  protected Mapping layerMapping
  protected boolean inheritMapping = true
  protected PositionSpec positionSpec = PositionSpec.of(CharmPositionType.IDENTITY)
  protected final Map<String, Object> params = [:]

  /**
   * Configures layer-level aesthetic mappings via closure DSL.
   *
   * @param configure mapping closure
   * @return this builder
   */
  LayerBuilder mapping(
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MappingDsl) Closure<?> configure
  ) {
    MappingDsl dsl = new MappingDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    Mapping mapped = new Mapping()
    mapped.apply(dsl.toMapping())
    this.layerMapping = mapped
    this
  }

  /**
   * Configures layer-level aesthetic mappings via named arguments.
   *
   * @param mappingEntries mapping entries
   * @return this builder
   */
  LayerBuilder mapping(Map<String, ?> mappingEntries) {
    Mapping mapped = new Mapping()
    mapped.apply(mappingEntries)
    this.layerMapping = mapped
    this
  }

  /**
   * Sets whether this layer inherits the plot-level mapping.
   *
   * @param value true to inherit, false to use only layer mapping
   * @return this builder
   */
  LayerBuilder inheritMapping(boolean value) {
    this.inheritMapping = value
    this
  }

  /**
   * Sets the position adjustment for this layer.
   *
   * @param value position type, spec, or string name
   * @return this builder
   */
  LayerBuilder position(Object value) {
    this.positionSpec = LayerDsl.parsePosition(value)
    this
  }

  /**
   * Returns the geometry type for this builder.
   *
   * @return geom type
   */
  protected abstract CharmGeomType geomType()

  /**
   * Returns the default stat type for this builder's geometry.
   *
   * @return stat type
   */
  protected abstract CharmStatType statType()

  /**
   * Builds the configured {@link LayerSpec}.
   *
   * @return immutable layer specification
   */
  LayerSpec build() {
    new LayerSpec(
        GeomSpec.of(geomType()),
        StatSpec.of(statType()),
        layerMapping?.copy(),
        inheritMapping,
        positionSpec,
        new LinkedHashMap<>(params)
    )
  }
}
