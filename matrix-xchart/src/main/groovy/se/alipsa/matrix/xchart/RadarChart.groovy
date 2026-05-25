package se.alipsa.matrix.xchart

import org.knowm.xchart.RadarChartBuilder
import org.knowm.xchart.RadarSeries
import org.knowm.xchart.style.RadarStyler

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A RadarChart is a type of chart that displays multivariate data in a circular layout.
 * Each variable is represented as a spoke, and the data points are connected to form a polygon.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Category': ['A', 'B', 'C'],
 *   'Value1': [0.10, 0.20, 0.30],
 *   'Value2': [0.15, 0.25, 0.35],
 *   'Value3': [0.20, 0.30, 0.40]
 * ).types([String, Number, Number, Number]).build()
 *
 * def radarChart = RadarChart.create(matrix)
 *   .addSeries('Category')
 *
 * File file = new File('radarChart.png')
 * radarChart.exportPng(file)
 * </code></pre>
 */
class RadarChart extends AbstractChart<RadarChart, org.knowm.xchart.RadarChart, RadarStyler, RadarSeries> {

  private int numSeries = 0

  private RadarChart(Matrix matrix, Integer width = null, Integer height = null) {
    def builder = new RadarChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
  }

  /**
   * Create a new radar chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new RadarChart instance
   */
  static RadarChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new RadarChart(matrix, width, height)
  }

  /**
   * Create a new radar chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new RadarChart instance
   */
  static RadarChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new RadarChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add all rows of the Matrix as radar series, using the specified column for series labels.
   * All other columns become radar axes (radii). Values should be normalized to [0, 1].
   *
   * @param seriesNameColumn the name of the column containing series labels (one series per row)
   * @param transparency the fill alpha value (0 = fully transparent, 255 = fully opaque); defaults to 150
   * @return this chart for method chaining
   */
  RadarChart addSeries(String seriesNameColumn, Integer transparency = 150) {
    def labels = matrix.columnNames() - seriesNameColumn
    xchart.radiiLabels = labels as String[]
    matrix.rows().each { Row row ->
      def label = row[seriesNameColumn].toString()
      def r = row - seriesNameColumn
      def s = xchart.addSeries(label, r as double[])
      makeFillTransparent(s, numSeries, transparency)
      numSeries++
    }
    this
  }

}
