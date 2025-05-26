package se.alipsa.matrix.xchart

import org.knowm.xchart.CategorySeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractCategoryChart

/**
 * A StickChart is a type of chart that displays data points as vertical lines (sticks) extending from a baseline.
 * It is useful for visualizing the distribution of data points along a category axis.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Category': ['A', 'B', 'C'],
 *   'Value': [10, 20, 15]
 * ).types([String, Number]).build()
 *
 * def stickChart = StickChart.create(matrix)
 *   .addSeries('Category', 'Value')
 * sc.getSeries("Value").marker = SeriesMarkers.CIRCLE
 * sc.style.setLegendPosition(Styler.LegendPosition.InsideNW)
 *
 * File file = new File('stickChart.png')
 * stickChart.exportPng(file)
 * </code></pre>
 */
class StickChart extends AbstractCategoryChart<StickChart> {

  private StickChart(Matrix matrix, Integer width, Integer height) {
    super(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Stick)
  }

  static create(Matrix matrix, Integer width = null, Integer height = null) {
    new StickChart(matrix, width, height)
  }
}
