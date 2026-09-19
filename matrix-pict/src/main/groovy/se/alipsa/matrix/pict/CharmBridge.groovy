package se.alipsa.matrix.pict

import static se.alipsa.matrix.ext.NumberExtension.PI

import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.LegendDirection
import se.alipsa.matrix.charm.LegendPosition
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.geom.AreaBuilder
import se.alipsa.matrix.charm.geom.BarBuilder
import se.alipsa.matrix.charm.geom.BoxplotBuilder
import se.alipsa.matrix.charm.geom.ColBuilder
import se.alipsa.matrix.charm.geom.HistogramBuilder
import se.alipsa.matrix.charm.geom.LayerBuilder
import se.alipsa.matrix.charm.geom.LineBuilder
import se.alipsa.matrix.charm.geom.PathBuilder
import se.alipsa.matrix.charm.geom.PieBuilder
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.charm.geom.PolygonBuilder
import se.alipsa.matrix.charm.geom.SegmentBuilder
import se.alipsa.matrix.charm.geom.TextBuilder
import se.alipsa.matrix.charm.geom.TileBuilder
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.charm.render.scale.ColorScaleUtil
import se.alipsa.matrix.core.Matrix

import java.awt.Color
import java.awt.Font

/**
 * Bridge that converts the legacy {@code charts} data model
 * into Charm {@link se.alipsa.matrix.charm.Chart} objects.
 *
 * <p>Each chart type factory (AreaChart, BarChart, etc.) populates a legacy
 * {@link Chart} with categorySeries, valueSeries, style, title, etc.
 * This bridge converts that into a Charm PlotSpec → immutable Chart → SVG.</p>
 */
@SuppressWarnings([
    'DuplicateListLiteral', 'DuplicateNumberLiteral', 'DuplicateStringLiteral',
    'ReturnsNullInsteadOfEmptyCollection', 'UnnecessaryCollectCall'
])
class CharmBridge {

  private static final String AES_X = 'x'
  private static final String AES_Y = 'y'
  private static final String AES_SERIES = 'series'
  private static final String AES_SIZE = 'size'
  private static final String AES_GROUP = 'group'
  private static final String AES_COLOR = 'color'
  private static final String AES_FILL = 'fill'
  private static final String AES_VALUE = 'value'
  private static final String AES_LABEL = 'label'
  private static final String AES_XMIN = 'xmin'
  private static final String AES_XMAX = 'xmax'
  private static final String AES_YMIN = 'ymin'
  private static final String AES_YMAX = 'ymax'
  private static final String AES_XEND = 'xend'
  private static final String AES_YEND = 'yend'
  private static final String COL_TEXT = 'text'
  private static final String COL_ROW = 'row'
  private static final String PARAM_COLOR = 'color'
  private static final String PARAM_FILL = 'fill'
  private static final BigDecimal HALF = 0.5
  private static final BigDecimal RADAR_LABEL_RADIUS = 1.12
  private static final BigDecimal RADAR_LIMIT = 1.25
  private static final String RADAR_GRID_COLOR = '#cccccc'
  private static final String DEFAULT_GRADIENT_LOW = '#132B43'
  private static final String DEFAULT_GRADIENT_HIGH = '#56B1F7'

  /**
   * Converts a pict chart {@link Chart} to a Charm {@link se.alipsa.matrix.charm.Chart}
   * using default dimensions (800x600).
   *
   * @param chart the legacy chart
   * @return an immutable Charm chart
   */
  static se.alipsa.matrix.charm.Chart convert(Chart chart) {
    buildSpec(chart).build()
  }

  /**
   * Renders a legacy chart directly to SVG with explicit dimensions.
   *
   * @param chart the legacy chart
   * @param width target width
   * @param height target height
   * @return rendered SVG
   */
  static se.alipsa.groovy.svg.Svg renderSvg(Chart chart, int width, int height) {
    se.alipsa.matrix.charm.Chart charmChart = convert(chart)
    RenderConfig config = new RenderConfig(width: width, height: height)
    new CharmRenderer().render(charmChart, config)
  }

  private static PlotSpec buildSpec(Chart chart) {
    switch (chart) {
      case AreaChart -> buildAreaSpec(chart as AreaChart)
      case BarChart -> buildBarSpec(chart as BarChart)
      case BoxChart -> buildBoxSpec(chart as BoxChart)
      case Histogram -> buildHistogramSpec(chart as Histogram)
      case LineChart -> buildLineSpec(chart as LineChart)
      case PieChart -> buildPieSpec(chart as PieChart)
      case ScatterChart -> buildScatterSpec(chart as ScatterChart)
      case BubbleChart -> buildBubbleSpec(chart as BubbleChart)
      case HeatmapChart -> buildHeatmapSpec(chart as HeatmapChart)
      case RadarChart -> buildRadarSpec(chart as RadarChart)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  private static PlotSpec buildAreaSpec(AreaChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_FILL): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
    AreaBuilder layer = new AreaBuilder()
    applySingleSeriesColor(layer, chart, multiSeries)
    spec.addLayer(layer)
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildBarSpec(BarChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    boolean horizontal = chart.direction == ChartDirection.HORIZONTAL
    PositionSpec position = switch (chart.chartType) {
      case ChartType.STACKED -> PositionSpec.of(CharmPositionType.STACK)
      case ChartType.GROUPED -> PositionSpec.of(CharmPositionType.DODGE)
      default -> PositionSpec.of(CharmPositionType.IDENTITY)
    }

    PlotSpec spec = Charts.plot(data)
    if (horizontal) {
      spec.mapping(multiSeries ? [(AES_X): AES_Y, (AES_Y): AES_X, (AES_FILL): AES_SERIES] : [(AES_X): AES_Y, (AES_Y): AES_X])
      BarBuilder layer = new BarBuilder()
      layer.position(position)
      applySingleSeriesColor(layer, chart, multiSeries)
      spec.addLayer(layer)
    } else {
      spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_FILL): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
      ColBuilder layer = new ColBuilder()
      layer.position(position)
      applySingleSeriesColor(layer, chart, multiSeries)
      spec.addLayer(layer)
    }
    applyLabelsAndTheme(spec, chart)
    spec
  }

  @SuppressWarnings('UnnecessaryToString')
  private static PlotSpec buildBoxSpec(BoxChart chart) {
    List<List<?>> rows = []
    List<?> categories = chart.categorySeries
    List<List<?>> allValues = chart.valueSeries
    for (int idx = 0; idx < categories.size(); idx++) {
      String category = categories[idx].toString()
      List<Number> values = allValues[idx] as List<Number>
      for (Number val : values) {
        rows.add([category, val])
      }
    }
    Matrix data = Matrix.builder()
        .columnNames(AES_X, AES_Y)
        .rows(rows)
        .build()

    PlotSpec spec = Charts.plot(data)
    boolean perBoxColors = resolveSeriesColors(chart) != null
    spec.mapping(perBoxColors ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_FILL): AES_X] : [(AES_X): AES_X, (AES_Y): AES_Y])
    spec.addLayer(new BoxplotBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildHistogramSpec(Histogram chart) {
    List<List<?>> rows = []
    for (Number val : chart.originalData) {
      rows.add([val])
    }
    Matrix data = Matrix.builder()
        .columnNames(AES_X)
        .rows(rows)
        .build()

    PlotSpec spec = Charts.plot(data)
    spec.mapping([(AES_X): AES_X])
    HistogramBuilder layer = new HistogramBuilder().bins(chart.numberOfBins)
    applySingleSeriesColor(layer, chart, false)
    spec.addLayer(layer)
    applyLabelsAndTheme(spec, chart as Chart)
    spec
  }

  private static PlotSpec buildLineSpec(LineChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_COLOR): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
    LineBuilder layer = new LineBuilder()
    applySingleSeriesColor(layer, chart, multiSeries)
    spec.addLayer(layer)
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildPieSpec(PieChart chart) {
    List<?> categories = chart.categorySeries
    List<?> values = chart.valueSeries[0]
    List<List<?>> rows = []
    for (int idx = 0; idx < categories.size(); idx++) {
      rows.add([categories[idx].toString(), values[idx]])
    }
    Matrix data = Matrix.builder()
        .columnNames(AES_X, AES_Y)
        .rows(rows)
        .types([String, BigDecimal])
        .build()

    PlotSpec spec = Charts.plot(data)
    spec.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_FILL): AES_X])
    spec.addLayer(new PieBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildScatterSpec(ScatterChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_COLOR): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
    PointBuilder layer = new PointBuilder()
    applySingleSeriesColor(layer, chart, multiSeries)
    spec.addLayer(layer)
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildBubbleSpec(BubbleChart chart) {
    List<?> xValues = chart.categorySeries
    List<?> yValues = chart.valueSeries[0]
    List<? extends Number> sizeValues = chart.sizeSeries

    if (chart.groupSeries) {
      List<List<?>> rows = []
      for (int i = 0; i < xValues.size(); i++) {
        rows.add([xValues[i], yValues[i], sizeValues[i], chart.groupSeries[i]])
      }
      Matrix data = Matrix.builder()
          .columnNames(AES_X, AES_Y, AES_SIZE, AES_GROUP)
          .rows(rows)
          .build()
      PlotSpec spec = Charts.plot(data)
      spec.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_SIZE): AES_SIZE, (AES_COLOR): AES_GROUP])
      spec.addLayer(new PointBuilder())
      applyLabelsAndTheme(spec, chart)
      return spec
    }

    List<List<?>> rows = []
    for (int i = 0; i < xValues.size(); i++) {
      rows.add([xValues[i], yValues[i], sizeValues[i]])
    }
    Matrix data = Matrix.builder()
        .columnNames(AES_X, AES_Y, AES_SIZE)
        .rows(rows)
        .build()
    PlotSpec spec = Charts.plot(data)
    spec.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_SIZE): AES_SIZE])
    PointBuilder layer = new PointBuilder()
    applySingleSeriesColor(layer, chart, false)
    spec.addLayer(layer)
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildHeatmapSpec(HeatmapChart chart) {
    int rowCount = chart.rowLabels.size()
    List<List<?>> rows = []
    chart.columnLabels.eachWithIndex { String column, int c ->
      chart.rowLabels.eachWithIndex { Object rowLabel, int r ->
        BigDecimal value = chart.values[c][r]
        BigDecimal x = c
        BigDecimal y = rowCount - 1 - r
        rows << [
            x, x - HALF, x + HALF, y, y - HALF, y + HALF, value,
            formatHeatmapValue(value, chart.valueDecimals)
        ]
      }
    }
    Matrix data = Matrix.builder()
        .columnNames(AES_X, AES_XMIN, AES_XMAX, AES_Y, AES_YMIN, AES_YMAX, AES_VALUE, COL_TEXT)
        .rows(rows)
        .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, String])
        .build()
    PlotSpec spec = Charts.plot(data)
    spec.mapping([
        (AES_X): AES_X, (AES_XMIN): AES_XMIN, (AES_XMAX): AES_XMAX,
        (AES_Y): AES_Y, (AES_YMIN): AES_YMIN, (AES_YMAX): AES_YMAX, (AES_FILL): AES_VALUE
    ])
    spec.addLayer(new TileBuilder())
    if (chart.showValues) {
      TextBuilder labels = new TextBuilder()
      labels.inheritMapping(false).mapping([
          (AES_X): AES_X, (AES_Y): AES_Y, (AES_LABEL): COL_TEXT, (AES_FILL): AES_VALUE
      ])
      if (chart.labelColor != null) {
        labels.color(colorToHex(chart.labelColor))
      } else {
        labels.autoContrastFill()
      }
      spec.addLayer(labels)
    }
    spec.scale.x(indexScale(chart.columnLabels))
    spec.scale.y(indexScale(chart.rowLabels.reverse()*.toString()))
    spec.scale.fill(heatmapFillScale(chart))
    if (!chart.legend?.title) {
      se.alipsa.matrix.charm.LabelsSpec labels = spec.labels as se.alipsa.matrix.charm.LabelsSpec
      labels.guides[AES_FILL] = chart instanceof CorrelationHeatmapChart
          ? (chart as CorrelationHeatmapChart).method
          : heatmapLegendTitle(chart)
    }
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static String heatmapLegendTitle(HeatmapChart chart) {
    chart.columnLabels.size() == 1 ? chart.columnLabels[0] : AES_VALUE
  }

  private static Scale indexScale(List<String> labels) {
    Scale scale = Scale.continuous()
    scale.params['limits'] = [-HALF, labels.size() - HALF]
    scale.params['expand'] = [0, 0]
    scale.breaks = (0..<labels.size()).collect { int i -> i as BigDecimal }
    scale.labels = labels
    scale
  }

  private static Scale heatmapFillScale(HeatmapChart chart) {
    String low = colorToHex(chart.lowColor) ?: DEFAULT_GRADIENT_LOW
    String high = colorToHex(chart.highColor) ?: DEFAULT_GRADIENT_HIGH
    Scale scale
    if (chart.midColor != null && chart.midpoint != null) {
      scale = Scale.gradient(low, high, colorToHex(chart.midColor), chart.midpoint)
    } else if (chart.midColor != null) {
      scale = Scale.gradientN([low, colorToHex(chart.midColor), high])
    } else if (chart.lowColor != null || chart.highColor != null) {
      scale = Scale.gradient(low, high)
    } else {
      scale = Scale.gradient()
    }
    if (chart.fillLimits) {
      scale.params['limits'] = chart.fillLimits
    }
    scale
  }

  private static String formatHeatmapValue(BigDecimal value, Integer decimals) {
    if (value == null) {
      return null
    }
    decimals == null ? value.stripTrailingZeros().toPlainString() :
        value.setScale(decimals, java.math.RoundingMode.HALF_UP).toPlainString()
  }

  private static PlotSpec buildRadarSpec(RadarChart chart) {
    int axisCount = chart.axisLabels.size()
    BigDecimal outer = chart.outerRadius()
    List<BigDecimal> angles = (0..<axisCount).collect { int i -> PI / 2 - (i / axisCount) * 2 * PI }
    Matrix polygonData = radarPolygonData(chart, angles, outer)
    Matrix ringData = radarRingData(chart.ringValues(), angles, outer)
    Matrix spokeData = radarSpokeData(angles)
    Matrix labelData = radarLabelData(chart.axisLabels, angles)
    PlotSpec spec = Charts.plot(polygonData)
    spec.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_GROUP): COL_ROW, (AES_COLOR): AES_SERIES, (AES_FILL): AES_SERIES])
    PathBuilder rings = new PathBuilder()
    rings.data(ringData)
    rings.inheritMapping(false)
    rings.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_GROUP): AES_GROUP])
    rings.color(RADAR_GRID_COLOR)
    spec.addLayer(rings)
    SegmentBuilder spokes = new SegmentBuilder()
    spokes.data(spokeData)
    spokes.inheritMapping(false)
    spokes.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_XEND): AES_XEND, (AES_YEND): AES_YEND])
    spokes.color(RADAR_GRID_COLOR)
    spec.addLayer(spokes)
    spec.addLayer(new PolygonBuilder().alpha(chart.fillAlpha))
    TextBuilder labels = new TextBuilder()
    labels.data(labelData)
    labels.inheritMapping(false)
    labels.mapping([(AES_X): AES_X, (AES_Y): AES_Y, (AES_LABEL): COL_TEXT])
    spec.addLayer(labels)
    spec.scale.x(unitScale())
    spec.scale.y(unitScale())
    applyLabelsAndTheme(spec, chart, false)
    se.alipsa.matrix.charm.ThemeSpec theme = spec.theme as se.alipsa.matrix.charm.ThemeSpec
    hideAxes(theme, true, true)
    theme.panelGridMajor = null
    theme.panelGridMinor = null
    theme.explicitNulls.addAll(['panelGridMajor', 'panelGridMinor'])
    spec
  }

  private static Matrix radarPolygonData(RadarChart chart, List<BigDecimal> angles, BigDecimal outer) {
    List<List<?>> rows = []
    chart.seriesValues.eachWithIndex { List<BigDecimal> values, int s ->
      String series = chart.seriesLabels[s]
      angles.eachWithIndex { BigDecimal angle, int i ->
        BigDecimal radius = values[i] / outer
        rows << [radius * angle.cos(), radius * angle.sin(), series, "row${s}".toString()]
      }
    }
    Matrix.builder().columnNames(AES_X, AES_Y, AES_SERIES, COL_ROW)
        .rows(rows).types([BigDecimal, BigDecimal, String, String]).build()
  }

  private static Matrix radarRingData(List<BigDecimal> ringValues, List<BigDecimal> angles, BigDecimal outer) {
    List<List<?>> rows = []
    ringValues.eachWithIndex { BigDecimal ringValue, int ring ->
      BigDecimal radius = ringValue / outer
      String group = "ring${ring}"
      (angles + [angles[0]]).each { BigDecimal angle -> rows << [radius * angle.cos(), radius * angle.sin(), group] }
    }
    Matrix.builder().columnNames(AES_X, AES_Y, AES_GROUP)
        .rows(rows).types([BigDecimal, BigDecimal, String]).build()
  }

  private static Matrix radarSpokeData(List<BigDecimal> angles) {
    List<List<?>> rows = []
    angles.each { BigDecimal angle -> rows << [0.0, 0.0, angle.cos(), angle.sin()] }
    Matrix.builder().columnNames(AES_X, AES_Y, AES_XEND, AES_YEND)
        .rows(rows).types([BigDecimal, BigDecimal, BigDecimal, BigDecimal]).build()
  }

  private static Matrix radarLabelData(List<String> axisLabels, List<BigDecimal> angles) {
    List<List<?>> rows = []
    angles.eachWithIndex { BigDecimal angle, int i ->
      rows << [RADAR_LABEL_RADIUS * angle.cos(), RADAR_LABEL_RADIUS * angle.sin(), axisLabels[i]]
    }
    Matrix.builder().columnNames(AES_X, AES_Y, COL_TEXT)
        .rows(rows).types([BigDecimal, BigDecimal, String]).build()
  }

  private static Scale unitScale() {
    Scale scale = Scale.continuous()
    scale.params['limits'] = [-RADAR_LIMIT, RADAR_LIMIT]
    scale.params['expand'] = [0, 0]
    scale
  }

  /**
   * Builds a long-format matrix from the legacy chart's categorySeries and valueSeries.
   * Single series: columns ['x', 'y']
   * Multi series: columns ['x', 'y', 'series']
   */
  private static Matrix buildLongFormatMatrix(Chart chart) {
    List<?> categories = chart.categorySeries
    List<List<?>> valueLists = chart.valueSeries
    List<String> seriesNames = chart.valueSeriesNames

    if (valueLists.size() == 1) {
      List<List<?>> rows = []
      for (int idx = 0; idx < categories.size(); idx++) {
        rows.add([categories[idx], valueLists[0][idx]])
      }
      return Matrix.builder()
          .columnNames(AES_X, AES_Y)
          .rows(rows)
          .build()
    }

    List<List<?>> rows = []
    for (int seriesIdx = 0; seriesIdx < valueLists.size(); seriesIdx++) {
      List<?> values = valueLists[seriesIdx]
      String seriesName = seriesNames != null && seriesIdx < seriesNames.size()
          ? seriesNames[seriesIdx] : "series${seriesIdx}"
      for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
        rows.add([categories[catIdx], values[catIdx], seriesName])
      }
    }
    Matrix.builder()
        .columnNames(AES_X, AES_Y, AES_SERIES)
        .rows(rows)
        .build()
  }

  /** Applies labels, scales, theme, legend and configured series colours. */
  private static void applyLabelsAndTheme(PlotSpec spec, Chart chart, boolean applyScales = true) {
    se.alipsa.matrix.charm.LabelsSpec labels = spec.labels as se.alipsa.matrix.charm.LabelsSpec
    applyLabels(spec, labels, chart)
    if (applyScales) {
      applyAxisScales(spec, chart)
    }

    se.alipsa.matrix.charm.ThemeSpec theme = spec.theme as se.alipsa.matrix.charm.ThemeSpec
    applyThemeBackgrounds(theme, chart)
    hideAxes(theme, chart.style?.xAxisVisible == false, chart.style?.yAxisVisible == false)
    applyLegend(theme, labels, chart.legend)
    if (!(chart instanceof HeatmapChart)) {
      applySeriesColors(spec, chart)
    }
  }

  private static void applyLabels(PlotSpec spec, se.alipsa.matrix.charm.LabelsSpec labels, Chart chart) {
    if (chart.style?.titleVisible != false && chart.title) {
      labels.title = chart.title
    }
    if (chart.xAxisTitle) {
      labels.x = chart.xAxisTitle
    }
    if (chart.yAxisTitle) {
      labels.y = chart.yAxisTitle
    }
    if (chart.style?.css?.trim()) {
      spec.stylesheet(chart.style.css)
    }
  }

  private static void applyThemeBackgrounds(se.alipsa.matrix.charm.ThemeSpec theme, Chart chart) {
    if (chart.style?.plotBackgroundColor) {
      theme.panelBackground = new se.alipsa.matrix.charm.theme.ElementRect(
          fill: colorToHex(chart.style.plotBackgroundColor)
      )
    }
    if (chart.style?.chartBackgroundColor) {
      theme.plotBackground = new se.alipsa.matrix.charm.theme.ElementRect(
          fill: colorToHex(chart.style.chartBackgroundColor)
      )
    }
  }

  private static void hideAxes(se.alipsa.matrix.charm.ThemeSpec theme, boolean x, boolean y) {
    if (x) {
      theme.axisLineX = null
      theme.axisTextX = null
      theme.axisTicksX = null
      theme.axisTitleX = null
      theme.explicitNulls.addAll(['axisLineX', 'axisTextX', 'axisTicksX', 'axisTitleX'])
    }
    if (y) {
      theme.axisLineY = null
      theme.axisTextY = null
      theme.axisTicksY = null
      theme.axisTitleY = null
      theme.explicitNulls.addAll(['axisLineY', 'axisTextY', 'axisTicksY', 'axisTitleY'])
    }
  }

  private static void applyLegend(se.alipsa.matrix.charm.ThemeSpec theme, se.alipsa.matrix.charm.LabelsSpec labels, Legend legend) {
    if (legend == null) {
      return
    }
    if (!legend.visible) {
      theme.legendPosition = LegendPosition.NONE
    } else if (legend.position) {
      theme.legendPosition = mapPosition(legend.position)
    }
    if (legend.direction) {
      theme.legendDirection = mapDirection(legend.direction)
    }
    if (legend.backgroundColor) {
      theme.legendBackground = new se.alipsa.matrix.charm.theme.ElementRect(
          fill: colorToHex(legend.backgroundColor)
      )
    }
    if (legend.font) {
      se.alipsa.matrix.charm.theme.ElementText fontElement = mapFont(legend.font)
      theme.legendText = fontElement
      theme.legendTitle = fontElement.copy()
    }
    if (legend.title) {
      labels.guides[AES_COLOR] = legend.title
      labels.guides[AES_FILL] = legend.title
    }
  }

  private static Map<String, String> resolveSeriesColors(Chart chart) {
    Style style = chart.style
    if (style == null || (!style.seriesColorMap && !style.seriesColors)) {
      return null
    }
    List<String> names = seriesNames(chart)
    List<String> defaults = ColorScaleUtil.defaultPalette(names.size())
    Map<String, String> resolved = [:]
    names.eachWithIndex { String name, int index ->
      Color configured = style.seriesColorMap?.get(name)
          ?: (index < (style.seriesColors?.size() ?: 0) ? style.seriesColors[index] : null)
      resolved[name] = configured != null ? colorToHex(configured) : defaults[index]
    }
    resolved
  }

  private static List<String> seriesNames(Chart chart) {
    if (chart instanceof RadarChart) {
      List<String> labels = new ArrayList<String>(chart.seriesLabels)
      return labels.unique(false) as List<String>
    }
    if (chart instanceof PieChart) {
      return chart.categorySeries.collect { Object category -> category.toString() } as List<String>
    }
    if (chart instanceof BubbleChart) {
      BubbleChart bubble = chart
      return bubble.groupSeries
          ? bubble.groupSeries.collect { Object group -> group.toString() }.unique() as List<String>
          : (chart.valueSeriesNames ?: [])
    }
    List<String> configured = chart.valueSeriesNames ?: []
    int seriesCount = chart.valueSeries?.size() ?: configured.size()
    (0..<seriesCount).collect { int index ->
      index < configured.size() ? configured[index] : "series${index}"
    } as List<String>
  }

  private static void applySeriesColors(PlotSpec spec, Chart chart) {
    Map<String, String> resolved = resolveSeriesColors(chart)
    if (resolved) {
      spec.scale.color(Scale.manual(resolved))
      spec.scale.fill(Scale.manual(resolved))
    }
  }

  private static void applySingleSeriesColor(LayerBuilder builder, Chart chart, boolean multiSeries) {
    if (multiSeries) {
      return
    }
    String hex = singleSeriesHex(chart)
    if (hex != null) {
      builder.param(PARAM_COLOR, hex)
      builder.param(PARAM_FILL, hex)
    }
  }

  private static String singleSeriesHex(Chart chart) {
    Map<String, String> resolved = resolveSeriesColors(chart)
    resolved ? resolved.values().first() : null
  }

  private static void applyAxisScales(PlotSpec spec, Chart chart) {
    if (chart.xAxisScale != null) {
      spec.scale.x(scaleFromAxisScale(chart.xAxisScale))
    }

    Scale yScale = chart.yAxisScale != null ? scaleFromAxisScale(chart.yAxisScale) : null
    if (chart.style?.yLabels) {
      yScale = yScale ?: Scale.continuous()
      applyYLabels(yScale, chart.style.yLabels)
    }
    if (yScale != null) {
      spec.scale.y(yScale)
    }
  }

  private static Scale scaleFromAxisScale(AxisScale axisScale) {
    Scale scale = Scale.continuous()
    scale.params['limits'] = [axisScale.start, axisScale.end]
    scale.breaks = axisBreaks(axisScale)
    scale
  }

  private static List<BigDecimal> axisBreaks(AxisScale axisScale) {
    if (axisScale.step <= 0) {
      throw new IllegalArgumentException("AxisScale step must be positive, got ${axisScale.step}")
    }
    if (axisScale.start > axisScale.end) {
      throw new IllegalArgumentException("AxisScale start (${axisScale.start}) must be <= end (${axisScale.end})")
    }
    List<BigDecimal> breaks = []
    BigDecimal current = axisScale.start
    while (current <= axisScale.end) {
      breaks << current
      current += axisScale.step
    }
    if (breaks.last() != axisScale.end) {
      breaks << axisScale.end
    }
    breaks
  }

  private static void applyYLabels(Scale scale, Map<String, String> yLabels) {
    List<String> sortedKeys = yLabels.keySet().sort { String key -> new BigDecimal(key) } as List<String>
    scale.breaks = sortedKeys.collect { String key -> new BigDecimal(key) }
    scale.labels = sortedKeys.collect { String key -> yLabels[key] }
  }

  /**
   * Maps a pict {@link Style.Position} to Charm's {@link LegendPosition}.
   */
  private static LegendPosition mapPosition(Style.Position pos) {
    switch (pos) {
      case Style.Position.TOP -> LegendPosition.TOP
      case Style.Position.BOTTOM -> LegendPosition.BOTTOM
      case Style.Position.LEFT -> LegendPosition.LEFT
      case Style.Position.RIGHT -> LegendPosition.RIGHT
      default -> LegendPosition.RIGHT
    }
  }

  /**
   * Maps a pict {@link Legend.Direction} to Charm's {@link LegendDirection}.
   */
  private static LegendDirection mapDirection(Legend.Direction dir) {
    switch (dir) {
      case Legend.Direction.HORIZONTAL -> LegendDirection.HORIZONTAL
      case Legend.Direction.VERTICAL -> LegendDirection.VERTICAL
      default -> LegendDirection.VERTICAL
    }
  }

  /**
   * Maps a java.awt.Font to Charm's {@link se.alipsa.matrix.charm.theme.ElementText}.
   */
  private static se.alipsa.matrix.charm.theme.ElementText mapFont(Font font) {
    String face = font.bold && font.italic ? 'bold.italic'
        : font.bold ? 'bold'
        : font.italic ? 'italic'
        : 'plain'
    new se.alipsa.matrix.charm.theme.ElementText(
        family: font.family,
        face: face,
        size: font.size
    )
  }

  private static String colorToHex(Color color) {
    if (color == null) {
      return null
    }
    String.format('#%02x%02x%02x', color.red, color.green, color.blue)
  }
}
