package se.alipsa.groovy.charts.swing

import org.knowm.xchart.BoxChartBuilder
import se.alipsa.groovy.charts.BoxChart

class SwingBoxChartConverter {

  static org.knowm.xchart.BoxChart convert(BoxChart chart) {
    org.knowm.xchart.BoxChart boxChart = new BoxChartBuilder()
        .title(chart.title)
        .xAxisTitle(chart.xAxisTitle ?: "X")
        .yAxisTitle(chart.yAxisTitle ?: "Y")
        .build()

    int idx = 0
    for (serie in chart.getCategorySeries()) {
      boxChart.addSeries(String.valueOf(serie), chart.getValueSerie(idx++) as List<? extends Number>)
    }
    SwingStyler.style(boxChart, chart)
    return boxChart
  }
}
