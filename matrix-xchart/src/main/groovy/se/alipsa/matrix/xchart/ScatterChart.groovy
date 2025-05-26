package se.alipsa.matrix.xchart

import org.knowm.xchart.XYSeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractXYChart

/**
 * A ChatterChart uses dots to represent values for two different numeric variables.
 * The position of each dot on the horizontal and vertical axis indicates values for an individual data point.
 * Scatter charts are used to observe relationships between variables.
 * Sample usage:
 * <pre><code>
 * Matrix mtcars = Dataset.mtcars()
 * def scatterPlot = ScatterChart.create(mtcars)
 *  .addSeries('wt-mpg', 'wt', 'mpg')
 * File file = new File('scatterPlot.png')
 * scatterPlot.exportPng(file)
 * </code></pre>
 */
class ScatterChart extends AbstractXYChart<ScatterChart> {

  private ScatterChart(Matrix matrix, Integer width = null, Integer height = null) {
    super(matrix, width, height, XYSeries.XYSeriesRenderStyle.Scatter)
    style.seriesMarkers = MatrixTheme.MARKERS
  }

  static ScatterChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new ScatterChart(matrix, width, height)
    chart.title = title
    chart
  }

  static ScatterChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new ScatterChart(matrix, width, height)
  }

  static ScatterChart create(String title, Matrix matrix, String xAxis, String yAxis, Integer width = null, Integer height = null) {
    def chart = new ScatterChart(matrix, width, height)
    chart.title = title
    chart.addSeries(xAxis, yAxis)
    chart
  }

  static ScatterChart create(String title, Matrix matrix, String xAxis, String yAxis, String seriesCol, Integer width = null, Integer height = null) {
    def chart = new ScatterChart(matrix, width, height)
    chart.title = title
    def seriesVals = matrix[seriesCol].toSet()
    for (val in seriesVals) {
      def series = matrix.subset(seriesCol, val)
      chart.addSeries("$seriesCol=$val", series.column(xAxis), series.column(yAxis))
    }
    chart
  }
}
