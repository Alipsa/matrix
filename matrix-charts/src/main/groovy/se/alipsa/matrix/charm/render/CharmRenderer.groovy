package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.geom.GeomEngine
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
        chart.facet.nrow
    )

    trainScales(context)
    renderCanvas(context)
    renderPanels(context)
    renderLabels(context)
    legendRenderer.render(context)
    svg
  }

  private void trainScales(RenderContext context) {
    List<Integer> rowIndexes = (0..<context.chart.data.rowCount()).collect { int idx -> idx }
    List<Object> xValues = []
    List<Object> yValues = []
    List<Object> colorValues = []
    List<Object> fillValues = []

    context.chart.layers.each { LayerSpec layer ->
      Aes aes = effectiveAes(context.chart.aes, layer)
      List<LayerData> pipelineData = runPipeline(context, layer, aes, rowIndexes)
      xValues.addAll(pipelineData.collect { LayerData d -> d.x })
      yValues.addAll(pipelineData.collect { LayerData d -> d.y })
      colorValues.addAll(pipelineData.collect { LayerData d -> d.color })
      fillValues.addAll(pipelineData.collect { LayerData d -> d.fill })
    }

    TrainedScales trained = ScaleEngine.train(
        context.chart, context.config, xValues, yValues, colorValues, fillValues)
    context.xScale = trained.x
    context.yScale = trained.y
    context.colorScale = trained.color
    context.fillScale = trained.fill
  }

  private void renderCanvas(RenderContext context) {
    String background = (context.chart.theme.raw?.background ?: '#ffffff') as String
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
    int stripHeight = faceted ? context.config.stripHeight : 0

    int totalSpacingX = (panelCols - 1) * context.config.panelSpacing
    int totalSpacingY = (panelRows - 1) * context.config.panelSpacing
    int panelWidth = ((context.config.plotWidth() - totalSpacingX) / panelCols) as int
    int panelHeight = ((context.config.plotHeight() - totalSpacingY - panelRows * stripHeight) / panelRows) as int

    context.panels.each { PanelSpec panel ->
      int panelX = context.config.marginLeft + panel.col * (panelWidth + context.config.panelSpacing)
      int panelY = context.config.marginTop + panel.row * (panelHeight + stripHeight + context.config.panelSpacing)
      G panelGroup = context.svg.addG().id("panel-${panel.row}-${panel.col}").transform("translate($panelX, $panelY)")

      G plotArea = panelGroup
      if (faceted) {
        panelGroup.addRect(panelWidth, stripHeight)
            .x(0).y(0).fill('#e9e9e9').stroke('#cccccc').styleClass('charm-strip')
        panelGroup.addText(panel.label ?: '')
            .x(panelWidth / 2)
            .y(stripHeight - 7)
            .textAnchor('middle')
            .fontSize(10)
            .fill('#333333')
            .styleClass('charm-strip-label')
        plotArea = panelGroup.addG().id("panel-plot-${panel.row}-${panel.col}").transform("translate(0, $stripHeight)")
      }

      plotArea.addRect(panelWidth, panelHeight)
          .x(0)
          .y(0)
          .fill((context.chart.theme.raw?.panelBackground ?: '#ffffff') as String)
          .stroke('#dddddd')
          .styleClass('charm-panel')

      gridRenderer.render(plotArea, context, panelWidth, panelHeight)
      G dataLayer = plotArea.addG().id("data-${panel.row}-${panel.col}")
      String clipId = "panel-clip-${panel.row}-${panel.col}"
      context.defs.addClipPath().id(clipId).addRect(panelWidth, panelHeight).x(0).y(0)
      dataLayer.addAttribute('clip-path', "url(#${clipId})")

      context.chart.layers.each { LayerSpec layer ->
        Aes aes = effectiveAes(context.chart.aes, layer)
        List<LayerData> layerData = runPipeline(context, layer, aes, panel.rowIndexes)
        renderLayer(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      }
      axisRenderer.render(plotArea, context, panelWidth, panelHeight)
    }
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

  private List<LayerData> runPipeline(RenderContext context, LayerSpec layer, Aes aes, List<Integer> rowIndexes) {
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

    List<LayerData> mapped = mapData(context.chart.data, aes, rowIndexes)
    List<LayerData> statData = applyStat(layer, mapped)
    List<LayerData> posData = applyPosition(layer, statData)
    List<LayerData> result = applyCoord(context.chart.coord, posData)
    layerCache[rowKey] = result
    result
  }

  private List<LayerData> mapData(se.alipsa.matrix.core.Matrix data, Aes aes, List<Integer> rowIndexes) {
    List<LayerData> values = []
    rowIndexes.each { int rowIndex ->
      LayerData datum = new LayerData(rowIndex: rowIndex)
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

  private static Object readValue(se.alipsa.matrix.core.Matrix data, int rowIndex, String columnName) {
    columnName == null ? null : data[rowIndex, columnName]
  }

  private static List<LayerData> applyStat(LayerSpec layer, List<LayerData> mapped) {
    StatEngine.apply(layer, mapped)
  }

  private static List<LayerData> applyPosition(LayerSpec layer, List<LayerData> data) {
    se.alipsa.matrix.charm.render.position.PositionEngine.apply(layer, data)
  }

  private static List<LayerData> applyCoord(se.alipsa.matrix.charm.CoordSpec coord, List<LayerData> data) {
    se.alipsa.matrix.charm.render.coord.CoordEngine.apply(coord, data)
  }


  private static Aes effectiveAes(Aes plotAes, LayerSpec layer) {
    Aes aes = layer.inheritAes ? plotAes.copy() : new Aes()
    if (layer.aes != null) {
      aes.apply(layer.aes.mappings())
    }
    aes
  }

  private void renderLabels(RenderContext context) {
    String textColor = (context.chart.theme.text?.color ?: '#222222') as String
    BigDecimal titleSize = (context.chart.theme.text?.titleSize ?: 16) as BigDecimal
    BigDecimal labelSize = (context.chart.theme.text?.size ?: 12) as BigDecimal

    // When coord is FLIP, swap x/y axis labels since data axes are swapped
    boolean flipped = context.chart.coord?.type == se.alipsa.matrix.charm.CharmCoordType.FLIP
    String xLabelText = flipped ? context.chart.labels?.y : context.chart.labels?.x
    String yLabelText = flipped ? context.chart.labels?.x : context.chart.labels?.y

    if (context.chart.labels?.title) {
      context.svg.addText(context.chart.labels.title)
          .x(context.config.width / 2)
          .y(30)
          .textAnchor('middle')
          .fontSize(titleSize)
          .fill(textColor)
          .styleClass('charm-title')
    }
    if (xLabelText) {
      context.svg.addText(xLabelText)
          .x(context.config.marginLeft + context.config.plotWidth() / 2)
          .y(context.config.height - 10)
          .textAnchor('middle')
          .fontSize(labelSize)
          .fill(textColor)
          .styleClass('charm-x-label')
    }
    if (yLabelText) {
      int yMid = context.config.marginTop + context.config.plotHeight().intdiv(2)
      context.svg.addText(yLabelText)
          .x(18)
          .y(yMid)
          .transform("rotate(-90, 18, $yMid)")
          .textAnchor('middle')
          .fontSize(labelSize)
          .fill(textColor)
          .styleClass('charm-y-label')
    }
  }

  private static PanelSpec defaultPanel(int rowCount) {
    PanelSpec panel = new PanelSpec(row: 0, col: 0, label: null)
    panel.rowIndexes = (0..<rowCount).collect { int idx -> idx }
    panel
  }
}
