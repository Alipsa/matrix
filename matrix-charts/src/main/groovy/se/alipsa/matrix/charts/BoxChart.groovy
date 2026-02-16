package se.alipsa.matrix.charts


import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ListConverter

class BoxChart extends Chart<BoxChart> {

  static BoxChart create(String title, Matrix data, String categoryColumnName, String valueColumn) {

    def groups = data.split(categoryColumnName).sort()
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = ListConverter.toStrings(groups.keySet())
    chart.valueSeries = groups.values().collect {it[valueColumn].removeNulls()}
    chart.valueSeriesNames = chart.categorySeries as List<String>
    chart.xAxisTitle = categoryColumnName
    chart.yAxisTitle = valueColumn
    return chart
  }

  static BoxChart create(title, Matrix data, List<String> columnNames) {
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = columnNames
    def valueColumns = []
    columnNames.each {
      valueColumns << data[it]
    }
    chart.valueSeries = valueColumns
    chart.valueSeriesNames = columnNames
    chart.xAxisTitle = 'X'
    chart.yAxisTitle = 'Y'
    return chart
  }

  /**
   * Creates a new fluent builder for constructing a {@link BoxChart}.
   *
   * <p>Usage with category/value columns:
   * <pre>
   * BoxChart chart = BoxChart.builder(data)
   *     .title('Salary Distribution')
   *     .x('department')
   *     .y('salary')
   *     .build()
   * </pre>
   *
   * <p>Usage with multiple columns:
   * <pre>
   * BoxChart chart = BoxChart.builder(data)
   *     .title('Feature Comparison')
   *     .columns(['mpg', 'hp', 'wt'])
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link BoxChart}.
   */
  static class Builder extends Chart.ChartBuilder<Builder, BoxChart> {

    private List<String> columnNames

    Builder(Matrix data) { super(data) }

    /**
     * Sets column names for the multi-column variant, where each
     * column becomes a separate box in the chart.
     *
     * @param cols the column names to include
     * @return this builder
     */
    Builder columns(List<String> cols) { this.columnNames = cols; this }

    /**
     * Builds the configured {@link BoxChart}.
     * <p>If {@link #columns} was called, uses the multi-column variant.
     * Otherwise, uses {@code x} as the category column and {@code y} as the value column.
     *
     * @return the box chart
     */
    BoxChart build() {
      BoxChart chart
      if (columnNames) {
        chart = BoxChart.create(title, data, columnNames)
      } else {
        chart = BoxChart.create(title, data, xCol, yCols[0])
      }
      applyTo(chart)
      chart
    }
  }
}
