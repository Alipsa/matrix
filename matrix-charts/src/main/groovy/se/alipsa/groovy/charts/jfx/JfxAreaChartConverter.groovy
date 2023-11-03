package se.alipsa.groovy.charts.jfx;

import javafx.scene.chart.*;

class JfxAreaChartConverter {


  static AreaChart<?,?> convert(se.alipsa.groovy.charts.AreaChart chart) {
    Axis<?> xAxis = new CategoryAxis()
    Axis<?> yAxis = new NumberAxis()
    AreaChart<?,?> fxChart = new AreaChart<>(xAxis, yAxis)
    fxChart.setTitle(chart.getTitle())

    ConverterUtil.populateVerticalSeries(fxChart, chart)
    return fxChart;
  }




}
