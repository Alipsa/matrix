package se.alipsa.groovy.charts

import se.alipsa.groovy.matrix.Matrix;


class AreaChart extends Chart {

  static AreaChart create(Matrix data) {
    if (data.columnCount() != 2) {
      throw new IllegalArgumentException("Table " + data.name + " does not contain 2 columns.")
    }
    AreaChart chart = new AreaChart()
    chart.title = data.name
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
