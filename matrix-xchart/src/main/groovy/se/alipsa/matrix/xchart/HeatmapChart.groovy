package se.alipsa.matrix.xchart

import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.HeatMapSeries
import org.knowm.xchart.style.HeatMapStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A heat map (or heatmap) is a 2-dimensional data visualization technique that represents the magnitude of
 * individual values within a dataset as a color.
 * Example usage:
 * <pre><code>
 * def data = [123, 210, 89, 87, 245, 187, 100, 110, 99, 59, 110, 101]
 * Matrix matrix = Matrix.builder().data( 'c': data ).types(Number).build()
 * def hc = HeatmapChart.create(matrix, 1000, 600)
 *   .setTitle(getClass().getSimpleName())
 *   .addSeries("Basic HeatMap",'c', 4)
 *
 * File file = new File("build/testHeatmap.png")
 * hc.exportPng(file)
 * </code></pre>
 */
class HeatmapChart extends AbstractChart<HeatmapChart, HeatMapChart, HeatMapStyler, HeatMapSeries> {

  final Number[] numberArray = new Number[]{}
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
    heatMapMatrix = Matrix.builder().rows(tmpRows).build()
    xchart.addSeries(seriesName, 1..nCols, 1..nRows, heatData)
    this
  }

  HeatmapChart addSeries(String seriesName, List<Column> columns) {
    int nCols = columns.size()
    int nRows = columns[0].size()
    addSeries(seriesName, 1..nRows, 1..nCols, columns)
  }

  HeatmapChart addSeries(String seriesName, List columnLabels, List rowLabels, List<Column> columns) {
    int nCols = columns.size()
    int nRows = columns[0].size()
    List<Number[]> heatData = []

    def tmpRows = []
    for (int r = 0; r < nRows; r++) {
      def tmpRow = []
      for (int c = 0; c < nCols; c++) {
        heatData << [c, r, columns[c][r]].toArray(numberArray)
        tmpRow << columns[c][r]
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = Matrix.builder().rows(tmpRows).build()
    xchart.addSeries(seriesName, rowLabels, columnLabels, heatData)
    this
  }

  HeatmapChart addAllToSeriesBy(String columnName) {
    int byIdx = matrix.columnIndex(columnName)
    List<Number[]> heatData = []
    def tmpRows = []
    matrix.rows().eachWithIndex { row, r ->
      def tmpRow = []
      def valueRow = row - byIdx
      valueRow.eachWithIndex { Object entry, int c ->
        heatData << [c, r, entry as Number].toArray(numberArray)
        tmpRow << entry
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = Matrix.builder()
        .rows(tmpRows)
        .columnNames(matrix.columnNames() - columnName)
        .build()
    def colNames = matrix.columnNames() - columnName
    xchart.addSeries(matrix.matrixName, colNames, matrix[columnName], heatData)
    this
  }
}
