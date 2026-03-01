package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.StyleOverride
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataRowAccess
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.charm.util.ColorUtil

/**
 * Shared helpers for Charm geom renderers.
 */
@CompileStatic
class GeomUtils {

  private static final List<String> DEFAULT_PALETTE = [
      '#F8766D', '#C49A00', '#53B400',
      '#00C094', '#00B6EB', '#A58AFF', '#FB61D7'
  ].asImmutable()
  private static final List<String> DEFAULT_SHAPES = [
      'circle', 'square', 'triangle', 'diamond', 'plus', 'x', 'cross'
  ].asImmutable()
  private static final List<String> DEFAULT_LINETYPES = [
      'solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash'
  ].asImmutable()

  private GeomUtils() {
    // Utility class
  }

  /**
   * Resolve stroke color: style callback > layer param > mapped aesthetic > default.
   */
  static String resolveStroke(RenderContext context, LayerSpec layer, LayerData datum,
                              String defaultColor = '#1f77b4') {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.color != null) {
      String raw = override.color.toString()
      return ColorUtil.normalizeColor(raw) ?: raw
    }
    if (layer.params.color != null) {
      String raw = layer.params.color.toString()
      return ColorUtil.normalizeColor(raw) ?: raw
    }
    if (datum.color != null && context.colorScale != null) {
      return context.colorScale.colorFor(datum.color)
    }
    defaultColor
  }

  /**
   * Resolve fill color: style callback > layer param > mapped fill > mapped color fallback > default.
   */
  static String resolveFill(RenderContext context, LayerSpec layer, LayerData datum) {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.fill != null) {
      String raw = override.fill.toString()
      return ColorUtil.normalizeColor(raw) ?: raw
    }
    if (layer.params.fill != null) {
      String raw = layer.params.fill.toString()
      return ColorUtil.normalizeColor(raw) ?: raw
    }
    if (datum.fill != null && context.fillScale != null) {
      return context.fillScale.colorFor(datum.fill)
    }
    if (datum.color != null && context.colorScale != null) {
      return context.colorScale.colorFor(datum.color)
    }
    '#1f77b4'
  }

  /**
   * Resolve alpha: style callback > layer param > datum > default.
   */
  static BigDecimal resolveAlpha(LayerSpec layer, LayerData datum, BigDecimal defaultValue = 1.0) {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.alpha != null) {
      return override.alpha
    }
    ValueConverter.asBigDecimal(layer.params.alpha) ?:
        ValueConverter.asBigDecimal(datum.alpha) ?: defaultValue
  }

  /**
   * Resolve alpha: style callback > layer param > mapped alpha scale > datum > default.
   */
  static BigDecimal resolveAlpha(RenderContext context, LayerSpec layer, LayerData datum, BigDecimal defaultValue = 1.0) {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.alpha != null) {
      return override.alpha
    }
    BigDecimal layerAlpha = ValueConverter.asBigDecimal(layer.params.alpha)
    if (layerAlpha != null) {
      return layerAlpha
    }
    if (datum.alpha != null && context?.alphaScale != null) {
      BigDecimal scaled = context.alphaScale.transform(datum.alpha)
      if (scaled != null) {
        return scaled.min(1.0).max(0.0)
      }
    }
    ValueConverter.asBigDecimal(datum.alpha) ?: defaultValue
  }

  /**
   * Resolve line width: style callback (size) > layer param > datum > default.
   */
  static BigDecimal resolveLineWidth(LayerSpec layer, LayerData datum, BigDecimal defaultValue = 1.0) {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.size != null) {
      return override.size
    }
    ValueConverter.asBigDecimal(layer.params.lineWidth) ?:
        ValueConverter.asBigDecimal(layer.params.linewidth) ?:
        ValueConverter.asBigDecimal(layer.params.size) ?:
        ValueConverter.asBigDecimal(datum.size) ?: defaultValue
  }

  /**
   * Resolve line width: style callback (size) > layer param > mapped size scale > datum > default.
   */
  static BigDecimal resolveLineWidth(RenderContext context, LayerSpec layer, LayerData datum, BigDecimal defaultValue = 1.0) {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.size != null) {
      return override.size
    }
    BigDecimal layerSize = ValueConverter.asBigDecimal(layer.params.lineWidth) ?:
        ValueConverter.asBigDecimal(layer.params.linewidth) ?:
        ValueConverter.asBigDecimal(layer.params.size)
    if (layerSize != null) {
      return layerSize
    }
    if (datum.size != null && context?.sizeScale != null) {
      BigDecimal scaled = context.sizeScale.transform(datum.size)
      if (scaled != null) {
        return scaled.max(0.0)
      }
    }
    ValueConverter.asBigDecimal(datum.size) ?: defaultValue
  }

  /**
   * Resolves linetype: style callback > layer param > mapped linetype scale > datum.
   */
  static Object resolveLinetype(RenderContext context, LayerSpec layer, LayerData datum) {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.linetype != null) {
      return override.linetype
    }
    if (layer.params.linetype != null) {
      return layer.params.linetype
    }
    if (datum.linetype != null && context?.linetypeScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale linetypeScale = context.linetypeScale as DiscreteCharmScale
      String mapped = mappedValue(linetypeScale, datum.linetype)
      if (mapped != null) {
        return mapped
      }
      int idx = linetypeScale.levels.indexOf(datum.linetype.toString())
      if (idx >= 0) {
        return DEFAULT_LINETYPES[idx % DEFAULT_LINETYPES.size()]
      }
    }
    datum.linetype
  }

  /**
   * Resolve point shape: style callback > layer param > mapped shape scale > datum > default.
   */
  static String resolveShape(RenderContext context, LayerSpec layer, LayerData datum, String defaultValue = 'circle') {
    StyleOverride override = cachedStyleOverride(layer, datum)
    if (override?.shape != null) {
      return override.shape
    }
    if (layer.params.shape != null) {
      return layer.params.shape.toString()
    }
    if (datum.shape != null && context?.shapeScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale shapeScale = context.shapeScale as DiscreteCharmScale
      String mapped = mappedValue(shapeScale, datum.shape)
      if (mapped != null) {
        return mapped
      }
      int idx = shapeScale.levels.indexOf(datum.shape.toString())
      if (idx >= 0) {
        return DEFAULT_SHAPES[idx % DEFAULT_SHAPES.size()]
      }
    }
    if (datum.shape != null) {
      return datum.shape.toString()
    }
    defaultValue
  }

  /**
   * Resolves the tooltip text for a rendered element.
   *
   * Resolution order:
   * 1) disabled -> null
   * 2) layer template
   * 3) mapped tooltip value
   * 4) auto-generated text (only when enabled)
   */
  static String resolveTooltip(LayerSpec layer, LayerData datum) {
    Boolean enabled = ValueConverter.asBoolean(layer?.params?.tooltipEnabled)
    if (Boolean.FALSE == enabled) {
      return null
    }

    Object templateValue = layer?.params?.tooltipTemplate
    if (templateValue instanceof CharSequence) {
      String rendered = renderTooltipTemplate(templateValue.toString(), datum)
      if (rendered != null && !rendered.isBlank()) {
        return rendered
      }
    }

    String mapped = datum?.tooltip
    if (mapped != null && !mapped.isBlank()) {
      return mapped
    }

    if (Boolean.TRUE == enabled) {
      return autoTooltip(datum)
    }
    null
  }

  /**
   * Adds an SVG {@code <title>} child when tooltip text is available.
   */
  static void addTooltip(SvgElement element, String tooltip) {
    if (element == null || tooltip == null || tooltip.isBlank()) {
      return
    }
    element.addTitle(tooltip)
  }

  /**
   * Evaluates the style callback once per datum and caches the result.
   */
  private static StyleOverride cachedStyleOverride(LayerSpec layer, LayerData datum) {
    if (layer.styleCallback == null) {
      return null
    }
    if (datum.meta.containsKey('__styleOverride')) {
      return datum.meta['__styleOverride'] as StyleOverride
    }
    StyleOverride override = new StyleOverride()
    Object dataRef = datum.meta['__data']
    Matrix data = dataRef instanceof Matrix ? dataRef as Matrix : null
    int rowIndex = datum.rowIndex
    if (data != null && rowIndex >= 0 && rowIndex < data.rowCount()) {
      Row row = data.row(rowIndex)
      layer.styleCallback.call(row, override)
    }
    datum.meta['__styleOverride'] = override
    override
  }

  private static String mappedValue(DiscreteCharmScale scale, Object value) {
    if (scale == null || value == null) {
      return null
    }
    Object values = scale.scaleSpec?.params?.values
    if (values instanceof Map) {
      Object direct = (values as Map)[value]
      if (direct == null) {
        direct = (values as Map)[value.toString()]
      }
      return direct?.toString()
    }
    if (values instanceof List) {
      int idx = scale.levels.indexOf(value.toString())
      if (idx >= 0) {
        List listValues = values as List
        if (listValues.isEmpty()) {
          return null
        }
        return listValues[idx % listValues.size()]?.toString()
      }
    }
    null
  }

  /**
   * Returns stroke-dasharray for a linetype string.
   */
  static String dashArray(Object linetype) {
    switch (linetype?.toString()?.toLowerCase()) {
      case 'dashed' -> '8,4'
      case 'dotted' -> '2,2'
      case 'dotdash' -> '2,2,8,2'
      case 'longdash' -> '12,4'
      case 'twodash' -> '4,2,8,2'
      case 'solid' -> null
      default -> null
    }
  }

  /**
   * Stable grouping for line/area-like geoms.
   */
  static Map<Object, List<LayerData>> groupSeries(List<LayerData> data) {
    Map<Object, List<LayerData>> groups = new LinkedHashMap<>()
    data.each { LayerData datum ->
      Object key = datum.group ?: datum.color ?: datum.fill ?: '__all__'
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }
    groups
  }

  /**
   * Sort by numeric x when possible, otherwise preserve insertion order.
   */
  static List<LayerData> sortByX(List<LayerData> data) {
    List<LayerData> sorted = new ArrayList<>(data)
    sorted.sort { LayerData a, LayerData b ->
      BigDecimal x1 = ValueConverter.asBigDecimal(a.x)
      BigDecimal x2 = ValueConverter.asBigDecimal(b.x)
      if (x1 != null && x2 != null) {
        return x1 <=> x2
      }
      0
    }
    sorted
  }

  /**
   * Default color fallback for unmapped discrete values.
   */
  static String defaultColor(Object value) {
    int index = value?.hashCode()?.abs() ?: 0
    DEFAULT_PALETTE[index % DEFAULT_PALETTE.size()]
  }

  /**
   * Draws a point shape.
   */
  static List<SvgElement> drawPoint(G group, BigDecimal cx, BigDecimal cy, BigDecimal radius,
                                    String fill, String stroke, String shape, BigDecimal alpha) {
    BigDecimal size = radius * 2
    BigDecimal half = size / 2
    List<SvgElement> elements = []

    switch (shape?.toLowerCase()) {
      case 'square' -> {
        def rect = group.addRect(size, size).x(cx - half).y(cy - half).fill(fill).stroke(stroke).styleClass('charm-point')
        applyAlpha(rect, alpha)
        elements << rect
      }
      case 'plus', 'cross' -> {
        def h = group.addLine(cx - half, cy, cx + half, cy).stroke(stroke).styleClass('charm-point')
        def v = group.addLine(cx, cy - half, cx, cy + half).stroke(stroke).styleClass('charm-point')
        applyStrokeAlpha(h, alpha)
        applyStrokeAlpha(v, alpha)
        elements << h
        elements << v
      }
      case 'x' -> {
        def d1 = group.addLine(cx - half, cy - half, cx + half, cy + half).stroke(stroke).styleClass('charm-point')
        def d2 = group.addLine(cx - half, cy + half, cx + half, cy - half).stroke(stroke).styleClass('charm-point')
        applyStrokeAlpha(d1, alpha)
        applyStrokeAlpha(d2, alpha)
        elements << d1
        elements << d2
      }
      case 'triangle' -> {
        BigDecimal h = size * (3.0 as BigDecimal).sqrt() / 2
        BigDecimal topY = cy - h * 2 / 3
        BigDecimal bottomY = cy + h / 3
        BigDecimal leftX = cx - half
        BigDecimal rightX = cx + half
        String d = "M ${cx} ${topY} L ${leftX} ${bottomY} L ${rightX} ${bottomY} Z"
        def path = group.addPath().d(d).fill(fill).stroke(stroke).styleClass('charm-point')
        applyAlpha(path, alpha)
        elements << path
      }
      case 'diamond' -> {
        String d = "M ${cx} ${cy - half} L ${cx + half} ${cy} L ${cx} ${cy + half} L ${cx - half} ${cy} Z"
        def path = group.addPath().d(d).fill(fill).stroke(stroke).styleClass('charm-point')
        applyAlpha(path, alpha)
        elements << path
      }
      default -> {
        def circle = group.addCircle().cx(cx).cy(cy).r(radius).fill(fill).stroke(stroke).styleClass('charm-point')
        applyAlpha(circle, alpha)
        elements << circle
      }
    }
    elements
  }

  /**
   * Applies gg-compatible CSS class/id/data-* attributes to an element when enabled.
   */
  static void applyCssAttributes(SvgElement element,
                                 RenderContext context,
                                 String geomType,
                                 int elementIndex,
                                 LayerData datum = null) {
    if (element == null || context == null || !isCssEnabled(context)) {
      return
    }
    def cssConfig = context.cssAttributes
    if (cssConfig.includeClasses) {
      String geomToken = normalizeIdToken(geomType)
      if (!geomToken.isEmpty()) {
        element.styleClass("gg-${geomToken}")
      }
    }
    if (cssConfig.includeIds) {
      String elementId = generateElementId(context, geomType, elementIndex)
      if (elementId != null) {
        element.id(elementId)
      }
    }
    if (cssConfig.includeDataAttributes) {
      applyDataAttributes(element, context, datum, elementIndex)
    }
  }

  /**
   * Generates a gg-compatible element id.
   */
  static String generateElementId(RenderContext context, String geomType, int elementIndex) {
    if (context == null || !isCssEnabled(context) || !context.cssAttributes.includeIds) {
      return null
    }

    String prefix = resolveIdPrefix(context)
    String normalizedGeom = normalizeIdToken(geomType)
    boolean faceted = context.chart?.facet?.type != FacetType.NONE
    if (faceted && context.panelRow != null && context.panelCol != null) {
      return "${prefix}-panel-${context.panelRow}-${context.panelCol}-layer-${context.layerIndex}-${normalizedGeom}-${elementIndex}"
    }
    "${prefix}-layer-${context.layerIndex}-${normalizedGeom}-${elementIndex}"
  }

  /**
   * Normalizes a token for id use.
   */
  static String normalizeIdToken(String token) {
    if (token == null) {
      return ''
    }
    token.toLowerCase().replace('_', '-')
  }

  private static boolean isCssEnabled(RenderContext context) {
    context?.cssAttributes?.enabled ?: false
  }

  private static void applyDataAttributes(SvgElement element, RenderContext context, LayerData datum, int elementIndex) {
    if (datum?.x != null) {
      element.addAttribute('data-x', datum.x.toString())
    }
    if (datum?.y != null) {
      element.addAttribute('data-y', datum.y.toString())
    }
    if (datum?.group != null) {
      element.addAttribute('data-group', datum.group.toString())
    }
    int dataRow = datum != null ? datum.rowIndex : elementIndex
    element.addAttribute('data-row', Integer.toString(dataRow))
    element.addAttribute('data-layer', Integer.toString(context.layerIndex))
    boolean faceted = context.chart?.facet?.type != FacetType.NONE
    if (faceted && context.panelRow != null && context.panelCol != null) {
      element.addAttribute('data-panel', "${context.panelRow}-${context.panelCol}")
    }
  }

  private static String resolveIdPrefix(RenderContext context) {
    def cssConfig = context.cssAttributes
    String chartPrefix = normalizeIdPrefixOrNull(cssConfig.chartIdPrefix)
    if (chartPrefix != null) {
      return chartPrefix
    }
    String fallback = normalizeIdPrefixOrNull(cssConfig.idPrefix)
    fallback ?: 'gg'
  }

  private static String normalizeIdPrefixOrNull(String prefix) {
    if (prefix == null || prefix.trim().isEmpty()) {
      return null
    }
    String normalized = prefix.toLowerCase()
        .replaceAll(/[\s_]+/, '-')
        .replaceAll(/[^a-z0-9-]/, '')
        .replaceAll(/-+/, '-')
        .replaceAll(/^-|-$/, '')
    if (normalized.isEmpty() || Character.isDigit(normalized.charAt(0))) {
      return null
    }
    normalized
  }

  private static void applyAlpha(SvgElement element, BigDecimal alpha) {
    if (alpha < 1.0) {
      element.addAttribute('fill-opacity', alpha)
    }
  }

  private static void applyStrokeAlpha(SvgElement element, BigDecimal alpha) {
    if (alpha < 1.0) {
      element.addAttribute('stroke-opacity', alpha)
    }
  }

  private static String renderTooltipTemplate(String template, LayerData datum) {
    if (template == null || template.isBlank()) {
      return null
    }
    String rendered = template
    Map<String, Object> values = tooltipValues(datum)
    values.each { String key, Object value ->
      rendered = rendered.replace("{${key}}", valueToTooltip(value))
    }
    rendered
  }

  private static String autoTooltip(LayerData datum) {
    if (datum == null) {
      return null
    }
    Map<String, Object> row = LayerDataRowAccess.rowMap(datum)
    if (row != null && !row.isEmpty()) {
      return row.collect { String key, Object value -> "${key}: ${valueToTooltip(value)}" }.join(', ')
    }
    Map<String, Object> values = tooltipValues(datum)
    Map<String, Object> filtered = values.findAll { String key, Object value ->
      value != null && !key.startsWith('__')
    }
    if (filtered.isEmpty()) {
      return null
    }
    filtered.collect { String key, Object value -> "${key}: ${valueToTooltip(value)}" }.join(', ')
  }

  private static Map<String, Object> tooltipValues(LayerData datum) {
    Map<String, Object> values = [:]
    if (datum == null) {
      return values
    }
    values.x = datum.x
    values.y = datum.y
    values.color = datum.color
    values.fill = datum.fill
    values.xend = datum.xend
    values.yend = datum.yend
    values.xmin = datum.xmin
    values.xmax = datum.xmax
    values.ymin = datum.ymin
    values.ymax = datum.ymax
    values.size = datum.size
    values.shape = datum.shape
    values.alpha = datum.alpha
    values.linetype = datum.linetype
    values.group = datum.group
    values.label = datum.label
    values.weight = datum.weight
    values.tooltip = datum.tooltip
    values.rowIndex = datum.rowIndex
    LayerDataRowAccess.rowMap(datum).each { String key, Object value ->
      values[key] = value
    }
    values
  }

  private static String valueToTooltip(Object value) {
    value == null ? '' : value.toString()
  }
}
