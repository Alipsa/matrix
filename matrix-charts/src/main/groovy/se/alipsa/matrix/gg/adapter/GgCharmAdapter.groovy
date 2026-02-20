package se.alipsa.matrix.gg.adapter

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.AnnotationSpec
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
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
import se.alipsa.matrix.charm.LogticksAnnotationSpec
import se.alipsa.matrix.charm.MapAnnotationSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.RasterAnnotationSpec
import se.alipsa.matrix.charm.RectAnnotationSpec
import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.charm.ScaleSpec
import se.alipsa.matrix.charm.SegmentAnnotationSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.charm.CustomAnnotationSpec
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.charm.util.NumberCoercionUtil
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
import se.alipsa.matrix.gg.geom.GeomCustom
import se.alipsa.matrix.gg.geom.GeomLogticks
import se.alipsa.matrix.gg.geom.GeomMap
import se.alipsa.matrix.gg.geom.GeomRasterAnn
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.GgRenderer
import se.alipsa.matrix.gg.scale.Scale as GgScale
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.theme.Theme as GgTheme
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
      CharmCoordType.FLIP
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
  private final GgRenderer ggRenderer = new GgRenderer() // TODO Phase 15: Remove legacy fallback renderer

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
    // Guide gate removed in Phase 10 — all guides now delegated to Charm.
    // Theme gate and label gate removed in Phase 9 — all themes and labels now delegated.

    Aes plotAes = mapAes(ggChart.globalAes, 'plot', reasons)
    if (!reasons.isEmpty() || plotAes == null) {
      return GgCharmAdaptation.fallback(reasons)
    }

    Facet mappedFacet = mapFacet(ggChart.facet, reasons)
    if (!reasons.isEmpty() || mappedFacet == null) {
      return GgCharmAdaptation.fallback(reasons)
    }

    Coord mappedCoord = mapCoord(ggChart.coord, reasons)
    if (!reasons.isEmpty() || mappedCoord == null) {
      return GgCharmAdaptation.fallback(reasons)
    }

    List<LayerSpec> mappedLayers = []
    List<AnnotationSpec> mappedAnnotations = []
    ggChart.layers.eachWithIndex { Layer layer, int idx ->
      List<AnnotationSpec> annotationSpecs = mapAnnotationLayer(layer, idx, reasons)
      if (!annotationSpecs.isEmpty()) {
        mappedAnnotations.addAll(annotationSpecs)
      } else {
        LayerSpec mapped = mapLayer(layer, idx, plotAes, reasons)
        if (mapped != null) {
          mappedLayers << mapped
        }
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

    GuidesSpec mappedGuides = mapGuides(ggChart.guides, ggChart.scales)

    Chart mappedChart = new Chart(
        ggChart.data,
        plotAes,
        mappedLayers,
        mappedScales,
        mapTheme(ggChart.theme),
        mappedFacet,
        mappedCoord,
        mappedLabels,
        mappedGuides,
        mappedAnnotations,
        mapCssAttributes(ggChart)
    )
    GgCharmAdaptation.delegated(mappedChart)
  }

  /**
   * Renders a gg chart through Charm when supported, otherwise via legacy gg renderer.
   * TODO Phase 15: Remove fallback path entirely; all rendering should go through CharmRenderer.
   */
  Svg render(GgChart ggChart) {
    if (isLegacyRendererForced()) {
      return ggRenderer.render(ggChart) // TODO Phase 15: Remove legacy fallback
    }

    GgCharmAdaptation adaptation = adapt(ggChart)
    if (!adaptation.delegated || adaptation.charmChart == null) {
      return ggRenderer.render(ggChart) // TODO Phase 15: Remove legacy fallback
    }
    try {
      RenderConfig config = new RenderConfig(width: ggChart.width, height: ggChart.height)
      return charmRenderer.render(adaptation.charmChart, config)
    } catch (Exception e) {
      log.warn("Charm delegation render failed, falling back to legacy gg renderer: ${e.message}", e)
      return ggRenderer.render(ggChart) // TODO Phase 15: Remove legacy fallback
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

  private List<AnnotationSpec> mapAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (layer == null || layer.geom == null) {
      return []
    }
    GeomSpec geomSpec
    try {
      geomSpec = layer.geom.toCharmGeomSpec()
    } catch (Exception ignored) {
      return []
    }

    switch (geomSpec.type) {
      case CharmGeomType.CUSTOM:
        return mapCustomAnnotationLayer(layer, idx, reasons)
      case CharmGeomType.LOGTICKS:
        return mapLogticksAnnotationLayer(layer, idx)
      case CharmGeomType.RASTER_ANN:
        return mapRasterAnnotationLayer(layer, idx, reasons)
      case CharmGeomType.MAP:
        if (!layer.inheritAes) {
          return mapMapAnnotationLayer(layer, idx, reasons)
        }
        return []
      case CharmGeomType.TEXT:
      case CharmGeomType.RECT:
      case CharmGeomType.SEGMENT:
        if (!layer.inheritAes) {
          return mapInlineAnnotationLayer(layer, geomSpec.type, idx, reasons)
        }
        return []
      default:
        return []
    }
  }

  private List<AnnotationSpec> mapCustomAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (!(layer.geom instanceof GeomCustom)) {
      reasons.add("Layer ${idx} custom annotation has incompatible geom '${layer.geom.class.simpleName}'".toString())
      return []
    }
    GeomCustom geom = layer.geom as GeomCustom
    if (geom.grob == null) {
      reasons.add("Layer ${idx} custom annotation requires a grob".toString())
      return []
    }
    Matrix bounds = layer.data
    Map<String, Object> annotationParams = filterParams(layer.params, ['grob', 'xmin', 'xmax', 'ymin', 'ymax'] as Set<String>)
    annotationParams['__source'] = 'gg'
    CustomAnnotationSpec spec = new CustomAnnotationSpec(
        grob: geom.grob,
        xmin: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'xmin', 0),
        xmax: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'xmax', 0),
        ymin: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'ymin', 0),
        ymax: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'ymax', 0),
        drawOrder: idx,
        params: annotationParams
    )
    [spec]
  }

  private List<AnnotationSpec> mapLogticksAnnotationLayer(Layer layer, int idx) {
    if (!(layer.geom instanceof GeomLogticks)) {
      return []
    }
    GeomLogticks geom = layer.geom as GeomLogticks
    Map<String, Object> params = [
        base     : geom.base,
        sides    : geom.sides,
        outside  : geom.outside,
        scaled   : geom.scaled,
        short    : geom.shortLength,
        mid      : geom.midLength,
        long     : geom.longLength,
        colour   : geom.colour,
        linewidth: geom.linewidth,
        linetype : geom.linetype,
        alpha    : geom.alpha
    ]
    params.putAll(filterParams(layer.params, [] as Set<String>))
    params['__source'] = 'gg'
    [new LogticksAnnotationSpec(drawOrder: idx, params: params)]
  }

  private List<AnnotationSpec> mapRasterAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (!(layer.geom instanceof GeomRasterAnn)) {
      reasons.add("Layer ${idx} raster annotation has incompatible geom '${layer.geom.class.simpleName}'".toString())
      return []
    }
    GeomRasterAnn geom = layer.geom as GeomRasterAnn
    if (geom.raster == null) {
      reasons.add("Layer ${idx} raster annotation requires raster data".toString())
      return []
    }
    Matrix bounds = layer.data
    RasterAnnotationSpec spec = new RasterAnnotationSpec(
        raster: geom.raster.collect { List<String> row -> row == null ? [] : new ArrayList<>(row) },
        xmin: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'xmin', 0),
        xmax: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'xmax', 0),
        ymin: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'ymin', 0),
        ymax: se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, 'ymax', 0),
        interpolate: geom.interpolate,
        drawOrder: idx,
        params: filterParams(layer.params, ['raster', 'xmin', 'xmax', 'ymin', 'ymax', 'interpolate'] as Set<String>)
    )
    spec.params['__source'] = 'gg'
    [spec]
  }

  private List<AnnotationSpec> mapMapAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (!(layer.geom instanceof GeomMap)) {
      reasons.add("Layer ${idx} map annotation has incompatible geom '${layer.geom.class.simpleName}'".toString())
      return []
    }
    GeomMap geom = layer.geom as GeomMap
    Matrix mapData = geom.map ?: (layer.params?.get('map') instanceof Matrix ? layer.params.get('map') as Matrix : null)
    if (mapData == null) {
      reasons.add("Layer ${idx} annotation_map requires a map Matrix".toString())
      return []
    }
    MapAnnotationSpec spec = new MapAnnotationSpec(
        map: mapData,
        data: layer.data,
        drawOrder: idx,
        mapping: extractMapMapping(layer.aes),
        params: filterParams(layer.params, ['map', 'data', 'mapping'] as Set<String>)
    )
    spec.params['__source'] = 'gg'
    [spec]
  }

  private List<AnnotationSpec> mapInlineAnnotationLayer(
      Layer layer,
      CharmGeomType type,
      int idx,
      List<String> reasons
  ) {
    Matrix data = layer.data
    GgAes aes = layer.aes
    if (data == null || aes == null || data.rowCount() == 0) {
      reasons.add("Layer ${idx} annotation ${type} is missing data/aes bindings".toString())
      return []
    }

    switch (type) {
      case CharmGeomType.TEXT:
        return mapTextAnnotations(data, aes, layer.params, idx, reasons)
      case CharmGeomType.RECT:
        return mapRectAnnotations(data, aes, layer.params, idx, reasons)
      case CharmGeomType.SEGMENT:
        return mapSegmentAnnotations(data, aes, layer.params, idx, reasons)
      default:
        return []
    }
  }

  private List<AnnotationSpec> mapTextAnnotations(
      Matrix data,
      GgAes aes,
      Map params,
      int idx,
      List<String> reasons
  ) {
    String xCol = resolveColumnRef(aes, 'x')
    String yCol = resolveColumnRef(aes, 'y')
    String labelCol = resolveColumnRef(aes, 'label')
    if (xCol == null || yCol == null || labelCol == null) {
      reasons.add("Layer ${idx} text annotation requires x, y, and label columns".toString())
      return []
    }

    Map<String, Object> style = filterParams(params, ['x', 'y', 'label'] as Set<String>)
    List<AnnotationSpec> specs = []
    for (int row = 0; row < data.rowCount(); row++) {
      Number x = coerceNumber(data[row, xCol])
      Number y = coerceNumber(data[row, yCol])
      String label = data[row, labelCol]?.toString()
      if (x == null || y == null || label == null) {
        continue
      }
      specs << new TextAnnotationSpec(x: x, y: y, label: label, drawOrder: idx, params: new LinkedHashMap<>(style))
    }
    specs
  }

  private List<AnnotationSpec> mapRectAnnotations(
      Matrix data,
      GgAes aes,
      Map params,
      int idx,
      List<String> reasons
  ) {
    String xminCol = resolveColumnRef(aes, 'xmin')
    String xmaxCol = resolveColumnRef(aes, 'xmax')
    String yminCol = resolveColumnRef(aes, 'ymin')
    String ymaxCol = resolveColumnRef(aes, 'ymax')
    if (xminCol == null || xmaxCol == null || yminCol == null || ymaxCol == null) {
      reasons.add("Layer ${idx} rect annotation requires xmin, xmax, ymin, and ymax columns".toString())
      return []
    }

    Map<String, Object> style = filterParams(params, ['xmin', 'xmax', 'ymin', 'ymax'] as Set<String>)
    List<AnnotationSpec> specs = []
    for (int row = 0; row < data.rowCount(); row++) {
      Number xmin = coerceNumber(data[row, xminCol])
      Number xmax = coerceNumber(data[row, xmaxCol])
      Number ymin = coerceNumber(data[row, yminCol])
      Number ymax = coerceNumber(data[row, ymaxCol])
      if (xmin == null || xmax == null || ymin == null || ymax == null) {
        continue
      }
      specs << new RectAnnotationSpec(
          xmin: xmin,
          xmax: xmax,
          ymin: ymin,
          ymax: ymax,
          drawOrder: idx,
          params: new LinkedHashMap<>(style)
      )
    }
    specs
  }

  private List<AnnotationSpec> mapSegmentAnnotations(
      Matrix data,
      GgAes aes,
      Map params,
      int idx,
      List<String> reasons
  ) {
    String xCol = resolveColumnRef(aes, 'x')
    String yCol = resolveColumnRef(aes, 'y')
    String xendCol = resolveColumnRef(aes, 'xend')
    String yendCol = resolveColumnRef(aes, 'yend')
    if (xCol == null || yCol == null || xendCol == null || yendCol == null) {
      reasons.add("Layer ${idx} segment annotation requires x, y, xend, and yend columns".toString())
      return []
    }

    Map<String, Object> style = filterParams(params, ['x', 'y', 'xend', 'yend'] as Set<String>)
    List<AnnotationSpec> specs = []
    for (int row = 0; row < data.rowCount(); row++) {
      Number x = coerceNumber(data[row, xCol])
      Number y = coerceNumber(data[row, yCol])
      Number xend = coerceNumber(data[row, xendCol])
      Number yend = coerceNumber(data[row, yendCol])
      if (x == null || y == null || xend == null || yend == null) {
        continue
      }
      specs << new SegmentAnnotationSpec(
          x: x,
          y: y,
          xend: xend,
          yend: yend,
          drawOrder: idx,
          params: new LinkedHashMap<>(style)
      )
    }
    specs
  }

  private static Map<String, String> extractMapMapping(GgAes aes) {
    Map<String, String> mapping = [:]
    if (aes == null) {
      return mapping
    }
    String mapId = resolveColumnRef(aes, 'map_id')
    if (mapId != null) {
      mapping['map_id'] = mapId
    }
    String x = resolveColumnRef(aes, 'x')
    if (x != null) {
      mapping['x'] = x
    }
    String y = resolveColumnRef(aes, 'y')
    if (y != null) {
      mapping['y'] = y
    }
    String group = resolveColumnRef(aes, 'group')
    if (group != null) {
      mapping['group'] = group
    }
    String fill = resolveColumnRef(aes, 'fill')
    if (fill != null) {
      mapping['fill'] = fill
    }
    String color = resolveColumnRef(aes, 'color')
    if (color != null) {
      mapping['color'] = color
    }
    mapping
  }

  private static String resolveColumnRef(GgAes aes, String aesthetic) {
    if (aes == null) {
      return null
    }
    Object value = aes.getAestheticValue(aesthetic)
    if (value instanceof CharSequence) {
      return value.toString()
    }
    if (value instanceof Factor && (value as Factor).value instanceof CharSequence) {
      return (value as Factor).value.toString()
    }
    null
  }

  private static Number coerceNumber(Object value) {
    if (value instanceof Number) {
      return value as Number
    }
    NumberCoercionUtil.coerceToBigDecimal(value)
  }

  private static Map<String, Object> filterParams(Map source, Set<String> excluded) {
    Map<String, Object> params = deepCopyMap(source)
    excluded.each { String key ->
      params.remove(key)
    }
    params
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

      // Guide type check removed in Phase 10 — all guide types now supported.

      enrichScale(charmScale, scale)

      if (scale instanceof ScaleDiscrete && scale.labels != null && !scale.labels.isEmpty()
          && aesthetic in ['x', 'y']) {
        reasons.add("Scale ${idx} (${scale.class.simpleName}) discrete scale with custom labels for '${aesthetic}' is not delegated yet".toString())
        return
      }

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
    // Map typed elements field-by-field from gg theme to charm theme
    theme.plotBackground = mapRect(source.plotBackground)
    theme.plotTitle = mapText(source.plotTitle)
    theme.plotSubtitle = mapText(source.plotSubtitle)
    theme.plotCaption = mapText(source.plotCaption)
    theme.plotMargin = source.plotMargin != null ? new ArrayList<>(source.plotMargin) : null

    theme.panelBackground = mapRect(source.panelBackground)
    theme.panelBorder = mapRect(source.panelBorder)
    theme.panelGridMajor = mapLine(source.panelGridMajor)
    theme.panelGridMinor = mapLine(source.panelGridMinor)
    theme.panelSpacing = source.panelSpacing != null ? new ArrayList<>(source.panelSpacing) : null

    theme.axisLineX = mapLine(source.axisLineX)
    theme.axisLineY = mapLine(source.axisLineY)
    theme.axisTicksX = mapLine(source.axisTicksX)
    theme.axisTicksY = mapLine(source.axisTicksY)
    theme.axisTextX = mapText(source.axisTextX)
    theme.axisTextY = mapText(source.axisTextY)
    theme.axisTitleX = mapText(source.axisTitleX)
    theme.axisTitleY = mapText(source.axisTitleY)
    theme.axisTickLength = source.axisTickLength

    theme.legendPosition = source.legendPosition ?: 'right'
    theme.legendDirection = source.legendDirection ?: 'vertical'
    theme.legendBackground = mapRect(source.legendBackground)
    theme.legendKey = mapRect(source.legendKey)
    theme.legendKeySize = source.legendKeySize != null ? new ArrayList<>(source.legendKeySize) : null
    theme.legendTitle = mapText(source.legendTitle)
    theme.legendText = mapText(source.legendText)
    theme.legendMargin = source.legendMargin != null ? new ArrayList<>(source.legendMargin) : null

    theme.stripBackground = mapRect(source.stripBackground)
    theme.stripText = mapText(source.stripText)

    theme.discreteColors = source.discreteColors != null ? new ArrayList<>(source.discreteColors) : null
    theme.gradientColors = source.gradientColors != null ? new ArrayList<>(source.gradientColors) : null
    theme.baseFamily = source.baseFamily
    theme.baseSize = source.baseSize
    theme.baseLineHeight = source.baseLineHeight
    theme.themeName = source.themeName
    theme.explicitNulls = source.explicitNulls != null ? new HashSet<>(source.explicitNulls) : new HashSet<>()
    theme
  }

  private static se.alipsa.matrix.charm.theme.ElementText mapText(se.alipsa.matrix.gg.theme.ElementText source) {
    if (source == null) {
      return null
    }
    new se.alipsa.matrix.charm.theme.ElementText(
        family: source.family,
        face: source.face,
        size: source.size,
        color: source.color,
        hjust: source.hjust,
        vjust: source.vjust,
        angle: source.angle,
        lineheight: source.lineheight,
        margin: source.margin != null ? new ArrayList<>(source.margin) : null
    )
  }

  private static se.alipsa.matrix.charm.theme.ElementLine mapLine(se.alipsa.matrix.gg.theme.ElementLine source) {
    if (source == null) {
      return null
    }
    new se.alipsa.matrix.charm.theme.ElementLine(
        color: source.color,
        size: source.size,
        linetype: source.linetype,
        lineend: source.lineend
    )
  }

  private static se.alipsa.matrix.charm.theme.ElementRect mapRect(se.alipsa.matrix.gg.theme.ElementRect source) {
    if (source == null) {
      return null
    }
    new se.alipsa.matrix.charm.theme.ElementRect(
        fill: source.fill,
        color: source.color,
        size: source.size,
        linetype: source.linetype
    )
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
      if (scale?.guide == null) return
      String aesthetic = GgCharmMappingRegistry.normalizeAesthetic(scale.aesthetic)
      if (aesthetic == null || aesthetic.isBlank()) return
      // Don't override if already set from explicit guides
      if (result.getSpec(aesthetic) != null) return

      GuideSpec mapped = mapSingleGuide(scale.guide)
      if (mapped != null) {
        result.setSpec(aesthetic, mapped)
      }
    }

    result
  }

  private static GuideSpec mapSingleGuide(Object spec) {
    if (spec == null) return null

    if (spec instanceof Guide) {
      Guide guide = spec as Guide
      GuideType type = GuideType.fromString(guide.type)
      if (type == null) return null
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
      String typeStr = map['type']?.toString()
      GuideType type = GuideType.fromString(typeStr)
      if (type == null) return null
      Map<String, Object> params = convertGuideParams(map.findAll { k, v -> k != 'type' })
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
      if (mapped != null) return mapped
      // Fallback: convert to map when guide type is unrecognized
      Guide guideValue = value as Guide
      log.warn("convertGuideParamValue: falling back to map conversion for unmapped Guide " +
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
      return guideSpec.toString()
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

  // TODO Phase 15: Remove isLegacyRendererForced() and associated system/env property support
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
