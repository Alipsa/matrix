package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmRenderException
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine
import se.alipsa.matrix.charm.render.scale.TrainedScales

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
    switch (layer.geomType) {
      case CharmGeomType.POINT -> renderPoints(dataLayer, context, layer, layerData)
      case CharmGeomType.LINE, CharmGeomType.SMOOTH -> renderLines(dataLayer, context, layer, layerData)
      case CharmGeomType.TILE -> renderTiles(dataLayer, context, layer, layerData)
      case CharmGeomType.BAR, CharmGeomType.COL, CharmGeomType.HISTOGRAM -> renderBars(dataLayer, context, layer, layerData, panelHeight)
      case CharmGeomType.AREA -> renderArea(dataLayer, context, layer, layerData, panelHeight)
      case CharmGeomType.PIE -> renderPie(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      case CharmGeomType.BOXPLOT -> renderBoxplot(dataLayer, context, layer, layerData, panelHeight)
      default -> throw new CharmRenderException("Unsupported geom type: ${layer.geomType}")
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
          .styleClass(layer.geomType == CharmGeomType.SMOOTH ? 'charm-smooth' : 'charm-line')
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
    boolean discreteX = context.xScale.isDiscrete()
    BigDecimal barWidth
    if (discreteX && context.xScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale dScale = context.xScale as DiscreteCharmScale
      BigDecimal step = dScale.levels.isEmpty() ? 20 : (dScale.rangeEnd - dScale.rangeStart) / dScale.levels.size()
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
    List<LayerData> result = applyPosition(layer, statData)
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
      values << datum
    }
    values
  }

  private static Object readValue(se.alipsa.matrix.core.Matrix data, int rowIndex, String columnName) {
    columnName == null ? null : data[rowIndex, columnName]
  }

  private List<LayerData> applyStat(LayerSpec layer, List<LayerData> mapped) {
    if (layer.geomType == CharmGeomType.HISTOGRAM) {
      return histogramStat(layer, mapped)
    }
    if (layer.geomType == CharmGeomType.BOXPLOT) {
      return boxplotStat(mapped)
    }
    if (layer.statType == CharmStatType.SMOOTH) {
      return smoothStat(mapped)
    }
    mapped
  }

  private static List<LayerData> applyPosition(LayerSpec layer, List<LayerData> data) {
    if (layer.positionType != CharmPositionType.STACK || !(layer.geomType in [CharmGeomType.BAR, CharmGeomType.COL, CharmGeomType.HISTOGRAM])) {
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
          xend: datum.xend,
          yend: datum.yend,
          xmin: datum.xmin,
          xmax: datum.xmax,
          ymin: datum.ymin,
          ymax: datum.ymax,
          size: datum.size,
          shape: datum.shape,
          alpha: datum.alpha,
          linetype: datum.linetype,
          group: datum.group,
          label: datum.label,
          weight: datum.weight,
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

  private void renderArea(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      List<LayerData> layerData,
      int panelHeight
  ) {
    if (layerData.size() < 2) {
      return
    }
    List<LayerData> sorted = new ArrayList<>(layerData)
    sorted.sort { LayerData a, LayerData b ->
      BigDecimal x1 = NumberCoercionUtil.coerceToBigDecimal(a.x) ?: 0
      BigDecimal x2 = NumberCoercionUtil.coerceToBigDecimal(b.x) ?: 0
      x1 <=> x2
    }
    BigDecimal baseline = context.yScale.transform(0)
    if (baseline == null) {
      baseline = panelHeight
    }
    List<BigDecimal[]> transformedPoints = []
    sorted.each { LayerData d ->
      BigDecimal px = context.xScale.transform(d.x)
      BigDecimal py = context.yScale.transform(d.y)
      if (px != null && py != null) {
        transformedPoints << ([px, py] as BigDecimal[])
      }
    }
    if (transformedPoints.size() < 2) {
      return
    }
    StringBuilder pathD = new StringBuilder()
    BigDecimal firstX = transformedPoints.first()[0]
    BigDecimal lastX = transformedPoints.last()[0]
    pathD.append("M ${firstX} ${baseline}")
    transformedPoints.each { BigDecimal[] pt ->
      pathD.append(" L ${pt[0]} ${pt[1]}")
    }
    pathD.append(" L ${lastX} ${baseline} Z")
    String fill = resolveFill(context, layer, sorted.first())
    String stroke = resolveStroke(context, layer, sorted.first())
    dataLayer.addPath().d(pathD.toString())
        .fill(fill)
        .stroke(stroke)
        .addAttribute('opacity', '0.7')
        .styleClass('charm-area')
  }

  private void renderPie(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      List<LayerData> layerData,
      int panelWidth,
      int panelHeight
  ) {
    if (layerData.isEmpty()) {
      return
    }
    double cx = panelWidth / 2.0d
    double cy = panelHeight / 2.0d
    double radius = Math.min(panelWidth, panelHeight) * 0.4d
    List<BigDecimal> values = layerData.collect { LayerData d ->
      BigDecimal v = NumberCoercionUtil.coerceToBigDecimal(d.y)
      v != null && v > 0 ? v : 0.0
    }
    BigDecimal total = values.sum() as BigDecimal
    if (total == 0) {
      return
    }
    double startAngle = -Math.PI / 2.0d
    layerData.eachWithIndex { LayerData d, int idx ->
      BigDecimal sliceValue = values[idx]
      if (sliceValue == 0) {
        return
      }
      double sweepAngle = (sliceValue / total) as double * 2.0d * Math.PI
      double endAngle = startAngle + sweepAngle
      double x1 = cx + radius * Math.cos(startAngle)
      double y1 = cy + radius * Math.sin(startAngle)
      double x2 = cx + radius * Math.cos(endAngle)
      double y2 = cy + radius * Math.sin(endAngle)
      int largeArc = sweepAngle > Math.PI ? 1 : 0
      String pathD = "M ${cx} ${cy} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2} Z"
      String fill = resolveFill(context, layer, d)
      dataLayer.addPath().d(pathD)
          .fill(fill)
          .stroke('#ffffff')
          .addAttribute('stroke-width', '1')
          .styleClass('charm-pie')
      startAngle = endAngle
    }
  }

  private void renderBoxplot(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      List<LayerData> layerData,
      int panelHeight
  ) {
    if (layerData.isEmpty()) {
      return
    }
    boolean discreteX = context.xScale.isDiscrete()
    BigDecimal boxWidth
    if (discreteX && context.xScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale dScale = context.xScale as DiscreteCharmScale
      BigDecimal step = dScale.levels.isEmpty() ? 20
          : (dScale.rangeEnd - dScale.rangeStart) / dScale.levels.size()
      boxWidth = step * 0.5
    } else {
      boxWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.boxWidth) ?: 20
    }
    String fill = layer.params.fill?.toString() ?: '#1f77b4'
    String stroke = layer.params.color?.toString() ?: '#333333'

    layerData.each { LayerData d ->
      BigDecimal xCenter = context.xScale.transform(d.x)
      if (xCenter == null) {
        return
      }
      BigDecimal q1 = context.yScale.transform(d.meta.q1)
      BigDecimal median = context.yScale.transform(d.meta.median)
      BigDecimal q3 = context.yScale.transform(d.meta.q3)
      BigDecimal whiskerLow = context.yScale.transform(d.meta.whiskerLow)
      BigDecimal whiskerHigh = context.yScale.transform(d.meta.whiskerHigh)
      if ([q1, median, q3, whiskerLow, whiskerHigh].any { it == null }) {
        return
      }
      BigDecimal xLeft = xCenter - boxWidth / 2
      BigDecimal boxTop = [q1, q3].min()
      BigDecimal boxHeight = (q3 - q1).abs()
      dataLayer.addRect(boxWidth, boxHeight)
          .x(xLeft).y(boxTop)
          .fill(fill).stroke(stroke)
          .styleClass('charm-boxplot-box')
      dataLayer.addLine(xLeft, median, xLeft + boxWidth, median)
          .stroke(stroke).strokeWidth(2)
          .styleClass('charm-boxplot-median')
      dataLayer.addLine(xCenter, boxTop, xCenter, whiskerHigh)
          .stroke(stroke).styleClass('charm-boxplot-whisker')
      BigDecimal boxBottom = [q1, q3].max()
      dataLayer.addLine(xCenter, boxBottom, xCenter, whiskerLow)
          .stroke(stroke).styleClass('charm-boxplot-whisker')
      BigDecimal capHalf = boxWidth / 4
      dataLayer.addLine(xCenter - capHalf, whiskerHigh, xCenter + capHalf, whiskerHigh)
          .stroke(stroke).styleClass('charm-boxplot-cap')
      dataLayer.addLine(xCenter - capHalf, whiskerLow, xCenter + capHalf, whiskerLow)
          .stroke(stroke).styleClass('charm-boxplot-cap')
      List outliers = d.meta.outliers as List ?: []
      outliers.each { Object outlierVal ->
        BigDecimal oy = context.yScale.transform(outlierVal)
        if (oy != null) {
          dataLayer.addCircle().cx(xCenter).cy(oy).r(3)
              .fill('none').stroke(stroke)
              .styleClass('charm-boxplot-outlier')
        }
      }
    }
  }

  private static List<LayerData> boxplotStat(List<LayerData> mapped) {
    Map<String, List<BigDecimal>> groups = [:]
    Map<String, LayerData> templates = [:]
    mapped.each { LayerData d ->
      String key = d.x?.toString() ?: ''
      BigDecimal yVal = NumberCoercionUtil.coerceToBigDecimal(d.y)
      if (yVal != null) {
        groups.computeIfAbsent(key) { [] } << yVal
        if (!templates.containsKey(key)) {
          templates[key] = d
        }
      }
    }
    List<LayerData> result = []
    groups.each { String key, List<BigDecimal> values ->
      values.sort()
      int n = values.size()
      BigDecimal q1 = percentile(values, 0.25)
      BigDecimal median = percentile(values, 0.5)
      BigDecimal q3 = percentile(values, 0.75)
      BigDecimal iqr = q3 - q1
      BigDecimal lowerFence = q1 - 1.5 * iqr
      BigDecimal upperFence = q3 + 1.5 * iqr
      BigDecimal whiskerLow = values.find { it >= lowerFence } ?: values.first()
      BigDecimal whiskerHigh = values.reverse().find { it <= upperFence } ?: values.last()
      List<BigDecimal> outliers = values.findAll { it < lowerFence || it > upperFence }
      LayerData template = templates[key]
      LayerData datum = new LayerData(
          x: template.x,
          y: median,
          color: template.color,
          fill: template.fill,
          rowIndex: -1
      )
      datum.meta.q1 = q1
      datum.meta.median = median
      datum.meta.q3 = q3
      datum.meta.whiskerLow = whiskerLow
      datum.meta.whiskerHigh = whiskerHigh
      datum.meta.outliers = outliers
      result << datum
    }
    result
  }

  private static BigDecimal percentile(List<BigDecimal> sorted, BigDecimal p) {
    if (sorted.isEmpty()) {
      return 0
    }
    if (sorted.size() == 1) {
      return sorted.first()
    }
    BigDecimal index = p * (sorted.size() - 1)
    int lower = index.intValue()
    int upper = lower + 1
    if (upper >= sorted.size()) {
      return sorted.last()
    }
    BigDecimal fraction = index - lower
    sorted[lower] + fraction * (sorted[upper] - sorted[lower])
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

  private static PanelSpec defaultPanel(int rowCount) {
    PanelSpec panel = new PanelSpec(row: 0, col: 0, label: null)
    panel.rowIndexes = (0..<rowCount).collect { int idx -> idx }
    panel
  }
}
