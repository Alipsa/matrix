package se.alipsa.matrix.charm.render.annotation

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.groovy.svg.io.SvgReader
import se.alipsa.matrix.charm.AnnotationConstants
import se.alipsa.matrix.charm.AnnotationSpec
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CustomAnnotationSpec
import se.alipsa.matrix.charm.LogticksAnnotationSpec
import se.alipsa.matrix.charm.MapAnnotationSpec
import se.alipsa.matrix.charm.RasterAnnotationSpec
import se.alipsa.matrix.charm.RectAnnotationSpec
import se.alipsa.matrix.charm.SegmentAnnotationSpec
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.geom.GeomUtils
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

/**
 * Dispatch and rendering helpers for charm annotations.
 */
@CompileStatic
class AnnotationEngine {

  private static final Logger log = Logger.getLogger(AnnotationEngine)

  private AnnotationEngine() {
    // Utility class
  }

  /**
   * Render one annotation into the panel data layer.
   *
   * @param dataLayer panel data group
   * @param context render context
   * @param annotation annotation spec
   * @param annotationIndex index within the chart annotation list
   */
  static void render(G dataLayer, RenderContext context, AnnotationSpec annotation, int annotationIndex) {
    switch (annotation) {
      case TextAnnotationSpec -> renderText(dataLayer, context, annotation as TextAnnotationSpec, annotationIndex)
      case RectAnnotationSpec -> renderRect(dataLayer, context, annotation as RectAnnotationSpec, annotationIndex)
      case SegmentAnnotationSpec -> renderSegment(dataLayer, context, annotation as SegmentAnnotationSpec, annotationIndex)
      case CustomAnnotationSpec -> renderCustom(dataLayer, context, annotation as CustomAnnotationSpec, annotationIndex)
      case LogticksAnnotationSpec -> renderLogticks(dataLayer, context, annotation as LogticksAnnotationSpec, annotationIndex)
      case RasterAnnotationSpec -> renderRaster(dataLayer, context, annotation as RasterAnnotationSpec, annotationIndex)
      case MapAnnotationSpec -> renderMap(dataLayer, context, annotation as MapAnnotationSpec, annotationIndex)
      default -> log.warn("Unsupported annotation spec type '${annotation?.class?.name}'")
    }
  }

  private static void renderText(G dataLayer, RenderContext context, TextAnnotationSpec spec, int annotationIndex) {
    BigDecimal x = context.xScale?.transform(spec.x)
    BigDecimal y = context.yScale?.transform(spec.y)
    if (x == null || y == null || spec.label == null) {
      return
    }

    BigDecimal sizeValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.size)
    BigDecimal size = sizeValue == null ? 11 : sizeValue
    String family = spec.params?.family?.toString() ?: 'sans-serif'
    String fontface = spec.params?.fontface?.toString() ?: 'normal'
    BigDecimal hjustValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.hjust)
    BigDecimal hjust = hjustValue == null ? 0.5 : hjustValue
    BigDecimal vjustValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.vjust)
    BigDecimal vjust = vjustValue == null ? 0.5 : vjustValue
    BigDecimal nudgeXValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.nudge_x)
    BigDecimal nudgeX = nudgeXValue == null ? 0 : nudgeXValue
    BigDecimal nudgeYValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.nudge_y)
    BigDecimal nudgeY = nudgeYValue == null ? 0 : nudgeYValue
    BigDecimal angleValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.angle)
    BigDecimal angle = angleValue == null ? 0 : angleValue
    BigDecimal alphaValue = NumberCoercionUtil.coerceToBigDecimal(spec.params?.alpha)
    BigDecimal alpha = alphaValue == null ? 1.0 : alphaValue
    String color = normalizeColor(spec.params?.color ?: spec.params?.colour ?: '#000000')
    BigDecimal drawX = x + nudgeX * 10
    BigDecimal drawY = y - nudgeY * 10

    def text = dataLayer.addText(spec.label)
        .x(drawX)
        .y(drawY)
        .fill(color)
        .fontSize(size)
        .styleClass('charm-annotation-text')

    String baseline = dominantBaseline(vjust)
    text.addAttribute('font-family', family)
    text.addAttribute('text-anchor', textAnchor(hjust))
    text.addAttribute('dominant-baseline', baseline)
    text.addAttribute('alignment-baseline', baseline)
    applyFontFace(text as SvgElement, fontface)
    if (angle != 0) {
      text.addAttribute('transform', "rotate(${-angle}, ${drawX}, ${drawY})")
    }
    if (alpha < 1.0) {
      text.addAttribute('fill-opacity', alpha)
    }

    LayerData datum = new LayerData(x: spec.x, y: spec.y, rowIndex: annotationIndex)
    GeomUtils.applyCssAttributes(
        text,
        context,
        CharmGeomType.TEXT.name(),
        annotationElementIndexBase(annotationIndex),
        datum
    )
  }

  private static void renderRect(G dataLayer, RenderContext context, RectAnnotationSpec spec, int annotationIndex) {
    Map<String, BigDecimal> bounds = resolveBounds(context, spec.xmin, spec.xmax, spec.ymin, spec.ymax)
    BigDecimal width = (bounds.xmaxPx - bounds.xminPx).abs()
    BigDecimal height = (bounds.ymaxPx - bounds.yminPx).abs()
    if (width <= 0 || height <= 0) {
      return
    }

    String fill = normalizeColor(spec.params?.fill ?: '#595959')
    String stroke = normalizeColor(spec.params?.color ?: spec.params?.colour ?: 'none')
    BigDecimal alpha = NumberCoercionUtil.coerceToBigDecimal(spec.params?.alpha) ?: 1.0
    BigDecimal strokeWidth = NumberCoercionUtil.coerceToBigDecimal(spec.params?.size) ?:
        NumberCoercionUtil.coerceToBigDecimal(spec.params?.linewidth) ?: 0.5

    def rect = dataLayer.addRect(width, height)
        .x([bounds.xminPx, bounds.xmaxPx].min())
        .y([bounds.yminPx, bounds.ymaxPx].min())
        .fill(fill)
        .stroke(stroke)
        .addAttribute('stroke-width', strokeWidth)
        .styleClass('charm-annotation-rect')
    String dashArray = GeomUtils.dashArray(spec.params?.linetype ?: spec.params?.lty)
    if (dashArray != null) {
      rect.addAttribute('stroke-dasharray', dashArray)
    }
    if (alpha < 1.0) {
      rect.addAttribute('fill-opacity', alpha)
      if (stroke != 'none') {
        rect.addAttribute('stroke-opacity', alpha)
      }
    }

    LayerData datum = new LayerData(x: spec.xmin, y: spec.ymin, rowIndex: annotationIndex)
    GeomUtils.applyCssAttributes(
        rect,
        context,
        CharmGeomType.RECT.name(),
        annotationElementIndexBase(annotationIndex),
        datum
    )
  }

  private static void renderSegment(G dataLayer, RenderContext context, SegmentAnnotationSpec spec, int annotationIndex) {
    BigDecimal x = context.xScale?.transform(spec.x)
    BigDecimal y = context.yScale?.transform(spec.y)
    BigDecimal xEnd = context.xScale?.transform(spec.xend)
    BigDecimal yEnd = context.yScale?.transform(spec.yend)
    if (x == null || y == null || xEnd == null || yEnd == null) {
      return
    }

    String stroke = normalizeColor(spec.params?.color ?: spec.params?.colour ?: '#000000')
    BigDecimal lineWidth = NumberCoercionUtil.coerceToBigDecimal(spec.params?.size) ?:
        NumberCoercionUtil.coerceToBigDecimal(spec.params?.linewidth) ?: 0.5
    BigDecimal alpha = NumberCoercionUtil.coerceToBigDecimal(spec.params?.alpha) ?: 1.0
    String dashArray = GeomUtils.dashArray(spec.params?.linetype)

    def line = dataLayer.addLine()
        .x1(x)
        .y1(y)
        .x2(xEnd)
        .y2(yEnd)
        .stroke(stroke)
        .addAttribute('stroke-width', lineWidth)
        .styleClass('charm-annotation-segment')
    if (dashArray != null) {
      line.addAttribute('stroke-dasharray', dashArray)
    }
    if (alpha < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }

    LayerData datum = new LayerData(x: spec.x, y: spec.y, rowIndex: annotationIndex)
    GeomUtils.applyCssAttributes(
        line,
        context,
        CharmGeomType.SEGMENT.name(),
        annotationElementIndexBase(annotationIndex),
        datum
    )
  }

  private static void renderCustom(G dataLayer, RenderContext context, CustomAnnotationSpec spec, int annotationIndex) {
    if (spec.grob == null) {
      return
    }
    Map<String, BigDecimal> bounds = resolveBounds(context, spec.xmin, spec.xmax, spec.ymin, spec.ymax)
    Map<String, BigDecimal> pixelBounds = [
        xmin: bounds.xminPx,
        xmax: bounds.xmaxPx,
        ymin: bounds.yminPx,
        ymax: bounds.ymaxPx
    ]

    G customGroup = dataLayer.addG().styleClass('charm-annotation-custom')
    GeomUtils.applyCssAttributes(
        customGroup,
        context,
        CharmGeomType.CUSTOM.name(),
        annotationElementIndexBase(annotationIndex),
        null
    )

    if (spec.grob instanceof Closure) {
      boolean ggSource = spec.params?.get('__source')?.toString() == 'gg'
      renderClosure(customGroup, spec.grob as Closure, pixelBounds, context, ggSource)
      return
    }
    if (spec.grob instanceof SvgElement) {
      (spec.grob as SvgElement).clone(customGroup)
      return
    }
    if (spec.grob instanceof String) {
      renderSvgString(customGroup, spec.grob as String)
      return
    }
    throw new IllegalArgumentException(
        "Unsupported grob type: ${spec.grob.class.name}. Expected Closure, SvgElement, or String.")
  }

  private static void renderLogticks(G dataLayer, RenderContext context, LogticksAnnotationSpec spec, int annotationIndex) {
    boolean xLog = isLogScale(context.xScale)
    boolean yLog = isLogScale(context.yScale)
    if (!xLog && !yLog) {
      return
    }

    Map<String, Object> params = spec.params ?: [:]
    int base = (params.base ?: 10) as int
    String sides = (params.sides ?: 'bl').toString().toLowerCase()
    boolean outside = (params.outside ?: false) as boolean
    BigDecimal shortLen = NumberCoercionUtil.coerceToBigDecimal(params.short) ?: 1.5
    BigDecimal midLen = NumberCoercionUtil.coerceToBigDecimal(params.mid) ?: 2.25
    BigDecimal longLen = NumberCoercionUtil.coerceToBigDecimal(params.long) ?: 4.5
    String color = normalizeColor(params.colour ?: params.color ?: 'black')
    BigDecimal lineWidth = NumberCoercionUtil.coerceToBigDecimal(params.linewidth) ?:
        NumberCoercionUtil.coerceToBigDecimal(params.size) ?: 0.5
    String linetype = params.linetype?.toString() ?: 'solid'
    BigDecimal alpha = NumberCoercionUtil.coerceToBigDecimal(params.alpha) ?: 1.0
    String dashArray = GeomUtils.dashArray(linetype)

    int elementIndex = annotationElementIndexBase(annotationIndex)
    if (xLog && context.xScale instanceof ContinuousCharmScale) {
      ContinuousCharmScale xScale = context.xScale as ContinuousCharmScale
      List<Map<String, Object>> ticks = generateLogTickPositions(xScale.domainMin, xScale.domainMax, base)
      if (sides.contains('b')) {
        elementIndex = drawXTicks(dataLayer, context, ticks, true, outside, shortLen, midLen, longLen,
            color, lineWidth, alpha, dashArray, elementIndex)
      }
      if (sides.contains('t')) {
        elementIndex = drawXTicks(dataLayer, context, ticks, false, outside, shortLen, midLen, longLen,
            color, lineWidth, alpha, dashArray, elementIndex)
      }
    }
    if (yLog && context.yScale instanceof ContinuousCharmScale) {
      ContinuousCharmScale yScale = context.yScale as ContinuousCharmScale
      List<Map<String, Object>> ticks = generateLogTickPositions(yScale.domainMin, yScale.domainMax, base)
      if (sides.contains('l')) {
        elementIndex = drawYTicks(dataLayer, context, ticks, true, outside, shortLen, midLen, longLen,
            color, lineWidth, alpha, dashArray, elementIndex)
      }
      if (sides.contains('r')) {
        drawYTicks(dataLayer, context, ticks, false, outside, shortLen, midLen, longLen,
            color, lineWidth, alpha, dashArray, elementIndex)
      }
    }
  }

  private static void renderRaster(G dataLayer, RenderContext context, RasterAnnotationSpec spec, int annotationIndex) {
    List<List<String>> raster = spec.raster ?: []
    if (raster.isEmpty()) {
      return
    }

    Map<String, BigDecimal> bounds = resolveBounds(context, spec.xmin, spec.xmax, spec.ymin, spec.ymax)
    BigDecimal totalWidth = (bounds.xmaxPx - bounds.xminPx).abs()
    BigDecimal totalHeight = (bounds.ymaxPx - bounds.yminPx).abs()
    if (totalWidth <= 0 || totalHeight <= 0) {
      return
    }

    int rows = raster.size()
    int cols = raster.find { List<String> row -> row != null && !row.isEmpty() }?.size() ?: 0
    if (rows == 0 || cols == 0) {
      return
    }

    BigDecimal cellWidth = totalWidth / cols
    BigDecimal cellHeight = totalHeight / rows
    BigDecimal startX = [bounds.xminPx, bounds.xmaxPx].min()
    BigDecimal startY = [bounds.yminPx, bounds.ymaxPx].min()

    G rasterGroup = dataLayer.addG().styleClass('charm-annotation-raster')
    if (spec.interpolate) {
      rasterGroup.addAttribute('style', 'image-rendering: smooth;')
    }

    int elementIndex = annotationElementIndexBase(annotationIndex)
    for (int row = 0; row < rows; row++) {
      List<String> rowData = raster[row]
      if (rowData == null || rowData.isEmpty()) {
        continue
      }
      for (int col = 0; col < cols; col++) {
        if (col >= rowData.size()) {
          continue
        }
        String color = rowData[col]
        if (color == null || color.isEmpty()) {
          continue
        }
        String normalized = normalizeColor(color)
        BigDecimal x = startX + col * cellWidth
        BigDecimal y = startY + row * cellHeight
        def rect = rasterGroup.addRect()
            .x(x as int)
            .y(y as int)
            .width((cellWidth as int).max(1))
            .height((cellHeight as int).max(1))
            .fill(normalized)
            .addAttribute('stroke', 'none')
        GeomUtils.applyCssAttributes(rect, context, CharmGeomType.RASTER_ANN.name(), elementIndex, null)
        elementIndex++
      }
    }
  }

  private static void renderMap(G dataLayer, RenderContext context, MapAnnotationSpec spec, int annotationIndex) {
    if (spec.map == null || spec.map.rowCount() == 0) {
      return
    }
    String mapIdCol = resolveMapIdColumn(spec.map, spec.params)
    String xCol = resolveMapXColumn(spec.map, spec.params)
    String yCol = resolveMapYColumn(spec.map, spec.params)
    String groupCol = resolveMapGroupColumn(spec.map, spec.params, mapIdCol)

    Matrix merged = spec.data != null ? mergeMapData(spec.map, spec.data, spec.params, spec.mapping, mapIdCol) : spec.map
    if (merged == null || merged.rowCount() == 0) {
      return
    }

    String fillParam = normalizeColor(spec.params?.fill ?: 'gray')
    String colorParam = normalizeColor(spec.params?.colour ?: spec.params?.color ?: 'black')
    BigDecimal lineWidth = NumberCoercionUtil.coerceToBigDecimal(spec.params?.linewidth) ?:
        NumberCoercionUtil.coerceToBigDecimal(spec.params?.size) ?: 1
    BigDecimal alpha = NumberCoercionUtil.coerceToBigDecimal(spec.params?.alpha) ?: 1.0
    String dashArray = GeomUtils.dashArray(spec.params?.linetype)
    String fillCol = spec.mapping?.get('fill')
    String colorCol = spec.mapping?.get('color')

    Map<Object, List<Integer>> groups = [:]
    for (int row = 0; row < merged.rowCount(); row++) {
      Object groupKey = merged[row, groupCol]
      if (groupKey == null) {
        groupKey = "__group_${row}"
      }
      List<Integer> indexes = groups[groupKey]
      if (indexes == null) {
        indexes = []
        groups[groupKey] = indexes
      }
      indexes << row
    }

    int elementIndex = annotationElementIndexBase(annotationIndex)
    groups.each { Object _, List<Integer> rowIndexes ->
      if (rowIndexes.size() < 3) {
        return
      }
      List<String> pathParts = []
      LayerData datum = new LayerData(rowIndex: annotationIndex)
      rowIndexes.eachWithIndex { Integer row, int idx ->
        BigDecimal x = context.xScale?.transform(merged[row, xCol])
        BigDecimal y = context.yScale?.transform(merged[row, yCol])
        if (x == null || y == null) {
          return
        }
        if (idx == 0) {
          pathParts << "M ${x} ${y}".toString()
        } else {
          pathParts << "L ${x} ${y}".toString()
        }
        if (idx == 0) {
          datum.x = merged[row, xCol]
          datum.y = merged[row, yCol]
        }
      }
      if (pathParts.size() < 3) {
        return
      }
      pathParts << 'Z'
      String fillColor = resolveMappedColor(context.fillScale, merged, rowIndexes.first(), fillCol, fillParam)
      String strokeColor = resolveMappedColor(context.colorScale, merged, rowIndexes.first(), colorCol, colorParam)

      def path = dataLayer.addPath()
          .d(pathParts.join(' '))
          .fill(fillColor)
          .stroke(strokeColor)
          .addAttribute('stroke-width', lineWidth)
          .styleClass('charm-annotation-map')
      if (dashArray != null) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
        path.addAttribute('stroke-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, CharmGeomType.MAP.name(), elementIndex, datum)
      elementIndex++
    }
  }

  private static Map<String, BigDecimal> resolveBounds(
      RenderContext context,
      Number xmin,
      Number xmax,
      Number ymin,
      Number ymax
  ) {
    BigDecimal xMinVal = AnnotationConstants.handleInfValue(xmin, true)
    BigDecimal xMaxVal = AnnotationConstants.handleInfValue(xmax, false)
    BigDecimal yMinVal = AnnotationConstants.handleInfValue(ymin, true)
    BigDecimal yMaxVal = AnnotationConstants.handleInfValue(ymax, false)

    BigDecimal xminPx = resolveBoundPixel(context.xScale, xMinVal, true, 'x')
    BigDecimal xmaxPx = resolveBoundPixel(context.xScale, xMaxVal, false, 'x')
    BigDecimal yminPx = resolveBoundPixel(context.yScale, yMinVal, true, 'y')
    BigDecimal ymaxPx = resolveBoundPixel(context.yScale, yMaxVal, false, 'y')

    [
        xminPx: xminPx,
        xmaxPx: xmaxPx,
        yminPx: yminPx,
        ymaxPx: ymaxPx
    ]
  }

  private static BigDecimal resolveBoundPixel(CharmScale scale, BigDecimal value, boolean isMin, String axis) {
    if (scale == null) {
      return null
    }
    if (!AnnotationConstants.isInfinite(value)) {
      return scale.transform(value)
    }
    if (scale instanceof ContinuousCharmScale) {
      ContinuousCharmScale continuous = scale as ContinuousCharmScale
      BigDecimal domainValue = isMin ? continuous.domainMin : continuous.domainMax
      BigDecimal transformed = scale.transform(domainValue)
      if (transformed != null) {
        return transformed
      }
    }
    BigDecimal low = [scale.rangeStart, scale.rangeEnd].min()
    BigDecimal high = [scale.rangeStart, scale.rangeEnd].max()
    if (axis == 'y') {
      return isMin ? high : low
    }
    isMin ? low : high
  }

  private static boolean isLogScale(CharmScale scale) {
    scale instanceof ContinuousCharmScale &&
        (scale as ContinuousCharmScale).transformStrategy?.id() == 'log10'
  }

  private static int annotationElementIndexBase(int annotationIndex) {
    int normalized = annotationIndex < 0 ? 0 : annotationIndex
    normalized * 1_000_000
  }

  private static List<Map<String, Object>> generateLogTickPositions(BigDecimal logMin, BigDecimal logMax, int base) {
    if (logMin == null || logMax == null) {
      return []
    }
    List<Map<String, Object>> ticks = []
    int minPow = logMin.floor().intValue()
    int maxPow = logMax.ceil().intValue()

    for (int pow = minPow; pow <= maxPow; pow++) {
      BigDecimal majorValue = (base ** pow) as BigDecimal
      BigDecimal logMajor = pow as BigDecimal
      if (logMajor >= logMin && logMajor <= logMax) {
        ticks << [value: majorValue as Object, type: 'major' as Object]
      }

      if (base >= 4) {
        for (int mult = 2; mult < base; mult++) {
          BigDecimal value = mult * (base ** pow)
          BigDecimal logValue = value.log(base)
          if (logValue >= logMin && logValue <= logMax) {
            String type
            if (base == 10) {
              type = (mult == 2 || mult == 5) ? 'intermediate' : 'minor'
            } else {
              type = (mult == 2) ? 'intermediate' : 'minor'
            }
            ticks << [value: value as Object, type: type as Object]
          }
        }
      }
    }
    ticks
  }

  private static int drawXTicks(
      G group,
      RenderContext context,
      List<Map<String, Object>> ticks,
      boolean bottom,
      boolean outside,
      BigDecimal shortLen,
      BigDecimal midLen,
      BigDecimal longLen,
      String color,
      BigDecimal lineWidth,
      BigDecimal alpha,
      String dashArray,
      int elementIndex
  ) {
    BigDecimal yBottom = [context.yScale.rangeStart, context.yScale.rangeEnd].max()
    BigDecimal yTop = [context.yScale.rangeStart, context.yScale.rangeEnd].min()
    BigDecimal yPos = bottom ? yBottom : yTop
    ticks.each { Map<String, Object> tick ->
      BigDecimal value = tick.value as BigDecimal
      BigDecimal x = context.xScale.transform(value)
      if (x == null) {
        return
      }
      BigDecimal len = tickLength(tick.type?.toString(), shortLen, midLen, longLen)
      BigDecimal y1 = yPos
      BigDecimal y2
      if (outside) {
        y2 = bottom ? (yPos + len) : (yPos - len)
      } else {
        y2 = bottom ? (yPos - len) : (yPos + len)
      }
      def line = group.addLine()
          .x1(x as int)
          .y1(y1 as int)
          .x2(x as int)
          .y2(y2 as int)
          .stroke(color)
          .addAttribute('stroke-width', lineWidth)
          .styleClass('charm-annotation-logticks')
      if (dashArray != null) {
        line.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(line, context, CharmGeomType.LOGTICKS.name(), elementIndex, null)
      elementIndex++
    }
    elementIndex
  }

  private static int drawYTicks(
      G group,
      RenderContext context,
      List<Map<String, Object>> ticks,
      boolean left,
      boolean outside,
      BigDecimal shortLen,
      BigDecimal midLen,
      BigDecimal longLen,
      String color,
      BigDecimal lineWidth,
      BigDecimal alpha,
      String dashArray,
      int elementIndex
  ) {
    BigDecimal xLeft = [context.xScale.rangeStart, context.xScale.rangeEnd].min()
    BigDecimal xRight = [context.xScale.rangeStart, context.xScale.rangeEnd].max()
    BigDecimal xPos = left ? xLeft : xRight
    ticks.each { Map<String, Object> tick ->
      BigDecimal value = tick.value as BigDecimal
      BigDecimal y = context.yScale.transform(value)
      if (y == null) {
        return
      }
      BigDecimal len = tickLength(tick.type?.toString(), shortLen, midLen, longLen)
      BigDecimal x1 = xPos
      BigDecimal x2
      if (outside) {
        x2 = left ? (xPos - len) : (xPos + len)
      } else {
        x2 = left ? (xPos + len) : (xPos - len)
      }
      def line = group.addLine()
          .x1(x1 as int)
          .y1(y as int)
          .x2(x2 as int)
          .y2(y as int)
          .stroke(color)
          .addAttribute('stroke-width', lineWidth)
          .styleClass('charm-annotation-logticks')
      if (dashArray != null) {
        line.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(line, context, CharmGeomType.LOGTICKS.name(), elementIndex, null)
      elementIndex++
    }
    elementIndex
  }

  private static BigDecimal tickLength(String type, BigDecimal shortLen, BigDecimal midLen, BigDecimal longLen) {
    switch (type) {
      case 'major' -> longLen
      case 'intermediate' -> midLen
      default -> shortLen
    }
  }

  private static String resolveMapIdColumn(Matrix mapData, Map<String, Object> params) {
    String fromParam = params?.get('map_id_col')?.toString() ?: params?.get('mapIdCol')?.toString()
    if (fromParam != null) {
      return fromParam
    }
    List<String> columns = mapData.columnNames()
    if (columns.contains('region')) return 'region'
    if (columns.contains('id')) return 'id'
    if (columns.contains('map_id')) return 'map_id'
    throw new IllegalArgumentException("annotation_map map data must include an id column (region, id, or map_id)")
  }

  private static String resolveMapXColumn(Matrix mapData, Map<String, Object> params) {
    String fromParam = params?.get('x')?.toString()
    if (fromParam != null) {
      return fromParam
    }
    List<String> columns = mapData.columnNames()
    if (columns.contains('x')) return 'x'
    if (columns.contains('long')) return 'long'
    throw new IllegalArgumentException("annotation_map map data must include an x column (x or long)")
  }

  private static String resolveMapYColumn(Matrix mapData, Map<String, Object> params) {
    String fromParam = params?.get('y')?.toString()
    if (fromParam != null) {
      return fromParam
    }
    List<String> columns = mapData.columnNames()
    if (columns.contains('y')) return 'y'
    if (columns.contains('lat')) return 'lat'
    throw new IllegalArgumentException("annotation_map map data must include a y column (y or lat)")
  }

  private static String resolveMapGroupColumn(Matrix mapData, Map<String, Object> params, String mapIdCol) {
    String fromParam = params?.get('group')?.toString()
    if (fromParam != null) {
      return fromParam
    }
    List<String> columns = mapData.columnNames()
    if (columns.contains('group')) return 'group'
    mapIdCol
  }

  private static Matrix mergeMapData(
      Matrix mapData,
      Matrix data,
      Map<String, Object> params,
      Map<String, String> mapping,
      String mapIdCol
  ) {
    String dataMapIdCol = params?.get('map_id')?.toString()
    if (dataMapIdCol == null && params?.get('mapId') != null) {
      dataMapIdCol = params.get('mapId').toString()
    }
    if (dataMapIdCol == null && mapping?.containsKey('map_id')) {
      dataMapIdCol = mapping.get('map_id')
    }
    if (dataMapIdCol == null && data.columnNames().contains(mapIdCol)) {
      dataMapIdCol = mapIdCol
    }
    if (dataMapIdCol == null) {
      throw new IllegalArgumentException("annotation_map requires map_id mapping/parameter when data is supplied")
    }

    Map<Object, Map<String, Object>> dataById = [:]
    for (int row = 0; row < data.rowCount(); row++) {
      Object id = data[row, dataMapIdCol]
      if (id == null || dataById.containsKey(id)) {
        continue
      }
      Map<String, Object> rowMap = [:]
      data.columnNames().each { String col ->
        rowMap[col] = data[row, col]
      }
      dataById[id] = rowMap
    }

    List<Map<String, Object>> mergedRows = []
    for (int row = 0; row < mapData.rowCount(); row++) {
      Map<String, Object> rowMap = [:]
      mapData.columnNames().each { String col ->
        rowMap[col] = mapData[row, col]
      }
      Object id = mapData[row, mapIdCol]
      Map<String, Object> dataRow = dataById[id]
      if (dataRow != null) {
        rowMap.putAll(dataRow)
      }
      mergedRows << rowMap
    }
    Matrix.builder().mapList(mergedRows).build()
  }

  private static String resolveMappedColor(
      ColorCharmScale scale,
      Matrix data,
      int row,
      String column,
      String fallback
  ) {
    if (scale == null || column == null || !data.columnNames().contains(column)) {
      return fallback
    }
    Object value = data[row, column]
    if (value == null) {
      return fallback
    }
    scale.colorFor(value)
  }

  private static String normalizeColor(Object value) {
    String raw = value?.toString()
    if (raw == null) {
      return '#000000'
    }
    ColorUtil.normalizeColor(raw) ?: raw
  }

  private static String textAnchor(BigDecimal hjust) {
    if (hjust <= 0.25) {
      return 'start'
    }
    if (hjust >= 0.75) {
      return 'end'
    }
    'middle'
  }

  private static String dominantBaseline(BigDecimal vjust) {
    if (vjust <= 0.25) {
      return 'text-after-edge'
    }
    if (vjust >= 0.75) {
      return 'text-before-edge'
    }
    'middle'
  }

  private static void applyFontFace(SvgElement text, String face) {
    switch (face?.toLowerCase()) {
      case 'bold' -> text.addAttribute('font-weight', 'bold')
      case 'italic' -> text.addAttribute('font-style', 'italic')
      case 'bold.italic' -> {
        text.addAttribute('font-weight', 'bold')
        text.addAttribute('font-style', 'italic')
      }
    }
  }

  @CompileDynamic
  private static void renderClosure(
      G customGroup,
      Closure<?> grob,
      Map<String, BigDecimal> bounds,
      RenderContext context,
      boolean ggSource
  ) {
    int arity = grob.maximumNumberOfParameters
    Map<String, Object> scalesArg = ggSource
        ? ggCompatibilityScales(context)
        : [x: context.xScale, y: context.yScale]
    Object coordArg = ggSource ? new GgCoordCompat() : context.chart.coord
    if (arity >= 4) {
      grob.call(customGroup, bounds, scalesArg, coordArg)
    } else if (arity == 3) {
      grob.call(customGroup, bounds, scalesArg)
    } else {
      grob.call(customGroup, bounds)
    }
  }

  @CompileDynamic
  private static Map<String, Object> ggCompatibilityScales(RenderContext context) {
    [
        x: new GgScaleCompat(scale: context.xScale),
        y: new GgScaleCompat(scale: context.yScale)
    ]
  }

  @CompileStatic
  private static class GgScaleCompat {
    CharmScale scale

    BigDecimal transform(Object value) {
      scale?.transform(value)
    }

    Object inverse(Object value) {
      value
    }

    List<Object> getComputedDomain() {
      if (scale instanceof ContinuousCharmScale) {
        ContinuousCharmScale continuous = scale as ContinuousCharmScale
        return [continuous.domainMin, continuous.domainMax]
      }
      []
    }

    List<Object> getDomain() {
      getComputedDomain()
    }

    BigDecimal getRangeStart() {
      scale?.rangeStart
    }

    BigDecimal getRangeEnd() {
      scale?.rangeEnd
    }
  }

  @CompileStatic
  private static class GgCoordCompat {
    List<Number> transform(Number x, Number y, Map<String, ?> scales) {
      GgScaleCompat xScale = scales?.get('x') as GgScaleCompat
      GgScaleCompat yScale = scales?.get('y') as GgScaleCompat
      Number xPx = xScale ? xScale.transform(x) : x
      Number yPx = yScale ? yScale.transform(y) : y
      [xPx, yPx]
    }

    List<Number> inverse(Number xPx, Number yPx, Map<String, ?> scales) {
      GgScaleCompat xScale = scales?.get('x') as GgScaleCompat
      GgScaleCompat yScale = scales?.get('y') as GgScaleCompat
      Number x = xScale ? xScale.inverse(xPx) as Number : xPx
      Number y = yScale ? yScale.inverse(yPx) as Number : yPx
      [x, y]
    }
  }

  @CompileDynamic
  private static void renderSvgString(G customGroup, String svgString) {
    String wrapped = svgString.trim()
    if (!wrapped.startsWith('<svg')) {
      wrapped = "<svg xmlns=\"http://www.w3.org/2000/svg\">${wrapped}</svg>"
    }
    Svg parsed = SvgReader.parse(wrapped)
    parsed.clone(customGroup)
  }
}
