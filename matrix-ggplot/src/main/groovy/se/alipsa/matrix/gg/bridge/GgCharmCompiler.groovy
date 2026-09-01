package se.alipsa.matrix.gg.bridge

import groovy.transform.CompileDynamic

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.AnnotationSpec
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.ColumnRef
import se.alipsa.matrix.charm.Coord
import se.alipsa.matrix.charm.CssAttributesSpec
import se.alipsa.matrix.charm.Facet
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.GuidesSpec
import se.alipsa.matrix.charm.Labels
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.Mapping
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.charm.ScaleSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.Guide
import se.alipsa.matrix.gg.Guides
import se.alipsa.matrix.gg.Label
import se.alipsa.matrix.gg.aes.Aes as GgAes
import se.alipsa.matrix.gg.aes.AfterScale
import se.alipsa.matrix.gg.aes.AfterStat
import se.alipsa.matrix.gg.aes.CutWidth
import se.alipsa.matrix.gg.aes.Expression
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.NewScaleMarker
import se.alipsa.matrix.gg.scale.Scale as GgScale
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.scale.ScaleXDate
import se.alipsa.matrix.gg.scale.ScaleXDatetime
import se.alipsa.matrix.gg.scale.ScaleXTime
import se.alipsa.matrix.gg.scale.ScaleYDate
import se.alipsa.matrix.gg.scale.ScaleYDatetime
import se.alipsa.matrix.gg.scale.ScaleYTime

/**
 * Adapter bridge that converts gg charts into Charm charts and renders
 * them through the Charm renderer pipeline when the mapped surface is supported.
 */
class GgCharmCompiler {

  private static final Logger log = Logger.getLogger(GgCharmCompiler)

  private static final String AES_X = 'x'
  private static final String AES_Y = 'y'
  private static final String AES_XEND = 'xend'
  private static final String AES_YEND = 'yend'
  private static final String AES_XMIN = 'xmin'
  private static final String AES_XMAX = 'xmax'
  private static final String AES_YMIN = 'ymin'
  private static final String AES_YMAX = 'ymax'
  private static final String AES_COLOR = 'color'
  private static final String AES_FILL = 'fill'
  private static final String AES_SIZE = 'size'
  private static final String AES_SHAPE = 'shape'
  private static final String AES_GROUP = 'group'
  private static final String AES_ALPHA = 'alpha'
  private static final String AES_LINETYPE = 'linetype'
  private static final String AES_LABEL = 'label'
  private static final String AES_TOOLTIP = 'tooltip'
  private static final String AES_WEIGHT = 'weight'

  private static final String PARAM_STAT = 'stat'
  private static final String PARAM_POSITION = 'position'
  private static final String PARAM_MAPPING = 'mapping'
  private static final String PARAM_LAYER_DATA = '__layer_data'
  private static final String PARAM_TITLE = 'title'
  private static final String PARAM_NAME = 'name'
  private static final String PARAM_TYPE = 'type'
  private static final String PARAM_DATE_FORMAT = 'dateFormat'
  private static final String PARAM_DATE_BREAKS = 'dateBreaks'
  private static final String PARAM_ZONE_ID = 'zoneId'

  private static final Set<String> LAYER_PARAM_SKIP_KEYS = [
      PARAM_STAT, PARAM_POSITION, PARAM_MAPPING, PARAM_LAYER_DATA
  ] as Set<String>

  private static final List<String> AESTHETIC_KEYS = [
      AES_X, AES_Y, AES_COLOR, AES_FILL, AES_SIZE, AES_SHAPE, AES_GROUP,
      AES_XEND, AES_YEND, AES_XMIN, AES_XMAX, AES_YMIN, AES_YMAX,
      AES_ALPHA, AES_LINETYPE, AES_LABEL, AES_TOOLTIP, AES_WEIGHT
  ]
  private static final Set<String> PARAM_COLUMN_AESTHETICS = [
      AES_X, AES_Y, AES_XEND, AES_YEND, AES_XMIN, AES_XMAX, AES_YMIN, AES_YMAX, AES_LABEL, AES_TOOLTIP, AES_WEIGHT
  ] as Set<String>

  private final GgCharmMappingRegistry mappingRegistry = new GgCharmMappingRegistry()
  private final CharmRenderer charmRenderer = new CharmRenderer()

  /**
   * Attempts to adapt a gg chart to a Charm chart model.
   *
   * @param ggChart source gg chart
   * @return adaptation result with delegated chart or fallback reasons
   */
  GgCharmCompilation adapt(GgChart ggChart) {
    List<String> reasons = []
    if (ggChart == null) {
      reasons << 'Chart is null'
      return GgCharmCompilation.fallback(reasons)
    }
    Matrix plotData = resolvePlotData(ggChart)
    GgAes plotSourceAes = ggChart.globalAes ?: new GgAes()
    // Guide gate removed in Phase 10 — all guides now delegated to Charm.
    // Theme gate and label gate removed in Phase 9 — all themes and labels now delegated.

    Mapping plotMapping = mapMapping(plotSourceAes, plotData, 'plot', reasons)
    if (!reasons.isEmpty() || plotMapping == null) {
      return GgCharmCompilation.fallback(reasons)
    }

    Facet mappedFacet = mapFacet(ggChart.facet, reasons)
    if (!reasons.isEmpty() || mappedFacet == null) {
      return GgCharmCompilation.fallback(reasons)
    }

    Coord mappedCoord = mapCoord(ggChart.coord, reasons)
    if (!reasons.isEmpty() || mappedCoord == null) {
      return GgCharmCompilation.fallback(reasons)
    }

    List<LayerSpec> mappedLayers = []
    List<AnnotationSpec> mappedAnnotations = []
    Set<Layer> annotationLayers = [] as Set<Layer>
    Set<Layer> mappedLayerIdentities = [] as Set<Layer>
    ggChart.layers.eachWithIndex { Layer layer, int idx ->
      List<AnnotationSpec> annotationSpecs = GgCharmAnnotationMapper.mapAnnotationLayer(layer, idx, reasons)
      if (!annotationSpecs.isEmpty()) {
        mappedAnnotations.addAll(annotationSpecs)
        annotationLayers << layer
      } else {
        LayerSpec mapped = mapLayer(layer, idx, plotMapping, plotData, mappedCoord, reasons)
        if (mapped != null) {
          mappedLayers << mapped
          mappedLayerIdentities << layer
        }
      }
    }
    if (!reasons.isEmpty()) {
      return GgCharmCompilation.fallback(reasons)
    }

    // Layers to skip in component walks: annotation layers + unmapped layers
    Set<Layer> skippedLayers = [] as Set<Layer>
    skippedLayers.addAll(annotationLayers)
    ggChart.layers.each { Layer layer ->
      if (!annotationLayers.contains(layer) && !mappedLayerIdentities.contains(layer)) {
        skippedLayers << layer
      }
    }

    List<GgScale> globalScales = extractGlobalScales(ggChart, skippedLayers)
    ScaleSpec mappedScales = mapScales(globalScales, reasons)
    if (!reasons.isEmpty()) {
      return GgCharmCompilation.fallback(reasons)
    }

    // Apply per-layer scales from NewScaleMarker partitioning
    applyPerLayerScales(ggChart, mappedLayers, skippedLayers)

    Labels mappedLabels = mapLabels(ggChart.labels, ggChart.guides, globalScales)

    GuidesSpec mappedGuides = mapGuides(ggChart.guides, globalScales)

    Chart mappedChart = new Chart(
        plotData,
        plotMapping,
        mappedLayers,
        mappedScales,
        GgCharmThemeMapper.mapTheme(ggChart.theme),
        mappedFacet,
        mappedCoord,
        mappedLabels,
        mappedGuides,
        mappedAnnotations,
        mapCssAttributes(ggChart)
    )
    GgCharmCompilation.delegated(mappedChart)
  }

  /**
   * Renders a gg chart through Charm.
   */
  Svg render(GgChart ggChart) {
    GgCharmCompilation adaptation = adapt(ggChart)
    if (!adaptation.delegated || adaptation.charmChart == null) {
      String reasons = adaptation?.reasons?.join('; ') ?: 'Unknown adaptation failure'
      throw new IllegalStateException("GG to Charm adaptation failed: ${reasons}")
    }
    RenderConfig config = new RenderConfig(width: ggChart.width, height: ggChart.height)
    charmRenderer.render(adaptation.charmChart, config)
  }

  /**
   * Walks the chart's ordered components list and assigns per-layer scales
   * to the correct mapped layers based on NewScaleMarker positions.
   */
  private void applyPerLayerScales(GgChart ggChart, List<LayerSpec> mappedLayers,
                                     Set<Layer> skippedLayers) {
    if (ggChart.components.isEmpty() || !ggChart.components.any { it instanceof NewScaleMarker }) {
      return
    }

    // Walk components in order: partition layers and scales around markers
    // active markers: aesthetic -> true means "next scales for this aesthetic go to per-layer"
    Map<String, Boolean> activeMarkers = [:]
    // Track the mapped layer index (skipping annotation and unmapped layers)
    int layerIdx = -1

    ggChart.components.each { Object component ->
      if (component instanceof Layer) {
        // Only count successfully mapped layers toward mappedLayers index
        if (!skippedLayers.contains(component)) {
          layerIdx++
        }
      } else if (component instanceof NewScaleMarker) {
        // Only activate per-layer partitioning after at least one layer has been seen;
        // markers before any layer are ignored so subsequent scales remain global.
        if (layerIdx >= 0) {
          NewScaleMarker marker = component as NewScaleMarker
          String markerAesthetic = GgCharmMappingRegistry.normalizeAesthetic(marker.aesthetic)
          if (markerAesthetic != null) {
            activeMarkers[markerAesthetic] = true
          }
        }
      } else if (component instanceof GgScale) {
        GgScale ggScale = component as GgScale
        String aesthetic = GgCharmMappingRegistry.normalizeAesthetic(ggScale.aesthetic)
        if (aesthetic != null && activeMarkers[aesthetic] && layerIdx >= 0 && layerIdx < mappedLayers.size()) {
          // This scale applies to the current layer as a per-layer override
          CharmScale charmScale = mapSingleScale(ggScale)
          if (charmScale != null) {
            LayerSpec target = mappedLayers[layerIdx]
            // Need to rebuild LayerSpec with the per-layer scale added
            Map<String, CharmScale> existingScales = new LinkedHashMap<>(target.scales ?: [:])
            existingScales[aesthetic] = charmScale
            LayerSpec updated = new LayerSpec(
                target.geomSpec,
                target.statSpec,
                target.mapping,
                target.inheritMapping,
                target.positionSpec,
                target.params,
                target.styleCallback,
                existingScales
            )
            mappedLayers[layerIdx] = updated
          }
        }
      }
    }
  }

  /**
   * Returns global chart-level scales by walking ordered components and excluding
   * scales that are activated after a new-scale marker for the same aesthetic.
   */
  private static List<GgScale> extractGlobalScales(GgChart ggChart, Set<Layer> skippedLayers = [] as Set) {
    List<GgScale> allScales = ggChart?.scales ?: []
    if (allScales.isEmpty()) {
      return []
    }
    if (ggChart?.components == null || ggChart.components.isEmpty()) {
      return new ArrayList<>(allScales)
    }
    if (!ggChart.components.any { it instanceof NewScaleMarker }) {
      return new ArrayList<>(allScales)
    }

    int componentScaleCount = ggChart.components.count { it instanceof GgScale } as int
    if (componentScaleCount == 0) {
      return new ArrayList<>(allScales)
    }

    Map<String, Boolean> markerActivated = [:]
    boolean layerSeen = false
    List<GgScale> globalScales = []
    ggChart.components.each { Object component ->
      if (component instanceof Layer) {
        if (!skippedLayers.contains(component)) {
          layerSeen = true
        }
      } else if (component instanceof NewScaleMarker) {
        // Only activate after at least one layer; markers before any layer are
        // ignored so subsequent scales are kept as global.
        if (layerSeen) {
          String markerAesthetic = GgCharmMappingRegistry.normalizeAesthetic((component as NewScaleMarker).aesthetic)
          if (markerAesthetic != null) {
            markerActivated[markerAesthetic] = true
          }
        }
      } else if (component instanceof GgScale) {
        GgScale scale = component as GgScale
        String aesthetic = GgCharmMappingRegistry.normalizeAesthetic(scale.aesthetic)
        if (aesthetic == null || Boolean.TRUE != markerActivated[aesthetic]) {
          globalScales << scale
        }
      }
    }
    globalScales
  }

  private LayerSpec mapLayer(
      Layer layer,
      int idx,
      Mapping plotMapping,
      Matrix plotData,
      Coord mappedCoord,
      List<String> reasons
  ) {
    if (layer == null) {
      reasons.add("Layer ${idx} is null".toString())
      return null
    }
    GeomSpec geomSpec = resolveLayerGeomSpec(layer, idx, reasons)
    if (geomSpec == null) {
      return null
    }

    CharmStatType statType = mapLayerStat(geomSpec, layer.stat, idx, reasons)
    if (statType == null) {
      return null
    }

    CharmPositionType positionType = mapLayerPosition(geomSpec, layer.position, idx, reasons)
    if (positionType == null) {
      return null
    }

    Matrix layerData = layer.data ?: plotData
    Mapping layerMapping = mapMapping(layer.aes, layerData, "layer ${idx}", reasons)
    if (!reasons.isEmpty()) {
      return null
    }

    Mapping effectiveMapping = mergeMappings(plotMapping, layerMapping, layer.inheritAes)
    applyParamColumnAesthetics(effectiveMapping, layer.params)

    Map<String, Object> layerParams = normalizeLayerParams(geomSpec.type, layer.params)
    if (layer.data != null) {
      layerParams[PARAM_LAYER_DATA] = layer.data
    }

    Map<String, Object> statParams = deepCopyMap(layer.statParams)
    Map<String, Object> positionParams = deepCopyMap(layer.positionParams)

    CharmGeomType delegatedGeomType = resolveGeomTypeForCoord(geomSpec.type, mappedCoord)

    GeomSpec delegatedGeom = new GeomSpec(
        delegatedGeomType,
        normalizeLayerParams(geomSpec.type, geomSpec.params),
        geomSpec.requiredAes,
        geomSpec.defaultAes,
        geomSpec.defaultStat,
        geomSpec.defaultPosition
    )

    new LayerSpec(
        delegatedGeom,
        StatSpec.of(statType, statParams),
        effectiveMapping,
        layer.inheritAes,
        PositionSpec.of(positionType, positionParams),
        layerParams
    )
  }

  private static CharmGeomType resolveGeomTypeForCoord(CharmGeomType source, Coord coord) {
    if ((coord?.type == CharmCoordType.POLAR || coord?.type == CharmCoordType.RADIAL)
        && (source == CharmGeomType.BAR || source == CharmGeomType.COL)) {
      return CharmGeomType.PIE
    }
    source
  }

  private GeomSpec resolveLayerGeomSpec(Layer layer, int idx, List<String> reasons) {
    if (layer?.geom != null) {
      return mapDeclaredGeom(layer, idx, reasons)
    }

    CharmGeomType inferred = inferGeomType(layer?.stat)
    if (inferred == null) {
      return null
    }
    new GeomSpec(
        inferred,
        [:],
        [],
        [:],
        mappingRegistry.mapStat(layer?.stat ?: StatType.IDENTITY) ?: CharmStatType.IDENTITY,
        mappingRegistry.mapPosition(layer?.position ?: PositionType.IDENTITY) ?: CharmPositionType.IDENTITY
    )
  }

  private static GeomSpec mapDeclaredGeom(Layer layer, int idx, List<String> reasons) {
    GeomSpec mapped = null
    try {
      mapped = layer.geom.toCharmGeomSpec()
    } catch (Exception e) {
      reasons.add("Layer ${idx} geom '${layer.geom.class.simpleName}' could not be mapped: ${e.message}".toString())
    }
    mapped
  }

  private static CharmGeomType inferGeomType(StatType statType) {
    switch (statType ?: StatType.IDENTITY) {
      case StatType.COUNT -> CharmGeomType.BAR
      case StatType.BIN -> CharmGeomType.HISTOGRAM
      case StatType.BOXPLOT -> CharmGeomType.BOXPLOT
      case StatType.SMOOTH -> CharmGeomType.SMOOTH
      case StatType.QUANTILE -> CharmGeomType.QUANTILE
      case StatType.SUMMARY -> CharmGeomType.POINT
      case StatType.DENSITY -> CharmGeomType.DENSITY
      case StatType.YDENSITY -> CharmGeomType.VIOLIN
      case StatType.DENSITY_2D -> CharmGeomType.DENSITY_2D
      case StatType.BIN2D -> CharmGeomType.BIN2D
      case StatType.BIN_HEX -> CharmGeomType.HEX
      case StatType.SUMMARY_HEX -> CharmGeomType.HEX
      case StatType.SUMMARY_2D -> CharmGeomType.BIN2D
      case StatType.CONTOUR -> CharmGeomType.CONTOUR
      case StatType.ECDF -> CharmGeomType.STEP
      case StatType.QQ -> CharmGeomType.QQ
      case StatType.QQ_LINE -> CharmGeomType.QQ_LINE
      case StatType.ELLIPSE -> CharmGeomType.PATH
      case StatType.SUMMARY_BIN -> CharmGeomType.BAR
      case StatType.UNIQUE -> CharmGeomType.POINT
      case StatType.SAMPLE -> CharmGeomType.POINT
      case StatType.FUNCTION -> CharmGeomType.FUNCTION
      case StatType.SF -> CharmGeomType.SF
      case StatType.SF_COORDINATES -> CharmGeomType.POINT
      case StatType.SPOKE -> CharmGeomType.SPOKE
      case StatType.ALIGN -> CharmGeomType.AREA
      default -> null
    }
  }

  private CharmStatType mapLayerStat(GeomSpec geomSpec, StatType ggStat, int idx, List<String> reasons) {
    if (ggStat == null) {
      return geomSpec?.defaultStat ?: CharmStatType.IDENTITY
    }
    CharmStatType mapped = mappingRegistry.mapStat(ggStat)
    if (mapped == null) {
      reasons.add("Layer ${idx} stat '${ggStat}' is not delegated".toString())
      return null
    }
    mapped
  }

  private CharmPositionType mapLayerPosition(GeomSpec geomSpec, PositionType ggPosition, int idx, List<String> reasons) {
    if (ggPosition == null) {
      return geomSpec?.defaultPosition ?: CharmPositionType.IDENTITY
    }
    CharmPositionType mapped = mappingRegistry.mapPosition(ggPosition)
    if (mapped == null) {
      reasons.add("Layer ${idx} position '${ggPosition}' is not delegated".toString())
      return null
    }
    mapped
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

      // Guide type check removed in Phase 10 — all guide types now supported.

      enrichScale(charmScale, scale)

      assignScale(mapped, aesthetic, charmScale, idx, reasons)
    }
    mapped
  }

  /**
   * Maps a single gg scale to a Charm scale for per-layer use.
   */
  private CharmScale mapSingleScale(GgScale scale) {
    if (scale == null) {
      return null
    }
    String aesthetic = GgCharmMappingRegistry.normalizeAesthetic(scale.aesthetic)
    if (aesthetic == null || aesthetic.isBlank()) {
      return null
    }
    CharmScale charmScale = mapScale(scale, aesthetic)
    if (charmScale != null) {
      enrichScale(charmScale, scale)
    }
    charmScale
  }

  private CharmScale mapScale(GgScale scale, String aesthetic) {
    CharmScale fromMethod = invokeToCharmScale(scale)
    if (fromMethod != null) {
      return fromMethod
    }
    CharmScale mapped = mappingRegistry.mapScale(scale, aesthetic)
    if (mapped != null) {
      return mapped
    }
    fallbackScale(scale, aesthetic)
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
      charmScale.params[PARAM_NAME] = ggScale.name
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
    applyTemporalScaleParams(charmScale, ggScale)
  }

  private static void applyTemporalScaleParams(CharmScale charmScale, GgScale ggScale) {
    if (ggScale instanceof ScaleXDate || ggScale instanceof ScaleYDate) {
      ScaleXDate dateScale = ggScale as ScaleXDate
      if (dateScale.dateFormat) {
        charmScale.params[PARAM_DATE_FORMAT] = dateScale.dateFormat
      }
      if (dateScale.dateBreaks) {
        charmScale.params[PARAM_DATE_BREAKS] = dateScale.dateBreaks
      }
      if (dateScale.zoneId) {
        charmScale.params[PARAM_ZONE_ID] = dateScale.zoneId
      }
    }
    if (ggScale instanceof ScaleXDatetime || ggScale instanceof ScaleYDatetime) {
      ScaleXDatetime datetimeScale = ggScale as ScaleXDatetime
      if (datetimeScale.dateFormat) {
        charmScale.params[PARAM_DATE_FORMAT] = datetimeScale.dateFormat
      }
      if (datetimeScale.dateBreaks) {
        charmScale.params[PARAM_DATE_BREAKS] = datetimeScale.dateBreaks
      }
      if (datetimeScale.zoneId) {
        charmScale.params[PARAM_ZONE_ID] = datetimeScale.zoneId
      }
    }
    if (ggScale instanceof ScaleXTime || ggScale instanceof ScaleYTime) {
      ScaleXTime timeScale = ggScale as ScaleXTime
      if (timeScale.timeFormat) {
        charmScale.params['timeFormat'] = timeScale.timeFormat
      }
      if (timeScale.timeBreaks) {
        charmScale.params['timeBreaks'] = timeScale.timeBreaks
      }
    }
  }

  private static CharmScale fallbackScale(GgScale scale, String aesthetic) {
    if (aesthetic == AES_X || aesthetic == AES_Y) {
      String simple = scale?.class?.simpleName?.toLowerCase(Locale.ROOT) ?: ''
      if (simple.contains('datetime')) {
        return CharmScale.datetime()
      }
      if (simple.contains('time')) {
        return CharmScale.time()
      }
      if (simple.contains('date')) {
        return CharmScale.date()
      }
      return scale instanceof ScaleDiscrete ? CharmScale.discrete() : CharmScale.continuous()
    }
    if (aesthetic == AES_COLOR || aesthetic == AES_FILL) {
      return scale instanceof ScaleDiscrete ? CharmScale.discrete() : CharmScale.continuous()
    }
    if (aesthetic == AES_SIZE || aesthetic == AES_ALPHA) {
      return CharmScale.continuous()
    }
    if (aesthetic == AES_SHAPE || aesthetic == AES_LINETYPE || aesthetic == AES_GROUP) {
      return CharmScale.discrete()
    }
    null
  }

  private static void assignScale(ScaleSpec spec, String aesthetic, CharmScale charmScale, int idx, List<String> reasons) {
    switch (aesthetic) {
      case AES_X -> spec.x = charmScale
      case AES_Y -> spec.y = charmScale
      case AES_COLOR -> spec.color = charmScale
      case AES_FILL -> spec.fill = charmScale
      case AES_SIZE -> spec.size = charmScale
      case AES_SHAPE -> spec.shape = charmScale
      case AES_ALPHA -> spec.alpha = charmScale
      case AES_LINETYPE -> spec.linetype = charmScale
      case AES_GROUP -> spec.group = charmScale
      default -> {
        String msg = "Scale ${idx} aesthetic '${aesthetic}' is not delegated"
        reasons.add(msg)
      }
    }
  }

  private Coord mapCoord(se.alipsa.matrix.gg.coord.Coord source, List<String> reasons) {
    CharmCoordType type = mappingRegistry.mapCoordType(source)
    if (type == null) {
      String msg = "Coord '${source?.class?.simpleName}' is not delegated"
      reasons << msg
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

    String msg = "Facet '${source.class.simpleName}' is not delegated"
    reasons << msg
    null
  }

  private static Mapping mergeMappings(Mapping plotMapping, Mapping layerMapping, boolean inheritAes) {
    Mapping merged = inheritAes ? plotMapping.copy() : new Mapping()
    if (layerMapping != null) {
      merged.apply(layerMapping.mappings())
    }
    merged
  }

  private static void applyParamColumnAesthetics(Mapping mapping, Map params) {
    if (mapping == null || params == null) {
      return
    }
    Map<String, Object> mapped = [:]
    PARAM_COLUMN_AESTHETICS.each { String key ->
      Object value = params[key]
      if (value instanceof CharSequence) {
        mapped[key] = value
      }
    }
    if (!mapped.isEmpty()) {
      mapping.apply(mapped)
    }
  }

  private static Map<String, Object> normalizeLayerParams(CharmGeomType geomType, Map params) {
    Map<String, Object> normalized = [:]
    (params ?: [:]).each { Object key, Object value ->
      String name = key
      if (name == null || LAYER_PARAM_SKIP_KEYS.contains(name)) {
        return
      }
      String targetKey = normalizeParamKey(geomType, name)
      normalized[targetKey] = deepCopyValue(value)
    }
    normalized
  }

  private static String normalizeParamKey(CharmGeomType geomType, String key) {
    String normalized = key == 'colour' ? AES_COLOR : key
    if ((geomType == CharmGeomType.LINE || geomType == CharmGeomType.SMOOTH) && (normalized == AES_SIZE || normalized == 'linewidth')) {
      return 'lineWidth'
    }
    if ((geomType == CharmGeomType.COL || geomType == CharmGeomType.BAR) && normalized == 'width') {
      return 'barWidth'
    }
    normalized
  }

  static Map<String, Object> deepCopyMap(Map map) {
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
      Map<Object, Object> copy = [:]
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

  private static Mapping mapMapping(GgAes source, Matrix data, String context, List<String> reasons) {
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
        mapped[key] = value
        continue
      }
      if (value instanceof Factor) {
        mapped[key] = data == null ? null : (value as Factor).addToMatrix(data)
        continue
      }
      if (value instanceof AfterStat) {
        mapped[key] = (value as AfterStat).stat
        continue
      }
      if (value instanceof AfterScale) {
        mapped[key] = (value as AfterScale).aesthetic
        continue
      }
      if (value instanceof CutWidth) {
        mapped[key] = data == null ? null : (value as CutWidth).addToMatrix(data)
        continue
      }
      if (value instanceof Expression) {
        mapped[key] = data == null ? null : (value as Expression).addToMatrix(data)
        continue
      }
      if (value instanceof Closure) {
        mapped[key] = data == null ? null : new Expression(value as Closure<Number>).addToMatrix(data)
        continue
      }
      if (value instanceof Identity) {
        mapped[key] = (value as Identity).value
        continue
      }
      reasons.add("Unsupported ${context} aes '${key}' mapping type '${value.getClass().simpleName}'".toString())
    }
    if (!reasons.isEmpty()) {
      return null
    }
    Mapping mapping = new Mapping()
    mapping.apply(mapped)
    mapping
  }

  private static Matrix resolvePlotData(GgChart chart) {
    if (chart?.data != null) {
      return chart.data
    }
    Matrix layerData = chart?.layers?.findResult { Layer layer -> layer?.data }
    if (layerData != null) {
      return layerData
    }
    new Matrix('gg-plot', [], [], [])
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
    source?.legendTitles?.each { String aesthetic, String title ->
      if (title != null && !title.isBlank()) {
        guideTitles[GgCharmMappingRegistry.normalizeAesthetic(aesthetic)] = title
      }
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
    Object title = null
    if (guideSpec instanceof Guide) {
      title = titleOrName((guideSpec as Guide).params)
    } else if (guideSpec instanceof Map) {
      title = titleOrName(guideSpec as Map)
    }
    title?.toString()
  }

  private static Object titleOrName(Map params) {
    params?.get(PARAM_TITLE) ?: params?.get(PARAM_NAME)
  }

  /**
   * Converts gg Guides to charm GuidesSpec.
   *
   * @param guides gg guides collection
   * @param scales gg scales (for per-scale guide params)
   * @return charm guides spec
   */
  private static GuidesSpec mapGuides(Guides guides, List<GgScale> scales) {
    GuidesSpec result = new GuidesSpec()

    // Map guide specs from Guides collection
    if (guides?.specs) {
      guides.specs.each { String key, Object spec ->
        String normalized = GgCharmMappingRegistry.normalizeAesthetic(key)
        GuideSpec mapped = mapSingleGuide(spec)
        if (mapped != null) {
          result.setSpec(normalized, mapped)
        }
      }
    }

    // Extract per-scale guide params
    (scales ?: []).each { GgScale scale ->
      if (scale?.guide == null) {
        return
      }
      String aesthetic = GgCharmMappingRegistry.normalizeAesthetic(scale.aesthetic)
      if (aesthetic == null || aesthetic.isBlank()) {
        return
      }
      // Don't override if already set from explicit guides
      if (result.getSpec(aesthetic) != null) {
        return
      }

      GuideSpec mapped = mapSingleGuide(scale.guide)
      if (mapped != null) {
        result.setSpec(aesthetic, mapped)
      }
    }

    result
  }

  private static GuideSpec mapSingleGuide(Object spec) {
    if (spec == null) {
      return null
    }

    if (spec instanceof Guide) {
      Guide guide = spec as Guide
      GuideType type = GuideType.fromString(guide.type)
      if (type == null) {
        return null
      }
      Map<String, Object> params = guide.params ? convertGuideParams(guide.params) : [:]
      return new GuideSpec(type, params)
    }

    if (spec instanceof CharSequence) {
      GuideType type = GuideType.fromString(spec.toString())
      if (type != null) {
        return new GuideSpec(type)
      }
      return null
    }

    if (spec == false || spec == Boolean.FALSE) {
      return GuideSpec.none()
    }

    if (spec instanceof Map) {
      Map map = spec as Map
      String typeStr = map[PARAM_TYPE]
      GuideType type = GuideType.fromString(typeStr)
      if (type == null) {
        return null
      }
      Map<String, Object> params = convertGuideParams(map.findAll { k, v -> k != PARAM_TYPE })
      return new GuideSpec(type, params)
    }

    null
  }

  /**
   * Recursively converts Guide objects in params to GuideSpec objects
   * and deep-copies other values.
   */
  private static Map<String, Object> convertGuideParams(Map params) {
    Map<String, Object> result = [:]
    (params ?: [:]).each { Object key, Object value ->
      if (key != null) {
        result[key.toString()] = convertGuideParamValue(value)
      }
    }
    result
  }

  private static Object convertGuideParamValue(Object value) {
    if (value instanceof Guide) {
      GuideSpec mapped = mapSingleGuide(value)
      if (mapped != null) {
        return mapped
      }
      // Fallback: convert to map when guide type is unrecognized
      Guide guideValue = value as Guide
      log.warn('convertGuideParamValue: falling back to map conversion for unmapped Guide ' +
          "with type='${guideValue.type}'")
      return [type: guideValue.type, params: deepCopyMap(guideValue.params)]
    }
    if (value instanceof List) {
      return (value as List).collect { convertGuideParamValue(it) }
    }
    if (value instanceof Map) {
      return convertGuideParams(value as Map)
    }
    value
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
      return guideSpec
    }
    guideSpec
  }

  private static CssAttributesSpec mapCssAttributes(GgChart ggChart) {
    if (ggChart?.cssAttributes == null) {
      return new CssAttributesSpec()
    }
    new CssAttributesSpec(
        enabled: ggChart.cssAttributes.enabled,
        includeClasses: ggChart.cssAttributes.includeClasses,
        includeIds: ggChart.cssAttributes.includeIds,
        includeDataAttributes: ggChart.cssAttributes.includeDataAttributes,
        chartIdPrefix: ggChart.cssAttributes.chartIdPrefix,
        idPrefix: ggChart.cssAttributes.idPrefix
    )
  }

  // Guide gate methods (SUPPORTED_GUIDE_TYPES, hasUnsupportedGuide, extractGuideType)
  // removed in Phase 10 — all guides now delegated via mapGuides().
  // Theme gate methods (isDefaultGrayTheme, sameRect, sameLine) and
  // hasUnsupportedLabels removed in Phase 9 — all themes and labels now delegated.
}
