package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.regression.LinearRegression

@CompileStatic
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
  @CompileStatic
  static class Builder extends Chart.ChartBuilder<Builder, LineChart> {

    Builder(Matrix data) { super(data) }

    /**
     * Builds the configured {@link LineChart}.
     *
     * @return the line chart
     */
    LineChart build() {
      LineChart chart = new LineChart()
      applyTo(chart)
      chart.categorySeries = data.column(xCol) as List<?>
      chart.valueSeries = yCols.collect { String col -> data.column(col) as List<?> }
      chart.valueSeriesNames = yCols
      chart
    }
  }
}
