package se.alipsa.matrix.gg.adapter

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.ColumnRef
import se.alipsa.matrix.charm.Coord
import se.alipsa.matrix.charm.Facet
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.Labels
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.charm.ScaleSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.Guide
import se.alipsa.matrix.gg.Guides
import se.alipsa.matrix.gg.Label
import se.alipsa.matrix.gg.aes.Aes as GgAes
import se.alipsa.matrix.gg.aes.AfterStat
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.GgRenderer
import se.alipsa.matrix.gg.scale.Scale as GgScale
import se.alipsa.matrix.gg.theme.ElementLine
import se.alipsa.matrix.gg.theme.ElementRect
import se.alipsa.matrix.gg.theme.Theme as GgTheme
import se.alipsa.matrix.gg.theme.Themes
import java.util.Locale

/**
 * Adapter bridge that converts gg charts into Charm charts and renders
 * them through the Charm renderer pipeline when the mapped surface is supported.
 */
@CompileStatic
class GgCharmAdapter {

  private static final Logger log = Logger.getLogger(GgCharmAdapter)

  private static final Set<CharmStatType> SUPPORTED_STATS = [
      CharmStatType.IDENTITY,
      CharmStatType.COUNT,
      CharmStatType.BIN,
      CharmStatType.BOXPLOT,
      CharmStatType.SMOOTH,
      CharmStatType.DENSITY,
      CharmStatType.YDENSITY
  ] as Set<CharmStatType>

  private static final Set<CharmPositionType> SUPPORTED_POSITIONS = [
      CharmPositionType.IDENTITY,
      CharmPositionType.DODGE,
      CharmPositionType.STACK,
      CharmPositionType.FILL
  ] as Set<CharmPositionType>

  private static final Set<CharmCoordType> SUPPORTED_COORDS = [
      CharmCoordType.CARTESIAN,
      CharmCoordType.FLIP,
      CharmCoordType.FIXED
  ] as Set<CharmCoordType>

  // Intentionally narrower than CharmGeomType.SUPPORTED during phased migration.
  private static final Set<CharmGeomType> DELEGATED_GEOMS = [
      CharmGeomType.POINT,
      CharmGeomType.LINE,
      CharmGeomType.SMOOTH,
      CharmGeomType.COL,
      CharmGeomType.BAR
  ] as Set<CharmGeomType>

  private static final Set<String> LAYER_PARAM_SKIP_KEYS = [
      'stat', 'position', 'mapping', '__layer_data'
  ] as Set<String>

  private static final List<String> AESTHETIC_KEYS = [
      'x', 'y', 'color', 'fill', 'size', 'shape', 'group',
      'xend', 'yend', 'xmin', 'xmax', 'ymin', 'ymax',
      'alpha', 'linetype', 'label', 'weight'
  ] as List<String>

  private final GgCharmMappingRegistry mappingRegistry = new GgCharmMappingRegistry()
  private final CharmRenderer charmRenderer = new CharmRenderer()
  private final GgRenderer ggRenderer = new GgRenderer()

  /**
   * Attempts to adapt a gg chart to a Charm chart model.
   *
   * @param ggChart source gg chart
   * @return adaptation result with delegated chart or fallback reasons
   */
  GgCharmAdaptation adapt(GgChart ggChart) {
    List<String> reasons = []
    if (ggChart == null) {
      reasons << 'Chart is null'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (ggChart.data == null) {
      reasons << 'ggplot(data: null, ...) is not delegated to Charm'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (ggChart.globalAes == null) {
      reasons << 'Missing global aes mapping'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (ggChart.cssAttributes?.enabled) {
      reasons << 'CSS attribute mode is gg-specific and currently uses legacy renderer'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (ggChart.guides?.specs && !ggChart.guides.specs.isEmpty()) {
      reasons << 'Custom guides are currently handled by legacy gg renderer'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (!isDefaultGrayTheme(ggChart.theme)) {
      reasons << 'Non-default gg themes currently use legacy gg renderer for visual parity'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (hasUnsupportedLabels(ggChart.labels)) {
      reasons << 'Subtitle/caption labels are currently handled by legacy gg renderer'
      return GgCharmAdaptation.fallback(reasons)
    }

    Aes plotAes = mapAes(ggChart.globalAes, 'plot', reasons)
    if (!reasons.isEmpty() || plotAes == null) {
      return GgCharmAdaptation.fallback(reasons)
    }

    Facet mappedFacet = mapFacet(ggChart.facet, reasons)
    if (!reasons.isEmpty() || mappedFacet == null) {
      return GgCharmAdaptation.fallback(reasons)
    }
    if (mappedFacet.type != FacetType.NONE) {
      reasons << 'Facet delegation is scheduled for phase 9'
      return GgCharmAdaptation.fallback(reasons)
    }

    Coord mappedCoord = mapCoord(ggChart.coord, reasons)
    if (!reasons.isEmpty() || mappedCoord == null) {
      return GgCharmAdaptation.fallback(reasons)
    }

    List<LayerSpec> mappedLayers = []
    ggChart.layers.eachWithIndex { Layer layer, int idx ->
      LayerSpec mapped = mapLayer(layer, idx, plotAes, reasons)
      if (mapped != null) {
        mappedLayers << mapped
      }
    }
    if (!reasons.isEmpty()) {
      return GgCharmAdaptation.fallback(reasons)
    }

    ScaleSpec mappedScales = mapScales(ggChart.scales, reasons)
    if (!reasons.isEmpty()) {
      return GgCharmAdaptation.fallback(reasons)
    }

    Labels mappedLabels = mapLabels(ggChart.labels, ggChart.guides, ggChart.scales)

    Chart mappedChart = new Chart(
        ggChart.data,
        plotAes,
        mappedLayers,
        mappedScales,
        mapTheme(ggChart.theme),
        mappedFacet,
        mappedCoord,
        mappedLabels,
        []
    )
    GgCharmAdaptation.delegated(mappedChart)
  }

  /**
   * Renders a gg chart through Charm when supported, otherwise via legacy gg renderer.
   */
  Svg render(GgChart ggChart) {
    if (isLegacyRendererForced()) {
      return ggRenderer.render(ggChart)
    }

    GgCharmAdaptation adaptation = adapt(ggChart)
    if (!adaptation.delegated || adaptation.charmChart == null) {
      return ggRenderer.render(ggChart)
    }
    try {
      RenderConfig config = new RenderConfig(width: ggChart.width, height: ggChart.height)
      return charmRenderer.render(adaptation.charmChart, config)
    } catch (Exception e) {
      log.warn("Charm delegation render failed, falling back to legacy gg renderer: ${e.message}", e)
      return ggRenderer.render(ggChart)
    }
  }

  private LayerSpec mapLayer(Layer layer, int idx, Aes plotAes, List<String> reasons) {
    if (layer == null) {
      reasons.add("Layer ${idx} is null".toString())
      return null
    }
    if (layer.geom == null) {
      reasons.add("Layer ${idx} is missing a geom".toString())
      return null
    }

    GeomSpec geomSpec
    try {
      geomSpec = layer.geom.toCharmGeomSpec()
    } catch (Exception e) {
      reasons.add("Layer ${idx} geom '${layer.geom.class.simpleName}' could not be mapped: ${e.message}".toString())
      return null
    }

    if (!(geomSpec.type in DELEGATED_GEOMS)) {
      reasons.add("Layer ${idx} geom '${geomSpec.type}' is mapped but not delegated yet".toString())
      return null
    }

    CharmStatType statType = mapLayerStat(geomSpec.type, layer.stat, idx, reasons)
    if (statType == null) {
      return null
    }

    CharmPositionType positionType = mapLayerPosition(geomSpec.type, layer.position, idx, reasons)
    if (positionType == null) {
      return null
    }

    Aes layerAes = mapAes(layer.aes, "layer ${idx}", reasons)
    if (!reasons.isEmpty()) {
      return null
    }

    Aes effectiveAes = mergeAes(plotAes, layerAes, layer.inheritAes)
    validateRequiredAes(geomSpec, effectiveAes, idx, reasons)
    if (!reasons.isEmpty()) {
      return null
    }

    Map<String, Object> layerParams = normalizeLayerParams(geomSpec.type, layer.params)
    if (layer.data != null) {
      layerParams['__layer_data'] = layer.data
    }

    // Charm smooth stat currently models lm-style regression only.
    if (geomSpec.type == CharmGeomType.SMOOTH && layerParams.containsKey('method')) {
      String method = layerParams['method']?.toString()?.trim()?.toLowerCase(Locale.ROOT)
      if (method != null && !method.isEmpty() && method != 'lm') {
        reasons.add("Layer ${idx} smooth method '${layerParams['method']}' is not delegated yet".toString())
        return null
      }
    }

    Map<String, Object> statParams = deepCopyMap(layer.statParams)
    Map<String, Object> positionParams = deepCopyMap(layer.positionParams)

    GeomSpec delegatedGeom = new GeomSpec(
        geomSpec.type,
        normalizeLayerParams(geomSpec.type, geomSpec.params),
        geomSpec.requiredAes,
        geomSpec.defaultAes,
        geomSpec.defaultStat,
        geomSpec.defaultPosition
    )

    new LayerSpec(
        delegatedGeom,
        StatSpec.of(statType, statParams),
        layerAes,
        layer.inheritAes,
        PositionSpec.of(positionType, positionParams),
        layerParams
    )
  }

  private CharmStatType mapLayerStat(CharmGeomType geomType, StatType ggStat, int idx, List<String> reasons) {
    StatType resolved = ggStat ?: StatType.IDENTITY
    if (geomType == CharmGeomType.SMOOTH) {
      if (resolved != StatType.SMOOTH) {
        reasons.add("Layer ${idx} smooth requires stat SMOOTH".toString())
        return null
      }
      return CharmStatType.SMOOTH
    }
    if (geomType in [CharmGeomType.POINT, CharmGeomType.LINE, CharmGeomType.COL, CharmGeomType.BAR]) {
      if (resolved != StatType.IDENTITY) {
        reasons.add("Layer ${idx} geom '${geomType}' with stat '${resolved}' is not delegated".toString())
        return null
      }
      return CharmStatType.IDENTITY
    }
    CharmStatType mapped = mappingRegistry.mapStat(resolved)
    if (mapped == null || !(mapped in SUPPORTED_STATS)) {
      reasons.add("Layer ${idx} stat '${resolved}' is not delegated".toString())
      return null
    }
    mapped
  }

  private CharmPositionType mapLayerPosition(CharmGeomType geomType, PositionType ggPosition, int idx, List<String> reasons) {
    PositionType resolved = ggPosition ?: PositionType.IDENTITY
    CharmPositionType mapped = mappingRegistry.mapPosition(resolved)
    if (mapped == null || !(mapped in SUPPORTED_POSITIONS)) {
      reasons.add("Layer ${idx} position '${resolved}' is not delegated".toString())
      return null
    }
    if (geomType in [CharmGeomType.POINT, CharmGeomType.LINE, CharmGeomType.SMOOTH] && mapped != CharmPositionType.IDENTITY) {
      reasons.add("Layer ${idx} geom '${geomType}' does not delegate position '${resolved}'".toString())
      return null
    }
    if (geomType in [CharmGeomType.COL, CharmGeomType.BAR] && !(mapped in [CharmPositionType.IDENTITY, CharmPositionType.STACK, CharmPositionType.DODGE, CharmPositionType.FILL])) {
      reasons.add("Layer ${idx} geom '${geomType}' does not delegate position '${resolved}'".toString())
      return null
    }
    mapped
  }

  private static void validateRequiredAes(GeomSpec geomSpec, Aes effectiveAes, int idx, List<String> reasons) {
    Map<String, ?> mappedAes = effectiveAes?.mappings() ?: [:]
    geomSpec.requiredAes.each { String aesName ->
      if (!mappedAes.containsKey(aesName)) {
        reasons.add("Layer ${idx} (${geomSpec.type}) requires '${aesName}' mapping for delegation".toString())
      }
    }
    if (mappedAes.containsKey('group') || mappedAes.containsKey('shape') ||
        mappedAes.containsKey('size') || mappedAes.containsKey('color') ||
        mappedAes.containsKey('fill')) {
      reasons.add("Layer ${idx} (${geomSpec.type}) uses mapped non-positional aesthetics not delegated yet".toString())
    }
  }

  private ScaleSpec mapScales(List<GgScale> ggScales, List<String> reasons) {
    ScaleSpec mapped = new ScaleSpec()
    (ggScales ?: []).eachWithIndex { GgScale scale, int idx ->
      if (scale == null) {
        return
      }
      String aesthetic = GgCharmMappingRegistry.normalizeAesthetic(scale.aesthetic)
      if (aesthetic == null || aesthetic.isBlank()) {
        reasons.add("Scale ${idx} is missing an aesthetic".toString())
        return
      }

      CharmScale charmScale = mapScale(scale, aesthetic)
      if (charmScale == null) {
        reasons.add("Scale ${idx} (${scale.class.simpleName}) for aesthetic '${aesthetic}' is not delegated yet".toString())
        return
      }

      if (hasUnsupportedGuide(scale.guide)) {
        reasons.add("Scale ${idx} guide '${extractGuideType(scale.guide)}' for aesthetic '${aesthetic}' is not delegated yet".toString())
        return
      }

      enrichScale(charmScale, scale)
      assignScale(mapped, aesthetic, charmScale, idx, reasons)
    }
    mapped
  }

  private CharmScale mapScale(GgScale scale, String aesthetic) {
    CharmScale fromMethod = invokeToCharmScale(scale)
    if (fromMethod != null) {
      return fromMethod
    }
    mappingRegistry.mapScale(scale, aesthetic)
  }

  @CompileDynamic
  private static CharmScale invokeToCharmScale(GgScale scale) {
    if (scale == null) {
      return null
    }
    if (!scale.metaClass.respondsTo(scale, 'toCharmScale')) {
      return null
    }
    scale.toCharmScale() as CharmScale
  }

  private static void enrichScale(CharmScale charmScale, GgScale ggScale) {
    if (ggScale.name) {
      charmScale.params['name'] = ggScale.name
    }
    if (ggScale.limits != null) {
      charmScale.params['limits'] = new ArrayList<>(ggScale.limits)
    }
    if (ggScale.expand != null) {
      charmScale.params['expand'] = new ArrayList<>(ggScale.expand)
    }
    if (ggScale.breaks != null) {
      charmScale.breaks = new ArrayList<>(ggScale.breaks)
    }
    if (ggScale.labels != null) {
      charmScale.labels = new ArrayList<>(ggScale.labels)
    }
    if (ggScale.guide != null) {
      charmScale.params['guide'] = normalizeGuide(ggScale.guide)
    }
  }

  private static void assignScale(ScaleSpec spec, String aesthetic, CharmScale charmScale, int idx, List<String> reasons) {
    switch (aesthetic) {
      case 'x' -> spec.x = charmScale
      case 'y' -> spec.y = charmScale
      case 'color' -> spec.color = charmScale
      case 'fill' -> spec.fill = charmScale
      case 'size' -> spec.size = charmScale
      case 'shape' -> spec.shape = charmScale
      case 'alpha' -> spec.alpha = charmScale
      case 'linetype' -> spec.linetype = charmScale
      case 'group' -> spec.group = charmScale
      default -> reasons.add("Scale ${idx} aesthetic '${aesthetic}' is not delegated".toString())
    }
  }

  private Coord mapCoord(se.alipsa.matrix.gg.coord.Coord source, List<String> reasons) {
    CharmCoordType type = mappingRegistry.mapCoordType(source)
    if (type == null) {
      reasons << "Coord '${source?.class?.simpleName}' is not delegated".toString()
      return null
    }
    if (!(type in SUPPORTED_COORDS)) {
      reasons << "Coord '${type}' is not delegated yet".toString()
      return null
    }

    new Coord(
        type: type,
        params: extractCoordParams(source)
    )
  }

  @CompileDynamic
  private static Map<String, Object> extractCoordParams(se.alipsa.matrix.gg.coord.Coord source) {
    if (source == null) {
      return [:]
    }
    Map<String, Object> params = [:]
    source.properties.each { Object key, Object value ->
      String name = key as String
      if (name in ['class', 'metaClass']) {
        return
      }
      if (value == null || value instanceof Closure) {
        return
      }
      if (value instanceof Number || value instanceof CharSequence || value instanceof Boolean || value instanceof List || value instanceof Map) {
        params[name] = deepCopyValue(value)
      }
    }
    params
  }

  private static Facet mapFacet(se.alipsa.matrix.gg.facet.Facet source, List<String> reasons) {
    if (source == null) {
      return new Facet(type: FacetType.NONE)
    }

    if (source instanceof FacetWrap) {
      FacetWrap wrap = source as FacetWrap
      return new Facet(
          type: FacetType.WRAP,
          vars: (wrap.facets ?: []).collect { String name -> new ColumnRef(name) },
          ncol: wrap.ncol,
          nrow: wrap.nrow,
          params: [
              scales      : wrap.scales,
              space       : wrap.space,
              labeller    : wrap.labeller,
              strip       : wrap.strip,
              panelSpacing: wrap.panelSpacing,
              dir         : wrap.dir,
              drop        : wrap.drop
          ]
      )
    }

    if (source instanceof FacetGrid) {
      FacetGrid grid = source as FacetGrid
      return new Facet(
          type: FacetType.GRID,
          rows: (grid.rows ?: []).collect { String name -> new ColumnRef(name) },
          cols: (grid.cols ?: []).collect { String name -> new ColumnRef(name) },
          params: [
              scales      : grid.scales,
              space       : grid.space,
              labeller    : grid.labeller,
              strip       : grid.strip,
              panelSpacing: grid.panelSpacing,
              margins     : grid.margins
          ]
      )
    }

    reasons << "Facet '${source.class.simpleName}' is not delegated".toString()
    null
  }

  private static Aes mergeAes(Aes plotAes, Aes layerAes, boolean inheritAes) {
    Aes merged = inheritAes ? plotAes.copy() : new Aes()
    if (layerAes != null) {
      merged.apply(layerAes.mappings())
    }
    merged
  }

  private static Map<String, Object> normalizeLayerParams(CharmGeomType geomType, Map params) {
    Map<String, Object> normalized = [:]
    (params ?: [:]).each { Object key, Object value ->
      String name = key?.toString()
      if (name == null || LAYER_PARAM_SKIP_KEYS.contains(name)) {
        return
      }
      String targetKey = normalizeParamKey(geomType, name)
      normalized[targetKey] = deepCopyValue(value)
    }
    normalized
  }

  private static String normalizeParamKey(CharmGeomType geomType, String key) {
    String normalized = key == 'colour' ? 'color' : key
    if ((geomType == CharmGeomType.LINE || geomType == CharmGeomType.SMOOTH) && (normalized == 'size' || normalized == 'linewidth')) {
      return 'lineWidth'
    }
    if ((geomType == CharmGeomType.COL || geomType == CharmGeomType.BAR) && normalized == 'width') {
      return 'barWidth'
    }
    normalized
  }

  private static Map<String, Object> deepCopyMap(Map map) {
    Map<String, Object> copy = [:]
    (map ?: [:]).each { Object key, Object value ->
      if (key != null) {
        copy[key.toString()] = deepCopyValue(value)
      }
    }
    copy
  }

  private static Object deepCopyValue(Object value) {
    if (value instanceof Map) {
      Map<Object, Object> copy = new LinkedHashMap<>()
      (value as Map).each { Object k, Object v ->
        copy[k] = deepCopyValue(v)
      }
      return copy
    }
    if (value instanceof List) {
      return (value as List).collect { Object v -> deepCopyValue(v) }
    }
    if (value instanceof Set) {
      Set<Object> copy = new LinkedHashSet<>()
      (value as Set).each { Object v ->
        copy << deepCopyValue(v)
      }
      return copy
    }
    value
  }

  private static Aes mapAes(GgAes source, String context, List<String> reasons) {
    if (source == null) {
      return null
    }
    Map<String, Object> mapped = [:]
    for (String key : AESTHETIC_KEYS) {
      Object value = source.getAestheticValue(key)
      if (value == null) {
        continue
      }
      if (value instanceof CharSequence) {
        mapped[key] = value.toString()
        continue
      }
      if (value instanceof Factor && (value as Factor).value instanceof CharSequence) {
        mapped[key] = (value as Factor).value.toString()
        continue
      }
      if (value instanceof AfterStat) {
        reasons.add("AfterStat mapping '${key}' = after_stat('${(value as AfterStat).stat}') is not delegated yet".toString())
        continue
      }
      reasons.add("Unsupported ${context} aes '${key}' mapping type '${value.getClass().simpleName}'".toString())
    }
    if (!reasons.isEmpty()) {
      return null
    }
    Aes aes = new Aes()
    aes.apply(mapped)
    aes
  }

  private static Theme mapTheme(GgTheme source) {
    Theme theme = new Theme()
    if (source == null) {
      return theme
    }
    theme.legend = ([
        position : source.legendPosition ?: 'right',
        direction: source.legendDirection ?: 'vertical'
    ] as Map<String, Object>)
    theme.axis = ([
        color     : source.axisLineX?.color ?: source.axisLineY?.color ?: '#333333',
        lineWidth : source.axisLineX?.size ?: source.axisLineY?.size ?: 1,
        tickLength: source.axisTickLength ?: 5
    ] as Map<String, Object>)
    theme.text = ([
        color      : source.axisTextX?.color ?: source.axisTextY?.color ?: source.plotTitle?.color ?: '#333333',
        size       : source.axisTextX?.size ?: source.axisTextY?.size ?: source.baseSize ?: 10,
        titleSize  : source.plotTitle?.size ?: ((source.baseSize ?: 11) + 4),
        family     : source.baseFamily,
        lineHeight : source.baseLineHeight,
        subtitleSize: source.plotSubtitle?.size,
        captionSize: source.plotCaption?.size
    ] as Map<String, Object>)
    theme.grid = ([
        color    : source.panelGridMajor?.color ?: '#eeeeee',
        lineWidth: source.panelGridMajor?.size ?: 1,
        minor    : source.panelGridMinor?.color
    ] as Map<String, Object>)
    theme.raw = ([
        background      : source.plotBackground?.fill ?: '#ffffff',
        panelBackground : source.panelBackground?.fill ?: '#ffffff',
        panelBorder     : source.panelBorder?.color,
        themeName       : source.themeName,
        plotMargin      : source.plotMargin,
        panelSpacing    : source.panelSpacing,
        legendKeySize   : source.legendKeySize,
        legendMargin    : source.legendMargin,
        discreteColors  : source.discreteColors,
        gradientColors  : source.gradientColors,
        explicitNulls   : source.explicitNulls
    ] as Map<String, Object>)
    theme
  }

  private static Labels mapLabels(Label source, Guides guides, List<GgScale> scales) {
    Labels labels = new Labels()
    if (source != null) {
      labels.title = source.title
      labels.subtitle = source.subTitle
      labels.caption = source.caption
      if (source.xSet || source.x != null) {
        labels.x = source.x
      }
      if (source.ySet || source.y != null) {
        labels.y = source.y
      }
    }

    Map<String, String> guideTitles = [:]
    if (source?.legendTitle) {
      guideTitles['color'] = source.legendTitle
    }
    guideTitles.putAll(extractGuideTitles(guides))
    guideTitles.putAll(extractGuideTitlesFromScales(scales))
    labels.guides = guideTitles
    labels
  }

  private static Map<String, String> extractGuideTitles(Guides guides) {
    Map<String, String> titles = [:]
    if (guides?.specs == null) {
      return titles
    }
    guides.specs.each { String key, Object spec ->
      String title = extractGuideTitle(spec)
      if (title != null && !title.isBlank()) {
        titles[GgCharmMappingRegistry.normalizeAesthetic(key)] = title
      }
    }
    titles
  }

  private static Map<String, String> extractGuideTitlesFromScales(List<GgScale> scales) {
    Map<String, String> titles = [:]
    (scales ?: []).each { GgScale scale ->
      if (scale == null) {
        return
      }
      String aes = GgCharmMappingRegistry.normalizeAesthetic(scale.aesthetic)
      if (aes == null || aes.isBlank()) {
        return
      }
      String title = extractGuideTitle(scale.guide)
      if (title == null || title.isBlank()) {
        title = scale.name
      }
      if (title != null && !title.isBlank()) {
        titles[aes] = title
      }
    }
    titles
  }

  private static String extractGuideTitle(Object guideSpec) {
    if (guideSpec instanceof Guide) {
      Guide guide = guideSpec as Guide
      Object title = guide.params?.get('title') ?: guide.params?.get('name')
      return title?.toString()
    }
    if (guideSpec instanceof Map) {
      Object title = (guideSpec as Map).get('title') ?: (guideSpec as Map).get('name')
      return title?.toString()
    }
    null
  }

  private static Object normalizeGuide(Object guideSpec) {
    if (guideSpec instanceof Guide) {
      Guide guide = guideSpec as Guide
      return [
          type  : guide.type,
          params: deepCopyMap(guide.params)
      ]
    }
    if (guideSpec instanceof Map) {
      return deepCopyMap(guideSpec as Map)
    }
    if (guideSpec instanceof CharSequence) {
      return guideSpec.toString()
    }
    guideSpec
  }

  private static final Set<String> SUPPORTED_GUIDE_TYPES = [
      'legend', 'colorbar', 'none'
  ] as Set<String>

  private static boolean hasUnsupportedGuide(Object guideSpec) {
    if (guideSpec == null) {
      return false
    }
    String type = extractGuideType(guideSpec)
    type != null && !(type in SUPPORTED_GUIDE_TYPES)
  }

  private static String extractGuideType(Object guideSpec) {
    if (guideSpec instanceof Guide) {
      return (guideSpec as Guide).type
    }
    if (guideSpec instanceof Map) {
      return (guideSpec as Map).get('type')?.toString()
    }
    if (guideSpec instanceof CharSequence) {
      return guideSpec.toString()
    }
    null
  }

  private static boolean hasUnsupportedLabels(Label labels) {
    if (labels == null) {
      return false
    }
    labels.subTitle != null || labels.caption != null
  }

  private static boolean isDefaultGrayTheme(GgTheme theme) {
    if (theme == null) {
      return true
    }
    GgTheme baseline = Themes.gray()
    if (!(theme.themeName == null || theme.themeName == baseline.themeName)) {
      return false
    }
    if (theme.explicitNulls != null && !theme.explicitNulls.isEmpty()) {
      return false
    }
    if (!sameRect(theme.plotBackground, baseline.plotBackground)) {
      return false
    }
    if (!sameRect(theme.panelBackground, baseline.panelBackground)) {
      return false
    }
    if (!sameLine(theme.panelGridMajor, baseline.panelGridMajor)) {
      return false
    }
    if (!sameLine(theme.panelGridMinor, baseline.panelGridMinor)) {
      return false
    }
    if (theme.axisLineX != null || theme.axisLineY != null ||
        theme.axisTicksX != null || theme.axisTicksY != null ||
        theme.axisTextX != null || theme.axisTextY != null ||
        theme.axisTitleX != null || theme.axisTitleY != null ||
        theme.plotSubtitle != null || theme.plotCaption != null ||
        theme.legendTitle != null || theme.legendText != null ||
        theme.legendBackground != null || theme.legendKey != null ||
        theme.stripBackground != null || theme.stripText != null ||
        theme.panelBorder != null) {
      return false
    }
    if (theme.legendPosition != null && theme.legendPosition != 'right') {
      return false
    }
    if (theme.legendDirection != null && theme.legendDirection != 'vertical') {
      return false
    }
    true
  }

  private static boolean sameRect(ElementRect left, ElementRect right) {
    if (left == null || right == null) {
      return left == right
    }
    left.fill == right.fill && left.color == right.color && left.size == right.size && left.linetype == right.linetype
  }

  private static boolean sameLine(ElementLine left, ElementLine right) {
    if (left == null || right == null) {
      return left == right
    }
    left.color == right.color && left.size == right.size && left.linetype == right.linetype && left.lineend == right.lineend
  }

  private static boolean isLegacyRendererForced() {
    String propertyValue = System.getProperty('matrix.charts.gg.delegateToCharm')
    if (propertyValue != null) {
      return !propertyValue.toBoolean()
    }
    String envValue = System.getenv('MATRIX_GG_DELEGATE_TO_CHARM')
    if (envValue != null) {
      return !envValue.toBoolean()
    }
    false
  }
}
