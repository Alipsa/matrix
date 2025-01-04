package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix;

class BarChart extends Chart {

  protected ChartType chartType = ChartType.BASIC
  private ChartDirection direction

  ChartType getChartType() {
    return chartType
  }

  ChartDirection getDirection() {
    return direction
  }

  static BarChart create(String title, ChartType chartType, ChartDirection direction, List<?> groupColumn, List<?>... valueColumn) {
    BarChart chart = new BarChart()
    chart.title = title
    chart.categorySeries = groupColumn
    chart.valueSeries = valueColumn
    chart.chartType = chartType
    chart.direction = direction
    chart.valueSeriesNames = 1..valueColumn.size() as List<String>
    return chart
  }

  static BarChart create(String title, ChartType chartType, Matrix data, String categoryColumnName, ChartDirection direction, String... valueColumn) {
    List<?> groupColumn = data.column(categoryColumnName)
    List<List> valueColumns = data.columns(valueColumn.toList())
    BarChart chart = new BarChart()
    chart.title = title
    chart.categorySeries = groupColumn
    chart.valueSeries = valueColumns
    chart.valueSeriesNames = valueColumn.toList()
    chart.chartType = chartType
    chart.direction = direction
    return chart
  }

  /**
   * Similar to Tablesaw ploty factory method i.e.
   *tech.tablesaw.plotly.api.HorizontalBarPlot.create(
   *     "Tornado Impact",
   *     summaryTable,
   *     "scale",
   *     tech.tablesaw.plotly.components.Layout.BarMode.STACK,
   *     "Sum [log injuries]",
   *     "Sum [Fatalities]",
   *   )
   */
  static BarChart createHorizontal(String title, Matrix data, String categoryColumnName, ChartType chartType, String... valueColumn) {
    return create(title, chartType, data, categoryColumnName, ChartDirection.HORIZONTAL, valueColumn)
  }

  /**
   * Similar to Tablesaw ploty factory method i.e.
   *tech.tablesaw.plotly.api.VerticalBarPlot.create(
   *     "Tornado Impact",
   *     summaryTable,
   *     "scale",
   *     tech.tablesaw.plotly.components.Layout.BarMode.STACK,
   *     "Sum [log injuries]",
   *     "Sum [Fatalities]",
   *   )
   */
  static BarChart createVertical(String title, Matrix data, String categoryColumnName, ChartType chartType = ChartType.BASIC, String... valueColumn) {
    return create(title, chartType, data, categoryColumnName, ChartDirection.VERTICAL, valueColumn)
  }

  boolean isStacked() {
    return chartType == ChartType.STACKED
  }
}
