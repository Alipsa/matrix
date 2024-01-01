package se.alipsa.groovy.charts.jfx

import javafx.scene.chart.Axis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.util.StringConverter
import se.alipsa.groovy.matrix.Stat;

class JfxLineChartConverter {

  static StringConverter<Number> plainStringConverter = new StringConverter<Number>() {
    @Override
    String toString(Number number) {
      return String.valueOf(number)
    }

    @Override
    Number fromString(String s) {
      return s as Number
    }
  }

  static LineChart<?,?> convert(se.alipsa.groovy.charts.LineChart chart) {
    Axis<Number> xAxis = new NumberAxis()
    Axis<Number> yAxis = new NumberAxis()
    LineChart<Number,Number> fxChart = new LineChart<>(xAxis, yAxis)
    fxChart.setTitle(chart.getTitle())
    Number minY = null
    Number maxY = null
    int serieIdx = 0
    for (List serie : chart.getValueSeries()) {
      var series = new XYChart.Series()
      series.name = chart.valueSeriesNames[serieIdx++]
      chart.getCategorySeries().eachWithIndex { xVal, Integer idx ->
        def yVal = serie[idx] as Number
        if (minY == null || yVal < minY) minY = yVal as Number
        if (maxY == null || yVal > maxY) maxY = yVal as Number
        series.getData().add(new XYChart.Data(xVal as Number, yVal))
      }
      fxChart.getData().add(series)
    }
    xAxis.setForceZeroInRange(false)
    yAxis.setForceZeroInRange(false)
    if (chart.getCategorySeries().size() == 1) {
      fxChart.setLegendVisible(false)
    }
    return fxChart
  }

}
