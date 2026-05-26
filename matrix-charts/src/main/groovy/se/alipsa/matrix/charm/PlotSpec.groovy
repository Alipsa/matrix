package se.alipsa.matrix.charm

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import org.apache.commons.text.similarity.LevenshteinDistance

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.facet.Labeller
import se.alipsa.matrix.charm.theme.CharmThemes
import se.alipsa.matrix.core.Matrix

/**
 * Mutable root plot specification that compiles into an immutable {@link Chart}.
 */
@SuppressWarnings('ClassSize')
@SuppressWarnings('DuplicateStringLiteral')
@SuppressWarnings('UnnecessaryCast')
@SuppressWarnings('UnnecessaryCollectCall')
class PlotSpec {

  private static final LevenshteinDistance COLUMN_DISTANCE = LevenshteinDistance.getDefaultInstance()

  private static final Map<CharmGeomType, List<String>> REQUIRED_MAPPINGS = [
      (CharmGeomType.POINT)    : ['x', 'y'],
      (CharmGeomType.LINE)     : ['x', 'y'],
      (CharmGeomType.SMOOTH)   : ['x', 'y'],
      (CharmGeomType.DENSITY)  : ['x'],
      (CharmGeomType.VIOLIN)   : ['x', 'y'],
      (CharmGeomType.TILE)     : ['x', 'y'],
      (CharmGeomType.TEXT)     : ['x', 'y'],
      (CharmGeomType.BAR)      : ['x'],
      (CharmGeomType.HISTOGRAM): ['x'],
      (CharmGeomType.BOXPLOT)  : ['y'],
      (CharmGeomType.AREA)     : ['x', 'y'],
      (CharmGeomType.PIE)      : ['x', 'y'],
      (CharmGeomType.BIN2D)    : ['x', 'y'],
      (CharmGeomType.CONTOUR)  : ['x', 'y'],
      (CharmGeomType.CONTOUR_FILLED): ['x', 'y'],
      (CharmGeomType.COUNT)    : ['x', 'y'],
      (CharmGeomType.CURVE)    : ['x', 'y', 'xend', 'yend'],
      (CharmGeomType.DENSITY_2D): ['x', 'y'],
      (CharmGeomType.DENSITY_2D_FILLED): ['x', 'y'],
      (CharmGeomType.DOTPLOT)  : ['x'],
      (CharmGeomType.FUNCTION) : ['x'],
      (CharmGeomType.LOGTICKS) : [] as List<String>,
      (CharmGeomType.MAG)      : ['x', 'y'],
      (CharmGeomType.MAP)      : ['x', 'y'],
      (CharmGeomType.PARALLEL) : ['x', 'y'],
      (CharmGeomType.QQ)       : ['x'],
      (CharmGeomType.QQ_LINE)  : ['x'],
      (CharmGeomType.QUANTILE) : ['x', 'y'],
      (CharmGeomType.RASTER)   : ['x', 'y'],
      (CharmGeomType.RASTER_ANN): ['x', 'y'],
      (CharmGeomType.SPOKE)    : ['x', 'y'],
      (CharmGeomType.SF)       : [] as List<String>,
      (CharmGeomType.SF_LABEL) : [] as List<String>,
      (CharmGeomType.SF_TEXT)  : [] as List<String>
  ]

  private final Matrix data
  private final MappingSpec mapping = new MappingSpec()
  private final List<LayerSpec> layers = []
  private final ScaleSpec scale = new ScaleSpec()
  private final ThemeSpec theme = new ThemeSpec()
  private final FacetSpec facet = new FacetSpec()
  private final CoordSpec coord = new CoordSpec()
  private final LabelsSpec labels = new LabelsSpec()
  private final GuidesSpec guides = new GuidesSpec()
  private AnimationSpec animation
  private CssAttributesSpec cssAttributes
  private final List<AnnotationSpec> annotations = []

  /**
   * Creates a new plot specification for the given data.
   *
   * @param data source matrix
   */
  PlotSpec(Matrix data) {
    if (data == null) {
      throw new IllegalArgumentException('data cannot be null')
    }
    this.data = data
  }

  /**
   * Returns source data.
   *
   * @return source matrix
   */
  Matrix getData() {
    data
  }

  /**
   * Returns plot-level mappings.
   *
   * @return plot-level mappings
   */
  MappingSpec getMapping() {
    mapping
  }

  /**
   * Returns the mutable layer list.
   *
   * @return layer list
   */
  List<LayerSpec> getLayers() {
    layers
  }

  /**
   * Returns the scale specification.
   *
   * @return scale specification
   */
  ScaleSpec getScale() {
    scale
  }

  /**
   * Returns the theme specification.
   *
   * @return theme specification
   */
  ThemeSpec getTheme() {
    theme
  }

  /**
   * Returns the facet specification.
   *
   * @return facet specification
   */
  FacetSpec getFacet() {
    facet
  }

  /**
   * Returns the coord specification.
   *
   * @return coord specification
   */
  CoordSpec getCoord() {
    coord
  }

  /**
   * Returns labels specification.
   *
   * @return labels specification
   */
  LabelsSpec getLabels() {
    labels
  }

  /**
   * Returns guides specification.
   *
   * @return guides specification
   */
  GuidesSpec getGuides() {
    guides
  }

  /**
   * Returns animation specification.
   *
   * @return animation spec or null
   */
  AnimationSpec getAnimation() {
    animation?.copy()
  }

  /**
   * Returns annotations list.
   *
   * @return annotation list
   */
  List<AnnotationSpec> getAnnotations() {
    annotations
  }

  /**
   * Configures mappings using closure syntax.
   *
   * @param configure closure for mapping configuration
   * @return this plot spec
   */
  PlotSpec mapping(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MappingDsl) Closure<?> configure) {
    MappingDsl dsl = new MappingDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    mapping.apply(dsl.toMapping())
    this
  }

  /**
   * Configures mappings using named arguments.
   *
   * @param mappingEntries named mapping entries
   * @return this plot spec
   */
  PlotSpec mapping(Map<String, ?> mappingEntries) {
    mapping.apply(mappingEntries)
    this
  }

  /**
   * Adds layers using the builder-style DSL.
   *
   * <p>Inside the closure, factory methods like {@code geomPoint()} return
   * fluent builders. Each builder is collected and converted into a
   * {@link LayerSpec} when the closure completes.</p>
   *
   * <pre>{@code
   * plot(data) {
   *   mapping { x = 'cty'; y = 'hwy' }
   *   layers {
   *     geomPoint().size(3).alpha(0.7)
   *   }
   * }
   * }</pre>
   *
   * @param configure layers closure
   * @return this plot spec
   */
  PlotSpec layers(
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = se.alipsa.matrix.charm.geom.LayersDsl)
      Closure<?> configure
  ) {
    se.alipsa.matrix.charm.geom.LayersDsl dsl = new se.alipsa.matrix.charm.geom.LayersDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    dsl.collected.each { se.alipsa.matrix.charm.geom.LayerBuilder builder ->
      layers << builder.build()
    }
    this
  }

  /**
   * Adds a single layer from a builder (programmatic escape hatch).
   *
   * <pre>{@code
   * import static se.alipsa.matrix.charm.geom.Geoms.geomPoint
   *
   * plot(data) {
   *   mapping { x = 'cty'; y = 'hwy' }
   *   addLayer geomPoint().size(3)
   * }
   * }</pre>
   *
   * @param builder configured layer builder
   * @return this plot spec
   */
  PlotSpec addLayer(se.alipsa.matrix.charm.geom.LayerBuilder builder) {
    layers << builder.build()
    this
  }

  // ---- Operator composition (ggplot2-style + chaining) ----

  /**
   * Adds a layer via the {@code +} operator.
   *
   * @param builder configured layer builder
   * @return this plot spec
   */
  PlotSpec plus(se.alipsa.matrix.charm.geom.LayerBuilder builder) {
    addLayer(builder)
  }

  /**
   * Merges a theme via the {@code +} operator.
   *
   * @param other theme to merge
   * @return this plot spec
   */
  PlotSpec plus(Theme other) {
    if (other == null) {
      return this
    }
    Theme merged = theme.plus(other)
    merged.properties.each { key, val ->
      String k = key as String
      if (k != 'class' && theme.hasProperty(k)) {
        theme.setProperty(k, val)
      }
    }
    this
  }

  /**
   * Replaces labels via the {@code +} operator.
   *
   * @param other labels to apply
   * @return this plot spec
   */
  PlotSpec plus(LabelsSpec other) {
    if (other == null) {
      return this
    }
    if (other.title != null) { labels.title = other.title }
    if (other.subtitle != null) { labels.subtitle = other.subtitle }
    if (other.caption != null) { labels.caption = other.caption }
    if (other.x != null) { labels.x = other.x }
    if (other.y != null) { labels.y = other.y }
    this
  }

  /**
   * Applies a coord spec via the {@code +} operator.
   *
   * @param other coord to apply
   * @return this plot spec
   */
  PlotSpec plus(CoordSpec other) {
    if (other == null) {
      return this
    }
    coord.type = other.type
    other.params.each { String k, Object v -> coord.params[k] = v }
    this
  }

  /**
   * Configures scales using closure syntax.
   *
   * @param configure closure for scale options
   * @return this plot spec
   */
  PlotSpec scale(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ScaleDsl) Closure<?> configure) {
    ScaleDsl dsl = new ScaleDsl(scale)
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Configures theme using closure syntax.
   *
   * @param configure closure for theme options
   * @return this plot spec
   */
  PlotSpec theme(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ThemeDsl) Closure<?> configure) {
    ThemeDsl dsl = new ThemeDsl(theme)
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Sets legend position as a shorthand for {@code theme.legendPosition}.
   *
   * @param value legend position
   * @return this plot spec
   */
  PlotSpec legendPosition(LegendPosition value) {
    theme.legendPosition = value
    theme.legendPositionCoords = null
    theme.explicitNulls.add('legendPositionCoords')
    this
  }

  /**
   * Sets legend position from absolute {@code [x, y]} coordinates.
   *
   * @param value coordinate list
   * @return this plot spec
   */
  PlotSpec legendPosition(List<Number> value) {
    theme.legendPositionCoords = value
    theme.explicitNulls.remove('legendPositionCoords')
    if (theme.legendPosition == LegendPosition.NONE) {
      theme.legendPosition = LegendPosition.RIGHT
    }
    this
  }

  /**
   * Configures faceting using closure syntax.
   *
   * @param configure closure for facet options
   * @return this plot spec
   */
  PlotSpec facet(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = FacetDsl) Closure<?> configure) {
    FacetDsl dsl = new FacetDsl(facet)
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Configures coordinates using closure syntax.
   *
   * @param configure closure for coord options
   * @return this plot spec
   */
  PlotSpec coord(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = CoordSpec) Closure<?> configure) {
    Closure<?> body = configure.rehydrate(coord, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Shorthand for {@code coord { type = FLIP }}.
   *
   * @return this plot spec
   */
  PlotSpec coordFlip() {
    coord.type = CharmCoordType.FLIP
    this
  }

  /**
   * Configures labels using closure syntax.
   *
   * @param configure closure for labels options
   * @return this plot spec
   */
  PlotSpec labels(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LabelsSpec) Closure<?> configure) {
    Closure<?> body = configure.rehydrate(labels, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Configures guides using closure syntax.
   *
   * @param configure closure for guide options
   * @return this plot spec
   */
  PlotSpec guides(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = GuidesDsl) Closure<?> configure) {
    GuidesDsl dsl = new GuidesDsl(guides)
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Configures optional SVG animation using closure syntax.
   *
   * @param configure closure for animation options
   * @return this plot spec
   */
  PlotSpec animation(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AnimationSpec) Closure<?> configure) {
    AnimationSpec spec = animation?.copy() ?: new AnimationSpec()
    Closure<?> body = configure.rehydrate(spec, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    animation = spec
    this
  }

  /**
   * Configures CSS attribute injection using closure syntax.
   *
   * @param configure closure for CSS attributes options
   * @return this plot spec
   */
  PlotSpec cssAttributes(
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = CssAttributesSpec) Closure<?> configure
  ) {
    CssAttributesSpec spec = cssAttributes?.copy() ?: new CssAttributesSpec()
    Closure<?> body = configure.rehydrate(spec, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    cssAttributes = spec
    this
  }

  /**
   * Adds annotations via closure DSL.
   *
   * @param configure closure for annotation declarations
   * @return this plot spec
   */
  PlotSpec annotate(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AnnotationDsl) Closure<?> configure) {
    AnnotationDsl dsl = new AnnotationDsl(annotations, layers.size())
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    this
  }

  /**
   * Compiles this mutable specification into an immutable chart model.
   *
   * @return immutable chart
   */
  Chart build() {
    try {
      validate()
      List<LayerSpec> compiledLayers = layers.collect { LayerSpec layer -> layer.copy() }
      List<AnnotationSpec> compiledAnnotations = annotations.collect { AnnotationSpec a -> a.copy() }
      ScaleSpec compiledScale = scale.copy()
      GuidesSpec compiledGuides = materializeScaleGuides(compiledScale, guides)
      AnimationSpec compiledAnimation = animation?.copy()
      new Chart(
          data,
          mapping.copy(),
          compiledLayers,
          compiledScale,
          theme.copy(),
          facet.copy(),
          coord.copy(),
          labels.copy(),
          compiledGuides,
          compiledAnnotations,
          cssAttributes?.copy(),
          compiledAnimation
      )
    } catch (CharmException e) {
      throw e
    } catch (Exception e) {
      throw new CharmCompilationException("Failed to compile plot specification: ${e.message}", e)
    }
  }

  /**
   * Compiles and renders this plot using default dimensions (800x600).
   *
   * @return SVG model object
   */
  Svg render() {
    build().render()
  }

  /**
   * Compiles and renders this plot at the given dimensions.
   *
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @return SVG model object
   */
  Svg render(int width, int height) {
    build().render(width, height)
  }

  /**
   * Compiles and writes this plot to the target file path.
   *
   * @param targetPath output file path
   */
  void writeTo(String targetPath) {
    build().writeTo(targetPath)
  }

  /**
   * Compiles and writes this plot to the target file.
   *
   * @param targetFile output file
   */
  void writeTo(File targetFile) {
    build().writeTo(targetFile)
  }

  private void validate() {
    validateMapping('plot mapping', mapping)
    layers.eachWithIndex { LayerSpec layer, int idx ->
      Mapping layerMapping = layer.mapping
      if (layerMapping != null) {
        validateMapping("layer ${idx} mapping", layerMapping)
      }
      Mapping effective = effectiveMapping(layer, layerMapping)
      validateLayerSemantics(layer, idx)
      validateRequiredMappings(layer, effective, idx)
    }
    facet.rows.each { ColumnExpr expr -> validateColumn(expr, 'facet.rows') }
    facet.cols.each { ColumnExpr expr -> validateColumn(expr, 'facet.cols') }
    facet.vars.each { ColumnExpr expr -> validateColumn(expr, 'facet.vars') }
  }

  private void validateLayerSemantics(LayerSpec layer, int idx) {
    if (!(layer.geomType in CharmGeomType.SUPPORTED)) {
      throw new CharmValidationException(
          "Layer ${idx}: unsupported geom type '${layer.geomType}'. Supported types: ${CharmGeomType.SUPPORTED}"
      )
    }
    if (layer.geomType == CharmGeomType.SMOOTH && layer.statType != CharmStatType.SMOOTH) {
      throw new CharmValidationException(
          "Invalid layer ${idx}: geom SMOOTH requires stat SMOOTH, but was ${layer.statType}"
      )
    }
  }

  private void validateRequiredMappings(LayerSpec layer, Mapping effective, int idx) {
    List<String> required = REQUIRED_MAPPINGS[layer.geomType] ?: []
    if (required.isEmpty()) {
      return
    }
    Map<String, ColumnExpr> mappings = effective.mappings()
    List<String> missing = required.findAll { String key -> !mappings.containsKey(key) }
    if (!missing.isEmpty()) {
      String inheritText = layer.inheritMapping ? 'with inherited plot mappings' : 'without inherited plot mappings'
      String hint = requiredMappingHint(layer.geomType, missing)
      throw new CharmValidationException(
          "Layer ${idx} (${layer.geomType}) is missing required mappings [${missing.join(', ')}] ${inheritText}${hint}"
      )
    }
  }

  private Mapping effectiveMapping(LayerSpec layer, Mapping layerMapping) {
    Mapping effective = layer.inheritMapping ? mapping.copy() : new Mapping()
    if (layerMapping != null) {
      effective.apply(layerMapping.mappings())
    }
    effective
  }

  private static GuidesSpec materializeScaleGuides(ScaleSpec scaleSpec, GuidesSpec explicitGuides) {
    GuidesSpec materialized = new GuidesSpec()
    materializeScaleGuide(materialized, 'x', scaleSpec?.x)
    materializeScaleGuide(materialized, 'y', scaleSpec?.y)
    materializeScaleGuide(materialized, 'color', scaleSpec?.color)
    materializeScaleGuide(materialized, 'fill', scaleSpec?.fill)
    materializeScaleGuide(materialized, 'size', scaleSpec?.size)
    materializeScaleGuide(materialized, 'shape', scaleSpec?.shape)
    materializeScaleGuide(materialized, 'alpha', scaleSpec?.alpha)
    materializeScaleGuide(materialized, 'linetype', scaleSpec?.linetype)
    materializeScaleGuide(materialized, 'group', scaleSpec?.group)
    materialized + (explicitGuides?.copy() ?: new GuidesSpec())
  }

  private static void materializeScaleGuide(GuidesSpec target, String aesthetic, Scale sourceScale) {
    if (sourceScale == null) {
      return
    }
    Map<String, Object> params = sourceScale.params
    if (params == null || !params.containsKey('guide')) {
      return
    }
    GuideSpec guide = GuideUtils.coerceGuide(params['guide'], aesthetic)
    if (guide != null) {
      target.setSpec(aesthetic, guide)
    }
  }

  private void validateMapping(String context, Mapping mappingValue) {
    mappingValue.mappings().each { String key, ColumnExpr value ->
      validateColumn(value, "${context}.${key}")
    }
  }

  private void validateColumn(ColumnExpr expr, String context) {
    if (expr == null) {
      return
    }
    String columnName = expr.columnName()
    List<String> names = data.columnNames()
    if (!names.contains(columnName)) {
      String suggestion = columnSuggestion(columnName, names)
      String hint = semanticHintForUnknownColumn(context, columnName)
      throw new CharmValidationException(
          "Unknown column '${columnName}' for ${context}. Available columns: ${names.join(', ')}${suggestion}${hint}"
      )
    }
  }

  private static String requiredMappingHint(CharmGeomType geomType, List<String> missing) {
    if ((geomType == CharmGeomType.POINT || geomType == CharmGeomType.LINE) && missing.contains('y')) {
      return '. Hint: point/line layers require both x and y mappings.'
    }
    ''
  }

  private static String semanticHintForUnknownColumn(String context, String columnName) {
    if (context?.endsWith('.color') || context?.endsWith('.fill')) {
      String value = columnName?.trim()
      if (looksLikeLiteralColor(value)) {
        return ". Hint: '${value}' looks like a literal color. Put literals on the layer, for example geomPoint().color('${value}'), instead of mapping it as a column."
      }
    }
    ''
  }

  private static boolean looksLikeLiteralColor(String value) {
    if (!value) {
      return false
    }
    String lower = value.toLowerCase(Locale.ROOT)
    if (lower.startsWith('#')) {
      return true
    }
    if (lower.startsWith('rgb(') || lower.startsWith('rgba(') || lower.startsWith('hsl(') || lower.startsWith('hsla(')) {
      return true
    }
    lower in ['black', 'white', 'red', 'green', 'blue', 'yellow', 'orange', 'purple', 'grey', 'gray', 'brown', 'pink']
  }

  private static String columnSuggestion(String columnName, List<String> availableColumns) {
    if (!columnName || availableColumns == null || availableColumns.isEmpty()) {
      return ''
    }

    String target = columnName.toLowerCase(Locale.ROOT)
    List<ColumnSuggestion> ranked = availableColumns.collect { String candidate ->
      new ColumnSuggestion(candidate, COLUMN_DISTANCE.apply(target, candidate.toLowerCase(Locale.ROOT)))
    }
    ranked.sort { ColumnSuggestion left, ColumnSuggestion right ->
      int compare = left.distance <=> right.distance
      compare != 0 ? compare : left.name <=> right.name
    }

    if (ranked.isEmpty()) {
      return ''
    }

    int threshold = Math.max(2, (int) Math.ceil(columnName.size() / 3.0))
    List<String> suggestions = ranked
        .findAll { ColumnSuggestion entry -> entry.distance <= threshold }
        .collect { ColumnSuggestion entry -> entry.name }
        .take(3)

    if (suggestions.isEmpty()) {
      return ''
    }
    " Did you mean: ${suggestions.join(', ')}?"
  }

  private static final class ColumnSuggestion {

    final String name
    final int distance

    ColumnSuggestion(String name, Integer distance) {
      this.name = name
      this.distance = distance == null ? Integer.MAX_VALUE : distance
    }

  }

  /**
   * Scale DSL delegate.
   */
  static class ScaleDsl {

    // ---- Legend position constants for guide configuration ----
    /** @see LegendPosition#RIGHT */
    static final LegendPosition RIGHT = LegendPosition.RIGHT
    /** @see LegendPosition#LEFT */
    static final LegendPosition LEFT = LegendPosition.LEFT
    /** @see LegendPosition#TOP */
    static final LegendPosition TOP = LegendPosition.TOP
    /** @see LegendPosition#BOTTOM */
    static final LegendPosition BOTTOM = LegendPosition.BOTTOM
    /** @see LegendPosition#NONE */
    static final LegendPosition NONE = LegendPosition.NONE

    // ---- Legend direction constants for guide configuration ----
    /** @see LegendDirection#VERTICAL */
    static final LegendDirection VERTICAL = LegendDirection.VERTICAL
    /** @see LegendDirection#HORIZONTAL */
    static final LegendDirection HORIZONTAL = LegendDirection.HORIZONTAL

    private final ScaleSpec scale

    /**
     * Creates scale DSL bound to a target scale spec.
     *
     * @param scale target scale spec
     */
    ScaleDsl(ScaleSpec scale) {
      this.scale = scale
    }

    /** Sets x scale. */
    void setX(Scale value) { scale.x = value }

    /** Sets x scale from a transform. */
    void setX(ScaleTransform value) { scale.x = Scale.transform(value) }

    /** Sets x scale from a transform name. */
    void setX(String value) { scale.x = Scale.transform(value) }

    /** Sets y scale. */
    void setY(Scale value) { scale.y = value }

    /** Sets y scale from a transform. */
    void setY(ScaleTransform value) { scale.y = Scale.transform(value) }

    /** Sets y scale from a transform name. */
    void setY(String value) { scale.y = Scale.transform(value) }

    /** Sets color scale. */
    void setColor(Scale value) { scale.color = value }

    /** Sets color scale from a transform. */
    void setColor(ScaleTransform value) { scale.color = Scale.transform(value) }

    /** Sets color scale from a transform name. */
    void setColor(String value) { scale.color = Scale.transform(value) }

    /** Sets fill scale. */
    void setFill(Scale value) { scale.fill = value }

    /** Sets fill scale from a transform. */
    void setFill(ScaleTransform value) { scale.fill = Scale.transform(value) }

    /** Sets fill scale from a transform name. */
    void setFill(String value) { scale.fill = Scale.transform(value) }

    /**
     * Creates a log10 transform scale.
     *
     * @return scale object
     */
    Scale log10() {
      Scale.transform('log10')
    }

    /**
     * Creates a sqrt transform scale.
     *
     * @return scale object
     */
    Scale sqrt() {
      Scale.transform('sqrt')
    }

    /**
     * Creates a reverse transform scale.
     *
     * @return scale object
     */
    Scale reverse() {
      Scale.transform('reverse')
    }

    /**
     * Creates a date transform scale.
     *
     * @return scale object
     */
    Scale date() {
      Scale.date()
    }

    /**
     * Creates a time transform scale.
     *
     * @return scale object
     */
    Scale time() {
      Scale.time()
    }

    /**
     * Creates a datetime transform scale.
     *
     * @return scale object
     */
    Scale datetime() {
      Scale.datetime()
    }

    /**
     * Creates a custom transform scale.
     *
     * @param id transform id
     * @param forward forward transform
     * @param inverse inverse transform
     * @return scale object
     */
    Scale custom(String id,
                  @ClosureParams(value = SimpleType, options = ['java.math.BigDecimal'])
                  Closure<BigDecimal> forward,
                  @ClosureParams(value = SimpleType, options = ['java.math.BigDecimal'])
                  Closure<BigDecimal> inverse = null) {
      Scale.custom(id, forward, inverse)
    }

    /**
     * Creates a continuous scale.
     *
     * @return scale object
     */
    Scale continuous() {
      Scale.continuous()
    }

    /**
     * Creates a discrete scale.
     *
     * @return scale object
     */
    Scale discrete() {
      Scale.discrete()
    }

    /**
     * Creates a manual color scale from a named map.
     *
     * @param namedValues named color map (e.g. {@code ['below': '#e74c3c', 'above': '#2ecc71']})
     * @return scale object
     */
    Scale manual(Map<String, String> namedValues) {
      Scale.manual(namedValues)
    }

    /**
     * Creates a manual color scale from a list of colors.
     *
     * @param values color list
     * @return scale object
     */
    Scale manual(List<String> values) {
      Scale.manual(values)
    }

    /**
     * Creates a ColorBrewer color scale.
     *
     * @param paletteName palette name
     * @param direction 1 for normal, -1 for reversed
     * @return scale object
     */
    Scale colorBrewer(String paletteName = 'Set1', int direction = 1) {
      Scale.brewer(paletteName, direction)
    }

    /**
     * Creates a two-color gradient scale.
     *
     * @param from low-end color
     * @param to high-end color
     * @return scale object
     */
    Scale colorGradient(String from = '#132B43', String to = '#56B1F7') {
      Scale.gradient(from, to)
    }

    /** Creates a legend guide spec. */
    GuideSpec legend(Map<String, Object> params = [:]) { GuideSpec.legend(params) }

    /** Creates a colorbar guide spec. */
    GuideSpec colorbar(Map<String, Object> params = [:]) { GuideSpec.colorbar(params) }

    /** Creates a colorsteps guide spec. */
    GuideSpec colorsteps(Map<String, Object> params = [:]) { GuideSpec.colorsteps(params) }

    /** Creates a none guide spec. */
    GuideSpec none() { GuideSpec.none() }

  }

  /**
   * Theme DSL delegate with flat property setters for IDE autocomplete.
   *
   * <p>All common theme options are exposed as single-level properties
   * inside the {@code theme {}} closure. This replaces the previous
   * nested closures ({@code legend {}}, {@code axis {}}, etc.) that
   * delegated to {@link MapDsl} and were invisible to the IDE.</p>
   */
  static class ThemeDsl {

    // ---- Legend position constants for IDE auto-complete ----
    /** @see LegendPosition#RIGHT */
    static final LegendPosition RIGHT = LegendPosition.RIGHT
    /** @see LegendPosition#LEFT */
    static final LegendPosition LEFT = LegendPosition.LEFT
    /** @see LegendPosition#TOP */
    static final LegendPosition TOP = LegendPosition.TOP
    /** @see LegendPosition#BOTTOM */
    static final LegendPosition BOTTOM = LegendPosition.BOTTOM
    /** @see LegendPosition#NONE */
    static final LegendPosition NONE = LegendPosition.NONE

    // ---- Legend direction constants for IDE auto-complete ----
    /** @see LegendDirection#VERTICAL */
    static final LegendDirection VERTICAL = LegendDirection.VERTICAL
    /** @see LegendDirection#HORIZONTAL */
    static final LegendDirection HORIZONTAL = LegendDirection.HORIZONTAL

    private final Theme theme

    /**
     * Creates theme DSL bound to a target theme spec.
     *
     * @param theme target theme
     */
    ThemeDsl(Theme theme) {
      this.theme = theme
    }

    // ---- Legend ----

    /**
     * Sets legend position from a {@link LegendPosition} enum constant.
     *
     * @param value legend position
     */
    void setLegendPosition(LegendPosition value) {
      theme.legendPosition = value
      theme.legendPositionCoords = null
      theme.explicitNulls.add('legendPositionCoords')
    }

    /**
     * Sets legend position from absolute {@code [x, y]} coordinates.
     *
     * @param value coordinate list
     */
    void setLegendPosition(List<Number> value) {
      theme.legendPositionCoords = value
      theme.explicitNulls.remove('legendPositionCoords')
      if (theme.legendPosition == LegendPosition.NONE) {
        theme.legendPosition = LegendPosition.RIGHT
      }
    }

    /**
     * Sets legend direction.
     *
     * @param value legend direction
     */
    void setLegendDirection(LegendDirection value) {
      theme.legendDirection = value
    }

    // ---- Axis ----

    /**
     * Sets axis line width for both x and y axes.
     *
     * @param value line width
     */
    void setAxisLineWidth(Number value) {
      theme.axisLineX = new se.alipsa.matrix.charm.theme.ElementLine(
          color: theme.axisLineX?.color ?: '#333333',
          size: value
      )
      theme.axisLineY = new se.alipsa.matrix.charm.theme.ElementLine(
          color: theme.axisLineY?.color ?: '#333333',
          size: value
      )
    }

    /**
     * Sets axis line colour for both x and y axes.
     *
     * @param value colour value
     */
    void setAxisColor(String value) {
      theme.axisLineX = new se.alipsa.matrix.charm.theme.ElementLine(
          color: value,
          size: theme.axisLineX?.size ?: 1
      )
      theme.axisLineY = new se.alipsa.matrix.charm.theme.ElementLine(
          color: value,
          size: theme.axisLineY?.size ?: 1
      )
    }

    /**
     * Sets axis tick length in pixels.
     *
     * @param value tick length
     */
    void setAxisTickLength(Number value) {
      theme.axisTickLength = value
    }

    // ---- Text ----

    /**
     * Sets axis text colour for both x and y axes.
     *
     * @param value colour value
     */
    void setTextColor(String value) {
      se.alipsa.matrix.charm.theme.ElementText xText = theme.axisTextX?.copy()
          ?: new se.alipsa.matrix.charm.theme.ElementText()
      se.alipsa.matrix.charm.theme.ElementText yText = theme.axisTextY?.copy()
          ?: new se.alipsa.matrix.charm.theme.ElementText()
      xText.color = value
      yText.color = value
      theme.axisTextX = xText
      theme.axisTextY = yText
    }

    /**
     * Sets axis text size for both x and y axes.
     *
     * @param value text size
     */
    void setTextSize(Number value) {
      se.alipsa.matrix.charm.theme.ElementText xText = theme.axisTextX?.copy()
          ?: new se.alipsa.matrix.charm.theme.ElementText()
      se.alipsa.matrix.charm.theme.ElementText yText = theme.axisTextY?.copy()
          ?: new se.alipsa.matrix.charm.theme.ElementText()
      xText.size = value
      yText.size = value
      theme.axisTextX = xText
      theme.axisTextY = yText
    }

    /**
     * Sets plot title size.
     *
     * @param value title size
     */
    void setTitleSize(Number value) {
      se.alipsa.matrix.charm.theme.ElementText title = theme.plotTitle?.copy()
          ?: new se.alipsa.matrix.charm.theme.ElementText()
      title.size = value
      theme.plotTitle = title
    }

    // ---- Font defaults ----

    /**
     * Sets base font family.
     *
     * @param value font family
     */
    void setBaseFamily(String value) {
      theme.baseFamily = value
    }

    /**
     * Sets base font size.
     *
     * @param value font size
     */
    void setBaseSize(Number value) {
      theme.baseSize = value
    }

    /**
     * Sets base line height.
     *
     * @param value line height
     */
    void setBaseLineHeight(Number value) {
      theme.baseLineHeight = value
    }

    // ---- Grid ----

    /**
     * Sets major grid line colour.
     *
     * @param value colour value
     */
    void setGridColor(String value) {
      theme.panelGridMajor = new se.alipsa.matrix.charm.theme.ElementLine(
          color: value,
          size: theme.panelGridMajor?.size ?: 1
      )
    }

    /**
     * Sets major grid line width.
     *
     * @param value line width
     */
    void setGridLineWidth(Number value) {
      theme.panelGridMajor = new se.alipsa.matrix.charm.theme.ElementLine(
          color: theme.panelGridMajor?.color ?: '#eeeeee',
          size: value
      )
    }

    /**
     * Sets minor grid line colour (also enables minor grid).
     *
     * @param value colour value for minor grid lines
     */
    void setGridMinor(String value) {
      theme.panelGridMinor = new se.alipsa.matrix.charm.theme.ElementLine(
          color: value,
          size: theme.panelGridMinor?.size ?: 1
      )
    }

    // ---- Theme presets ----

    /** Applies the preset by name (e.g. 'minimal', 'dark', 'classic'). */
    void setPreset(String name) { apply(resolvePreset(name)) }

    /** Applies a preset theme, deep-merging its settings into the current theme. */
    void apply(Theme preset) {
      if (preset == null) {
        throw new CharmValidationException('preset theme cannot be null')
      }
      Theme merged = theme.plus(preset)
      merged.properties.each { key, val ->
        String k = key as String
        if (k != 'class' && theme.hasProperty(k)) {
          theme.setProperty(k, val)
        }
      }
    }

    /** @return gray theme (ggplot2 default) */
    static Theme gray() { CharmThemes.gray() }

    /** @return classic theme */
    static Theme classic() { CharmThemes.classic() }

    /** @return black and white theme */
    static Theme bw() { CharmThemes.bw() }

    /** @return minimal theme */
    static Theme minimal() { CharmThemes.minimal() }

    /** @return void theme (data only) */
    static Theme void_() { CharmThemes.void_() }

    /** @return light theme */
    static Theme light() { CharmThemes.light() }

    /** @return dark theme */
    static Theme dark() { CharmThemes.dark() }

    /** @return linedraw theme */
    static Theme linedraw() { CharmThemes.linedraw() }

    private static Theme resolvePreset(String name) {
      switch (name?.toLowerCase(Locale.ROOT)) {
        case 'gray', 'grey' -> CharmThemes.gray()
        case 'classic' -> CharmThemes.classic()
        case 'bw' -> CharmThemes.bw()
        case 'minimal' -> CharmThemes.minimal()
        case 'void' -> CharmThemes.void_()
        case 'light' -> CharmThemes.light()
        case 'dark' -> CharmThemes.dark()
        case 'linedraw' -> CharmThemes.linedraw()
        default -> throw new CharmValidationException("Unknown theme preset: '${name}'")
      }
    }

  }

  /**
   * Facet DSL delegate.
   */
  static class FacetDsl {

    private final Facet facet
    private boolean wrapConfigured
    private boolean gridConfigured

    /**
     * Creates facet DSL bound to a target facet spec.
     *
     * @param facet target facet
     */
    FacetDsl(Facet facet) {
      this.facet = facet
      this.wrapConfigured = facet.type == FacetType.WRAP
      this.gridConfigured = facet.type == FacetType.GRID
    }

    /**
     * Sets grid rows and activates GRID facet mode.
     *
     * @param rows row expressions
     */
    void setRows(List<?> rows) {
      assertGridMode()
      facet.rows = coerceColumns(rows)
      facet.type = FacetType.GRID
      gridConfigured = true
    }

    /**
     * Sets grid columns and activates GRID facet mode.
     *
     * @param cols column expressions
     */
    void setCols(List<?> cols) {
      assertGridMode()
      facet.cols = coerceColumns(cols)
      facet.type = FacetType.GRID
      gridConfigured = true
    }

    /**
     * Configures WRAP facet mode.
     *
     * @param configure wrap closure
     */
    void wrap(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = WrapDsl) Closure<?> configure) {
      if (gridConfigured || !facet.rows.isEmpty() || !facet.cols.isEmpty()) {
        throw new CharmValidationException('Cannot combine wrap{} with rows/cols in the same facet block')
      }
      WrapDsl wrapDsl = new WrapDsl()
      Closure<?> body = configure.rehydrate(wrapDsl, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      facet.type = FacetType.WRAP
      facet.vars = coerceColumns(wrapDsl.vars)
      facet.ncol = wrapDsl.ncol
      facet.nrow = wrapDsl.nrow
      if (wrapDsl.labeller != null) {
        facet.params['labeller'] = wrapDsl.labeller
      }
      wrapConfigured = true
    }

    /**
     * Sets the labeller for facet strip labels.
     *
     * @param labeller labeller instance
     */
    void setLabeller(Labeller labeller) {
      facet.params['labeller'] = labeller
    }

    /**
     * Creates a value-only labeller (e.g. {@code "setosa"}).
     *
     * @return value labeller
     */
    static Labeller value() {
      Labeller.value()
    }

    /**
     * Creates a "variable: value" labeller (e.g. {@code "Species: setosa"}).
     *
     * @return both-style labeller
     */
    static Labeller both() {
      Labeller.both()
    }

    /**
     * Creates a custom labeller from a closure.
     *
     * @param fn labelling closure
     * @return custom labeller
     */
    static Labeller label(Closure<String> fn) {
      Labeller.label(fn)
    }

    private void assertGridMode() {
      if (wrapConfigured) {
        throw new CharmValidationException('Cannot set rows/cols after wrap{} in the same facet block')
      }
    }

    private static List<ColumnExpr> coerceColumns(List<?> values) {
      if (values == null) {
        return []
      }
      values.collect { Object v -> Mapping.coerceToColumnExpr(v, 'facet') }
    }

    /**
     * Delegate used for {@code wrap {}} configuration.

     */
    static class WrapDsl {

      List<String> vars = []
      Integer ncol
      Integer nrow
      Labeller labeller

    }

  }

  /**
   * Guides DSL delegate for configuring guide types per aesthetic.
   */
  static class GuidesDsl {

    private final GuidesSpec guides

    /**
     * Creates guides DSL bound to a target guides spec.
     *
     * @param guides target guides spec
     */
    GuidesDsl(GuidesSpec guides) {
      this.guides = guides
    }

    /** Sets the color guide from a spec. */
    void setColor(GuideSpec value) { guides.setSpec('color', value) }

    /** Sets the color guide from a type. */
    void setColor(GuideType value) { guides.setSpec('color', new GuideSpec(value)) }

    /** Sets the color guide from a type name. */
    void setColor(String value) { guides.setSpec('color', GuideUtils.coerceGuide(value, 'color')) }

    /** Disables the color guide (false) or throws on true. */
    void setColor(boolean value) { guides.setSpec('color', coerceBool(value, 'color')) }

    /** Sets the fill guide from a spec. */
    void setFill(GuideSpec value) { guides.setSpec('fill', value) }

    /** Sets the fill guide from a type. */
    void setFill(GuideType value) { guides.setSpec('fill', new GuideSpec(value)) }

    /** Sets the fill guide from a type name. */
    void setFill(String value) { guides.setSpec('fill', GuideUtils.coerceGuide(value, 'fill')) }

    /** Disables the fill guide (false) or throws on true. */
    void setFill(boolean value) { guides.setSpec('fill', coerceBool(value, 'fill')) }

    /** Sets the size guide from a spec. */
    void setSize(GuideSpec value) { guides.setSpec('size', value) }

    /** Sets the size guide from a type. */
    void setSize(GuideType value) { guides.setSpec('size', new GuideSpec(value)) }

    /** Sets the size guide from a type name. */
    void setSize(String value) { guides.setSpec('size', GuideUtils.coerceGuide(value, 'size')) }

    /** Disables the size guide (false) or throws on true. */
    void setSize(boolean value) { guides.setSpec('size', coerceBool(value, 'size')) }

    /** Sets the shape guide from a spec. */
    void setShape(GuideSpec value) { guides.setSpec('shape', value) }

    /** Sets the shape guide from a type. */
    void setShape(GuideType value) { guides.setSpec('shape', new GuideSpec(value)) }

    /** Sets the shape guide from a type name. */
    void setShape(String value) { guides.setSpec('shape', GuideUtils.coerceGuide(value, 'shape')) }

    /** Disables the shape guide (false) or throws on true. */
    void setShape(boolean value) { guides.setSpec('shape', coerceBool(value, 'shape')) }

    /** Sets the alpha guide from a spec. */
    void setAlpha(GuideSpec value) { guides.setSpec('alpha', value) }

    /** Sets the alpha guide from a type. */
    void setAlpha(GuideType value) { guides.setSpec('alpha', new GuideSpec(value)) }

    /** Sets the alpha guide from a type name. */
    void setAlpha(String value) { guides.setSpec('alpha', GuideUtils.coerceGuide(value, 'alpha')) }

    /** Disables the alpha guide (false) or throws on true. */
    void setAlpha(boolean value) { guides.setSpec('alpha', coerceBool(value, 'alpha')) }

    /** Sets the x guide from a spec. */
    void setX(GuideSpec value) { guides.setSpec('x', value) }

    /** Sets the x guide from a type. */
    void setX(GuideType value) { guides.setSpec('x', new GuideSpec(value)) }

    /** Sets the x guide from a type name. */
    void setX(String value) { guides.setSpec('x', GuideUtils.coerceGuide(value, 'x')) }

    /** Disables the x guide (false) or throws on true. */
    void setX(boolean value) { guides.setSpec('x', coerceBool(value, 'x')) }

    /** Sets the y guide from a spec. */
    void setY(GuideSpec value) { guides.setSpec('y', value) }

    /** Sets the y guide from a type. */
    void setY(GuideType value) { guides.setSpec('y', new GuideSpec(value)) }

    /** Sets the y guide from a type name. */
    void setY(String value) { guides.setSpec('y', GuideUtils.coerceGuide(value, 'y')) }

    /** Disables the y guide (false) or throws on true. */
    void setY(boolean value) { guides.setSpec('y', coerceBool(value, 'y')) }

    private static GuideSpec coerceBool(boolean value, String aesthetic) {
      if (value) {
        throw new CharmValidationException(
            "Cannot set '${aesthetic}' guide to true — use a GuideSpec, GuideType, or guide type name")
      }
      GuideSpec.none()
    }

    /** Creates a legend guide spec. */
    GuideSpec legend(Map<String, Object> params = [:]) { GuideSpec.legend(params) }

    /** Creates a colorbar guide spec. */
    GuideSpec colorbar(Map<String, Object> params = [:]) { GuideSpec.colorbar(params) }

    /** Creates a colorsteps guide spec. */
    GuideSpec colorsteps(Map<String, Object> params = [:]) { GuideSpec.colorsteps(params) }

    /** Creates a none guide spec. */
    GuideSpec none() { GuideSpec.none() }

    /** Creates an axis guide spec. */
    GuideSpec axis(Map<String, Object> params = [:]) { GuideSpec.axis(params) }

    /** Creates an axis_logticks guide spec. */
    GuideSpec axisLogticks(Map<String, Object> params = [:]) { GuideSpec.axisLogticks(params) }

  }

  /**
   * Annotation DSL delegate.
   */
  static class AnnotationDsl {

    private final List<AnnotationSpec> target
    private final int drawOrder

    /**
     * Creates annotation DSL bound to an annotation list.
     *
     * @param target annotation target list
     */
    AnnotationDsl(List<AnnotationSpec> target, int drawOrder = 0) {
      this.target = target
      this.drawOrder = drawOrder
    }

    /**
     * Adds text annotation.
     *
     * @param configure text closure
     */
    void text(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TextAnnotationSpec) Closure<?> configure) {
      TextAnnotationSpec spec = new TextAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

    /**
     * Adds rectangle annotation.
     *
     * @param configure rectangle closure
     */
    void rect(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = RectAnnotationSpec) Closure<?> configure) {
      RectAnnotationSpec spec = new RectAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

    /**
     * Adds segment annotation.
     *
     * @param configure segment closure
     */
    void segment(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = SegmentAnnotationSpec) Closure<?> configure) {
      SegmentAnnotationSpec spec = new SegmentAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

    /**
     * Adds custom-grob annotation.
     *
     * @param configure custom closure
     */
    void custom(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = CustomAnnotationSpec) Closure<?> configure) {
      CustomAnnotationSpec spec = new CustomAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

    /**
     * Adds logticks annotation.
     *
     * @param configure logticks closure
     */
    void logticks(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LogticksAnnotationSpec) Closure<?> configure) {
      LogticksAnnotationSpec spec = new LogticksAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

    /**
     * Adds raster annotation.
     *
     * @param configure raster closure
     */
    void raster(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = RasterAnnotationSpec) Closure<?> configure) {
      RasterAnnotationSpec spec = new RasterAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

    /**
     * Adds map annotation.
     *
     * @param configure map closure
     */
    void map(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MapAnnotationSpec) Closure<?> configure) {
      MapAnnotationSpec spec = new MapAnnotationSpec()
      Closure<?> body = configure.rehydrate(spec, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      spec.drawOrder = drawOrder
      target << spec
    }

  }

}
