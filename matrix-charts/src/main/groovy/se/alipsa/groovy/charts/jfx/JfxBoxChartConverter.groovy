package se.alipsa.groovy.charts.jfx

import javafx.scene.chart.Axis
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.Chart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import se.alipsa.groovy.charts.BoxChart

class JfxBoxChartConverter {

  static XYChart convert(BoxChart chart) {
    // create a bar chart for now until we have created a proper box chart
    Axis<?> xAxis = new CategoryAxis()
    Axis<?> yAxis = new NumberAxis()
    def fxChart = new StackedBarChart<>(xAxis, yAxis)
    ConverterUtil.populateVerticalSeries(fxChart, chart)
    fxChart.setTitle(chart.getTitle())
    xAxis.setLabel(chart.xAxisTitle)
    yAxis.setLabel(chart.yAxisTitle)
    return fxChart
  }
}
