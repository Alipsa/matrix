package se.alipsa.matrix.xchart

import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.HeatMapSeries
import org.knowm.xchart.style.HeatMapStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.abstractions.AbstractChart

class HeatmapChart extends AbstractChart<HeatmapChart, HeatMapChart, HeatMapStyler, HeatMapSeries> {

  Matrix heatMapMatrix

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

  HeatmapChart addSeries(String seriesName, String colName, Integer columns = null) {
    addSeries(seriesName, matrix[colName], columns)
  }

  HeatmapChart addSeries(String seriesName, Column col, Integer columns = null) {
    int nCols = columns ?: (Math.sqrt(col.size() as int))
    int nRows = (int) (col.size() / nCols)
    List<Number[]> heatData = []
    Number[] numberArray = new Number[]{}
    def tmpRows = []
    for (int r = 0; r < nRows; r++) {
      for (int c = 0; c < nCols; c++) {
        heatData << [r, c, col[r+c]].toArray(numberArray)
        tmpRows << [r, c, col[r+c]]
      }
    }
    def xData = 1..nRows
    def yData = 1..nCols
    heatMapMatrix = new MatrixBuilder().rows(tmpRows).build()
    xchart.addSeries(seriesName, xData, yData, heatData)
    this
  }
}
