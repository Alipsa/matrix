package se.alipsa.matrix.xchart

import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.HeatMapSeries
import org.knowm.xchart.style.HeatMapStyler
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.xchart.abstractions.AbstractChart

class CorrelationHeatmapChart extends AbstractChart<CorrelationHeatmapChart, HeatMapChart, HeatMapStyler, HeatMapSeries> {

  final Number[] numberArray = new Number[]{}

  private CorrelationHeatmapChart(Matrix matrix, Integer width = null, Integer height = null) {
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
    new CorrelationHeatmapChart(matrix, width, height)
  }

  CorrelationHeatmapChart addSeries(String title, List<String> columnNames) {
    if (columnNames.isEmpty()) {
      throw new IllegalArgumentException("At least one column name must be provided for the correlation heatmap series.")
    }
    Matrix data = matrix.selectColumns(columnNames)
    def size = columnNames.size()
    def corr = [size<..0, 0..<size].combinations().collect { int i, int j ->
      def xValues = ListConverter.toBigDecimals(data[j])
      def yValues = ListConverter.toBigDecimals(data[i])
      Correlation.cor(xValues, yValues).round(2)
    }
    def corrMatrix = Matrix.builder().data(X: 0..<corr.size(), Heat: corr)
        .types([Number] * 2)
        .matrixName('Heatmap')
        .build()
    //println "Correlation matrix: ${corrMatrix.content()}"
    addSeries(
        title,
        columnNames.reverse(),
        columnNames,
        corrMatrix['Heat'].collate(size)
    )
  }

  CorrelationHeatmapChart addSeries(String seriesName, List columnLabels, List rowLabels, List<List> columns) {
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
    xchart.addSeries(seriesName, rowLabels, columnLabels, heatData)
    this
  }
}
