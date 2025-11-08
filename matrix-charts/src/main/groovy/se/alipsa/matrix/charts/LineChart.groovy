package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.regression.LinearRegression

class LineChart extends Chart<LineChart> {

  static LineChart create(LinearRegression model) {
    null
  }

  static LineChart create(Matrix data, String xAxis, String... valueColumns) {
    create(data.matrixName, data, xAxis, valueColumns)
  }

  static LineChart create(String title, Matrix data, String xAxis, String... valueColumns) {
    LineChart chart = new LineChart()
    chart.title = title
    chart.valueSeries = data.columns(valueColumns)
    chart.categorySeries = data[xAxis] as List<Number>
    chart.valueSeriesNames = valueColumns as List<String>
    chart.xAxisTitle = xAxis
    chart.yAxisTitle = valueColumns.length == 1 ? valueColumns[0] : ''
    return chart
  }
}
