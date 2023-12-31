package se.alipsa.groovy.charts.jfx

import javafx.scene.chart.Axis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import se.alipsa.groovy.charts.AxisScale
import se.alipsa.groovy.charts.ScatterChart
import se.alipsa.groovy.matrix.Stat

import java.math.RoundingMode

class JfXScatterChartConverter {

  static javafx.scene.chart.ScatterChart convert(ScatterChart chart) {
    def series = chart.getValueSeries()[0]
    def categories = chart.getCategorySeries()
    Axis<Number> xAxis = createAxis(chart.xAxisScale, categories as List<? extends Number>)
    Axis<Number> yAxis = createAxis(chart.yAxisScale, series as List<? extends Number>)
    xAxis.setLabel(chart.xAxisTitle)
    yAxis.setLabel(chart.yAxisTitle)
    def jfxChart = new javafx.scene.chart.ScatterChart(xAxis, yAxis)
    jfxChart.setLegendVisible(false)

    XYChart.Series fxSeries = new XYChart.Series()
    for (int i = 0; i < series.size(); i++) {
      def xVal = categories.get(i) as Number
      def yVal = series.get(i) as Number
      if (xVal == null || yVal == null) continue
      fxSeries.getData().add(new XYChart.Data(xVal, yVal))
    }
    jfxChart.getData().add(fxSeries)

    jfxChart.setTitle(chart.title)
    return jfxChart
  }

  static Axis<Number> createAxis(AxisScale scale, List<? extends Number> series) {
    def axis
    if (scale == null) {
      def start = Stat.min(series) as Double
      if (start > 0) {
        start = Math.floor(start * 0.98)
      } else {
        start = Math.ceil(start * 1.02)
      }
      def end = Stat.max(series) as Double
      if (end > 0) {
        end = Math.ceil(end * 1.02)
      } else {
        end = Math.floor(end * 0.98)
      }
      def step = (((end - start) / 12) as BigDecimal).setScale(1, RoundingMode.HALF_EVEN)
      if (step > 2) {
        step = Math.round(step)
      }
      //log.info("start = $start, end = $end, step = $step")
      axis = new NumberAxis(start, end, step)
    } else {
      axis = new NumberAxis(scale.start, scale.end, scale.step)
    }
    return axis
  }
}
