package se.alipsa.matrix.xchart

import org.knowm.xchart.BoxChartBuilder
import org.knowm.xchart.BoxSeries
import org.knowm.xchart.style.BoxStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

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
