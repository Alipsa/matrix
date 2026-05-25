package se.alipsa.matrix.xchart

import org.knowm.xchart.PieChartBuilder
import org.knowm.xchart.PieSeries
import org.knowm.xchart.style.PieStyler

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A PieChart is a circular statistical graphic that is divided into slices to illustrate numerical proportions.
 * Each slice of the pie represents a category's contribution to the whole.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Category': ['A', 'B', 'C', 'D'],
 *   'Value': [30, 20, 40, 10]
 * ).types([String, Number]).matrixName('Pie Chart Example').build()
 *
 * def pieChart = PieChart.create(matrix)
 *   .addSeries('Category', 'Value')
 *
 * File file = new File('pieChart.png')
 * pieChart.exportPng(file)
 * </code></pre>
 */
class PieChart extends AbstractChart<PieChart, org.knowm.xchart.PieChart, PieStyler, PieSeries> {

  private PieChart(Matrix matrix, Integer width = null, Integer height = null) {
    PieChartBuilder builder = new PieChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
  }

  /**
   * Create a new pie chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new PieChart instance
   */
  static PieChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new PieChart(matrix, width, height)
  }

  /**
   * Create a new pie chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new PieChart instance
   */
  static PieChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new PieChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Create a new donut chart with default styling.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new PieChart configured as a donut
   */
  static PieChart createDonut(Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new PieChart(matrix, width, height)
    chart.style.setDefaultSeriesRenderStyle(PieSeries.PieSeriesRenderStyle.Donut)
    chart.style.setLabelType(PieStyler.LabelType.NameAndValue)
    chart.style.setLabelsDistance(.82d)
    chart.style.setPlotContentSize(.9d)
    chart.style.setSumVisible(true)
    chart
  }

  /**
   * Create a new donut chart with custom styling applied via a closure.
   * The closure receives the {@link PieStyler} for additional configuration.
   * <pre><code>
   * def dc = PieChart.createDonut(matrix) { style ->
   *     style.labelsDistance = 0.85
   *     style.plotContentSize = 0.95
   * }
   * </code></pre>
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @param config a closure receiving the PieStyler for custom configuration
   * @return a new PieChart configured as a donut with custom styling
   */
  static PieChart createDonut(Matrix matrix, Integer width = null, Integer height = null,
                              @DelegatesTo(PieStyler) Closure config) {
    def chart = createDonut(matrix, width, height)
    config.delegate = chart.style
    config.resolveStrategy = Closure.DELEGATE_FIRST
    config.call(chart.style)
    chart
  }

  /**
   * Add pie slices using column names from the source Matrix.
   *
   * @param xValueCol the name of the column containing slice labels
   * @param yValueCol the name of the column containing slice values
   * @return this chart for method chaining
   */
  PieChart addSeries(String xValueCol, String yValueCol) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol))
  }

  /**
   * Add pie slices using Column objects.
   *
   * @param xCol the column containing slice labels
   * @param yCol the column containing slice values
   * @return this chart for method chaining
   * @throws IllegalArgumentException if xCol or yCol is null, or if they have different sizes
   */
  PieChart addSeries(Column xCol, Column yCol) {
    if (xCol == null) {
      throw new IllegalArgumentException('The xCol is null, cannot add series')
    }
    if (yCol == null) {
      throw new IllegalArgumentException('The yCol is null, cannot add series')
    }
    if (xCol.size() != yCol.size()) {
      throw new IllegalArgumentException("xCol and yCol must be of equal length but xCol has ${xCol.size()} elements whereas yCol has ${yCol.size()} elements.")
    }
    xCol.eachWithIndex { Object name, int i ->
      xchart.addSeries(ValueConverter.asString(name), yCol[i] as Number)
    }
    this
  }

}
