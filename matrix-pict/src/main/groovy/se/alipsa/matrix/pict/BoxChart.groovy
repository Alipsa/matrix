package se.alipsa.matrix.pict

import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

/** Box-and-whisker chart for visualizing the distribution of numerical data. */
@SuppressWarnings('UnnecessaryObjectReferences')
class BoxChart extends Chart<BoxChart> {

  private static BoxChart fromCategoryValue(String title, Matrix data, String categoryCol, String valueCol) {
    Map<String, Matrix> groups = data.split(categoryCol).sort() as Map<String, Matrix>
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = ListConverter.toStrings(groups.keySet()) as List<?>
    chart.valueSeries = groups.values().collect { Matrix m -> m[valueCol].removeNulls() as List<?> }
    chart.valueSeriesNames = chart.categorySeries as List<String>
    chart.xAxisTitle = categoryCol
    chart.yAxisTitle = valueCol
    chart
  }

  private static BoxChart fromColumns(String title, Matrix data, List<String> columnNames) {
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = columnNames as List<?>
    chart.valueSeries = columnNames.collect { String col -> data[col] as List<?> }
    chart.valueSeriesNames = columnNames
    chart.xAxisTitle = 'X'
    chart.yAxisTitle = 'Y'
    chart
  }

  /**
   * Creates a box chart from category and value columns.
   *
   * @param title chart title
   * @param data chart data
   * @param categoryColumnName category column name
   * @param valueColumn value column name
   * @return box chart
   * @deprecated Use {@link #builder(Matrix)} for new code.
   */
  @Deprecated
  static BoxChart create(String title, Matrix data, String categoryColumnName, String valueColumn) {
    fromCategoryValue(title, data, categoryColumnName, valueColumn)
  }

  /**
   * Creates a box chart from multiple value columns.
   *
   * @param title chart title
   * @param data chart data
   * @param columnNames value column names
   * @return box chart
   * @deprecated Use {@link #builder(Matrix)} for new code.
   */
  @Deprecated
  static BoxChart create(String title, Matrix data, List<String> columnNames) {
    fromColumns(title, data, columnNames)
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
      if (!columnNames) {
        if (xCol == null) {
          throw new IllegalStateException('x(...) or columns(...) must be called before build()')
        }
        if (yCols == null || yCols.isEmpty()) {
          throw new IllegalStateException('y(...) or columns(...) must be called before build()')
        }
      }
      String chartTitle = this.@title
      BoxChart chart = columnNames
          ? BoxChart.fromColumns(chartTitle, data, columnNames)
          : BoxChart.fromCategoryValue(chartTitle, data, xCol, yCols[0])
      applyTo(chart)
      chart
    }

  }

}
