package se.alipsa.matrix.pict

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

@CompileStatic
class BoxChart extends Chart<BoxChart> {

  static BoxChart create(String title, Matrix data, String categoryColumnName, String valueColumn) {
    Map<String, Matrix> groups = data.split(categoryColumnName).sort() as Map<String, Matrix>
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = ListConverter.toStrings(groups.keySet()) as List<?>
    chart.valueSeries = groups.values().collect { Matrix m -> m[valueColumn].removeNulls() as List<?> }
    chart.valueSeriesNames = chart.categorySeries as List<String>
    chart.xAxisTitle = categoryColumnName
    chart.yAxisTitle = valueColumn
    return chart
  }

  static BoxChart create(String title, Matrix data, List<String> columnNames) {
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = columnNames as List<?>
    List<List<?>> valueColumns = []
    columnNames.each { String col ->
      valueColumns.add(data[col] as List<?>)
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
  @CompileStatic
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
      String chartTitle = this.@title
      BoxChart chart
      if (columnNames) {
        chart = BoxChart.create(chartTitle, data, columnNames)
      } else {
        chart = BoxChart.create(chartTitle, data, xCol, yCols[0])
      }
      applyTo(chart)
      chart
    }
  }
}
