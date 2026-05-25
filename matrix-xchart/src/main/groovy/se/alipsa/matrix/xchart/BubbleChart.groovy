package se.alipsa.matrix.xchart

import org.knowm.xchart.BubbleChartBuilder
import org.knowm.xchart.BubbleSeries
import org.knowm.xchart.style.BubbleStyler

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A bubble chart is a type of chart that displays three dimensions of data. Each entity with its triplet
 * (v<sub>1</sub>, v<sub>2</sub>, v<sub>3</sub>) is represented as a bubble in the chart where
 * the associated data is plotted as a disk that expresses two of the v<sub>i</sub> values through the disk's
 * xy location and the third through its size.
 * Sample usage:
 * <pre><code>
 * Matrix m2 = Matrix.builder().data(
 *   xData: [1, 2.0, 3.0, 4.0, 5, 6, 1.5, 2.6, 3.3, 4.9, 5.5, 6.3],
 *   yData: [1, 2, 3, 4, 5, 6, 10, 8.5, 4, 1, 4.7, 9],
 *   values: [37, 35, 80, 27, 29, 44, 57, 40, 50, 33, 26, 20]
 *   )
 *   .types([Number]*3)
 *   .build()
 *  def bc = BubbleChart.create(matrix)
 *    .addSeries('xData', 'yData', 'values')
 *    .addSeries('B', m2.xData, m2.yData, m2.values, 50)
 *
 * File file = new File('build/testBubbleChart.png')
 * bc.exportPng(file)
 */
class BubbleChart extends AbstractChart<BubbleChart, org.knowm.xchart.BubbleChart, BubbleStyler, BubbleSeries> {

  private int numSeries = 0

  private BubbleChart(Matrix matrix, Integer width = null, Integer height = null) {
    def builder = new BubbleChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
  }

  /**
   * Create a new bubble chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new BubbleChart instance
   */
  static BubbleChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BubbleChart(matrix, width, height)
  }

  /**
   * Create a new bubble chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new BubbleChart instance
   */
  static BubbleChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new BubbleChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add a bubble series using column names from the source Matrix.
   * The series will be named after the value column.
   *
   * @param xCol the name of the column containing X-axis values
   * @param yCol the name of the column containing Y-axis values
   * @param valueCol the name of the column containing bubble size values
   * @param transparency the fill alpha value (0 = fully transparent, 255 = fully opaque); defaults to 185
   * @return this chart for method chaining
   */
  BubbleChart addSeries(String xCol, String yCol, String valueCol, Integer transparency = 185) {
    addSeries(matrix.column(xCol), matrix.column(yCol), matrix.column(valueCol), transparency)
  }

  /**
   * Add a named bubble series using column names from the source Matrix.
   *
   * @param seriesName the name for this series (displayed in legend)
   * @param xCol the name of the column containing X-axis values
   * @param yCol the name of the column containing Y-axis values
   * @param valueCol the name of the column containing bubble size values
   * @param transparency the fill alpha value (0 = fully transparent, 255 = fully opaque); defaults to 185
   * @return this chart for method chaining
   */
  BubbleChart addSeries(String seriesName, String xCol, String yCol, String valueCol, Integer transparency = 185) {
    addSeries(seriesName, matrix.column(xCol), matrix.column(yCol), matrix.column(valueCol), transparency)
  }

  /**
   * Add a bubble series using Column objects.
   * The series will be named after the value column.
   *
   * @param xCol the column containing X-axis values
   * @param yCol the column containing Y-axis values
   * @param valueCol the column containing bubble size values
   * @param transparency the fill alpha value (0 = fully transparent, 255 = fully opaque); defaults to 185
   * @return this chart for method chaining
   */
  BubbleChart addSeries(Column xCol, Column yCol, Column valueCol, Integer transparency = 185) {
    addSeries(valueCol.name, xCol, yCol, valueCol, transparency)
  }

  /**
   * Add a named bubble series using Column objects.
   *
   * @param seriesName the name for this series (displayed in legend)
   * @param xCol the column containing X-axis values
   * @param yCol the column containing Y-axis values
   * @param valueCol the column containing bubble size values
   * @param transparency the fill alpha value (0 = fully transparent, 255 = fully opaque); defaults to 185
   * @return this chart for method chaining
   * @throws IllegalArgumentException if valueCol is null
   */
  BubbleChart addSeries(String seriesName, Column xCol, Column yCol, Column valueCol, Integer transparency = 185) {
    if (valueCol == null) {
      throw new IllegalArgumentException('The valueCol is null, cannot add series')
    }
    def s = xchart.addSeries(seriesName, xCol, yCol, valueCol)
    makeFillTransparent(s, numSeries, transparency)
    numSeries++
    this
  }

}
