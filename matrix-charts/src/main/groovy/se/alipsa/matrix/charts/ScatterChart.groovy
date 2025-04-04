package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix;

class ScatterChart extends Chart<ScatterChart> {

  static ScatterChart create(String title, Matrix data, String xAxis, String yAxis) {
    ScatterChart chart = new ScatterChart()
    chart.title = title
    chart.categorySeries = data.column(xAxis)
    chart.valueSeries = [data.column(yAxis)]
    chart.xAxisTitle = xAxis
    chart.yAxisTitle = yAxis
    return chart
  }
}
