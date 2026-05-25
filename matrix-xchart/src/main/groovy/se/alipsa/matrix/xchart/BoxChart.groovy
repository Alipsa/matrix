package se.alipsa.matrix.xchart

import org.knowm.xchart.BoxChartBuilder
import org.knowm.xchart.BoxSeries
import org.knowm.xchart.style.BoxStyler

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A BoxChart is a chart that displays data in the form of box plots.
 * It can be used to visualize the distribution of data points across different categories.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder(). data(
 *   'aaa': [40, 30, 20, 60, 50],
 *   'bbb': [-20, -10, -30, -15, -25],
 *   'ccc': [50, -20, 10, 5, 1]
 *    )
 *   .types([Number]*3)
 *   .matrixName('Box chart')
 *   .build()
 *
 * def bc = BoxChart.create(matrix)
 *   .addSeries('aaa')
 *   .addSeries('BBB', matrix.bbb)
 *   .addSeries(matrix['ccc'])
 *
 * File file = new File('build/testBoxChart.png')
 * bc.exportPng(file)
 * </code></pre>
 */
class BoxChart extends AbstractChart<BoxChart, org.knowm.xchart.BoxChart, BoxStyler, BoxSeries> {

  private BoxChart(Matrix matrix, Integer width = null, Integer height = null) {
    def builder = new BoxChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
  }

  /**
   * Create a new box chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new BoxChart instance
   */
  static BoxChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BoxChart(matrix, width, height)
  }

  /**
   * Create a new box chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new BoxChart instance
   */
  static BoxChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new BoxChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add a box plot series using a column name from the source Matrix.
   * The series will be named after the column.
   *
   * @param valueCol the name of the numeric column to plot
   * @return this chart for method chaining
   */
  BoxChart addSeries(String valueCol) {
    addSeries(matrix.column(valueCol))
  }

  /**
   * Add a named box plot series using a column name from the source Matrix.
   *
   * @param seriesName the name for this series (displayed in legend)
   * @param valueCol the name of the numeric column to plot
   * @return this chart for method chaining
   */
  BoxChart addSeries(String seriesName, String valueCol) {
    addSeries(seriesName, matrix.column(valueCol))
  }

  /**
   * Add a box plot series using a Column object.
   * The series will be named after the column.
   *
   * @param valueCol the Column containing numeric values to plot
   * @return this chart for method chaining
   */
  BoxChart addSeries(Column valueCol) {
    addSeries(valueCol.name, valueCol)
  }

  /**
   * Add a named box plot series using a Column object.
   *
   * @param seriesName the name for this series (displayed in legend)
   * @param valueCol the Column containing numeric values to plot
   * @return this chart for method chaining
   * @throws IllegalArgumentException if valueCol is null
   */
  BoxChart addSeries(String seriesName, Column valueCol) {
    if (valueCol == null) {
      throw new IllegalArgumentException('The valueCol is null, cannot add series')
    }
    xchart.addSeries(seriesName, valueCol)
    this
  }

  /**
   * Add all numeric columns from the source Matrix as individual box plot series.
   * Each numeric column becomes a series named after the column.
   *
   * @return this chart for method chaining
   */
  BoxChart addAllSeries() {
    matrix.columnNames().each { String colName ->
      if (Number.isAssignableFrom(matrix.type(colName))) {
        addSeries(colName)
      }
    }
    this
  }

}
