package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Mutable root plot specification that compiles into an immutable {@link Chart}.
 */
@CompileStatic
class PlotSpec {

  private static final Map<Geom, List<String>> REQUIRED_AESTHETICS = [
      (Geom.POINT)    : ['x', 'y'],
      (Geom.LINE)     : ['x', 'y'],
      (Geom.SMOOTH)   : ['x', 'y'],
      (Geom.TILE)     : ['x', 'y'],
      (Geom.BAR)      : ['x'],
      (Geom.HISTOGRAM): ['x'],
      (Geom.BOXPLOT)  : ['y'],
      (Geom.AREA)     : ['x', 'y'],
      (Geom.PIE)      : ['x', 'y']
  ]

  private final Matrix data
  private AesSpec aes = new AesSpec()
  private final List<LayerSpec> layers = []
  private ScaleSpec scale = new ScaleSpec()
  private ThemeSpec theme = new ThemeSpec()
  private FacetSpec facet = new FacetSpec()
  private CoordSpec coord = new CoordSpec()
  private LabelsSpec labels = new LabelsSpec()
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
   * Returns plot-level aesthetics.
   *
   * @return plot-level aesthetics
   */
  AesSpec getAes() {
    aes
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
   * Returns annotations list.
   *
   * @return annotation list
   */
  List<AnnotationSpec> getAnnotations() {
    annotations
  }

  /**
   * Configures aesthetics using closure syntax.
   *
   * @param configure closure for aesthetic mapping
   * @return this plot spec
   */
  PlotSpec aes(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AesDsl) Closure<?> configure) {
    AesDsl dsl = new AesDsl()
    Closure<?> body = configure.rehydrate(dsl, this, this)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    aes.apply(dsl.toMapping())
    this
  }

  /**
   * Configures aesthetics using named arguments.
   *
   * @param mapping named mapping entries
   * @return this plot spec
   */
  PlotSpec aes(Map<String, ?> mapping) {
    aes.apply(mapping)
    this
  }

  /**
   * Adds a point layer.
   *
   * @param configure optional layer parameter closure
   * @return this plot spec
   */
  PlotSpec points(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure = null) {
    addLayer(Geom.POINT, Stat.IDENTITY, configure)
  }

  /**
   * Adds a line layer.
   *
   * @param configure optional layer parameter closure
   * @return this plot spec
   */
  PlotSpec line(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure = null) {
    addLayer(Geom.LINE, Stat.IDENTITY, configure)
  }

  /**
   * Adds a tile layer.
   *
   * @param configure optional layer parameter closure
   * @return this plot spec
   */
  PlotSpec tile(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure = null) {
    addLayer(Geom.TILE, Stat.IDENTITY, configure)
  }

  /**
   * Adds a smooth layer.
   *
   * @param configure optional layer parameter closure
   * @return this plot spec
   */
  PlotSpec smooth(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure = null) {
    addLayer(Geom.SMOOTH, Stat.SMOOTH, configure)
  }

  /**
   * Adds an area layer.
   *
   * @param configure optional layer parameter closure
   * @return this plot spec
   */
  PlotSpec area(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure = null) {
    addLayer(Geom.AREA, Stat.IDENTITY, configure)
  }

  /**
   * Adds a pie layer.
   *
   * @param configure optional layer parameter closure
   * @return this plot spec
   */
  PlotSpec pie(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure = null) {
    addLayer(Geom.PIE, Stat.IDENTITY, configure)
  }

  /**
   * Adds a layer with explicit geometry and options.
   *
   * @param geom geometry type
   * @param options free-form layer options
   * @return this plot spec
   */
  PlotSpec layer(Geom geom, Map<String, Object> options = [:]) {
    if (geom == null) {
      throw new CharmValidationException('layer geom cannot be null')
    }
    Map<String, Object> params = new LinkedHashMap<>(options ?: [:])
    Aes layerAes = coerceLayerAes(params.remove('aes'))
    boolean inheritAes = params.containsKey('inheritAes')
        ? parseBooleanOption(params.remove('inheritAes'), 'inheritAes')
        : true
    Position position = coercePosition(params.remove('position'))
    Stat stat = coerceStat(params.remove('stat'), geom)
    LayerSpec layer = new LayerSpec(geom, stat, layerAes, inheritAes, position, params)
    layers << layer
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
   * Adds annotations via closure DSL.
   *
   * @param configure closure for annotation declarations
   * @return this plot spec
   */
  PlotSpec annotate(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AnnotationDsl) Closure<?> configure) {
    AnnotationDsl dsl = new AnnotationDsl(annotations)
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
      new Chart(
          data,
          aes.copy(),
          compiledLayers,
          scale.copy(),
          theme.copy(),
          facet.copy(),
          coord.copy(),
          labels.copy(),
          compiledAnnotations
      )
    } catch (CharmException e) {
      throw e
    } catch (Exception e) {
      throw new CharmCompilationException("Failed to compile plot specification: ${e.message}", e)
    }
  }

  private PlotSpec addLayer(
      Geom geom,
      Stat stat,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = LayerDsl) Closure<?> configure
  ) {
    LayerDsl options = new LayerDsl()
    if (configure != null) {
      Closure<?> body = configure.rehydrate(options, this, this)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
    }
    layers << new LayerSpec(
        geom,
        stat,
        options.layerAes(),
        options.inheritAes,
        options.position,
        options.values()
    )
    this
  }

  private void validate() {
    validateAes('plot aes', aes)
    layers.eachWithIndex { LayerSpec layer, int idx ->
      validateLayerSemantics(layer, idx)
      Aes layerAes = layer.aes
      if (layerAes != null) {
        validateAes("layer ${idx} aes", layerAes)
      }
      validateRequiredAesthetics(layer, layerAes, idx)
    }
    facet.rows.each { ColumnExpr expr -> validateColumn(expr, 'facet.rows') }
    facet.cols.each { ColumnExpr expr -> validateColumn(expr, 'facet.cols') }
    facet.vars.each { ColumnExpr expr -> validateColumn(expr, 'facet.vars') }
  }

  private void validateLayerSemantics(LayerSpec layer, int idx) {
    if (layer.geom == Geom.SMOOTH && layer.stat != Stat.SMOOTH) {
      throw new CharmValidationException(
          "Invalid layer ${idx}: geom SMOOTH requires stat SMOOTH, but was ${layer.stat}"
      )
    }
  }

  private void validateRequiredAesthetics(LayerSpec layer, Aes layerAes, int idx) {
    List<String> required = REQUIRED_AESTHETICS[layer.geom] ?: []
    if (required.isEmpty()) {
      return
    }
    Aes effective = effectiveAes(layer, layerAes)
    Map<String, ColumnExpr> mappings = effective.mappings()
    List<String> missing = required.findAll { String key -> !mappings.containsKey(key) }
    if (!missing.isEmpty()) {
      String inheritText = layer.inheritAes ? 'with inherited plot mappings' : 'without inherited plot mappings'
      throw new CharmValidationException(
          "Layer ${idx} (${layer.geom}) is missing required aesthetics [${missing.join(', ')}] ${inheritText}"
      )
    }
  }

  private Aes effectiveAes(LayerSpec layer, Aes layerAes) {
    Aes effective = layer.inheritAes ? aes.copy() : new Aes()
    if (layerAes != null) {
      effective.apply(layerAes.mappings())
    }
    effective
  }

  private void validateAes(String context, Aes aesValue) {
    aesValue.mappings().each { String key, ColumnExpr value ->
      validateColumn(value, "${context}.${key}")
    }
  }

  private void validateColumn(ColumnExpr expr, String context) {
    if (expr == null) {
      return
    }
    List<String> names = data.columnNames()
    if (!names.contains(expr.columnName())) {
      throw new CharmValidationException(
          "Unknown column '${expr.columnName()}' for ${context}. Available columns: ${names.join(', ')}"
      )
    }
  }

  private static Aes coerceLayerAes(Object value) {
    if (value == null) {
      return null
    }
    if (value instanceof Aes) {
      return (value as Aes).copy()
    }
    if (value instanceof Map) {
      Aes mapped = new Aes()
      mapped.apply(value as Map<String, ?>)
      return mapped
    }
    throw new CharmValidationException(
        "Unsupported layer aes value type '${value.getClass().name}'. Expected Aes or Map."
    )
  }

  private static Position coercePosition(Object value) {
    LayerDsl.parsePosition(value)
  }

  private static Stat coerceStat(Object value, Geom geom) {
    if (value == null) {
      return geom == Geom.SMOOTH ? Stat.SMOOTH : Stat.IDENTITY
    }
    if (value instanceof Stat) {
      return value as Stat
    }
    if (value instanceof CharSequence) {
      try {
        return Stat.valueOf(value.toString().trim().toUpperCase(Locale.ROOT))
      } catch (IllegalArgumentException e) {
        throw new CharmValidationException("Unsupported layer stat '${value}'")
      }
    }
    throw new CharmValidationException("Unsupported layer stat type '${value.getClass().name}'")
  }

  private static boolean parseBooleanOption(Object value, String optionName) {
    if (value == null) {
      throw new CharmValidationException("Layer option '${optionName}' cannot be null")
    }
    if (value instanceof Boolean) {
      return value as boolean
    }
    if (value instanceof CharSequence) {
      String normalized = value.toString().trim().toLowerCase(Locale.ROOT)
      if (normalized == 'true') {
        return true
      }
      if (normalized == 'false') {
        return false
      }
      throw new CharmValidationException(
          "Unsupported boolean value '${value}' for layer option '${optionName}'. Use true or false."
      )
    }
    throw new CharmValidationException(
        "Unsupported type '${value.getClass().name}' for layer option '${optionName}'. Use Boolean or true/false string."
    )
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
   * Theme DSL delegate.
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

    /**
     * Configures legend theme options.
     *
     * @param configure legend closure
     */
    void legend(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MapDsl) Closure<?> configure) {
      theme.legend = configureMap(configure)
    }

    /**
     * Configures axis theme options.
     *
     * @param configure axis closure
     */
    void axis(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MapDsl) Closure<?> configure) {
      theme.axis = configureMap(configure)
    }

    /**
     * Configures text theme options.
     *
     * @param configure text closure
     */
    void text(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MapDsl) Closure<?> configure) {
      theme.text = configureMap(configure)
    }

    /**
     * Configures grid theme options.
     *
     * @param configure grid closure
     */
    void grid(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MapDsl) Closure<?> configure) {
      theme.grid = configureMap(configure)
    }

    private static Map<String, Object> configureMap(
        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MapDsl) Closure<?> configure
    ) {
      MapDsl mapDsl = new MapDsl()
      Closure<?> body = configure.rehydrate(mapDsl, mapDsl, mapDsl)
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      mapDsl.values()
    }
  }

  /**
   * Facet DSL delegate.
   */
  @CompileStatic
  static class FacetDsl {

    private final Facet facet
    private final Cols col = new Cols()
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
     * Returns column namespace proxy.
     *
     * @return col proxy
     */
    Cols getCol() {
      col
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
      values.collect { Object v -> Aes.coerceToColumnExpr(v, 'facet') }
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
   * Annotation DSL delegate.
   */
  @CompileStatic
  static class AnnotationDsl {

    private final List<AnnotationSpec> target

    /**
     * Creates annotation DSL bound to an annotation list.
     *
     * @param target annotation target list
     */
    AnnotationDsl(List<AnnotationSpec> target) {
      this.target = target
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
      target << spec
    }
  }
}
