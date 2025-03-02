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
}
