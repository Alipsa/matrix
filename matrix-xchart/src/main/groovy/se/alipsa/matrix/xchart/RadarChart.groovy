package se.alipsa.matrix.xchart

import org.knowm.xchart.RadarChartBuilder
import org.knowm.xchart.RadarSeries
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.RadarStyler
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.xchart.abstractions.AbstractChart

import java.awt.Color

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
    //println "labels are $labels"
    xchart.radiiLabels = labels as String[]
    matrix.rows().eachWithIndex { Row row, int idx ->
      def r = row - seriesNameColumn
      //println "adding series ${labels[idx]} with data ${r}"
      def s = xchart.addSeries(labels[idx], r as double[])
      makeFillTransparent(s, numSeries, transparency)
      numSeries++
    }
    this
  }


}
