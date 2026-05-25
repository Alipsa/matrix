package se.alipsa.matrix.xchart

import org.knowm.xchart.CategorySeries

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractCategoryChart

/**
 * A BarChart is a chart that displays data in the form of bars.
 * It can be used to visualize categorical data with rectangular bars.
 * The length of each bar is proportional to the value it represents.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   name: ['Per', 'Karin', 'Tage', 'Sixten', 'Ulrik'],
 *   score: [4, 5, 9, 6, 5]
 *   ).types(String, Number)
 *   .build()
 * def bc = BarChart.create(matrix, 800, 600)
 *   .addSeries(matrix['name'], matrix['score'])
 *
 * File file = new File('build/testBarChart.png')
 * bc.exportPng(file)
 * </code></pre>
 */
class BarChart extends AbstractCategoryChart<BarChart> {

  private BarChart(Matrix matrix, Integer width = null, Integer height = null, CategorySeries.CategorySeriesRenderStyle type) {
    super(matrix, width, height, type)
  }

  /**
   * Create a new bar chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new BarChart instance
   */
  static BarChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BarChart(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Bar)
  }

  /**
   * Create a new bar chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new BarChart instance
   */
  static BarChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new BarChart(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Bar)
    chart.title = title
    chart
  }

  /**
   * Create a new stacked bar chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new stacked BarChart instance
   */
  static BarChart createStacked(Matrix matrix, Integer width = null, Integer height = null) {
    def bc = new BarChart(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Bar)
    bc.style.setStacked(true)
    bc
  }

}
