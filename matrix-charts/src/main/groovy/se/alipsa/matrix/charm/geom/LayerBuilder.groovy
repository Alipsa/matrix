package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LayerDsl
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.Mapping
import se.alipsa.matrix.charm.MappingDsl
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.core.Matrix

import java.util.Locale

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
  protected CharmStatType statType
  protected final Map<String, Object> statParams = [:]
  protected final Map<String, Object> params = [:]
  protected Closure styleCallback
  protected final Map<String, Scale> layerScales = [:]

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
   * Sets the statistical transformation for this layer.
   *
   * @param stat stat value as enum, spec, or string name
   * @return this builder
   */
  LayerBuilder stat(Object stat) {
    statParams.clear()
    if (stat instanceof StatSpec) {
      StatSpec statSpec = stat as StatSpec
      this.statType = statSpec.type
      if (statSpec.params != null) {
        this.statParams.putAll(statSpec.params)
      }
      return this
    }
    this.statType = parseStatType(stat)
    this
  }

  /**
   * Sets an arbitrary parameter on this layer.
   *
   * @param key parameter name
   * @param value parameter value
   * @return this builder
   */
  LayerBuilder param(String key, Object value) {
    params[key] = value
    this
  }

  /**
   * Sets multiple arbitrary parameters on this layer.
   *
   * @param values parameter values
   * @return this builder
   */
  LayerBuilder params(Map<String, Object> values) {
    if (values != null) {
      params.putAll(values)
    }
    this
  }

  /**
   * Sets layer-specific data, overriding the chart-level data.
   *
   * @param value the data matrix for this layer
   * @return this builder
   */
  LayerBuilder data(Matrix value) {
    params['__layer_data'] = value
    this
  }

  /**
   * Sets a per-datum style override callback.
   *
   * <p>The closure receives a {@code Row} from the original data matrix and a mutable
   * {@link se.alipsa.matrix.charm.StyleOverride} object. Non-null properties set on the
   * override take highest priority, above layer params and mapped aesthetics.</p>
   *
   * @param callback style override closure
   * @return this builder
   */
  LayerBuilder style(Closure callback) {
    this.styleCallback = callback
    this
  }

  /**
   * Sets a tooltip template for this layer.
   *
   * <p>Use placeholders like {@code {x}}, {@code {y}}, or any column name
   * from the source row (for example {@code {category}}).</p>
   *
   * @param template tooltip template
   * @return this builder
   */
  LayerBuilder tooltip(String template) {
    params['tooltipTemplate'] = template
    params['tooltipEnabled'] = true
    this
  }

  /**
   * Enables or disables tooltip rendering for this layer.
   *
   * <p>When enabled and no mapped/template tooltip is available, a default
   * tooltip string is generated from row values.</p>
   *
   * @param enabled true to enable, false to disable
   * @return this builder
   */
  LayerBuilder tooltip(boolean enabled) {
    params['tooltipEnabled'] = enabled
    this
  }

  /**
   * Adds a per-layer scale override for a single aesthetic.
   *
   * @param aesthetic aesthetic name (e.g. 'color', 'fill', 'x', 'y', 'size', 'shape', 'alpha', 'linetype')
   * @param scale the scale to use for this aesthetic on this layer
   * @return this builder
   */
  LayerBuilder scale(String aesthetic, Scale scale) {
    if (aesthetic != null && scale != null) {
      String key = aesthetic.trim().toLowerCase(Locale.ROOT)
      if (key == 'colour') {
        key = 'color'
      }
      layerScales[key] = scale
    }
    this
  }

  /**
   * Configures per-layer scale overrides via closure DSL.
   *
   * <p>Example usage:
   * <pre>
   *   geom_point().scale {
   *     color Scale.manual(['A': 'red', 'B': 'blue'])
   *     x Scale.transform('log10')
   *   }
   * </pre>
   *
   * @param configure scale configuration closure
   * @return this builder
   */
  LayerBuilder scale(
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerScaleDsl) Closure<?> configure
  ) {
    LayerScaleDsl dsl = new LayerScaleDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    if (dsl.collected != null) {
      dsl.collected.each { k, v ->
        String key = k == null ? null : k.toString().trim()
        if (key && v != null) {
          layerScales[key] = v
        }
      }
    }
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
  protected abstract CharmStatType defaultStatType()

  /**
   * Builds the configured {@link LayerSpec}.
   *
   * @return immutable layer specification
   */
  LayerSpec build() {
    CharmStatType effectiveStat = statType ?: defaultStatType()
    new LayerSpec(
        GeomSpec.of(geomType()),
        StatSpec.of(effectiveStat, new LinkedHashMap<>(statParams)),
        layerMapping?.copy(),
        inheritMapping,
        positionSpec,
        new LinkedHashMap<>(params),
        styleCallback,
        new LinkedHashMap<>(layerScales)
    )
  }

  private static CharmStatType parseStatType(Object stat) {
    if (stat == null) {
      return null
    }
    if (stat instanceof CharmStatType) {
      return stat as CharmStatType
    }
    if (stat instanceof CharSequence) {
      String normalized = stat.toString().trim()
      if (normalized.isEmpty()) {
        return null
      }
      try {
        return CharmStatType.valueOf(normalized.toUpperCase(Locale.ROOT))
      } catch (IllegalArgumentException e) {
        throw new CharmValidationException("Unsupported stat '${stat}'", e)
      }
    }
    throw new CharmValidationException("Unsupported stat type '${stat.getClass().name}'")
  }

  /**
   * DSL for configuring per-layer scale overrides.
   *
   * <p>Each method sets a scale for an aesthetic channel. Accepts any
   * {@link Scale} instance (e.g. {@code Scale.manual(...)}, {@code Scale.transform('log10')}).</p>
   */
  @CompileStatic
  static class LayerScaleDsl {

    final Map<String, Scale> collected = [:]

    /** Sets the x scale override. */
    void x(Scale scale) { collected['x'] = scale }

    /** Sets the y scale override. */
    void y(Scale scale) { collected['y'] = scale }

    /** Sets the color scale override. */
    void color(Scale scale) { collected['color'] = scale }

    /** Sets the fill scale override. */
    void fill(Scale scale) { collected['fill'] = scale }

    /** Sets the size scale override. */
    void size(Scale scale) { collected['size'] = scale }

    /** Sets the shape scale override. */
    void shape(Scale scale) { collected['shape'] = scale }

    /** Sets the alpha scale override. */
    void alpha(Scale scale) { collected['alpha'] = scale }

    /** Sets the linetype scale override. */
    void linetype(Scale scale) { collected['linetype'] = scale }
  }
}
