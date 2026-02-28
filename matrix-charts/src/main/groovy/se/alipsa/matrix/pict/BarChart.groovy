package se.alipsa.matrix.pict

import se.alipsa.matrix.core.Matrix

class BarChart extends Chart<BarChart> {

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

  /**
   * Creates a new fluent builder for constructing a {@link BarChart}.
   *
   * <p>Example:
   * <pre>
   * BarChart chart = BarChart.builder(data)
   *     .title('Sales by Region')
   *     .x('region')
   *     .y('q1', 'q2', 'q3')
   *     .vertical()
   *     .stacked()
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link BarChart}.
   */
  static class Builder extends Chart.ChartBuilder<Builder, BarChart> {

    private ChartType chartType = ChartType.BASIC
    private ChartDirection direction = ChartDirection.VERTICAL

    Builder(Matrix data) { super(data) }

    /** Sets the chart type (BASIC, STACKED, GROUPED). */
    Builder chartType(ChartType type) { this.chartType = type; this }

    /** Sets the chart direction (VERTICAL or HORIZONTAL). */
    Builder direction(ChartDirection dir) { this.direction = dir; this }

    /** Sets the direction to horizontal. */
    Builder horizontal() { this.direction = ChartDirection.HORIZONTAL; this }

    /** Sets the direction to vertical. */
    Builder vertical() { this.direction = ChartDirection.VERTICAL; this }

    /** Sets the chart type to stacked. */
    Builder stacked() { this.chartType = ChartType.STACKED; this }

    /**
     * Builds the configured {@link BarChart}.
     *
     * @return the bar chart
     */
    BarChart build() {
      def chart = new BarChart()
      applyTo(chart)
      chart.chartType = chartType
      chart.direction = direction
      chart.categorySeries = data.column(xCol)
      chart.valueSeries = yCols.collect { data.column(it) }
      chart.valueSeriesNames = yCols
      chart
    }
  }
}
