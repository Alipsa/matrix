package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.AnnotationSpec
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.coord.CoordEngine
import se.alipsa.matrix.charm.render.position.PositionEngine
import se.alipsa.matrix.charm.theme.ElementText
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.charm.render.geom.GeomEngine
import se.alipsa.matrix.charm.render.annotation.AnnotationEngine
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine
import se.alipsa.matrix.charm.render.scale.TrainedScales
import se.alipsa.matrix.charm.render.stat.StatEngine

/**
 * Charm SVG renderer.
 *
 * Data flow:
 * data + mappings -> stat -> position -> scale -> coord -> geom -> theme -> svg
 */
@CompileStatic
class CharmRenderer {

  private static final Logger log = Logger.getLogger(CharmRenderer)

  private final AxisRenderer axisRenderer = new AxisRenderer()
  private final GridRenderer gridRenderer = new GridRenderer()
  private final LegendRenderer legendRenderer = new LegendRenderer()
  private final FacetRenderer facetRenderer = new FacetRenderer()

  /**
   * Renders a chart using default render config.
   *
   * @param chart immutable compiled chart
   * @return rendered svg
   */
  Svg render(Chart chart) {
    render(chart, new RenderConfig())
  }

  /**
   * Renders a chart using explicit render config.
   *
   * @param chart immutable compiled chart
   * @param config render config
   * @return rendered svg
   */
  Svg render(Chart chart, RenderConfig config) {
    RenderConfig cfg = config?.copy() ?: new RenderConfig()
    Svg svg = new Svg()
    svg.width(cfg.width)
    svg.height(cfg.height)
    svg.viewBox("0 0 ${cfg.width} ${cfg.height}")
    RenderContext context = new RenderContext(chart, cfg, svg)
    context.defs = svg.addDefs()
    context.panels = facetRenderer.computePanels(
        chart.data,
        chart.facet.type,
        chart.facet.rows,
        chart.facet.cols,
        chart.facet.vars,
        chart.facet.ncol,
        chart.facet.nrow,
        chart.facet.params
    )

    trainScales(context)
    renderCanvas(context)
    renderPanels(context)
    renderLabels(context)
    legendRenderer.render(context)
    svg
  }

  private void trainScales(RenderContext context) {
    List<Object> xValues = []
    List<Object> yValues = []
    List<Object> colorValues = []
    List<Object> fillValues = []
    List<Object> sizeValues = []
    List<Object> shapeValues = []
    List<Object> alphaValues = []
    List<Object> linetypeValues = []

    context.chart.layers.each { LayerSpec layer ->
      Matrix sourceData = resolveLayerData(context.chart.data, layer)
      List<Integer> rowIndexes = defaultRowIndexes(sourceData?.rowCount() ?: 0)
      Aes aes = effectiveAes(context.chart.aes, layer)
      List<LayerData> pipelineData = runPipeline(context, layer, sourceData, aes, rowIndexes)
      xValues.addAll(pipelineData.collect { LayerData d -> d.x })
      yValues.addAll(pipelineData.collect { LayerData d -> d.y })
      colorValues.addAll(pipelineData.collect { LayerData d -> d.color })
      fillValues.addAll(pipelineData.collect { LayerData d -> d.fill })
      sizeValues.addAll(pipelineData.collect { LayerData d -> d.size })
      shapeValues.addAll(pipelineData.collect { LayerData d -> d.shape })
      alphaValues.addAll(pipelineData.collect { LayerData d -> d.alpha })
      linetypeValues.addAll(pipelineData.collect { LayerData d -> d.linetype })
    }

    TrainedScales trained = ScaleEngine.train(
        context.chart, context.config, xValues, yValues, colorValues, fillValues,
        sizeValues, shapeValues, alphaValues, linetypeValues)
    context.xScale = trained.x
    context.yScale = trained.y
    context.colorScale = trained.color
    context.fillScale = trained.fill
    context.sizeScale = trained.size
    context.shapeScale = trained.shape
    context.alphaScale = trained.alpha
    context.linetypeScale = trained.linetype
  }

  private void renderCanvas(RenderContext context) {
    String background = context.chart.theme.plotBackground?.fill ?: '#ffffff'
    context.svg.addRect(context.config.width, context.config.height)
        .x(0)
        .y(0)
        .fill(background)
        .styleClass('charm-canvas')
  }

  private void renderPanels(RenderContext context) {
    if (context.panels == null || context.panels.isEmpty()) {
      context.panels = [defaultPanel(context.chart.data.rowCount())]
    }
    int panelRows = context.panels.collect { PanelSpec p -> p.row }.max() + 1
    int panelCols = context.panels.collect { PanelSpec p -> p.col }.max() + 1
    boolean faceted = context.chart.facet.type != FacetType.NONE
    boolean isGrid = context.chart.facet.type == FacetType.GRID
    boolean hasRowStrips = isGrid && context.panels.any { PanelSpec p -> p.rowLabel != null && !p.rowLabel.isEmpty() }
    int stripHeight = faceted ? context.config.stripHeight : 0
    int stripWidth = hasRowStrips ? context.config.stripWidth : 0

    // Theme-driven strip styling
    Set<String> nulls = context.chart.theme.explicitNulls
    boolean drawStripBackground = !nulls.contains('stripBackground')
    boolean drawStripText = !nulls.contains('stripText')
    String stripBgFill = context.chart.theme.stripBackground?.fill ?: '#D9D9D9'
    String stripBgStroke = context.chart.theme.stripBackground?.color ?: null
    String stripTextColor = context.chart.theme.stripText?.color ?: '#333333'
    BigDecimal stripTextSize = (context.chart.theme.stripText?.size ?: 10) as BigDecimal
    String stripTextFamily = context.chart.theme.stripText?.family

    int totalSpacingX = (panelCols - 1) * context.config.panelSpacing
    int totalSpacingY = (panelRows - 1) * context.config.panelSpacing
    int panelWidth = ((context.config.plotWidth() - totalSpacingX - stripWidth) / panelCols) as int
    int panelHeight = ((context.config.plotHeight() - totalSpacingY - panelRows * stripHeight) / panelRows) as int
    Map<Integer, List<AnnotationRenderEntry>> annotationsByOrder = annotationOrderMap(context.chart.annotations)

    context.panels.each { PanelSpec panel ->
      context.panelRow = panel.row
      context.panelCol = panel.col
      int panelX = context.config.marginLeft + panel.col * (panelWidth + context.config.panelSpacing)
      int panelY = context.config.marginTop + panel.row * (panelHeight + stripHeight + context.config.panelSpacing)
      G panelGroup = context.svg.addG().id("panel-${panel.row}-${panel.col}").transform("translate($panelX, $panelY)")

      G plotArea = panelGroup
      if (faceted) {
        // Column strip (top of each panel)
        String colStripLabel = panel.colLabel ?: panel.label ?: ''
        if (drawStripBackground) {
          def stripRect = panelGroup.addRect(panelWidth, stripHeight)
              .x(0).y(0).fill(stripBgFill).styleClass('charm-strip')
          if (stripBgStroke) {
            stripRect.stroke(stripBgStroke)
          }
        }
        if (drawStripText && colStripLabel) {
          def stripTextEl = panelGroup.addText(colStripLabel)
              .x(panelWidth / 2)
              .y(stripHeight - 7)
              .textAnchor('middle')
              .fontSize(stripTextSize)
              .fill(stripTextColor)
              .styleClass('charm-strip-label')
          if (stripTextFamily) {
            stripTextEl.addAttribute('font-family', stripTextFamily)
          }
        }
        plotArea = panelGroup.addG().id("panel-plot-${panel.row}-${panel.col}").transform("translate(0, $stripHeight)")

        // Row strip (right side, GRID only)
        if (hasRowStrips && panel.col == panelCols - 1 && panel.rowLabel) {
          int rowStripX = panelWidth
          if (drawStripBackground) {
            def rowRect = panelGroup.addRect(stripWidth, panelHeight + stripHeight)
                .x(rowStripX).y(0).fill(stripBgFill).styleClass('charm-strip-row')
            if (stripBgStroke) {
              rowRect.stroke(stripBgStroke)
            }
          }
          if (drawStripText) {
            int textX = rowStripX + stripWidth / 2 as int
            int textY = (panelHeight + stripHeight) / 2 as int
            def rowTextEl = panelGroup.addText(panel.rowLabel)
                .x(textX)
                .y(textY)
                .textAnchor('middle')
                .fontSize(stripTextSize)
                .fill(stripTextColor)
                .transform("rotate(90, $textX, $textY)")
                .styleClass('charm-strip-label-row')
            if (stripTextFamily) {
              rowTextEl.addAttribute('font-family', stripTextFamily)
            }
          }
        }
      }

      String panelFill = context.chart.theme.panelBackground?.fill ?: '#ffffff'
      String panelStroke = context.chart.theme.panelBorder?.color
          ?: context.chart.theme.panelBackground?.color ?: '#dddddd'
      plotArea.addRect(panelWidth, panelHeight)
          .x(0)
          .y(0)
          .fill(panelFill)
          .stroke(panelStroke)
          .styleClass('charm-panel')

      gridRenderer.render(plotArea, context, panelWidth, panelHeight)
      G dataLayer = plotArea.addG().id("data-${panel.row}-${panel.col}")
      String clipId = "panel-clip-${panel.row}-${panel.col}"
      context.defs.addClipPath().id(clipId).addRect(panelWidth, panelHeight).x(0).y(0)
      dataLayer.addAttribute('clip-path', "url(#${clipId})")
      renderAnnotationsBeforeOrder(dataLayer, context, annotationsByOrder, 0)

      context.chart.layers.eachWithIndex { LayerSpec layer, int layerIndex ->
        renderAnnotationsAtOrder(dataLayer, context, annotationsByOrder, layerIndex)
        context.layerIndex = layerIndex
        Matrix sourceData = resolveLayerData(context.chart.data, layer)
        boolean layerHasOwnData = sourceData != null && !sourceData.is(context.chart.data)
        List<Integer> rowIndexes
        if (layerHasOwnData) {
          if (faceted) {
            log.warn("Layer-specific data with facets is not fully supported yet; " +
                "the full layer dataset will be rendered in every panel")
          }
          rowIndexes = defaultRowIndexes(sourceData.rowCount())
        } else {
          rowIndexes = panel.rowIndexes
        }
        Aes aes = effectiveAes(context.chart.aes, layer)
        List<LayerData> layerData = runPipeline(context, layer, sourceData, aes, rowIndexes)
        renderLayer(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      }
      renderAnnotationsFromOrder(dataLayer, context, annotationsByOrder, context.chart.layers.size())
      context.layerIndex = -1
      axisRenderer.render(plotArea, context, panelWidth, panelHeight)
    }
    context.panelRow = null
    context.panelCol = null
  }

  private void renderLayer(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      List<LayerData> layerData,
      int panelWidth,
      int panelHeight
  ) {
    GeomEngine.render(dataLayer, context, layer, layerData, panelWidth, panelHeight)
  }

  private static void renderAnnotationsAtOrder(
      G dataLayer,
      RenderContext context,
      Map<Integer, List<AnnotationRenderEntry>> annotationsByOrder,
      int drawOrder
  ) {
    if (annotationsByOrder == null || annotationsByOrder.isEmpty()) {
      return
    }
    List<AnnotationRenderEntry> entries = annotationsByOrder[drawOrder]
    if (entries == null || entries.isEmpty()) {
      return
    }
    entries.each { AnnotationRenderEntry entry ->
      context.layerIndex = entry.drawOrder
      AnnotationEngine.render(dataLayer, context, entry.annotation, entry.annotationIndex)
    }
  }

  private static void renderAnnotationsBeforeOrder(
      G dataLayer,
      RenderContext context,
      Map<Integer, List<AnnotationRenderEntry>> annotationsByOrder,
      int startOrder
  ) {
    if (annotationsByOrder == null || annotationsByOrder.isEmpty()) {
      return
    }
    List<Integer> orders = annotationsByOrder.keySet()
        .findAll { Integer order -> order < startOrder }
        .sort()
    orders.each { Integer order ->
      renderAnnotationsAtOrder(dataLayer, context, annotationsByOrder, order)
    }
  }

  private static void renderAnnotationsFromOrder(
      G dataLayer,
      RenderContext context,
      Map<Integer, List<AnnotationRenderEntry>> annotationsByOrder,
      int startOrder
  ) {
    if (annotationsByOrder == null || annotationsByOrder.isEmpty()) {
      return
    }
    List<Integer> orders = annotationsByOrder.keySet()
        .findAll { Integer order -> order >= startOrder }
        .sort()
    orders.each { Integer order ->
      renderAnnotationsAtOrder(dataLayer, context, annotationsByOrder, order)
    }
  }

  private static Map<Integer, List<AnnotationRenderEntry>> annotationOrderMap(List<AnnotationSpec> annotations) {
    Map<Integer, List<AnnotationRenderEntry>> byOrder = [:]
    (annotations ?: []).eachWithIndex { AnnotationSpec annotation, int idx ->
      int order = annotation?.drawOrder ?: 0
      List<AnnotationRenderEntry> entries = byOrder[order]
      if (entries == null) {
        entries = []
        byOrder[order] = entries
      }
      entries << new AnnotationRenderEntry(annotation: annotation, annotationIndex: idx, drawOrder: order)
    }
    byOrder
  }

  private List<LayerData> runPipeline(
      RenderContext context,
      LayerSpec layer,
      Matrix dataMatrix,
      Aes aes,
      List<Integer> rowIndexes
  ) {
    Map<List<Integer>, List<LayerData>> layerCache = context.pipelineCache[layer]
    if (layerCache == null) {
      layerCache = [:]
      context.pipelineCache[layer] = layerCache
    }
    List<Integer> rowKey = Collections.unmodifiableList(new ArrayList<>(rowIndexes))
    List<LayerData> cached = layerCache[rowKey]
    if (cached != null) {
      return cached
    }

    List<LayerData> mapped = mapData(dataMatrix, aes, rowIndexes)
    List<LayerData> statData = applyStat(layer, mapped)
    List<LayerData> posData = applyPosition(layer, statData)
    List<LayerData> result = layer.geomType == CharmGeomType.PIE
        ? posData
        : applyCoord(context.chart.coord, posData)
    layerCache[rowKey] = result
    result
  }

  private List<LayerData> mapData(Matrix data, Aes aes, List<Integer> rowIndexes) {
    List<LayerData> values = []
    rowIndexes.each { int rowIndex ->
      LayerData datum = new LayerData(rowIndex: rowIndex)
      LayerDataRowAccess.attach(datum, data, rowIndex)
      datum.x = readValue(data, rowIndex, aes.x?.columnName())
      datum.y = readValue(data, rowIndex, aes.y?.columnName())
      datum.color = readValue(data, rowIndex, aes.color?.columnName())
      datum.fill = readValue(data, rowIndex, aes.fill?.columnName())
      datum.xend = readValue(data, rowIndex, aes.xend?.columnName())
      datum.yend = readValue(data, rowIndex, aes.yend?.columnName())
      datum.xmin = readValue(data, rowIndex, aes.xmin?.columnName())
      datum.xmax = readValue(data, rowIndex, aes.xmax?.columnName())
      datum.ymin = readValue(data, rowIndex, aes.ymin?.columnName())
      datum.ymax = readValue(data, rowIndex, aes.ymax?.columnName())
      datum.size = readValue(data, rowIndex, aes.size?.columnName())
      datum.shape = readValue(data, rowIndex, aes.shape?.columnName())
      datum.alpha = readValue(data, rowIndex, aes.alpha?.columnName())
      datum.linetype = readValue(data, rowIndex, aes.linetype?.columnName())
      datum.group = readValue(data, rowIndex, aes.group?.columnName())
      datum.label = readValue(data, rowIndex, aes.label?.columnName())
      datum.weight = readValue(data, rowIndex, aes.weight?.columnName())
      values << datum
    }
    values
  }

  private static Object readValue(Matrix data, int rowIndex, String columnName) {
    if (data == null || rowIndex < 0 || rowIndex >= data.rowCount()) {
      return null
    }
    columnName == null ? null : data[rowIndex, columnName]
  }

  private static List<LayerData> applyStat(LayerSpec layer, List<LayerData> mapped) {
    StatEngine.apply(layer, mapped)
  }

  private static List<LayerData> applyPosition(LayerSpec layer, List<LayerData> data) {
    PositionEngine.apply(layer, data)
  }

  private static List<LayerData> applyCoord(CoordSpec coord, List<LayerData> data) {
    CoordEngine.apply(coord, data)
  }


  private static Aes effectiveAes(Aes plotAes, LayerSpec layer) {
    Aes aes = layer.inheritAes ? plotAes.copy() : new Aes()
    if (layer.aes != null) {
      aes.apply(layer.aes.mappings())
    }
    aes
  }

  private void renderLabels(RenderContext context) {
    ElementText titleStyle = context.chart.theme.plotTitle
    ElementText subtitleStyle = context.chart.theme.plotSubtitle
    ElementText captionStyle = context.chart.theme.plotCaption
    ElementText xTitleStyle = context.chart.theme.axisTitleX
    ElementText yTitleStyle = context.chart.theme.axisTitleY
    String defaultColor = '#222222'
    BigDecimal defaultTitleSize = (context.chart.theme.baseSize ?: 11) + 4 as BigDecimal
    BigDecimal defaultLabelSize = (context.chart.theme.baseSize ?: 11) as BigDecimal

    // When coord is FLIP, swap x/y axis labels since data axes are swapped
    boolean flipped = context.chart.coord?.type == CharmCoordType.FLIP
    String xLabelText = flipped ? context.chart.labels?.y : context.chart.labels?.x
    String yLabelText = flipped ? context.chart.labels?.x : context.chart.labels?.y

    int titleY = 30
    boolean titleRendered = false
    BigDecimal renderedTitleSize = defaultTitleSize
    if (context.chart.labels?.title && !context.chart.theme.explicitNulls.contains('plotTitle')) {
      titleRendered = true
      String titleColor = titleStyle?.color ?: defaultColor
      renderedTitleSize = (titleStyle?.size ?: defaultTitleSize) as BigDecimal
      String titleAnchor = resolveTextAnchor(titleStyle?.hjust)
      BigDecimal titleX = resolveHjustX(titleStyle?.hjust, context.config.width)
      def text = context.svg.addText(context.chart.labels.title)
          .x(titleX)
          .y(titleY)
          .textAnchor(titleAnchor)
          .fontSize(renderedTitleSize)
          .fill(titleColor)
          .styleClass('charm-title')
      if (titleStyle?.face == 'bold' || titleStyle?.face == 'bold.italic') {
        text.addAttribute('font-weight', 'bold')
      }
      if (titleStyle?.face == 'italic' || titleStyle?.face == 'bold.italic') {
        text.addAttribute('font-style', 'italic')
      }
      if (titleStyle?.family) {
        text.addAttribute('font-family', titleStyle.family)
      }
    }

    if (context.chart.labels?.subtitle && !context.chart.theme.explicitNulls.contains('plotSubtitle')) {
      String stColor = subtitleStyle?.color ?: defaultColor
      BigDecimal stSize = (subtitleStyle?.size ?: defaultLabelSize) as BigDecimal
      String stAnchor = resolveTextAnchor(subtitleStyle?.hjust)
      BigDecimal stX = resolveHjustX(subtitleStyle?.hjust, context.config.width)
      int stY = titleRendered ? (titleY + renderedTitleSize as int + 4) : titleY
      def text = context.svg.addText(context.chart.labels.subtitle)
          .x(stX)
          .y(stY)
          .textAnchor(stAnchor)
          .fontSize(stSize)
          .fill(stColor)
          .styleClass('charm-subtitle')
      if (subtitleStyle?.family) {
        text.addAttribute('font-family', subtitleStyle.family)
      }
      if (subtitleStyle?.face == 'italic' || subtitleStyle?.face == 'bold.italic') {
        text.addAttribute('font-style', 'italic')
      }
    }

    if (xLabelText && !context.chart.theme.explicitNulls.contains('axisTitleX')) {
      String xColor = xTitleStyle?.color ?: defaultColor
      BigDecimal xSize = (xTitleStyle?.size ?: defaultLabelSize) as BigDecimal
      context.svg.addText(xLabelText)
          .x(context.config.marginLeft + context.config.plotWidth() / 2)
          .y(context.config.height - 10)
          .textAnchor('middle')
          .fontSize(xSize)
          .fill(xColor)
          .styleClass('charm-x-label')
    }
    if (yLabelText && !context.chart.theme.explicitNulls.contains('axisTitleY')) {
      String yColor = yTitleStyle?.color ?: defaultColor
      BigDecimal ySize = (yTitleStyle?.size ?: defaultLabelSize) as BigDecimal
      int yMid = context.config.marginTop + context.config.plotHeight().intdiv(2)
      context.svg.addText(yLabelText)
          .x(18)
          .y(yMid)
          .transform("rotate(-90, 18, $yMid)")
          .textAnchor('middle')
          .fontSize(ySize)
          .fill(yColor)
          .styleClass('charm-y-label')
    }

    if (context.chart.labels?.caption && !context.chart.theme.explicitNulls.contains('plotCaption')) {
      String capColor = captionStyle?.color ?: '#666666'
      BigDecimal capSize = (captionStyle?.size ?: (defaultLabelSize * 0.8)) as BigDecimal
      int capY = context.config.height - 5
      int capX = context.config.width - context.config.marginRight
      def text = context.svg.addText(context.chart.labels.caption)
          .x(capX)
          .y(capY)
          .textAnchor('end')
          .fontSize(capSize)
          .fill(capColor)
          .styleClass('charm-caption')
      if (captionStyle?.family) {
        text.addAttribute('font-family', captionStyle.family)
      }
      if (captionStyle?.face == 'italic' || captionStyle?.face == 'bold.italic') {
        text.addAttribute('font-style', 'italic')
      }
    }
  }

  private static String resolveTextAnchor(Number hjust) {
    if (hjust == null) {
      return 'middle'
    }
    BigDecimal h = hjust as BigDecimal
    if (h < 0.25) {
      return 'start'
    }
    if (h > 0.75) {
      return 'end'
    }
    'middle'
  }

  private static BigDecimal resolveHjustX(Number hjust, int width) {
    if (hjust == null) {
      return width / 2 as BigDecimal
    }
    // Interpolate: hjust 0 -> left margin (10px), hjust 1 -> right margin (width - 10)
    BigDecimal h = hjust as BigDecimal
    10 + h * (width - 20) as BigDecimal
  }

  private static PanelSpec defaultPanel(int rowCount) {
    PanelSpec panel = new PanelSpec(row: 0, col: 0, label: null)
    panel.rowIndexes = (0..<rowCount).collect { int idx -> idx }
    panel
  }

  private static Matrix resolveLayerData(Matrix chartData, LayerSpec layer) {
    Object layerData = layer.params['__layer_data']
    layerData instanceof Matrix ? layerData as Matrix : chartData
  }

  private static List<Integer> defaultRowIndexes(int rowCount) {
    (0..<rowCount).collect { int idx -> idx }
  }

  private static class AnnotationRenderEntry {
    AnnotationSpec annotation
    int annotationIndex
    int drawOrder
  }
}
