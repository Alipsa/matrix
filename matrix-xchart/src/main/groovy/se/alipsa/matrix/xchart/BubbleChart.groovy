package se.alipsa.matrix.xchart

import groovy.transform.CompileStatic
import org.knowm.xchart.BubbleChartBuilder
import org.knowm.xchart.BubbleSeries
import org.knowm.xchart.style.BubbleStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

import java.awt.Color

/**
 * A bubble chart is a type of chart that displays three dimensions of data. Each entity with its triplet
 * (v<sub>1</sub>, v<sub>2</sub>, v<sub>3</sub>) is represented as a bubble in the chart where
 * the associated data is plotted as a disk that expresses two of the v<sub>i</sub> values through the disk's
 * xy location and the third through its size.
 * Sample usage:
 * <pre><code>
 * Matrix m2 = Matrix.builder().data(
 *   xData: [1, 2.0, 3.0, 4.0, 5, 6, 1.5, 2.6, 3.3, 4.9, 5.5, 6.3],
 *   yData: [1, 2, 3, 4, 5, 6, 10, 8.5, 4, 1, 4.7, 9],
 *   values: [37, 35, 80, 27, 29, 44, 57, 40, 50, 33, 26, 20]
 *   )
 *   .types([Number]*3)
 *   .build()
 *  def bc = BubbleChart.create(matrix)
 *    .addSeries('xData', 'yData', 'values')
 *    .addSeries('B', m2.xData, m2.yData, m2.values, 50)
 *
 * File file = new File('build/testBubbleChart.png')
 * bc.exportPng(file)
 */
@CompileStatic
class BubbleChart extends AbstractChart<BubbleChart, org.knowm.xchart.BubbleChart, BubbleStyler, BubbleSeries> {

  int numSeries = 0

  private BubbleChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    def builder = new BubbleChartBuilder()
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

  static BubbleChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BubbleChart(matrix, width, height)
  }

  BubbleChart addSeries(String xCol, String yCol,String valueCol, Integer transparency = 185) {
    addSeries(matrix.column(xCol), matrix.column(yCol),matrix.column(valueCol), transparency)
  }

  BubbleChart addSeries(String seriesName, String xCol, String yCol,String valueCol, Integer transparency = 185) {
    addSeries(seriesName, matrix.column(xCol), matrix.column(yCol),matrix.column(valueCol), transparency)
  }

  BubbleChart addSeries(Column xCol, Column yCol,Column valueCol, Integer transparency = 185) {
    addSeries(valueCol.name, xCol, yCol, valueCol, transparency)
  }

  BubbleChart addSeries(String seriesName, Column xCol, Column yCol, Column valueCol, Integer transparency = 185) {
    if (valueCol == null) {
      throw new IllegalArgumentException("The valueCol is null, cannot add series")
    }
    def s = xchart.addSeries(seriesName, xCol, yCol, valueCol)
    makeFillTransparent(s, numSeries, transparency)
    numSeries++
    this
  }
}
