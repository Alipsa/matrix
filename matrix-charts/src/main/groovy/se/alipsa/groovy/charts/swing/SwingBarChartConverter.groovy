package se.alipsa.groovy.charts.swing

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import se.alipsa.groovy.charts.BarChart

class SwingBarChartConverter {

  static CategoryChart convert(BarChart chart) {
    CategoryChart categoryChart =
        new CategoryChartBuilder()
            .title(chart.getTitle())
            .xAxisTitle(chart.getxAxisTitle())
            .yAxisTitle(chart.getyAxisTitle())
            .build()
    categoryChart.getStyler().setLegendVisible(false)
    categoryChart.getStyler().setStacked(chart.isStacked())
    int serieIdx = 0
    for (String serieName in chart.valueSeriesNames) {
      categoryChart.addSeries(serieName, chart.getCategorySeries(), chart.getValueSeries()[serieIdx++] as List<? extends Number>)
    }
    return categoryChart
  }
}
