package se.alipsa.matrix.pict

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
import se.alipsa.matrix.charm.geom.LineBuilder
import se.alipsa.matrix.charm.geom.PieBuilder
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
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
class CharmBridge {

  private static final String AES_X = 'x'
  private static final String AES_Y = 'y'
  private static final String AES_SERIES = 'series'
  private static final String AES_SIZE = 'size'
  private static final String AES_GROUP = 'group'
  private static final String AES_COLOR = 'color'
  private static final String AES_FILL = 'fill'

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
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  private static PlotSpec buildAreaSpec(AreaChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_FILL): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
    spec.addLayer(new AreaBuilder())
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
      spec.addLayer(new BarBuilder().position(position))
    } else {
      spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_FILL): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
      spec.addLayer(new ColBuilder().position(position))
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
    spec.mapping([(AES_X): AES_X, (AES_Y): AES_Y])
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
    spec.addLayer(new HistogramBuilder().bins(chart.numberOfBins))
    applyLabelsAndTheme(spec, chart as Chart)
    spec
  }

  private static PlotSpec buildLineSpec(LineChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [(AES_X): AES_X, (AES_Y): AES_Y, (AES_COLOR): AES_SERIES] : [(AES_X): AES_X, (AES_Y): AES_Y])
    spec.addLayer(new LineBuilder())
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
    spec.addLayer(new PointBuilder())
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
    spec.addLayer(new PointBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
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

  /**
   * Applies labels and theme from the legacy chart to the Charm PlotSpec.
   */
  private static void applyLabelsAndTheme(PlotSpec spec, Chart chart) {
    se.alipsa.matrix.charm.LabelsSpec labels = spec.labels as se.alipsa.matrix.charm.LabelsSpec
    applyLabels(spec, labels, chart)
    applyAxisScales(spec, chart)

    se.alipsa.matrix.charm.ThemeSpec theme = spec.theme as se.alipsa.matrix.charm.ThemeSpec
    applyThemeBackgrounds(theme, chart)
    applyAxisVisibility(theme, chart)
    applyLegend(theme, labels, chart.legend)
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

  private static void applyAxisVisibility(se.alipsa.matrix.charm.ThemeSpec theme, Chart chart) {
    if (chart.style?.xAxisVisible == false) {
      theme.axisLineX = null
      theme.axisTextX = null
      theme.axisTicksX = null
      theme.axisTitleX = null
      theme.explicitNulls.addAll(['axisLineX', 'axisTextX', 'axisTicksX', 'axisTitleX'])
    }
    if (chart.style?.yAxisVisible == false) {
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
