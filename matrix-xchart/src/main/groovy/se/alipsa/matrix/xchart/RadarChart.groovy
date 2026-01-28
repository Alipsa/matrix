package se.alipsa.matrix.xchart

import groovy.transform.CompileStatic

import org.knowm.xchart.RadarChartBuilder
import org.knowm.xchart.RadarSeries
import org.knowm.xchart.style.RadarStyler
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A RadarChart is a type of chart that displays multivariate data in a circular layout.
 * Each variable is represented as a spoke, and the data points are connected to form a polygon.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Category': ['A', 'B', 'C'],
 *   'Value1': [0.10, 0.20, 0.30],
 *   'Value2': [0.15, 0.25, 0.35],
 *   'Value3': [0.20, 0.30, 0.40]
 * ).types([String, Number, Number, Number]).build()
 *
 * def radarChart = RadarChart.create(matrix)
 *   .addSeries('Category')
 *
 * File file = new File('radarChart.png')
 * radarChart.exportPng(file)
 * </code></pre>
 */
@CompileStatic
class RadarChart extends AbstractChart<RadarChart, org.knowm.xchart.RadarChart, RadarStyler, RadarSeries> {

  int numSeries = 0

  private RadarChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    def builder = new RadarChartBuilder()
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

  static RadarChart create(Matrix matrix,  Integer width = null, Integer height = null) {
    new RadarChart(matrix, width, height)
  }

  RadarChart addSeries(String seriesNameColumn, Integer transparency = 150) {
    def labels = matrix.columnNames() - seriesNameColumn
    xchart.radiiLabels = labels as String[]
    matrix.rows().each { Row row ->
      def label = row[seriesNameColumn].toString()
      def r = row - seriesNameColumn
      def s = xchart.addSeries(label, r as double[])
      makeFillTransparent(s, numSeries, transparency)
      numSeries++
    }
    this
  }


}
