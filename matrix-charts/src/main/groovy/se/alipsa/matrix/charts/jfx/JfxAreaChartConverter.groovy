package se.alipsa.matrix.charts.jfx;

import javafx.scene.chart.*;

class JfxAreaChartConverter {


  static AreaChart<?,?> convert(se.alipsa.matrix.charts.AreaChart chart) {
    Axis<?> xAxis = new CategoryAxis()
    Axis<?> yAxis = new NumberAxis()
    AreaChart<?,?> fxChart = new AreaChart<>(xAxis, yAxis)
    fxChart.setTitle(chart.getTitle())

    ConverterUtil.populateVerticalSeries(fxChart, chart)
    JfxStyler.style(fxChart, chart)
    return fxChart
  }




}
