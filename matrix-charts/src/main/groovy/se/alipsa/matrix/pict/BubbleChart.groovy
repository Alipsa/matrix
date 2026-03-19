package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Bubble chart — a scatter plot where each point's radius encodes a third
 * numeric variable (the "size" aesthetic).
 *
 * <p>Optionally, a grouping column can be specified to map distinct colours
 * to each group. The chart delegates to the Charm POINT geom with size
 * aesthetic mapping via {@link CharmBridge}.</p>
 *
 * <p>Usage via factory method:
 * <pre>{@code
 * BubbleChart chart = BubbleChart.create('Bubble', data, 'x', 'y', 'size')
 * }</pre>
 *
 * <p>Usage via fluent builder:
 * <pre>{@code
 * BubbleChart chart = BubbleChart.builder(data)
 *     .title('Bubble Chart')
 *     .x('x')
 *     .y('y')
 *     .size('size')
 *     .build()
 * }</pre>
 */
@CompileStatic
class BubbleChart extends Chart<BubbleChart> {

  /** Size values — one per data point, mapped to point radius. */
  List<? extends Number> sizeSeries = []

  /** Group values — one per data point, mapped to colour aesthetic. */
  List<?> groupSeries = []

  /** Optional grouping column name. */
  String groupColumn

  /**
   * Creates a bubble chart from a Matrix.
   *
   * @param title the chart title
   * @param data the data matrix
   * @param xCol column name for x-axis values
   * @param yCol column name for y-axis values
   * @param sizeCol column name for size values
   * @return a configured BubbleChart
   */
  static BubbleChart create(String title, Matrix data, String xCol, String yCol, String sizeCol) {
    BubbleChart chart = new BubbleChart()
    chart.title = title
    chart.categorySeries = data.column(xCol) as List<?>
    chart.valueSeries = [data.column(yCol) as List<?>]
    chart.sizeSeries = data.column(sizeCol) as List<? extends Number>
    chart.xAxisTitle = xCol
    chart.yAxisTitle = yCol
    chart.valueSeriesNames = [yCol]
    chart
  }

  /**
   * Creates a grouped bubble chart from a Matrix.
   *
   * @param title the chart title
   * @param data the data matrix
   * @param xCol column name for x-axis values
   * @param yCol column name for y-axis values
   * @param sizeCol column name for size values
   * @param groupCol column name for grouping (colour aesthetic)
   * @return a configured BubbleChart with group data
   */
  static BubbleChart create(String title, Matrix data, String xCol, String yCol, String sizeCol, String groupCol) {
    BubbleChart chart = create(title, data, xCol, yCol, sizeCol)
    chart.groupColumn = groupCol
    chart.groupSeries = data.column(groupCol) as List<?>
    chart
  }

  /**
   * Creates a new fluent builder for constructing a {@link BubbleChart}.
   *
   * <p>Example:
   * <pre>
   * BubbleChart chart = BubbleChart.builder(data)
   *     .title('Population')
   *     .x('gdp')
   *     .y('lifeExpectancy')
   *     .size('population')
   *     .group('continent')
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link BubbleChart}.
   */
  @CompileStatic
  static class Builder extends Chart.ChartBuilder<Builder, BubbleChart> {

    private String sizeCol
    private String groupCol

    Builder(Matrix data) { super(data) }

    /** Sets the column mapped to point size. */
    Builder size(String col) { this.sizeCol = col; this }

    /** Sets the column used for grouping (colour aesthetic). */
    Builder group(String col) { this.groupCol = col; this }

    /**
     * Builds the configured {@link BubbleChart}.
     *
     * @return the bubble chart
     */
    BubbleChart build() {
      if (xCol == null) {
        throw new IllegalStateException("x(...) must be called before build() to specify the x-axis column")
      }
      if (yCols == null || yCols.isEmpty()) {
        throw new IllegalStateException("y(...) must be called before build() to specify exactly one y-axis column")
      }
      if (yCols.size() != 1) {
        throw new IllegalStateException("BubbleChart requires exactly one y-axis column, but got ${yCols.size()}")
      }
      if (sizeCol == null) {
        throw new IllegalStateException("size(...) must be called before build() to specify the bubble size column")
      }
      String yCol = yCols[0]
      BubbleChart chart = groupCol
          ? BubbleChart.create(this.@title, data, xCol, yCol, sizeCol, groupCol)
          : BubbleChart.create(this.@title, data, xCol, yCol, sizeCol)
      applyTo(chart)
      chart
    }
  }
}
