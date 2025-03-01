package se.alipsa.matrix.xchart

import org.knowm.xchart.XYSeries
import se.alipsa.matrix.core.Matrix

class AreaChart extends AbstractXYChart<AreaChart> {

  private AreaChart(Matrix matrix, int width, int height) {
    super(matrix, width, height, XYSeries.XYSeriesRenderStyle.Area)
  }

  static AreaChart create(Matrix matrix, int width, int height) {
    return new AreaChart(matrix, width, height)
  }

}
