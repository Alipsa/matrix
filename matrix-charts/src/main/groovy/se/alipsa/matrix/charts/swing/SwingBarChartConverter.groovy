package se.alipsa.matrix.charts.swing

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import se.alipsa.matrix.charts.BarChart

class SwingBarChartConverter {

  static CategoryChart convert(BarChart chart) {
    CategoryChart categoryChart =
        new CategoryChartBuilder()
            .title(chart.getTitle())
            .xAxisTitle(chart.getxAxisTitle())
            .yAxisTitle(chart.getyAxisTitle())
            .build()

    categoryChart.getStyler().setStacked(chart.isStacked())
    int serieIdx = 0
    for (String serieName in chart.valueSeriesNames) {
      categoryChart.addSeries(serieName, chart.getCategorySeries(), chart.getValueSeries()[serieIdx++] as List<? extends Number>)
    }
    SwingStyler.style(categoryChart, chart)
    return categoryChart
  }
}
