package se.alipsa.groovy.charts.swing

import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import se.alipsa.groovy.charts.AreaChart

class SwingAreaChartConverter {

  static XYChart convert(AreaChart chart) {

    XYChart xyChart =
        new XYChartBuilder()
            .title(chart.getTitle())
            .xAxisTitle(chart.getxAxisTitle())
            .yAxisTitle(chart.getyAxisTitle())
            .build()

    xyChart.getStyler().setLegendVisible(false)

    def series = chart.getValueSeries()
    def categories = chart.getCategorySeries()

    for (int i = 0; i < series.size(); i++) {
      xyChart.addSeries(String.valueOf(chart.valueSeriesNames[i]), series[i] as List<? extends Number>)
    }
    SwingStyler.style(xyChart, chart)
    return xyChart
  }
}
