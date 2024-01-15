package se.alipsa.groovy.charts.jfx

import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import se.alipsa.groovy.charts.Histogram

class JfxHistogramConverter {

  static BarChart<String,Number> convert(Histogram histogram) {
    final CategoryAxis xAxis = new CategoryAxis()
    final NumberAxis yAxis = new NumberAxis()
    final BarChart<String,Number> barChart = new BarChart<>(xAxis,yAxis)
    xAxis.setLabel("Range")
    yAxis.setLabel("Frequency")

    XYChart.Series series1 = new XYChart.Series()
    series1.setName("Histogram")
    def final data = series1.getData()
    histogram.ranges.each {
      data.add(new XYChart.Data(it.key.toString(), it.value))
    }
    barChart.getData().add(series1)
    barChart.setTitle(histogram.getTitle() ?: '')
    barChart.setLegendVisible(false)
    JfxStyler.style(barChart, histogram)
    return barChart
  }
}
