package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.Geom
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.Position
import se.alipsa.matrix.charm.Stat
import se.alipsa.matrix.charm.util.NumberCoercionUtil

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

    context.xScale = ScaleModel.train(xValues, context.chart.scale.x, 0, context.config.plotWidth())
    context.yScale = ScaleModel.train(yValues, context.chart.scale.y, context.config.plotHeight(), 0)
    context.colorScale = colorValues.find { Object v -> v != null } == null
        ? null
        : ScaleModel.trainColor(colorValues, context.chart.scale.color)
    context.fillScale = fillValues.find { Object v -> v != null } == null
        ? null
        : ScaleModel.trainColor(fillValues, context.chart.scale.fill)
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
    switch (layer.geom) {
      case Geom.POINT -> renderPoints(dataLayer, context, layer, layerData)
      case Geom.LINE, Geom.SMOOTH -> renderLines(dataLayer, context, layer, layerData)
      case Geom.TILE -> renderTiles(dataLayer, context, layer, layerData)
      case Geom.BAR, Geom.COL, Geom.HISTOGRAM -> renderBars(dataLayer, context, layer, layerData, panelHeight)
      default -> renderPoints(dataLayer, context, layer, layerData)
    }
  }

  private void renderPoints(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal radius = NumberCoercionUtil.coerceToBigDecimal(layer.params.size) ?: context.config.pointRadius
    layerData.each { LayerData d ->
      BigDecimal x = context.xScale.transform(d.x)
      BigDecimal y = context.yScale.transform(d.y)
      if (x == null || y == null) {
        return
      }
      String fill = resolveFill(context, layer, d)
      String stroke = resolveStroke(context, layer, d)
      dataLayer.addCircle()
          .cx(x)
          .cy(y)
          .r(radius)
          .fill(fill)
          .stroke(stroke)
          .styleClass('charm-point')
    }
  }

  private void renderLines(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }
    List<LayerData> sorted = new ArrayList<>(layerData)
    sorted.sort { LayerData a, LayerData b ->
      BigDecimal x1 = NumberCoercionUtil.coerceToBigDecimal(a.x) ?: 0
      BigDecimal x2 = NumberCoercionUtil.coerceToBigDecimal(b.x) ?: 0
      x1 <=> x2
    }
    String stroke = resolveStroke(context, layer, sorted.first())
    BigDecimal width = NumberCoercionUtil.coerceToBigDecimal(layer.params.lineWidth) ?: 2
    for (int i = 0; i < sorted.size() - 1; i++) {
      BigDecimal x1 = context.xScale.transform(sorted[i].x)
      BigDecimal y1 = context.yScale.transform(sorted[i].y)
      BigDecimal x2 = context.xScale.transform(sorted[i + 1].x)
      BigDecimal y2 = context.yScale.transform(sorted[i + 1].y)
      if (x1 == null || y1 == null || x2 == null || y2 == null) {
        continue
      }
      dataLayer.addLine(x1, y1, x2, y2)
          .stroke(stroke)
          .strokeWidth(width)
          .styleClass(layer.geom == Geom.SMOOTH ? 'charm-smooth' : 'charm-line')
    }
  }

  private void renderTiles(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal tileWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.width) ?: 10
    BigDecimal tileHeight = NumberCoercionUtil.coerceToBigDecimal(layer.params.height) ?: 10
    layerData.each { LayerData d ->
      BigDecimal x = context.xScale.transform(d.x)
      BigDecimal y = context.yScale.transform(d.y)
      if (x == null || y == null) {
        return
      }
      dataLayer.addRect(tileWidth, tileHeight)
          .x(x - tileWidth / 2)
          .y(y - tileHeight / 2)
          .fill(resolveFill(context, layer, d))
          .stroke(resolveStroke(context, layer, d))
          .styleClass('charm-tile')
    }
  }

  private void renderBars(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      List<LayerData> layerData,
      int panelHeight
  ) {
    if (layerData.isEmpty()) {
      return
    }
    BigDecimal baseline = context.yScale.transform(0)
    if (baseline == null) {
      baseline = panelHeight
    }
    boolean discreteX = context.xScale.discrete
    BigDecimal barWidth
    if (discreteX) {
      BigDecimal step = context.xScale.levels.isEmpty() ? 20 : (context.xScale.rangeEnd - context.xScale.rangeStart) / context.xScale.levels.size()
      barWidth = step * 0.75
    } else {
      barWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.barWidth) ?: 12
    }

    layerData.each { LayerData d ->
      BigDecimal yValue = context.yScale.transform(d.y)
      if (yValue == null) {
        return
      }
      BigDecimal xLeft
      BigDecimal width
      if (d.meta.containsKey('binStart') && d.meta.containsKey('binEnd')) {
        BigDecimal xStart = context.xScale.transform(d.meta.binStart)
        BigDecimal xEnd = context.xScale.transform(d.meta.binEnd)
        if (xStart == null || xEnd == null) {
          return
        }
        xLeft = [xStart, xEnd].min()
        width = (xEnd - xStart).abs()
      } else {
        BigDecimal xCenter = context.xScale.transform(d.x)
        if (xCenter == null) {
          return
        }
        xLeft = xCenter - barWidth / 2
        width = barWidth
      }
      BigDecimal rectY = [yValue, baseline].min()
      BigDecimal rectHeight = (yValue - baseline).abs()
      dataLayer.addRect(width, rectHeight)
          .x(xLeft)
          .y(rectY)
          .fill(resolveFill(context, layer, d))
          .stroke(resolveStroke(context, layer, d))
          .styleClass('charm-bar')
    }
  }

  private List<LayerData> runPipeline(RenderContext context, LayerSpec layer, Aes aes, List<Integer> rowIndexes) {
    List<LayerData> mapped = mapData(context.chart.data, aes, rowIndexes)
    List<LayerData> statData = applyStat(layer, mapped)
    applyPosition(layer, statData)
  }

  private List<LayerData> mapData(se.alipsa.matrix.core.Matrix data, Aes aes, List<Integer> rowIndexes) {
    List<LayerData> values = []
    rowIndexes.each { int rowIndex ->
      LayerData datum = new LayerData(rowIndex: rowIndex)
      datum.x = readValue(data, rowIndex, aes.x?.columnName())
      datum.y = readValue(data, rowIndex, aes.y?.columnName())
      datum.color = readValue(data, rowIndex, aes.color?.columnName())
      datum.fill = readValue(data, rowIndex, aes.fill?.columnName())
      values << datum
    }
    values
  }

  private static Object readValue(se.alipsa.matrix.core.Matrix data, int rowIndex, String columnName) {
    columnName == null ? null : data[rowIndex, columnName]
  }

  private List<LayerData> applyStat(LayerSpec layer, List<LayerData> mapped) {
    if (layer.geom == Geom.HISTOGRAM) {
      return histogramStat(layer, mapped)
    }
    if (layer.stat == Stat.SMOOTH) {
      return smoothStat(mapped)
    }
    mapped
  }

  private static List<LayerData> applyPosition(LayerSpec layer, List<LayerData> data) {
    if (layer.position != Position.STACK || !(layer.geom in [Geom.BAR, Geom.COL, Geom.HISTOGRAM])) {
      return data
    }
    Map<String, BigDecimal> cumulative = [:]
    List<LayerData> stacked = []
    data.each { LayerData datum ->
      String key = datum.x?.toString() ?: ''
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y) ?: 0
      BigDecimal next = (cumulative[key] ?: 0) + y
      cumulative[key] = next
      LayerData updated = new LayerData(
          x: datum.x,
          y: next,
          color: datum.color,
          fill: datum.fill,
          rowIndex: datum.rowIndex,
          meta: new LinkedHashMap<>(datum.meta)
      )
      stacked << updated
    }
    stacked
  }

  private static List<LayerData> smoothStat(List<LayerData> mapped) {
    List<LayerData> numeric = mapped.findAll { LayerData d ->
      NumberCoercionUtil.coerceToBigDecimal(d.x) != null && NumberCoercionUtil.coerceToBigDecimal(d.y) != null
    }
    if (numeric.size() < 2) {
      return mapped
    }
    BigDecimal n = numeric.size()
    BigDecimal sumX = numeric.sum { LayerData d -> NumberCoercionUtil.coerceToBigDecimal(d.x) } as BigDecimal
    BigDecimal sumY = numeric.sum { LayerData d -> NumberCoercionUtil.coerceToBigDecimal(d.y) } as BigDecimal
    BigDecimal sumXY = numeric.sum { LayerData d ->
      NumberCoercionUtil.coerceToBigDecimal(d.x) * NumberCoercionUtil.coerceToBigDecimal(d.y)
    } as BigDecimal
    BigDecimal sumX2 = numeric.sum { LayerData d ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(d.x)
      x * x
    } as BigDecimal
    BigDecimal denominator = n * sumX2 - sumX * sumX
    if (denominator == 0) {
      return mapped
    }
    BigDecimal slope = (n * sumXY - sumX * sumY) / denominator
    BigDecimal intercept = (sumY - slope * sumX) / n

    BigDecimal minX = numeric.collect { LayerData d -> NumberCoercionUtil.coerceToBigDecimal(d.x) }.min()
    BigDecimal maxX = numeric.collect { LayerData d -> NumberCoercionUtil.coerceToBigDecimal(d.x) }.max()
    LayerData p1 = new LayerData(x: minX, y: intercept + slope * minX, color: numeric.first().color, fill: numeric.first().fill, rowIndex: -1)
    LayerData p2 = new LayerData(x: maxX, y: intercept + slope * maxX, color: numeric.first().color, fill: numeric.first().fill, rowIndex: -1)
    [p1, p2]
  }

  private static List<LayerData> histogramStat(LayerSpec layer, List<LayerData> mapped) {
    List<BigDecimal> values = mapped.collect { LayerData d ->
      NumberCoercionUtil.coerceToBigDecimal(d.x)
    }.findAll { BigDecimal v -> v != null } as List<BigDecimal>
    if (values.isEmpty()) {
      return []
    }
    int bins = (layer.params.bins instanceof Number) ? (layer.params.bins as Number).intValue() : 10
    bins = bins < 1 ? 10 : bins
    BigDecimal min = values.min()
    BigDecimal max = values.max()
    if (min == max) {
      max = max + 1
    }
    BigDecimal width = (max - min) / bins
    List<Integer> counts = new ArrayList<>(Collections.nCopies(bins, 0))
    values.each { BigDecimal value ->
      int idx = ((value - min) / width) as int
      idx = Math.min(Math.max(idx, 0), bins - 1)
      counts[idx] = counts[idx] + 1
    }
    List<LayerData> histogram = []
    for (int i = 0; i < bins; i++) {
      BigDecimal start = min + width * i
      BigDecimal end = start + width
      BigDecimal center = start + width / 2
      LayerData datum = new LayerData(
          x: center,
          y: counts[i],
          color: mapped.first()?.color,
          fill: mapped.first()?.fill,
          rowIndex: -1
      )
      datum.meta.binStart = start
      datum.meta.binEnd = end
      histogram << datum
    }
    histogram
  }

  private static Aes effectiveAes(Aes plotAes, LayerSpec layer) {
    Aes aes = layer.inheritAes ? plotAes.copy() : new Aes()
    if (layer.aes != null) {
      aes.apply(layer.aes.mappings())
    }
    aes
  }

  private static String resolveStroke(RenderContext context, LayerSpec layer, LayerData datum) {
    if (datum.color != null && context.colorScale != null) {
      return context.colorScale.colorFor(datum.color)
    }
    if (layer.params.color != null) {
      return layer.params.color.toString()
    }
    '#1f77b4'
  }

  private static String resolveFill(RenderContext context, LayerSpec layer, LayerData datum) {
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

  private void renderLabels(RenderContext context) {
    String textColor = (context.chart.theme.text?.color ?: '#222222') as String
    BigDecimal titleSize = (context.chart.theme.text?.titleSize ?: 16) as BigDecimal
    BigDecimal labelSize = (context.chart.theme.text?.size ?: 12) as BigDecimal

    if (context.chart.labels?.title) {
      context.svg.addText(context.chart.labels.title)
          .x(context.config.width / 2)
          .y(30)
          .textAnchor('middle')
          .fontSize(titleSize)
          .fill(textColor)
          .styleClass('charm-title')
    }
    if (context.chart.labels?.x) {
      context.svg.addText(context.chart.labels.x)
          .x(context.config.marginLeft + context.config.plotWidth() / 2)
          .y(context.config.height - 10)
          .textAnchor('middle')
          .fontSize(labelSize)
          .fill(textColor)
          .styleClass('charm-x-label')
    }
    if (context.chart.labels?.y) {
      int yMid = context.config.marginTop + context.config.plotHeight().intdiv(2)
      context.svg.addText(context.chart.labels.y)
          .x(18)
          .y(yMid)
          .transform("rotate(-90, 18, $yMid)")
          .textAnchor('middle')
          .fontSize(labelSize)
          .fill(textColor)
          .styleClass('charm-y-label')
    }
  }
}
