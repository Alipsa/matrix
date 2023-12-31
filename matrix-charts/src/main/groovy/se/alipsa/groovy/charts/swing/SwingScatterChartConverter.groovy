package se.alipsa.groovy.charts.swing

import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle
import se.alipsa.groovy.charts.ScatterChart

class SwingScatterChartConverter {

  static XYChart convert(ScatterChart chart) {
    def scatterChart = new XYChartBuilder().build()
    scatterChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter)
    scatterChart.addSeries(chart.title, chart.categorySeries, chart.valueSeries[0] as List<? extends Number>)
    scatterChart.getStyler().setLegendVisible(false)
    scatterChart.setXAxisTitle(chart.xAxisTitle)
    scatterChart.setYAxisTitle(chart.yAxisTitle)
    scatterChart.setTitle(chart.getTitle() ?: 'ScatterChart')
    return scatterChart
  }
}
