package se.alipsa.matrix.pict

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmPositionType
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
    List<List> rows = []
    chart.categorySeries.eachWithIndex { Object category, int idx ->
      List values = chart.valueSeries[idx]
      values.each { Object val ->
        List row = [category.toString(), val]
        rows.add(row)
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
    List<List> rows = []
    chart.originalData.each { Number val ->
      List row = [val]
      rows.add(row)
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
    List categories = chart.categorySeries
    List values = chart.valueSeries[0]
    List<List> rows = []
    categories.eachWithIndex { Object cat, int idx ->
      rows << [cat.toString(), values[idx]]
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
    List categories = chart.categorySeries
    List<List> valueLists = chart.valueSeries
    List<String> seriesNames = chart.valueSeriesNames

    if (valueLists.size() == 1) {
      List<List> rows = []
      categories.eachWithIndex { Object cat, int idx ->
        rows << [cat, valueLists[0][idx]]
      }
      return Matrix.builder()
          .columnNames('x', 'y')
          .rows(rows)
          .build()
    }

    List<List> rows = []
    valueLists.eachWithIndex { List values, int seriesIdx ->
      String seriesName = seriesNames != null && seriesIdx < seriesNames.size()
          ? seriesNames[seriesIdx] : "series${seriesIdx}"
      categories.eachWithIndex { Object cat, int catIdx ->
        rows << [cat, values[catIdx], seriesName]
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
    if (chart.style?.legendVisible == false) {
      theme.legendPosition = 'none'
    }
  }

  private static String colorToHex(Color color) {
    if (color == null) {
      return null
    }
    String.format('#%02x%02x%02x', color.red, color.green, color.blue)
  }
}
