package se.alipsa.groovy.charts

import se.alipsa.groovy.matrix.Matrix;

class ScatterChart extends Chart {

  static create(String title, Matrix data, String xAxis, String yAxis) {
    ScatterChart chart = new ScatterChart()
    chart.title = title
    chart.categorySeries = data.column(xAxis)
    chart.valueSeries = [data.column(yAxis)]
    chart.xAxisTitle = xAxis
    chart.yAxisTitle = yAxis
    return chart
  }
}
