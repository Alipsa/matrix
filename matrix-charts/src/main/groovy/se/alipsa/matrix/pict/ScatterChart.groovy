package se.alipsa.matrix.pict

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

@CompileStatic
class ScatterChart extends Chart<ScatterChart> {

  static ScatterChart create(String title, Matrix data, String xAxis, String yAxis) {
    ScatterChart chart = new ScatterChart()
    chart.title = title
    chart.categorySeries = data.column(xAxis) as List<?>
    chart.valueSeries = [data.column(yAxis) as List<?>]
    chart.xAxisTitle = xAxis
    chart.yAxisTitle = yAxis
    return chart
  }

  /**
   * Creates a new fluent builder for constructing a {@link ScatterChart}.
   *
   * <p>Example:
   * <pre>
   * ScatterChart chart = ScatterChart.builder(data)
   *     .title('MPG vs Weight')
   *     .x('wt')
   *     .y('mpg')
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link ScatterChart}.
   */
  @CompileStatic
  static class Builder extends Chart.ChartBuilder<Builder, ScatterChart> {

    Builder(Matrix data) { super(data) }

    /**
     * Builds the configured {@link ScatterChart}.
     *
     * @return the scatter chart
     */
    ScatterChart build() {
      ScatterChart chart = new ScatterChart()
      applyTo(chart)
      chart.categorySeries = data.column(xCol) as List<?>
      chart.valueSeries = yCols.collect { String col -> data.column(col) as List<?> }
      chart.valueSeriesNames = yCols
      chart
    }
  }
}
