package se.alipsa.matrix.xchart

import org.knowm.xchart.XYSeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractXYChart

class LineChart extends AbstractXYChart<LineChart> {

  private LineChart(Matrix matrix, int width, int height) {
    super(matrix, width, height, XYSeries.XYSeriesRenderStyle.Line)
  }

  static LineChart create(Matrix matrix, int width, int height) {
    return new LineChart(matrix, width, height)
  }

}
