package se.alipsa.groovy.charts.swing

import javafx.scene.chart.XYChart
import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import se.alipsa.groovy.charts.Histogram
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.matrix.ValueConverter

import java.awt.Color

class SwingHistogramConverter {

  static CategoryChart convert(Histogram chart) {
    CategoryChart categoryChart =
        new CategoryChartBuilder()
            .title(chart.getTitle() ?: '')
            .xAxisTitle("Range")
            .yAxisTitle("Frequency")
            .build()
    /*
    def histogram = new org.knowm.xchart.Histogram(chart.originalData, chart.numberOfBins)

    categoryChart.addSeries(
        "Histogram",
        histogram.getxAxisData(),
        histogram.getyAxisData()
    )
     */
    def rangeList = []
    List<Double> valueList = []
    chart.ranges.each {
      rangeList << it.key.toString()
      valueList << ValueConverter.asDouble(it.value)
    }
    categoryChart.addSeries('Histogram', rangeList, valueList)
    categoryChart.getStyler().seriesColors = [Color.BLUE]
    categoryChart.getStyler().legendVisible = false
    categoryChart.getStyler().setAxisTickPadding(5)
    categoryChart
  }
}
