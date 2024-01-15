package se.alipsa.groovy.charts.jfx;

import javafx.scene.chart.*
import se.alipsa.groovy.charts.ChartDirection
import se.alipsa.groovy.charts.ChartType;

class JfxBarChartConverter {

  static XYChart<?,?> convert(se.alipsa.groovy.charts.BarChart chart) {

    Axis<?> xAxis = new CategoryAxis()
    Axis<?> yAxis = new NumberAxis()

    XYChart<?,?> fxChart
    if (ChartType.STACKED == chart.getChartType()) {
      if (ChartDirection.HORIZONTAL == chart.getDirection()) {
        //xAxis.setTickLabelRotation(90); TODO: make this a styling option
        fxChart = new StackedBarChart<>(yAxis, xAxis)
        ConverterUtil.populateHorizontalSeries(fxChart, chart)
      } else {
        fxChart = new StackedBarChart<>(xAxis, yAxis)
        ConverterUtil.populateVerticalSeries(fxChart, chart)
      }
    } else {
      if (ChartDirection.HORIZONTAL == chart.getDirection()) {
        //xAxis.setTickLabelRotation(90); // TODO: make this a styling option
        fxChart = new BarChart<>(yAxis, xAxis)
        ConverterUtil.populateHorizontalSeries(fxChart, chart)
      } else {
        fxChart = new BarChart<>(xAxis, yAxis)
        ConverterUtil.populateVerticalSeries(fxChart, chart)
      }
    }
    ConverterUtil.maybeHideLegend(chart, fxChart)

    fxChart.setTitle(chart.getTitle())
    JfxStyler.style(fxChart, chart)

    return fxChart
  }
}
