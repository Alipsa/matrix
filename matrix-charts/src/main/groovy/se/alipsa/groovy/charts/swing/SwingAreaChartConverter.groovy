package se.alipsa.groovy.charts.swing

import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import se.alipsa.groovy.charts.AreaChart

class SwingAreaChartConverter {

  static XYChart convert(AreaChart chart) {

    XYChart xyChart =
        new XYChartBuilder()
            //.width(800)
            //.height(600)
            .title(chart.getTitle())
            .xAxisTitle(chart.getxAxisTitle())
            .yAxisTitle(chart.getyAxisTitle())
            .build()

    def series = chart.getValueSeries()
    def categories = chart.getCategorySeries()

    for (int i = 0; i < series.size(); i++) {
      xyChart.addSeries(String.valueOf(categories[i]), series[i] as List<? extends Number>)
    }
    return xyChart
  }
}
