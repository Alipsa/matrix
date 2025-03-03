package se.alipsa.matrix.xchart

import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.HeatMapSeries
import org.knowm.xchart.style.HeatMapStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

class HeatmapChart extends AbstractChart<HeatmapChart, HeatMapChart, HeatMapStyler, HeatMapSeries> {

  private HeatmapChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    def builder = new HeatMapChartBuilder()
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

  static create(Matrix matrix, Integer width = null, Integer height = null) {
    new HeatmapChart(matrix, width, height)
  }

  HeatmapChart addSeries(String seriesName, String redCol, String greenCol, String blueCol) {
    addSeries(seriesName, matrix[redCol], matrix[greenCol], matrix[blueCol])
  }

  HeatmapChart addSeries(String seriesName, Column red, Column green, Column blue) {
    List<Number[]> heatData = []
    int nrows = red.size()
    for (int r = 0; r < nrows; r++) {
      heatData << ([red[r], green[r], blue[r]] as Number[])
    }
    def xData = 1..nrows
    def yData = 1..3
    xchart.addSeries(seriesName, xData, yData, heatData)
    this
  }
}
