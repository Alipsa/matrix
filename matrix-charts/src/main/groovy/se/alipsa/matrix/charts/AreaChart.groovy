package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix


class AreaChart extends Chart<AreaChart> {

  static AreaChart create(Matrix data) {
    if (data.columnCount() != 2) {
      throw new IllegalArgumentException("Table " + data.matrixName + " does not contain 2 columns.")
    }
    AreaChart chart = new AreaChart()
    chart.title = data.matrixName
    chart.categorySeries = data.column(0)
    chart.valueSeries = [data.column(1)]
    chart.valueSeriesNames = [data.columnName(1)]
    return chart
  }

  static AreaChart create(String title, List<?> groupColumn, List<?>... valueColumn) {
    AreaChart chart = new AreaChart()
    chart.title = title
    chart.categorySeries = groupColumn
    chart.valueSeries = valueColumn
    return chart
  }

  /**
   * AreaPlot.create(
   *         "Boston Robberies by month: Jan 1966-Oct 1975", robberies, "Record", "Robberies")
   */
  static AreaChart create(String title, Matrix data, String xCol, String yCol) {
    def xColumn = data.column(xCol)
    def yColumn = data.column(yCol)
    def chart = create(title, xColumn, yColumn)
    chart.valueSeriesNames = [yCol]
    return chart
  }

  /**
   * TODO: figure out how groupCol works
   */
  static AreaChart create(String title, Matrix data, String xCol, String yCol, String groupCol) {
    throw new RuntimeException("Not yet implemented")
  }
}
