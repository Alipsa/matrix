package se.alipsa.matrix.charts.jfx

import javafx.scene.chart.Axis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import se.alipsa.matrix.charts.AxisScale
import se.alipsa.matrix.charts.ScatterChart

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
    JfxStyler.style(jfxChart, chart)
    return jfxChart
  }

  static Axis<Number> createAxis(AxisScale scale, List<? extends Number> series) {
    def axis
    if (scale == null) {
      def start = series.min()
      if (start > 0) {
        start = (start * 0.98).floor()
      } else {
        start = (start * 1.02).ceil()
      }
      def end = series.max()
      if (end > 0) {
        end = end * 1.02.ceil()
      } else {
        end = (end * 0.98).floor()
      }
      def step = (((end - start) / 12) as BigDecimal).setScale(1, RoundingMode.HALF_EVEN)
      if (step > 2) {
        step = step.round()
      }
      //log.info("start = $start, end = $end, step = $step")
      axis = new NumberAxis(start, end, step)
    } else {
      axis = new NumberAxis(scale.start, scale.end, scale.step)
    }
    return axis
  }
}
