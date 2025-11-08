package se.alipsa.matrix.charts.swing


import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import se.alipsa.matrix.charts.Histogram
import se.alipsa.matrix.core.ValueConverter

import java.awt.Color

class SwingHistogramConverter {

  static CategoryChart convert(Histogram chart) {
    CategoryChart categoryChart =
        new CategoryChartBuilder()
        .title(chart.getTitle() ?: '')
        .xAxisTitle("Range")
        .yAxisTitle("Frequency")
        .build()
    /* the xchart standard below gives counts for a mean with the means as x axis lables
     which is not exactly what we want so rolling it manually instead...
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

    SwingStyler.style(categoryChart, chart)
    categoryChart
  }
}
