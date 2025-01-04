package se.alipsa.matrix.charts.jfx

import javafx.scene.chart.Axis
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import se.alipsa.matrix.charts.BoxChart

class JfxBoxChartConverter {

  static XYChart convert(BoxChart chart) {
    // create a bar chart for now until we have created a proper box chart
    Axis<?> xAxis = new CategoryAxis()
    Axis<?> yAxis = new NumberAxis()
    def fxChart = new StackedBarChart<>(xAxis, yAxis)
    chart.categorySeries.eachWithIndex { serie, idx ->
      XYChart.Series fxSeries = new XYChart.Series()
      def columnVals = chart.valueSeries[idx]
      for (val in columnVals) {
        if (val instanceof Number) {
          fxSeries.getData().add(new XYChart.Data(serie, (Number)val))
        }
      }
      fxChart.getData().add(fxSeries)
    }
    fxChart.setTitle(chart.getTitle() ?: '')
    xAxis.setLabel(chart.xAxisTitle ?: 'X')
    yAxis.setLabel(chart.yAxisTitle ?: 'Y')
    JfxStyler.style(fxChart, chart)
    return fxChart
  }
}
