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
 *   .matrixName("Box chart")
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
    super.matrix = matrix
    def builder = new BoxChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    xchart = builder.build()
    style.theme = new MatrixTheme()
    def matrixName = matrix.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      title = matrix.matrixName
    }
  }

  static BoxChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BoxChart(matrix, width, height)
  }

  BoxChart addSeries(String valueCol) {
    addSeries(matrix.column(valueCol))
  }

  BoxChart addSeries(String seriesName, String valueCol) {
    addSeries(seriesName, matrix.column(valueCol))
  }

  BoxChart addSeries(Column valueCol) {
    addSeries(valueCol.name, valueCol)
  }

  BoxChart addSeries(String seriesName, Column valueCol) {
    if (valueCol == null) {
      throw new IllegalArgumentException("The valueCol is null, cannot add series")
    }
    xchart.addSeries(seriesName, valueCol)
    this
  }
}
