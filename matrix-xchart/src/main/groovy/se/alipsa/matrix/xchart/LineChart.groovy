package se.alipsa.matrix.xchart

import org.knowm.xchart.XYSeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractXYChart

/**
 * A LineChart is a chart that displays data as a series of points connected by straight lines.
 * It is useful for visualizing trends over time or continuous data.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Time': [1, 2, 3, 4, 5],
 *   'Value': [10, 20, 15, 25, 30]
 * ).types([Number]*2).matrixName("Line Chart").build()
 *
 * def lineChart = LineChart.create(matrix, 800, 600)
 *   .addSeries('Value', 'Time', 'Value')
 *
 * File file = new File('build/testLineChart.png')
 * lineChart.exportPng(file)
 * </code></pre>
 */
class LineChart extends AbstractXYChart<LineChart> {

  private LineChart(Matrix matrix, int width, int height) {
    super(matrix, width, height, XYSeries.XYSeriesRenderStyle.Line)
  }

  static LineChart create(Matrix matrix, int width, int height) {
    return new LineChart(matrix, width, height)
  }

}
