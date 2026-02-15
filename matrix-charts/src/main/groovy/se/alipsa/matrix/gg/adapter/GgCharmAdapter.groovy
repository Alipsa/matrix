package se.alipsa.matrix.gg.adapter

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Coord
import se.alipsa.matrix.charm.CoordType
import se.alipsa.matrix.charm.Facet
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.Geom as CharmGeom
import se.alipsa.matrix.charm.Labels
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.Position as CharmPosition
import se.alipsa.matrix.charm.ScaleSpec
import se.alipsa.matrix.charm.Stat as CharmStat
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.Label
import se.alipsa.matrix.gg.aes.Aes as GgAes
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.GgRenderer
import se.alipsa.matrix.gg.scale.Scale as GgScale
import se.alipsa.matrix.gg.theme.ElementLine
import se.alipsa.matrix.gg.theme.ElementRect
import se.alipsa.matrix.gg.theme.Theme as GgTheme
import se.alipsa.matrix.gg.theme.Themes

/**
 * Adapter bridge that converts supported gg charts into Charm charts and renders
 * them through the Charm renderer pipeline.
 *
 * Unsupported constructs automatically fall back to legacy gg rendering to keep
 * the public gg API behavior stable.
 */
@CompileStatic
class GgCharmAdapter {

  private static final Set<String> SUPPORTED_POINT_PARAM_KEYS = [
      'color', 'colour', 'fill', 'size', 'alpha'
  ] as Set<String>
  private static final Set<String> SUPPORTED_LINE_PARAM_KEYS = [
      'color', 'colour', 'size', 'linewidth', 'alpha', 'linetype'
  ] as Set<String>
  private static final Set<String> SUPPORTED_SMOOTH_PARAM_KEYS = [
      'color', 'colour', 'size', 'linewidth', 'alpha', 'linetype', 'method'
  ] as Set<String>
  private static final Set<String> SUPPORTED_HISTOGRAM_PARAM_KEYS = [
      'color', 'colour', 'fill', 'alpha', 'bins'
  ] as Set<String>
  private static final Set<String> SUPPORTED_COL_PARAM_KEYS = [
      'color', 'colour', 'fill', 'alpha', 'width'
  ] as Set<String>
  private static final Set<CharmGeom> DELEGATED_GEOMS = [
      CharmGeom.POINT,
      CharmGeom.LINE,
      CharmGeom.SMOOTH,
      CharmGeom.HISTOGRAM,
      CharmGeom.COL,
      CharmGeom.BAR
  ] as Set<CharmGeom>

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
    if (ggChart.facet != null) {
      reasons << 'Facet delegation is not enabled yet for gg adapter'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (ggChart.scales != null && !ggChart.scales.isEmpty()) {
      reasons << 'Explicit gg scales currently use legacy gg renderer'
      return GgCharmAdaptation.fallback(reasons)
    }
    if (ggChart.coord != null && !(ggChart.coord instanceof CoordCartesian)) {
      CoordType coordType = mappingRegistry.mapCoordType(ggChart.coord)
      reasons.add("Coord '${ggChart.coord.class.simpleName}' mapped as ${coordType ?: 'unsupported'} but renderer parity is pending".toString())
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

    List<LayerSpec> mappedLayers = []
    ggChart.layers.eachWithIndex { Layer layer, int idx ->
      if (layer == null) {
        reasons.add("Layer ${idx} is null".toString())
        return
      }
      if (layer.data != null) {
        reasons.add("Layer ${idx} uses layer-specific data".toString())
        return
      }
      CharmGeom geom = mappingRegistry.mapGeom(layer.geom)
      if (geom == null) {
        reasons.add("Layer ${idx} geom '${layer.geom?.class?.simpleName}' is unsupported".toString())
        return
      }
      if (!DELEGATED_GEOMS.contains(geom)) {
        reasons.add("Layer ${idx} geom '${geom}' is mapped but not delegated yet".toString())
        return
      }

      CharmStat stat = mapLayerStat(geom, layer.stat, idx, reasons)
      if (stat == null) {
        return
      }
      CharmPosition position = mapLayerPosition(geom, layer.position, idx, reasons)
      if (position == null) {
        return
      }

      Aes layerAes = mapAes(layer.aes, "layer ${idx}", reasons)
      Aes effectiveAes = mergeAes(plotAes, layerAes, layer.inheritAes)
      validateDelegatedAes(geom, effectiveAes, idx, reasons)
      Map<String, Object> params = normalizeLayerParams(geom, layer.params, idx, reasons)
      if (!reasons.isEmpty()) {
        return
      }
      mappedLayers << new LayerSpec(geom, stat, layerAes, layer.inheritAes, position, params)
    }
    if (!reasons.isEmpty()) {
      return GgCharmAdaptation.fallback(reasons)
    }

    Chart mappedChart = new Chart(
        ggChart.data,
        plotAes,
        mappedLayers,
        new ScaleSpec(),
        mapTheme(ggChart.theme),
        new Facet(type: FacetType.NONE),
        new Coord(type: CoordType.CARTESIAN),
        mapLabels(ggChart.labels),
        []
    )
    GgCharmAdaptation.delegated(mappedChart)
  }

  /**
   * Renders a gg chart through Charm when supported, otherwise via legacy gg renderer.
   *
   * @param ggChart source gg chart
   * @return rendered SVG
   */
  Svg render(GgChart ggChart) {
    GgCharmAdaptation adaptation = adapt(ggChart)
    if (!adaptation.delegated || adaptation.charmChart == null) {
      return ggRenderer.render(ggChart)
    }
    RenderConfig config = new RenderConfig(width: ggChart.width, height: ggChart.height)
    charmRenderer.render(adaptation.charmChart, config)
  }

  private static Aes mergeAes(Aes plotAes, Aes layerAes, boolean inheritAes) {
    Aes merged = inheritAes ? plotAes.copy() : new Aes()
    if (layerAes != null) {
      merged.apply(layerAes.mappings())
    }
    merged
  }

  private static Map<String, Object> normalizeLayerParams(CharmGeom geom, Map params, int idx, List<String> reasons) {
    Set<String> allowed = switch (geom) {
      case CharmGeom.POINT -> SUPPORTED_POINT_PARAM_KEYS
      case CharmGeom.LINE -> SUPPORTED_LINE_PARAM_KEYS
      case CharmGeom.SMOOTH -> SUPPORTED_SMOOTH_PARAM_KEYS
      case CharmGeom.HISTOGRAM -> SUPPORTED_HISTOGRAM_PARAM_KEYS
      case CharmGeom.COL, CharmGeom.BAR -> SUPPORTED_COL_PARAM_KEYS
      default -> null
    }
    if (allowed == null) {
      reasons.add("Layer ${idx} geom '${geom}' has no delegated param mapping".toString())
      return [:]
    }

    Map<String, Object> normalized = [:]
    (params ?: [:]).each { Object key, Object value ->
      String name = key?.toString()
      if (name == null) {
        return
      }
      if (!allowed.contains(name)) {
        reasons.add("Layer ${idx} ${geom} param '${name}' is not delegated yet".toString())
        return
      }
      String targetKey = normalizeParamKey(geom, name)
      normalized[targetKey] = deepCopyValue(value)
    }

    if (geom == CharmGeom.SMOOTH && normalized.containsKey('method')) {
      String method = normalized['method']?.toString()?.trim()?.toLowerCase(Locale.ROOT)
      if (method != null && !method.isEmpty() && method != 'lm') {
        reasons.add("Layer ${idx} smooth method '${normalized['method']}' is not delegated yet".toString())
      }
    }
    if (geom == CharmGeom.HISTOGRAM) {
      Object binsValue = normalized['bins']
      if (binsValue != null && (!(binsValue instanceof Number) || (binsValue as Number).intValue() <= 0)) {
        reasons.add("Layer ${idx} histogram bins must be a positive number".toString())
      }
    }
    normalized
  }

  private static String normalizeParamKey(CharmGeom geom, String key) {
    String normalized = key == 'colour' ? 'color' : key
    if ((geom == CharmGeom.LINE || geom == CharmGeom.SMOOTH) && (normalized == 'size' || normalized == 'linewidth')) {
      return 'lineWidth'
    }
    if ((geom == CharmGeom.COL || geom == CharmGeom.BAR) && normalized == 'width') {
      return 'barWidth'
    }
    normalized
  }

  private CharmStat mapLayerStat(CharmGeom geom, StatType ggStat, int idx, List<String> reasons) {
    StatType resolved = ggStat ?: StatType.IDENTITY
    if (geom == CharmGeom.SMOOTH) {
      if (resolved != StatType.SMOOTH) {
        reasons.add("Layer ${idx} smooth requires stat SMOOTH".toString())
        return null
      }
      return CharmStat.SMOOTH
    }
    if (geom == CharmGeom.HISTOGRAM) {
      if (!(resolved in [StatType.BIN, StatType.IDENTITY])) {
        reasons.add("Layer ${idx} histogram stat '${resolved}' is not delegated".toString())
        return null
      }
      return CharmStat.IDENTITY
    }
    if (geom in [CharmGeom.POINT, CharmGeom.LINE, CharmGeom.COL, CharmGeom.BAR]) {
      if (resolved != StatType.IDENTITY) {
        reasons.add("Layer ${idx} geom '${geom}' with stat '${resolved}' is not delegated".toString())
        return null
      }
      return CharmStat.IDENTITY
    }
    CharmStat mapped = mappingRegistry.mapStat(resolved)
    if (mapped == null) {
      reasons.add("Layer ${idx} stat '${resolved}' is not delegated".toString())
    }
    mapped
  }

  private CharmPosition mapLayerPosition(CharmGeom geom, PositionType ggPosition, int idx, List<String> reasons) {
    PositionType resolved = ggPosition ?: PositionType.IDENTITY
    CharmPosition mapped = mappingRegistry.mapPosition(resolved)
    if (mapped == null) {
      reasons.add("Layer ${idx} position '${resolved}' is not delegated".toString())
      return null
    }
    if (geom in [CharmGeom.POINT, CharmGeom.LINE, CharmGeom.SMOOTH, CharmGeom.HISTOGRAM] && mapped != CharmPosition.IDENTITY) {
      reasons.add("Layer ${idx} geom '${geom}' does not delegate position '${resolved}'".toString())
      return null
    }
    if (geom in [CharmGeom.COL, CharmGeom.BAR] && !(mapped in [CharmPosition.IDENTITY, CharmPosition.STACK])) {
      reasons.add("Layer ${idx} geom '${geom}' does not delegate position '${resolved}'".toString())
      return null
    }
    mapped
  }

  private static void validateDelegatedAes(CharmGeom geom, Aes effectiveAes, int idx, List<String> reasons) {
    boolean needsX = geom in [CharmGeom.POINT, CharmGeom.LINE, CharmGeom.SMOOTH, CharmGeom.HISTOGRAM, CharmGeom.COL, CharmGeom.BAR]
    boolean needsY = geom in [CharmGeom.POINT, CharmGeom.LINE, CharmGeom.SMOOTH, CharmGeom.COL, CharmGeom.BAR]
    if (needsX && effectiveAes?.x == null) {
      reasons.add("Layer ${idx} (${geom}) requires x mapping for delegation".toString())
    }
    if (needsY && effectiveAes?.y == null) {
      reasons.add("Layer ${idx} (${geom}) requires y mapping for delegation".toString())
    }
    if (geom == CharmGeom.HISTOGRAM && effectiveAes?.y != null) {
      reasons.add("Layer ${idx} histogram with mapped y is not delegated yet".toString())
    }
    if (effectiveAes?.group != null || effectiveAes?.shape != null ||
        effectiveAes?.size != null || effectiveAes?.color != null || effectiveAes?.fill != null) {
      reasons.add("Layer ${idx} (${geom}) uses mapped non-positional aesthetics not delegated yet".toString())
    }
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
    for (String key : ['x', 'y', 'color', 'fill', 'size', 'shape', 'group']) {
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
    theme.legend = ([position: source.legendPosition ?: 'right'] as Map<String, Object>)
    theme.axis = ([
        color    : source.axisLineX?.color ?: source.axisLineY?.color ?: '#333333',
        lineWidth: source.axisLineX?.size ?: source.axisLineY?.size ?: 1
    ] as Map<String, Object>)
    theme.text = ([
        color    : source.axisTextX?.color ?: source.axisTextY?.color ?: source.plotTitle?.color ?: '#333333',
        size     : source.axisTextX?.size ?: source.axisTextY?.size ?: source.baseSize ?: 10,
        titleSize: source.plotTitle?.size ?: ((source.baseSize ?: 11) + 4)
    ] as Map<String, Object>)
    theme.grid = ([
        color    : source.panelGridMajor?.color ?: '#eeeeee',
        lineWidth: source.panelGridMajor?.size ?: 1
    ] as Map<String, Object>)
    theme.raw = ([
        background     : source.plotBackground?.fill ?: '#ffffff',
        panelBackground: source.panelBackground?.fill ?: '#ffffff',
        themeName      : source.themeName
    ] as Map<String, Object>)
    theme
  }

  private static Labels mapLabels(Label source) {
    Labels labels = new Labels()
    if (source == null) {
      return labels
    }
    labels.title = source.title
    if (source.xSet || source.x != null) {
      labels.x = source.x
    }
    if (source.ySet || source.y != null) {
      labels.y = source.y
    }
    if (source.legendTitle) {
      labels.guides = [color: source.legendTitle]
    }
    labels
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
}
