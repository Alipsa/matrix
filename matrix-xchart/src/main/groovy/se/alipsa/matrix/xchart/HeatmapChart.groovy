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
    style.setPlotContentSize(1)
    style.setShowValue(true)
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
    int idx = 0
    for (int r = 0; r < nRows; r++) {
      def tmpRow = []
      for (int c = 0; c < nCols; c++) {
        heatData << [c, r, col[idx]].toArray(numberArray)
        tmpRow << col[idx]
        idx++
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = new MatrixBuilder().rows(tmpRows).build()
    xchart.addSeries(seriesName, 1..nCols, 1..nRows, heatData)
    this
  }

  HeatmapChart addSeries(String seriesName, Column... columns) {
    int nCols = columns.length
    int nRows = columns[0].size()
    List<Number[]> heatData = []
    final Number[] numberArray = new Number[]{}
    def tmpRows = []
    for (int r = 0; r < nRows; r++) {
      def tmpRow = []
      for (int c = 0; c < nCols; c++) {
        heatData << [c, r, columns[c][r]].toArray(numberArray)
        tmpRow << columns[c][r]
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = new MatrixBuilder().rows(tmpRows).build()
    xchart.addSeries(seriesName, 1..nCols, 1..nRows, heatData)
    this
  }
}
