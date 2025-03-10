package se.alipsa.matrix.xchart

import org.knowm.xchart.OHLCChart
import org.knowm.xchart.OHLCChartBuilder
import org.knowm.xchart.OHLCSeries
import org.knowm.xchart.style.OHLCStyler
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * Also known as a Candle Stick chart
 */
class OhlcChart extends AbstractChart<OhlcChart, OHLCChart, OHLCStyler, OHLCSeries> {

  private OhlcChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    def builder = new OHLCChartBuilder()
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


  static OhlcChart create(Matrix table, Integer width = null, Integer height = null) {
    new OhlcChart(table, width, height)
  }

  OhlcChart addSeries(String name, String xData, String open, String high, String low, String close) {
    addSeries(name, matrix[xData], matrix[open], matrix[high], matrix[low], matrix[close])
  }

  OhlcChart addSeries(String name, List<Number> xData, List<Number> open, List<Number> high, List<Number> low, List<Number> close) {
    xchart.addSeries(name, xData, open, high, low, close)
    this
  }
}
