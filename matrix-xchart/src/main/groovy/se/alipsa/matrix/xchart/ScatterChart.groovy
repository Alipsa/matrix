package se.alipsa.matrix.xchart

import org.knowm.xchart.XYSeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractXYChart

class ScatterChart extends AbstractXYChart<ScatterChart> {

  private ScatterChart(Matrix matrix, Integer width = null, Integer height = null) {
    super(matrix, width, height, XYSeries.XYSeriesRenderStyle.Scatter)
    style.seriesMarkers = MatrixTheme.MARKERS
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
