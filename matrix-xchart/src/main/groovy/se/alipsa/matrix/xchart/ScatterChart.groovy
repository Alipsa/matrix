package se.alipsa.matrix.xchart

import org.knowm.xchart.XYSeries

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractXYChart

/**
 * A ScatterChart uses dots to represent values for two different numeric variables.
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
    style.seriesMarkers = MatrixTheme.MARKERS as org.knowm.xchart.style.markers.Marker[]
  }

  /**
   * Create a new scatter chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new ScatterChart instance
   */
  static ScatterChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new ScatterChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Create a new scatter chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new ScatterChart instance
   */
  static ScatterChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new ScatterChart(matrix, width, height)
  }

  /**
   * Create a new scatter chart with a title, pre-configured with a single X/Y series.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param xAxis the name of the column containing X values
   * @param yAxis the name of the column containing Y values
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new ScatterChart instance with one series added
   */
  static ScatterChart create(String title, Matrix matrix, String xAxis, String yAxis, Integer width = null, Integer height = null) {
    def chart = new ScatterChart(matrix, width, height)
    chart.title = title
    chart.addSeries(xAxis, yAxis)
    chart
  }

  /**
   * Create a new scatter chart with a title, splitting data into multiple series by a grouping column.
   * Each unique value in the series column becomes a separate scatter series.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param xAxis the name of the column containing X values
   * @param yAxis the name of the column containing Y values
   * @param seriesCol the name of the column used to group data into separate series
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new ScatterChart instance with one series per unique value in seriesCol
   */
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
