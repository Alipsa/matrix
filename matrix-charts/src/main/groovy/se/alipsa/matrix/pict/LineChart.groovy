package se.alipsa.matrix.pict

import se.alipsa.matrix.core.Matrix

/** Line chart for visualizing data trends over a continuous axis. */
@SuppressWarnings('UnnecessaryObjectReferences')
class LineChart extends Chart<LineChart> {

  static LineChart create(Matrix data, String xAxis, String... valueColumns) {
    create(data.matrixName, data, xAxis, valueColumns)
  }

  static LineChart create(String title, Matrix data, String xAxis, String... valueColumns) {
    LineChart chart = new LineChart()
    chart.title = title
    chart.valueSeries = data.columns(valueColumns.toList()) as List<List<?>>
    chart.categorySeries = data[xAxis] as List<?>
    chart.valueSeriesNames = valueColumns.toList()
    chart.xAxisTitle = xAxis
    chart.yAxisTitle = valueColumns.length == 1 ? valueColumns[0] : ''
    return chart
  }

  /**
   * Creates a new fluent builder for constructing a {@link LineChart}.
   *
   * <p>Example:
   * <pre>
   * LineChart chart = LineChart.builder(data)
   *     .title('Trends')
   *     .x('year')
   *     .y('sales', 'profit')
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link LineChart}.
   */
  static class Builder extends Chart.ChartBuilder<Builder, LineChart> {

    Builder(Matrix data) { super(data) }

    /**
     * Builds the configured {@link LineChart}.
     *
     * @return the line chart
     */
    LineChart build() {
      if (xCol == null) {
        throw new IllegalStateException('x(...) must be called before build()')
      }
      if (yCols == null || yCols.isEmpty()) {
        throw new IllegalStateException('y(...) must be called before build()')
      }
      LineChart chart = new LineChart()
      applyTo(chart)
      chart.categorySeries = data.column(xCol) as List<?>
      chart.valueSeries = yCols.collect { String col -> data.column(col) as List<?> }
      chart.valueSeriesNames = yCols
      chart
    }

  }

}
