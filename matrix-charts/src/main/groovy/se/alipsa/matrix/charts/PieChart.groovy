package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix


class PieChart extends Chart<PieChart> {

  static PieChart create(String title, List<?> groupCol, List<?> numberCol){
    PieChart chart = new PieChart()
    chart.categorySeries = groupCol
    chart.valueSeries = [numberCol]
    chart.title = title
    return chart
  }

  static PieChart create(Matrix table, String groupColName, String numberColName){
    return create(table.matrixName, table, groupColName, numberColName)
  }

  static PieChart create(String title, Matrix table, String groupColName, String numberColName){
    return create(title, table.column(groupColName), table.column(numberColName))
  }

  /**
   * Creates a new fluent builder for constructing a {@link PieChart}.
   *
   * <p>Example:
   * <pre>
   * PieChart chart = PieChart.builder(data)
   *     .title('Market Share')
   *     .x('company')
   *     .y('revenue')
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link PieChart}.
   */
  static class Builder extends Chart.ChartBuilder<Builder, PieChart> {

    Builder(Matrix data) { super(data) }

    /**
     * Builds the configured {@link PieChart}.
     * Uses {@code x} as the category column and {@code y} as the value column.
     *
     * @return the pie chart
     */
    PieChart build() {
      def chart = new PieChart()
      applyTo(chart)
      chart.categorySeries = data.column(xCol)
      chart.valueSeries = [data.column(yCols[0])]
      chart.valueSeriesNames = yCols
      chart
    }
  }
}
