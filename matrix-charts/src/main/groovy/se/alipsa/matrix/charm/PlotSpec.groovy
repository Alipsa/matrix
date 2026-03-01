package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import org.apache.commons.text.similarity.LevenshteinDistance
import se.alipsa.matrix.core.Matrix
import java.util.Locale

/**
 * Mutable root plot specification that compiles into an immutable {@link Chart}.
 */
@CompileStatic
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
  private MappingSpec mapping = new MappingSpec()
  private final List<LayerSpec> layers = []
  private ScaleSpec scale = new ScaleSpec()
  private ThemeSpec theme = new ThemeSpec()
  private FacetSpec facet = new FacetSpec()
  private CoordSpec coord = new CoordSpec()
  private LabelsSpec labels = new LabelsSpec()
  private GuidesSpec guides = new GuidesSpec()
  private AnimationSpec animation
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
   * @param value legend position ('right', 'left', 'top', 'bottom', 'none', or [x, y])
   * @return this plot spec
   */
  PlotSpec legendPosition(Object value) {
    theme.legendPosition = value
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
          null,
          compiledAnimation
      )
    } catch (CharmException e) {
      throw e
    } catch (Exception e) {
      throw new CharmCompilationException("Failed to compile plot specification: ${e.message}", e)
    }
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

  @CompileStatic
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
  @CompileStatic
  static class ScaleDsl {

    private final ScaleSpec scale

    /**
     * Creates scale DSL bound to a target scale spec.
     *
     * @param scale target scale spec
     */
    ScaleDsl(ScaleSpec scale) {
      this.scale = scale
    }

    /**
     * Sets x scale.
     *
     * @param value scale input
     */
    void setX(Object value) {
      scale.x = coerce(value, 'x')
    }

    /**
     * Sets y scale.
     *
     * @param value scale input
     */
    void setY(Object value) {
      scale.y = coerce(value, 'y')
    }

    /**
     * Sets color scale.
     *
     * @param value scale input
     */
    void setColor(Object value) {
      scale.color = coerce(value, 'color')
    }

    /**
     * Sets fill scale.
     *
     * @param value scale input
     */
    void setFill(Object value) {
      scale.fill = coerce(value, 'fill')
    }

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
    Scale custom(String id, Closure<BigDecimal> forward, Closure<BigDecimal> inverse = null) {
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
     * Creates a manual color scale from a list of colors or a named map.
     *
     * @param values color list or named map (e.g. {@code ['below': '#e74c3c', 'above': '#2ecc71']})
     * @return scale object
     */
    Scale manual(Object values) {
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

    private Scale coerce(Object value, String axis) {
      if (value == null) {
        return null
      }
      if (value instanceof Scale) {
        return value as Scale
      }
      if (value instanceof ScaleTransform) {
        return Scale.transform(value as ScaleTransform)
      }
      if (value instanceof CharSequence) {
        return Scale.transform(value.toString())
      }
      throw new CharmValidationException("Unsupported scale value for '${axis}': ${value.getClass().name}")
    }
  }

  /**
   * Theme DSL delegate with flat property setters for IDE autocomplete.
   *
   * <p>All common theme options are exposed as single-level properties
   * inside the {@code theme {}} closure. This replaces the previous
   * nested closures ({@code legend {}}, {@code axis {}}, etc.) that
   * delegated to {@link MapDsl} and were invisible to the IDE.</p>
   */
  @CompileStatic
  static class ThemeDsl {

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
     * Sets legend position ('right', 'left', 'top', 'bottom', 'none', or [x, y]).
     *
     * @param value legend position
     */
    void setLegendPosition(Object value) {
      theme.legendPosition = value
    }

    /**
     * Sets legend direction ('vertical' or 'horizontal').
     *
     * @param value legend direction
     */
    void setLegendDirection(String value) {
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
  }

  /**
   * Facet DSL delegate.
   */
  @CompileStatic
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
      wrapConfigured = true
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
     * Delegate used for `wrap {}` configuration.
     */
    @CompileStatic
    static class WrapDsl {
      List vars = []
      Integer ncol
      Integer nrow
    }
  }

  /**
   * Guides DSL delegate for configuring guide types per aesthetic.
   */
  @CompileStatic
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

    /** Sets the color guide. */
    void setColor(Object value) { guides.setSpec('color', GuideUtils.coerceGuide(value, 'color')) }

    /** Sets the fill guide. */
    void setFill(Object value) { guides.setSpec('fill', GuideUtils.coerceGuide(value, 'fill')) }

    /** Sets the size guide. */
    void setSize(Object value) { guides.setSpec('size', GuideUtils.coerceGuide(value, 'size')) }

    /** Sets the shape guide. */
    void setShape(Object value) { guides.setSpec('shape', GuideUtils.coerceGuide(value, 'shape')) }

    /** Sets the alpha guide. */
    void setAlpha(Object value) { guides.setSpec('alpha', GuideUtils.coerceGuide(value, 'alpha')) }

    /** Sets the x guide. */
    void setX(Object value) { guides.setSpec('x', GuideUtils.coerceGuide(value, 'x')) }

    /** Sets the y guide. */
    void setY(Object value) { guides.setSpec('y', GuideUtils.coerceGuide(value, 'y')) }

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
  @CompileStatic
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
