package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Shared helpers for Charm geom renderers.
 */
@CompileStatic
class GeomUtils {

  private static final List<String> DEFAULT_PALETTE = [
      '#F8766D', '#C49A00', '#53B400',
      '#00C094', '#00B6EB', '#A58AFF', '#FB61D7'
  ].asImmutable()

  private GeomUtils() {
    // Utility class
  }

  /**
   * Resolve stroke color from mapped color scale or layer params.
   */
  static String resolveStroke(RenderContext context, LayerSpec layer, LayerData datum) {
    if (datum.color != null && context.colorScale != null) {
      return context.colorScale.colorFor(datum.color)
    }
    if (layer.params.color != null) {
      return layer.params.color.toString()
    }
    '#1f77b4'
  }

  /**
   * Resolve fill color from mapped fill/color scale or layer params.
   */
  static String resolveFill(RenderContext context, LayerSpec layer, LayerData datum) {
    if (datum.fill != null && context.fillScale != null) {
      return context.fillScale.colorFor(datum.fill)
    }
    if (datum.color != null && context.colorScale != null) {
      return context.colorScale.colorFor(datum.color)
    }
    if (layer.params.fill != null) {
      return layer.params.fill.toString()
    }
    '#1f77b4'
  }

  /**
   * Resolve alpha from datum/layer/default.
   */
  static BigDecimal resolveAlpha(LayerSpec layer, LayerData datum, BigDecimal defaultValue = 1.0) {
    NumberCoercionUtil.coerceToBigDecimal(datum.alpha) ?:
        NumberCoercionUtil.coerceToBigDecimal(layer.params.alpha) ?: defaultValue
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
      BigDecimal x1 = NumberCoercionUtil.coerceToBigDecimal(a.x)
      BigDecimal x2 = NumberCoercionUtil.coerceToBigDecimal(b.x)
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
    def cssConfig = context.chart.cssAttributes
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
    if (context == null || !isCssEnabled(context) || !context.chart.cssAttributes.includeIds) {
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
    context?.chart?.cssAttributes?.enabled ?: false
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
    def cssConfig = context.chart.cssAttributes
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
}
