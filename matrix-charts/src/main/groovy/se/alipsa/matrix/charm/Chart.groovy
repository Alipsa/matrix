package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderBuilder

/**
 * Immutable compiled Charm chart.
 */
@CompileStatic
class Chart {

  private final Matrix data
  private final MappingSpec mapping
  private final List<LayerSpec> layers
  private final ScaleSpec scale
  private final ThemeSpec theme
  private final FacetSpec facet
  private final CoordSpec coord
  private final LabelsSpec labels
  private final GuidesSpec guides
  private final List<AnnotationSpec> annotations
  private final CssAttributesSpec cssAttributes
  private final AnimationSpec animation

  /**
   * Creates a new compiled chart model.
   *
   * @param data source matrix
   * @param mapping plot-level mappings
   * @param layers layer list
   * @param scale scale spec
   * @param theme theme spec
   * @param facet facet spec
   * @param coord coord spec
   * @param labels labels spec
   * @param guides guides spec
   * @param annotations annotation list
   * @param cssAttributes css attribute configuration
   * @param animation animation specification
   */
  Chart(
      Matrix data,
      Mapping mapping,
      List<LayerSpec> layers,
      ScaleSpec scale,
      Theme theme,
      Facet facet,
      Coord coord,
      Labels labels,
      GuidesSpec guides,
      List<AnnotationSpec> annotations,
      CssAttributesSpec cssAttributes = null,
      AnimationSpec animation = null
  ) {
    this.data = data
    this.mapping = toMappingSpec(mapping)
    this.layers = Collections.unmodifiableList(
        layers.collect { Layer layer -> toLayerSpec(layer) }
    )
    this.scale = scale?.copy()
    this.theme = toThemeSpec(theme)
    this.facet = toFacetSpec(facet)
    this.coord = toCoordSpec(coord)
    this.labels = toLabelsSpec(labels)
    this.guides = guides?.copy()
    this.annotations = Collections.unmodifiableList(
        annotations.collect { AnnotationSpec a -> a.copy() }
    )
    this.cssAttributes = cssAttributes?.copy() ?: new CssAttributesSpec()
    this.animation = animation?.copy()
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
   * @return mappings
   */
  MappingSpec getMapping() {
    mapping?.copy()
  }

  /**
   * Returns immutable layers.
   *
   * @return layer list
   */
  List<LayerSpec> getLayers() {
    layers
  }

  /**
   * Returns scale specification.
   *
   * @return scale spec
   */
  ScaleSpec getScale() {
    scale?.copy()
  }

  /**
   * Returns theme specification.
   *
   * @return theme spec
   */
  ThemeSpec getTheme() {
    theme?.copy()
  }

  /**
   * Returns facet specification.
   *
   * @return facet spec
   */
  FacetSpec getFacet() {
    facet?.copy()
  }

  /**
   * Returns coord specification.
   *
   * @return coord spec
   */
  CoordSpec getCoord() {
    coord?.copy()
  }

  /**
   * Returns labels specification.
   *
   * @return labels spec
   */
  LabelsSpec getLabels() {
    labels?.copy()
  }

  /**
   * Returns guides specification.
   *
   * @return guides spec
   */
  GuidesSpec getGuides() {
    guides?.copy()
  }

  /**
   * Returns immutable annotations snapshot.
   *
   * Annotation specs are still mutable DSL/value objects in v1 for compatibility.
   * This getter therefore returns defensive copies to preserve `Chart` immutability.
   *
   * @return annotations list
   */
  List<AnnotationSpec> getAnnotations() {
    Collections.unmodifiableList(annotations.collect { AnnotationSpec a -> a.copy() })
  }

  /**
   * Returns CSS attribute configuration.
   *
   * @return css attributes spec
   */
  CssAttributesSpec getCssAttributes() {
    cssAttributes?.copy()
  }

  /**
   * Returns animation specification.
   *
   * @return animation spec or null
   */
  AnimationSpec getAnimation() {
    animation?.copy()
  }

  private static MappingSpec toMappingSpec(Mapping value) {
    if (value == null) {
      return new MappingSpec()
    }
    if (value instanceof MappingSpec) {
      return (value as MappingSpec).copy()
    }
    MappingSpec converted = new MappingSpec()
    converted.apply(value.mappings())
    converted
  }

  private static LayerSpec toLayerSpec(Layer value) {
    Map<String, Object> frozenParams = deepFreezeParams(value.params)
    Map<String, Scale> copiedScales = value.scales ? value.scales.collectEntries { String k, Scale v -> [(k): v.copy()] } as Map<String, Scale> : [:]
    new LayerSpec(value.geomSpec.copy(), value.statSpec.copy(), value.mapping, value.inheritMapping, value.positionSpec.copy(), frozenParams, value.styleCallback, copiedScales)
  }

  private static Map<String, Object> deepFreezeParams(Map<String, Object> params) {
    if (params == null || params.isEmpty()) {
      return [:]
    }
    Map<String, Object> frozen = new LinkedHashMap<>()
    params.each { String key, Object value ->
      frozen[key] = deepFreezeValue(value)
    }
    frozen
  }

  private static Object deepFreezeValue(Object value) {
    if (value instanceof Map) {
      Map<Object, Object> frozen = new LinkedHashMap<>()
      (value as Map).each { Object k, Object v ->
        frozen[k] = deepFreezeValue(v)
      }
      return Collections.unmodifiableMap(frozen)
    }
    if (value instanceof List) {
      List<Object> frozen = (value as List).collect { Object v -> deepFreezeValue(v) } as List<Object>
      return Collections.unmodifiableList(frozen)
    }
    if (value instanceof Set) {
      Set<Object> frozen = new LinkedHashSet<>()
      (value as Set).each { Object v ->
        frozen << deepFreezeValue(v)
      }
      return Collections.unmodifiableSet(frozen)
    }
    value
  }

  private static ThemeSpec toThemeSpec(Theme value) {
    if (value == null) {
      return new ThemeSpec()
    }
    if (value instanceof ThemeSpec) {
      return (value as ThemeSpec).copy()
    }
    ThemeSpec spec = new ThemeSpec()
    Theme copy = value.copy()
    copy.properties.each { key, val ->
      String k = key as String
      if (k != 'class' && spec.hasProperty(k)) {
        spec.setProperty(k, val)
      }
    }
    spec
  }

  private static FacetSpec toFacetSpec(Facet value) {
    if (value == null) {
      return new FacetSpec()
    }
    if (value instanceof FacetSpec) {
      return (value as FacetSpec).copy()
    }
    new FacetSpec(
        type: value.type,
        rows: new ArrayList<>(value.rows),
        cols: new ArrayList<>(value.cols),
        vars: new ArrayList<>(value.vars),
        ncol: value.ncol,
        nrow: value.nrow,
        params: new LinkedHashMap<>(value.params)
    )
  }

  private static CoordSpec toCoordSpec(Coord value) {
    if (value == null) {
      return new CoordSpec()
    }
    if (value instanceof CoordSpec) {
      return (value as CoordSpec).copy()
    }
    new CoordSpec(type: value.type, params: new LinkedHashMap<>(value.params))
  }

  private static LabelsSpec toLabelsSpec(Labels value) {
    if (value == null) {
      return new LabelsSpec()
    }
    if (value instanceof LabelsSpec) {
      return (value as LabelsSpec).copy()
    }
    new LabelsSpec(
        title: value.title,
        subtitle: value.subtitle,
        caption: value.caption,
        x: value.x,
        y: value.y,
        guides: new LinkedHashMap<>(value.guides)
    )
  }

  /**
   * Returns a fluent render builder for custom configuration.
   *
   * <p>Example:</p>
   * <pre>
   * Svg svg = chart.renderConfig()
   *     .width(640)
   *     .height(420)
   *     .render()
   * </pre>
   *
   * @return render builder
   */
  RenderBuilder renderConfig() {
    new RenderBuilder(this)
  }

  /**
   * Renders this chart to an SVG object.
   *
   * @return SVG model object
   */
  Svg render() {
    try {
      new CharmRenderer().render(this)
    } catch (Exception e) {
      throw new CharmRenderException("Failed to render Charm chart: ${e.message}", e)
    }
  }

  /**
   * Renders this chart at the given dimensions.
   *
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @return SVG model object
   */
  Svg render(int width, int height) {
    renderConfig().width(width).height(height).render()
  }

  /**
   * Writes rendered SVG to the target file path.
   *
   * @param targetPath path to output `.svg` file
   */
  void writeTo(String targetPath) {
    if (targetPath == null || targetPath.trim().isEmpty()) {
      throw new IllegalArgumentException('targetPath cannot be blank')
    }
    writeTo(new File(targetPath))
  }

  /**
   * Writes rendered SVG to a target file.
   *
   * @param targetFile output file
   */
  void writeTo(File targetFile) {
    if (targetFile == null) {
      throw new IllegalArgumentException('targetFile cannot be null')
    }
    targetFile.text = SvgWriter.toXmlPretty(render())
  }
}
