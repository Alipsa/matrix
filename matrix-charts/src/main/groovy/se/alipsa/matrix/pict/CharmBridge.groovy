package se.alipsa.matrix.pict

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.LegendDirection
import se.alipsa.matrix.charm.LegendPosition
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.Charts
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

/**
 * Bridge that converts the legacy {@code charts} data model
 * into Charm {@link se.alipsa.matrix.charm.Chart} objects.
 *
 * <p>Each chart type factory (AreaChart, BarChart, etc.) populates a legacy
 * {@link Chart} with categorySeries, valueSeries, style, title, etc.
 * This bridge converts that into a Charm PlotSpec → immutable Chart → SVG.</p>
 */
@CompileStatic
class CharmBridge {

  /**
   * Converts a legacy charts {@link Chart} to a Charm {@link se.alipsa.matrix.charm.Chart}
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
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  private static PlotSpec buildAreaSpec(AreaChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [x: 'x', y: 'y', fill: 'series'] : [x: 'x', y: 'y'])
    spec.addLayer(new AreaBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildBarSpec(BarChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    boolean horizontal = chart.direction == ChartDirection.HORIZONTAL
    PositionSpec position = chart.stacked ? PositionSpec.of(CharmPositionType.STACK) : PositionSpec.of(CharmPositionType.IDENTITY)

    PlotSpec spec = Charts.plot(data)
    if (horizontal) {
      spec.mapping(multiSeries ? [x: 'y', y: 'x', fill: 'series'] : [x: 'y', y: 'x'])
      spec.addLayer(new BarBuilder().position(position))
    } else {
      spec.mapping(multiSeries ? [x: 'x', y: 'y', fill: 'series'] : [x: 'x', y: 'y'])
      spec.addLayer(new ColBuilder().position(position))
    }
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildBoxSpec(BoxChart chart) {
    List<List<?>> rows = []
    List<?> categories = chart.categorySeries
    List<List<?>> allValues = chart.valueSeries
    for (int idx = 0; idx < categories.size(); idx++) {
      String category = categories[idx].toString()
      List<Number> values = allValues[idx] as List<Number>
      for (Number val : values) {
        rows.add([category, val] as List<?>)
      }
    }
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .build()

    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x', y: 'y'])
    spec.addLayer(new BoxplotBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildHistogramSpec(Histogram chart) {
    List<List<?>> rows = []
    for (Number val : chart.originalData) {
      rows.add([val] as List<?>)
    }
    Matrix data = Matrix.builder()
        .columnNames('x')
        .rows(rows)
        .build()

    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x'])
    spec.addLayer(new HistogramBuilder().bins(chart.numberOfBins))
    applyLabelsAndTheme(spec, chart as Chart)
    spec
  }

  private static PlotSpec buildLineSpec(LineChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    boolean multiSeries = chart.valueSeries.size() > 1
    PlotSpec spec = Charts.plot(data)
    spec.mapping(multiSeries ? [x: 'x', y: 'y', color: 'series'] : [x: 'x', y: 'y'])
    spec.addLayer(new LineBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildPieSpec(PieChart chart) {
    List<?> categories = chart.categorySeries
    List<?> values = chart.valueSeries[0]
    List<List<?>> rows = []
    for (int idx = 0; idx < categories.size(); idx++) {
      rows.add([categories[idx].toString(), values[idx]] as List<?>)
    }
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types([String, BigDecimal])
        .build()

    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x', y: 'y', fill: 'x'])
    spec.addLayer(new PieBuilder())
    applyLabelsAndTheme(spec, chart)
    spec
  }

  private static PlotSpec buildScatterSpec(ScatterChart chart) {
    Matrix data = buildLongFormatMatrix(chart)
    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x', y: 'y'])
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
        rows.add([categories[idx], valueLists[0][idx]] as List<?>)
      }
      return Matrix.builder()
          .columnNames('x', 'y')
          .rows(rows)
          .build()
    }

    List<List<?>> rows = []
    for (int seriesIdx = 0; seriesIdx < valueLists.size(); seriesIdx++) {
      List<?> values = valueLists[seriesIdx]
      String seriesName = seriesNames != null && seriesIdx < seriesNames.size()
          ? seriesNames[seriesIdx] : "series${seriesIdx}"
      for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
        rows.add([categories[catIdx], values[catIdx], seriesName] as List<?>)
      }
    }
    Matrix.builder()
        .columnNames('x', 'y', 'series')
        .rows(rows)
        .build()
  }

  /**
   * Applies labels and theme from the legacy chart to the Charm PlotSpec.
   */
  private static void applyLabelsAndTheme(PlotSpec spec, Chart chart) {
    se.alipsa.matrix.charm.LabelsSpec labels = spec.labels as se.alipsa.matrix.charm.LabelsSpec
    if (chart.style?.titleVisible != false && chart.title) {
      labels.title = chart.title
    }
    if (chart.xAxisTitle) {
      labels.x = chart.xAxisTitle
    }
    if (chart.yAxisTitle) {
      labels.y = chart.yAxisTitle
    }

    se.alipsa.matrix.charm.ThemeSpec theme = spec.theme as se.alipsa.matrix.charm.ThemeSpec
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
    Legend legend = chart.legend
    if (legend != null) {
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
      if (legend.title) {
        labels.guides['color'] = legend.title
        labels.guides['fill'] = legend.title
      }
    }
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

  private static String colorToHex(Color color) {
    if (color == null) {
      return null
    }
    String.format('#%02x%02x%02x', color.red, color.green, color.blue)
  }
}
